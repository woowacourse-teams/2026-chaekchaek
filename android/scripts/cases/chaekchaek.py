"""책췍 전용 APK 캡처/자산 어댑터 및 과거 수동 검토 보조 도구.

픽셀 일치를 자동 판정하지 않는다. 미검토, 실패, 낡은 근거는 모두 완료를 막는다.
"""
import argparse
import hashlib
import html
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))
from design_guard import DesignJob

JOB = ROOT / "visual-contracts/chaekchaek.job.json"
CONTRACT = ROOT / "visual-contracts/screens.json"
OUT = ROOT / "build/figma-ui"
PACKAGE = "com.chamsae.chaekchaek"
ICON_SOURCES = {
    "home_active": "nav-home", "home_inactive": "nav-home-inactive",
    "feed_active": "nav-feed-active", "feed_inactive": "nav-feed",
    "discover_active": "nav-discover-active", "discover_inactive": "nav-discover",
    "library_active": "nav-library-active", "library_inactive": "nav-library",
}


def release_apk():
    return ROOT / "app/build/outputs/apk/release/app-release.apk"


def verify_icon_assets(root=ROOT):
    """Figma SVG와 Android 벡터의 경로, 획, 색, 크기 및 실제 탭 연결 비교."""
    failures = []
    android = "{http://schemas.android.com/apk/res/android}"
    callsite = (root / "shared/src/commonMain/kotlin/com/chaekchaek/app/ui/RootScreen.kt").read_text()
    for icon, source in ICON_SOURCES.items():
        svg = ET.parse(root / f"visual-contracts/assets/{source}.svg").getroot()
        vector = ET.parse(root / f"shared/src/commonMain/composeResources/drawable/ic_nav_{icon}.xml").getroot()
        source_paths = list(svg.iter("{http://www.w3.org/2000/svg}path"))
        paths = vector.findall("path")
        if (vector.get(android + "width") != svg.get("width") + "dp" or
                vector.get(android + "height") != svg.get("height") + "dp" or
                vector.get(android + "viewportWidth") != svg.get("width") or
                vector.get(android + "viewportHeight") != svg.get("height") or len(paths) != len(source_paths)):
            failures.append(f"{icon}: 크기/경로 수 불일치")
        for original, actual in zip(source_paths, paths):
            for src, dst in (("d", "pathData"), ("stroke", "strokeColor"), ("stroke-width", "strokeWidth"),
                             ("stroke-linecap", "strokeLineCap"), ("stroke-linejoin", "strokeLineJoin")):
                normalize = lambda value: re.sub(r"[\s,]+", "", value or "")
                if normalize(original.get(src)) != normalize(actual.get(android + dst)):
                    failures.append(f"{icon}: {dst} 불일치")
            if actual.get(android + "fillColor") != "#00000000":
                failures.append(f"{icon}: 원본에 없는 채움")
    for name, label in (("home", "홈"), ("feed", "피드"), ("discover", "발견"), ("library", "내 서재")):
        if f'("{label}", Res.drawable.ic_nav_{name}_active, Res.drawable.ic_nav_{name}_inactive)' not in callsite:
            failures.append(f"{label}: 원본 아이콘 연결 누락")
    if "tint = Color.Unspecified" not in callsite:
        failures.append("아이콘 원본 색상 보존 확인 필요")
    return failures


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def source_digest():
    files = []
    for directory in ("shared/src", "app/src", "gradle", "visual-contracts"):
        files.extend(p for p in (ROOT / directory).rglob("*") if p.is_file())
    files.extend(ROOT.glob("*.kts"))
    files.extend((ROOT / "app").glob("*.kts"))
    files.extend((ROOT / "shared").glob("*.kts"))
    files.extend(ROOT.glob("gradle.properties"))
    result = hashlib.sha256()
    for path in sorted(set(files)):
        result.update(str(path.relative_to(ROOT)).encode())
        result.update(bytes.fromhex(digest(path)))
    return result.hexdigest()


def read(path):
    return json.loads(Path(path).read_text())


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def png_size(path):
    data = Path(path).read_bytes()
    if len(data) < 33 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValueError(f"PNG가 아닙니다: {path}")
    return struct.unpack(">II", data[16:24])


def contract():
    value = read(CONTRACT)
    ids = [c["id"] for s in value["screens"] for c in s["checks"]]
    if len(ids) != len(set(ids)) or not ids:
        raise ValueError("검증 항목 ID가 비어 있거나 중복됩니다")
    return value


def screen_by_id(name):
    return next(s for s in contract()["screens"] if s["id"] == name)


def command(args, binary=False):
    return subprocess.check_output(args, text=not binary, timeout=60)


def adb(serial, *args, binary=False):
    return command([os.environ.get("ADB", "adb"), "-s", serial, *args], binary)


def hierarchy(serial):
    adb(serial, "shell", "uiautomator", "dump", "/sdcard/chaekchaek-visual.xml")
    data = adb(serial, "shell", "cat", "/sdcard/chaekchaek-visual.xml")
    root = ET.fromstring(data[data.index("<?xml"):])
    if not any(n.get("package") == PACKAGE for n in root.iter("node")):
        raise ValueError("검증 앱이 전면에 없습니다. 런처나 다른 앱 캡처는 거부합니다")
    return data, root


def validate_screen(root, screen):
    labels = {n.get(key, "") for n in root.iter("node") for key in ("text", "content-desc")}
    for anchor in screen["anchors"]:
        if not any(anchor in label for label in labels):
            raise ValueError(f"화면/로딩 확인 실패: {screen['id']}에서 {anchor!r}를 찾지 못했습니다")
    tab = {"home": "홈", "feed": "피드", "discover": "발견", "library": "내 서재"}.get(screen["id"])
    if tab:
        selected_labels = {child.get("text") for node in root.iter("node")
                           if node.get("selected") == "true" for child in node.iter("node")}
        if tab not in selected_labels:
            raise ValueError(f"요청한 탭이 선택되지 않았습니다: {tab}")


def validate_state(root, screen, state):
    labels = [n.get(k, "") for n in root.iter("node") for k in ("text", "content-desc")]
    required = {
        ("home", "reading"): "이어서 읽기",
        ("home", "empty"): "지금 읽고 있는 책",
        ("library", "empty"): "아직 서재가 비어 있어요",
        ("review-menu", "owned"): "삭제",
    }.get((screen, state))
    if required and not any(required in label for label in labels):
        raise ValueError(f"요청한 상태를 찾지 못했습니다: {screen}/{state}")
    if screen == "library" and state in ("populated", "scrolled") and any("서재가 비어" in label for label in labels):
        raise ValueError("빈 서재를 책이 있는 서재로 캡처할 수 없습니다")


def capture(args):
    screen = screen_by_id(args.screen)
    if args.state not in screen["states"]:
        raise ValueError(f"허용된 상태: {screen['states']}")
    receipt = read(OUT / "build.json")
    if receipt["source"] != source_digest():
        raise ValueError("빌드 이후 코드 변경: build 명령을 다시 실행하세요")
    job = DesignJob(args.job)
    if receipt.get("guard_context") != job.context_hash() or receipt.get("guard_implementation") != job.implementation_hash():
        raise ValueError("공통 원본/구현 영수증 불일치: 원본을 확인하고 다시 빌드하세요")
    remote = adb(args.serial, "shell", "pm", "path", PACKAGE).strip()
    if not remote.startswith("package:") or "\n" in remote:
        raise ValueError("검증할 단일 APK 경로를 확인할 수 없습니다")
    with tempfile.TemporaryDirectory(prefix="chaekchaek-apk-") as temporary:
        installed = Path(temporary) / "installed.apk"
        adb(args.serial, "pull", remote.removeprefix("package:"), str(installed))
        if digest(installed) != receipt["apk"]:
            raise ValueError("설치된 APK가 검증 빌드와 다릅니다")
    xml, tree = hierarchy(args.serial)
    validate_screen(tree, screen)
    validate_state(tree, args.screen, args.state)
    density_text = adb(args.serial, "shell", "wm", "density")
    density = int(re.findall(r"(?:Physical|Override) density: (\d+)", density_text)[-1])
    key = f"{args.screen}-{args.state}-{args.width}"
    path = OUT / f"{key}.png"
    path.write_bytes(adb(args.serial, "exec-out", "screencap", "-p", binary=True))
    width, height = png_size(path)
    if abs(width * 160 / density - args.width) > 1:
        path.unlink()
        raise ValueError("실제 화면 dp 너비가 요청한 너비와 다릅니다")
    # 화면 이동 도중의 캡처를 통과시키지 않는다.
    _, after = hierarchy(args.serial)
    validate_screen(after, screen)
    validate_state(after, args.screen, args.state)
    (OUT / f"{key}.xml").write_text(xml)
    write(OUT / f"{key}.json", {
        "screen": args.screen, "state": args.state, "width": args.width,
        "pixels": [width, height], "density": density, "source": receipt["source"],
        "apk": receipt["apk"], "image": path.name, "sha256": digest(path),
    })
    if key in {scenario["id"] for scenario in job.spec["scenarios"]}:
        publish_guard_receipt(job, key, receipt)
    print(f"캡처 완료: {path}. 상태와 각 시각 항목은 별도 검토가 필요합니다")


def publish_guard_receipt(job, key, build_receipt):
    artifact = release_apk()
    if digest(artifact) != build_receipt["apk"]:
        raise ValueError("로컬 APK가 설치 검증한 산출물과 다릅니다")
    paths = {"image": OUT / f"{key}.png", "artifact": artifact, "observation": OUT / f"{key}.xml"}
    roles = {role: os.path.relpath(path, job.path.parent) for role, path in paths.items()}
    files = {os.path.relpath(path, job.path.parent): digest(path)
             for path in [*paths.values(), OUT / f"{key}.json", OUT / "build.json"]}
    write(ROOT / "build/design-guard" / f"{key}.receipt.json", {
        "context": build_receipt["guard_context"], "implementation": build_receipt["guard_implementation"],
        "scenario": key, "roles": roles, "files": files,
    })


def select_tab(args):
    label = {"home": "홈", "feed": "피드", "discover": "발견", "library": "내 서재"}[args.screen]
    _, tree = hierarchy(args.serial)
    candidates = [n for n in tree.iter("node") if n.get("text") == label]
    if not candidates:
        raise ValueError(f"내비게이션 라벨 없음: {label}")
    # 제목과 내비 라벨이 같으면 아래쪽 내비 라벨을 선택한다.
    bounds = [list(map(int, re.findall(r"\d+", n.get("bounds", "")))) for n in candidates]
    x1, y1, x2, y2 = max((b for b in bounds if len(b) == 4), key=lambda b: b[3])
    adb(args.serial, "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        _, tree = hierarchy(args.serial)
        try:
            validate_screen(tree, screen_by_id(args.screen))
            print(f"선택 확인: {label}")
            return
        except ValueError:
            time.sleep(1)
    raise ValueError(f"탭 진입 확인 시간 초과: {label}")


def build(args):
    before = source_digest()
    job = DesignJob(args.job)
    context, implementation = job.context_hash(), job.implementation_hash()
    subprocess.run([str(ROOT / "gradlew"), ":shared:testAndroidHostTest", ":app:assembleRelease"],
                   cwd=ROOT, check=True)
    if source_digest() != before or job.context_hash() != context or job.implementation_hash() != implementation:
        raise ValueError("빌드 중 코드가 변경됐습니다")
    apk = release_apk()
    write(OUT / "build.json", {"source": before, "apk": digest(apk),
                               "guard_context": context, "guard_implementation": implementation})
    print(adb(args.serial, "install", "-r", str(apk)))
    print(adb(args.serial, "shell", "am", "start", "-W", "-n",
              PACKAGE + "/com.chamsae.chaekchaek.MainActivity"))


def evaluate(spec, evidence, current_source, folder):
    """검토값을 신뢰하기 전에 원본/캡처/코드의 동일성과 전체 범위를 검사한다."""
    rows = []
    for screen in spec["screens"]:
        reference = ROOT / screen["reference"]
        reference_hash = digest(reference) if reference.is_file() else None
        for check in screen["checks"]:
            for width in spec["widths"]:
                key = f"{check['id']}@{width}"
                item = evidence.get(key, {})
                status, reason = "pending", "시각 검토 없음"
                if item:
                    status, reason = item.get("status", "pending"), item.get("note", "")
                    if status not in ("pass", "fail") or not reason.strip():
                        status, reason = "pending", "명시적 판정/관찰 근거 누락"
                    elif item.get("source") != current_source:
                        status, reason = "stale", "소스 변경 후 재검토 필요"
                    elif not reference_hash or item.get("reference") != reference_hash:
                        status, reason = "stale", "Figma 원본 누락 또는 변경"
                    else:
                        captures = item.get("captures", [])
                        states = set()
                        if not captures:
                            status, reason = "pending", "캡처 근거 없음"
                        for capture in captures:
                            try:
                                metadata = folder / capture["metadata"]
                                if digest(metadata) != capture["metadata_sha256"]:
                                    raise ValueError("캡처 메타데이터 변경")
                                meta = read(metadata)
                                image = folder / meta["image"]
                                png_size(image)
                                valid = (meta["source"] == current_source and
                                         meta["screen"] == screen["id"] and meta["width"] == width and
                                         meta["sha256"] == digest(image) == capture["sha256"])
                                if not valid:
                                    raise ValueError("캡처 변경/다른 화면/다른 너비/낡은 빌드")
                                states.add(meta["state"])
                            except (OSError, KeyError, ValueError) as error:
                                status, reason = "stale", str(error)
                        if status == "pass" and not set(check["states"]).issubset(states):
                            status, reason = "pending", "필수 상태 캡처 누락: " + ", ".join(set(check["states"]) - states)
                rows.append({"key": key, "screen": screen["id"], "expected": check["expected"],
                             "status": status, "reason": reason})
    return rows


def review(args):
    spec = contract()
    screen, check = next((s, c) for s in spec["screens"] for c in s["checks"] if c["id"] == args.check)
    captures = []
    for state in args.states:
        path = OUT / f"{screen['id']}-{state}-{args.width}.json"
        meta = read(path)
        captures.append({"metadata": path.name, "metadata_sha256": digest(path), "sha256": meta["sha256"]})
    path = OUT / "reviews.json"
    evidence = read(path) if path.exists() else {}
    evidence[f"{args.check}@{args.width}"] = {
        "status": args.status, "note": args.note, "reviewer": args.reviewer,
        "source": source_digest(), "reference": digest(ROOT / screen["reference"]), "captures": captures,
    }
    write(path, evidence)


def report(args):
    spec = contract()
    path = OUT / "reviews.json"
    current_source = source_digest()
    rows = evaluate(spec, read(path) if path.exists() else {}, current_source, OUT)
    write(OUT / "report.json", rows)
    escape = lambda text: html.escape(str(text))
    parts = ['<!doctype html><meta charset="utf-8"><title>Figma UI 검증</title>',
             '<style>body{font:16px system-ui;margin:24px}td,th{padding:10px;text-align:left;border-bottom:1px solid #ddd}img{width:390px;vertical-align:top}figure{display:inline-block;vertical-align:top;margin:8px} .fail,.stale{color:#a20}.pending{color:#765}</style>',
             '<h1>Figma UI 검증</h1><p>그림자와 아이콘은 항목별 육안 판정이 필요합니다. pending, stale, fail은 모두 완료를 막습니다.</p>']
    for screen in spec["screens"]:
        parts.append(f'<h2>{escape(screen["id"])}</h2><a href="https://www.figma.com/design/{spec["file_key"]}/?node-id={screen["node"]}">Figma 원본</a><div>')
        reference = ROOT / screen["reference"]
        parts.append(f'<img alt="Figma 원본" src="{escape(os.path.relpath(reference, OUT))}">')
        for capture in sorted(OUT.glob(f'{screen["id"]}-*.png')):
            metadata = capture.with_suffix(".json")
            meta = read(metadata) if metadata.exists() else {}
            current = meta.get("source") == current_source and meta.get("sha256") == digest(capture)
            label = capture.stem + (" · 최신" if current else " · STALE 재캡처 필요")
            display_width = int(meta.get("width", 390))
            parts.append(f'<figure><figcaption>{escape(label)}</figcaption><img style="width:{display_width}px" alt="{escape(capture.stem)}" src="{escape(capture.name)}"></figure>')
        parts.append('</div><table><tr><th>항목</th><th>기대 결과</th><th>판정</th><th>관찰</th></tr>')
        for row in rows:
            if row["screen"] == screen["id"]:
                parts.append(f'<tr class="{row["status"]}">' + ''.join(f'<td>{escape(row[k])}</td>' for k in ("key", "expected", "status", "reason")) + '</tr>')
        parts.append('</table>')
    (OUT / "report.html").write_text('\n'.join(parts))
    totals = {status: sum(r["status"] == status for r in rows) for status in ("pass", "fail", "pending", "stale")}
    icon_failures = verify_icon_assets()
    print(json.dumps(totals), OUT / "report.html")
    print("내비 원본 자산 검사:", icon_failures or "8개 아이콘 및 4탭 연결 일치")
    return 0 if not icon_failures and all(r["status"] == "pass" for r in rows) else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    b = sub.add_parser("build", help="테스트, APK 빌드, 데이터 유지 설치, 앱 실행")
    b.add_argument("--serial", default="emulator-5554")
    b.add_argument("--job", type=Path, default=JOB)
    c = sub.add_parser("capture", help="현재 화면의 APK와 너비를 확인하고 캡처")
    c.add_argument("screen")
    c.add_argument("state")
    c.add_argument("--width", type=int, choices=(320, 390), required=True)
    c.add_argument("--serial", default="emulator-5554")
    c.add_argument("--job", type=Path, default=JOB)
    t = sub.add_parser("tab", help="실제 내비 라벨로 탭을 전환하고 선택 상태 확인")
    t.add_argument("screen", choices=("home", "feed", "discover", "library"))
    t.add_argument("--serial", default="emulator-5554")
    r = sub.add_parser("review", help="원본과 캡처를 비교한 항목 하나의 관찰 기록")
    r.add_argument("check")
    r.add_argument("--width", type=int, choices=(320, 390), required=True)
    r.add_argument("--states", nargs="+", required=True)
    r.add_argument("--status", choices=("pass", "fail"), required=True)
    r.add_argument("--note", required=True)
    r.add_argument("--reviewer", required=True)
    sub.add_parser("verify", help="누락/실패/낡은 근거가 있으면 exit 1")
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    try:
        if args.command == "verify":
            return report(args)
        {"build": build, "capture": capture, "review": review, "tab": select_tab}[args.command](args)
        return 0
    except (OSError, ValueError, KeyError, StopIteration, subprocess.SubprocessError) as error:
        print(f"검증 실패: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
