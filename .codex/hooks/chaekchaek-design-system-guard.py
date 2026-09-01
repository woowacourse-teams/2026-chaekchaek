#!/usr/bin/python3
"""Chaekchaek 디자인 토큰과 design-first UI 증거를 검사한다."""

# usage-stats: hook chaekchaek-design-system-guard
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DESIGN_FILE = Path("/Users/ujeonghyeon/Downloads/designs.pen")
STATE_VERSION = 1
HIG_TEXT_SIZES = {11.0, 12.0, 13.0, 15.0, 16.0, 17.0, 20.0, 22.0, 28.0, 34.0}
APPROVED_COLORS = {
    "#00000000", "#00000022", "#00000033", "#00000073", "#171717",
    "#1A1A1A", "#242424", "#252525", "#302C27", "#4A3520", "#4A4035",
    "#666666", "#7A7570", "#999999", "#A05A27", "#AAA39A", "#C92A24",
    "#8E918F", "#C9C3BA", "#C9C9C9", "#EEEEEE", "#F0F0EC", "#F1E9DE", "#F6F2EC",
    "#F7F2EC", "#F7F2ECCC", "#FCFAF7", "#FF6B5A", "#FF9800", "#FFB74D",
    "#FFBF66", "#FFF4DF", "#FFF4DF80", "#FFFFFF", "#FFFFFFB8",
}
APPROVED_FONTS = {"Funnel Sans", "Geist Mono", "Newsreader", "Roboto"}
HEX_COLOR = re.compile(r"#[0-9A-Fa-f]{3}(?:[0-9A-Fa-f]{3}(?:[0-9A-Fa-f]{2})?)?\b")
FONT_FAMILY = re.compile(r"fontFamily\s*:\s*['\"]([^'\"]+)['\"]")
PENCIL_FONT_SIZE = re.compile(r"['\"]?fontSize['\"]?\s*:\s*(\d+(?:\.\d+)?)")
PENCIL_OBJECT = re.compile(r"\{[^{}]*\}", re.DOTALL)
KOTLIN_FONT_SIZE = re.compile(r"\bfontSize\s*=\s*(\d+(?:\.\d+)?)\.sp\b")
PATCH_FILE = re.compile(r"^\*\*\* (?:Add|Update|Delete) File: (.+)$", re.MULTILINE)
GIT_DIFF_FILE = re.compile(r"^\+\+\+ (?!/dev/null)(?:b/)?(.+)$", re.MULTILINE)
COMPOSE_UI = re.compile(r"@Composable|\bModifier\.|\bMaterialTheme\b|\b(?:Text|Button|Checkbox|Row|Column|Box|Scaffold|Surface|Card|Image)\s*\(")
PENCIL_MUTATION = re.compile(r"\b(?:Set|SetVariables|Update|Insert|Delete|Move|Clone|Copy|Generate|Replace)\s*\(")
APPLY_PATCH_CALL = re.compile(r"\b(?:tools\.)?apply_patch\s*\(")
UI_PROMPT = re.compile(r"(?:\bui\b|화면|시안|컴포넌트|레이아웃|스타일|디자인)", re.IGNORECASE)
MUTATION_PROMPT = re.compile(r"(?:수정|변경|구현|추가|삭제|적용|만들|fix|change|implement|add|delete|update)", re.IGNORECASE)
FRONTEND_UI_SUFFIXES = {".html", ".css", ".jsx", ".tsx", ".vue", ".svelte"}
STATE_DIR = Path(tempfile.gettempdir()) / "chaekchaek-design-gates"


def record_usage() -> None:
    script = Path.home() / ".agents/skills/usage-stats/scripts/usage_stats.py"
    subprocess.run(
        ["python3", str(script), "record", "hook", "chaekchaek-design-system-guard"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False,
    )


def normalized_tool_input(payload: dict) -> dict:
    value = payload.get("tool_input") or {}
    return value if isinstance(value, dict) else {"input": str(value)}


def git_common_dir(value):
    try:
        path = Path(value).resolve()
        directory = path if path.is_dir() else path.parent
        result = subprocess.run(
            ["git", "-C", str(directory), "rev-parse", "--path-format=absolute", "--git-common-dir"],
            text=True, capture_output=True, check=False,
        )
        return Path(result.stdout.strip()).resolve() if result.returncode == 0 and result.stdout.strip() else None
    except (OSError, TypeError):
        return None


def git_worktree_root(value):
    try:
        path = Path(value).resolve()
        directory = path if path.is_dir() else path.parent
        result = subprocess.run(
            ["git", "-C", str(directory), "rev-parse", "--path-format=absolute", "--show-toplevel"],
            text=True, capture_output=True, check=False,
        )
        return Path(result.stdout.strip()).resolve() if result.returncode == 0 and result.stdout.strip() else None
    except (OSError, TypeError):
        return None


def is_project_path(value, project_root: Path = PROJECT_ROOT) -> bool:
    try:
        path = Path(value).resolve()
        root = project_root.resolve()
        if path == root or root in path.parents:
            return True
        expected_repository = git_common_dir(root)
        return expected_repository is not None and git_common_dir(path) == expected_repository
    except (OSError, TypeError):
        return False


def snippets(payload: dict) -> str:
    tool_input = normalized_tool_input(payload)
    values = [str(tool_input.get(key, "")) for key in ("command", "input", "patch")]
    values.extend(str(edit.get("replace", "")) for edit in tool_input.get("edits", []) if isinstance(edit, dict))
    return "\n".join(values)


def targets_design_file(payload: dict) -> bool:
    tool_input = normalized_tool_input(payload)
    path = str(tool_input.get("filePath", ""))
    if path:
        try:
            return Path(path).resolve() == DESIGN_FILE
        except OSError:
            return False
    return str(DESIGN_FILE) in snippets(payload)


def display_size(value: float) -> str:
    return str(int(value)) if value.is_integer() else str(value)


def find_violations(code: str, check_font_size: bool = False) -> list[str]:
    colors = sorted({value.upper() for value in HEX_COLOR.findall(code)} - APPROVED_COLORS)
    fonts = sorted({value for value in FONT_FAMILY.findall(code) if not value.startswith("$")} - APPROVED_FONTS)
    font_sizes = []
    if check_font_size:
        objects = list(PENCIL_OBJECT.finditer(code))
        for match in PENCIL_FONT_SIZE.finditer(code):
            container = next((item.group() for item in objects if item.start() <= match.start() < item.end()), "")
            approved_sizes = HIG_TEXT_SIZES | ({14.0} if set(FONT_FAMILY.findall(container)) == {"Roboto"} else set())
            size = float(match.group(1))
            if size not in approved_sizes:
                font_sizes.append(size)
        font_sizes = sorted(set(font_sizes))
    violations = []
    if colors:
        violations.append(f"SxMn5에 없는 색상: {', '.join(colors)}")
    if fonts:
        violations.append(f"SxMn5에 없는 서체: {', '.join(fonts)}")
    if font_sizes:
        values = ", ".join(display_size(value) for value in font_sizes)
        violations.append(f"HIG 의미 역할에 없는 fontSize: {values}")
    return violations


def deny(reason: str) -> None:
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": reason,
    }}, ensure_ascii=False))


def identity(payload: dict) -> dict:
    return {"session_id": str(payload.get("session_id", "")), "turn_id": str(payload.get("turn_id", ""))}


def gate_path(payload: dict) -> Path:
    active_root = git_worktree_root(payload.get("cwd") or PROJECT_ROOT) or PROJECT_ROOT
    key = f'{payload.get("session_id", "")}:{payload.get("turn_id", "")}:{active_root}'
    return STATE_DIR / hashlib.sha256(key.encode()).hexdigest()


def read_state(payload: dict):
    try:
        state = json.loads(gate_path(payload).read_text())
    except (OSError, ValueError):
        return None
    return state if valid_state(state, payload) else None


def valid_state(state: dict, payload: dict) -> bool:
    if not isinstance(state, dict) or state.get("version") != STATE_VERSION:
        return False
    if state.get("identity") != identity(payload) or state.get("design_file") != str(DESIGN_FILE):
        return False
    epoch = state.get("epoch")
    verified_epoch = state.get("verified_epoch")
    processed = state.get("processed_tool_use_ids")
    if isinstance(epoch, bool) or not isinstance(epoch, int) or epoch < 0:
        return False
    if not isinstance(processed, list) or any(not isinstance(item, str) or not item for item in processed) or len(processed) != len(set(processed)):
        return False
    if verified_epoch is None:
        return not any(key in state for key in ("node", "screenshot_tool_use_id"))
    return (
        not isinstance(verified_epoch, bool)
        and isinstance(verified_epoch, int)
        and 0 <= verified_epoch <= epoch
        and isinstance(state.get("node"), str)
        and bool(state["node"])
        and isinstance(state.get("screenshot_tool_use_id"), str)
        and bool(state["screenshot_tool_use_id"])
    )


def initial_state(payload: dict) -> dict:
    return {
        "version": STATE_VERSION,
        "identity": identity(payload),
        "design_file": str(DESIGN_FILE),
        "epoch": 0,
        "verified_epoch": None,
        "processed_tool_use_ids": [],
    }


def write_state(payload: dict, state: dict) -> None:
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    state.update({"version": STATE_VERSION, "identity": identity(payload), "design_file": str(DESIGN_FILE)})
    descriptor, temp_name = tempfile.mkstemp(prefix=".design-gate-", dir=STATE_DIR)
    try:
        with os.fdopen(descriptor, "w") as handle:
            json.dump(state, handle)
        os.replace(temp_name, gate_path(payload))
    finally:
        try:
            os.unlink(temp_name)
        except FileNotFoundError:
            pass


def screenshot_node_ids(payload: dict) -> list[str]:
    tool_input = normalized_tool_input(payload)
    node_id = str(tool_input.get("nodeId", ""))
    if node_id:
        return [node_id]
    calls = re.findall(r"TakeScreenshot\s*\(\s*\[([^]]*)]", snippets(payload))
    return [node for call in calls for node in re.findall(r"['\"]([^'\"]+)['\"]", call)]


def exact_target_node(payload: dict) -> str:
    nodes = screenshot_node_ids(payload)
    return nodes[0] if len(nodes) == 1 and nodes[0] not in {"document", "SxMn5"} else ""


def successful_post(payload: dict) -> bool:
    response = payload.get("tool_response")
    return (
        payload.get("hook_event_name") == "PostToolUse"
        and bool(response)
        and isinstance(response, dict)
        and not any(response.get(key) for key in ("isError", "is_error", "error"))
    )


def has_image_content(payload: dict) -> bool:
    response = payload.get("tool_response") or {}
    return any(
        isinstance(item, dict)
        and item.get("type") == "image"
        and bool(item.get("data") or item.get("image_url"))
        for item in response.get("content", [])
    )


def tool_use_id(payload: dict) -> str:
    return str(payload.get("tool_use_id") or payload.get("tool_call_id") or "")


def is_design_mutation(payload: dict) -> bool:
    return targets_design_file(payload) and bool(PENCIL_MUTATION.search(snippets(payload)))


def next_state(previous: dict, payload: dict) -> dict:
    event_id = tool_use_id(payload)
    if (
        not successful_post(payload)
        or not targets_design_file(payload)
        or not event_id
        or event_id in previous["processed_tool_use_ids"]
    ):
        return previous
    mutation = is_design_mutation(payload)
    node = exact_target_node(payload) if has_image_content(payload) else ""
    if not mutation and not node:
        return previous
    state = dict(previous)
    state["processed_tool_use_ids"] = previous["processed_tool_use_ids"] + [event_id]
    if mutation:
        state["epoch"] = previous["epoch"] + 1
        state["verified_epoch"] = None
        state.pop("node", None)
        state.pop("screenshot_tool_use_id", None)
    if node:
        state["verified_epoch"] = state["epoch"]
        state["node"] = node
        state["design_file"] = str(DESIGN_FILE)
        state["screenshot_tool_use_id"] = event_id
    return state


def record_successful_post(payload: dict) -> None:
    previous = read_state(payload) or initial_state(payload)
    state = next_state(previous, payload)
    if state != previous:
        write_state(payload, state)


def has_fresh_screenshot(payload: dict) -> bool:
    state = read_state(payload)
    return bool(
        state
        and state.get("verified_epoch") == state.get("epoch")
        and state.get("node")
        and state.get("screenshot_tool_use_id")
    )


def patch_sections(command: str) -> list[tuple[str, str]]:
    matches = list(PATCH_FILE.finditer(command))
    if not matches:
        matches = list(GIT_DIFF_FILE.finditer(command))
    return [
        (match.group(1).strip(), command[match.end():(matches[index + 1].start() if index + 1 < len(matches) else len(command))])
        for index, match in enumerate(matches)
    ]


def classify_patch(command: str) -> tuple[bool, bool]:
    sections = patch_sections(command)
    direct_pen_edit = any(Path(path).name == "designs.pen" for path, _ in sections)
    for path, body in sections:
        normalized = path.lower().replace("\\", "/")
        suffix = Path(normalized).suffix
        if "/frontend/" in f"/{normalized}" and suffix in FRONTEND_UI_SUFFIXES:
            return direct_pen_edit, True
        if "/res/layout/" in normalized or "/res/drawable" in normalized or "/res/mipmap" in normalized:
            return direct_pen_edit, True
        if suffix == ".kt" and ("/ui/" in normalized or "/theme/" in normalized or COMPOSE_UI.search(body)):
            return direct_pen_edit, True
    return direct_pen_edit, False


def is_ios_reachable_kotlin(path: str) -> bool:
    normalized = "/" + path.lower().replace("\\", "/").lstrip("/")
    return normalized.endswith(".kt") and ("/src/commonmain/" in normalized or "/src/iosmain/" in normalized)


def added_lines(body: str) -> str:
    return "\n".join(
        line[1:].split("//", 1)[0]
        for line in body.splitlines()
        if line.startswith("+") and not line.startswith("+++")
    )


def find_ios_kotlin_violations(command: str) -> list[str]:
    violations = []
    for path, body in patch_sections(command):
        if not is_ios_reachable_kotlin(path):
            continue
        sizes = sorted({float(value) for value in KOTLIN_FONT_SIZE.findall(added_lines(body)) if float(value) not in HIG_TEXT_SIZES})
        if sizes:
            values = ", ".join(display_size(value) for value in sizes)
            violations.append(f"{path}: {values}sp")
    return violations


def is_apply_patch_call(payload: dict) -> bool:
    tool_name = str(payload.get("tool_name", ""))
    return tool_name == "apply_patch" or (tool_name == "functions.exec" and bool(APPLY_PATCH_CALL.search(snippets(payload))))


def check_diff(base_sha: str) -> int:
    if not re.fullmatch(r"[0-9A-Fa-f]{7,64}", base_sha):
        print("ios_hig_diff: base SHA 형식이 올바르지 않습니다.", file=sys.stderr)
        return 2
    result = subprocess.run(
        [
            "git", "-C", str(PROJECT_ROOT), "diff", "--no-ext-diff", "--unified=0",
            f"{base_sha}...HEAD", "--",
            "android/shared/src/commonMain", "android/shared/src/iosMain",
        ],
        text=True, capture_output=True, check=False,
    )
    if result.returncode != 0:
        print("ios_hig_diff: git diff를 생성하지 못했습니다.", file=sys.stderr)
        return 2
    violations = find_ios_kotlin_violations(result.stdout)
    if violations:
        details = "; ".join(violations)
        print("ios_hig_diff: HIG 의미 역할에 없는 신규 fontSize가 있습니다: " + details, file=sys.stderr)
        return 1
    print("ios_hig_diff: ok")
    return 0


def self_test() -> None:
    global STATE_DIR
    assert PROJECT_ROOT == Path(__file__).resolve().parents[2]
    worktrees = subprocess.run(
        ["git", "-C", str(PROJECT_ROOT), "worktree", "list", "--porcelain"],
        text=True, capture_output=True, check=True,
    ).stdout.splitlines()
    existing_worktrees = [Path(line.removeprefix("worktree ")) for line in worktrees if line.startswith("worktree ") and Path(line.removeprefix("worktree ")).exists()]
    assert existing_worktrees and all(is_project_path(path) for path in existing_worktrees)
    assert not find_violations('Update("x",{fill:"#C92A24",fontFamily:"Funnel Sans",fontSize:11})', True)
    assert not find_violations('Update("x",{fill:"#FFFFFF",stroke:"#8E918F",fontFamily:"Roboto",fontSize:14})', True)
    assert not find_violations('Update("x", {fontFamily: "Roboto", fontSize: 14})', True)
    assert not find_violations("Update('x', {fontFamily:'Roboto', fontSize:14})", True)
    assert find_violations('Update("a",{fontFamily:"Roboto",fontSize:14}); Update("b",{fontFamily:"Funnel Sans",fontSize:14})', True) == ["HIG 의미 역할에 없는 fontSize: 14"]
    assert find_violations('Update("x",{fill:"#123456"})') == ["SxMn5에 없는 색상: #123456"]
    assert find_violations('Update("x",{"fontSize":10.5})', True) == ["HIG 의미 역할에 없는 fontSize: 10.5"]
    assert exact_target_node({"tool_input": {"input": "TakeScreenshot(['target'])"}}) == "target"
    assert not exact_target_node({"tool_input": {"input": "TakeScreenshot(['SxMn5'])"}})
    assert not exact_target_node({"tool_input": {"input": "TakeScreenshot(['a','b'])"}})
    payload = {"session_id": "session", "turn_id": "turn"}
    state = initial_state(payload)
    assert valid_state(state, payload)
    assert not valid_state({**state, "version": STATE_VERSION + 1}, payload)
    assert not valid_state({**state, "identity": {"session_id": "other", "turn_id": "turn"}}, payload)
    assert not valid_state({**state, "design_file": "/tmp/fake.pen"}, payload)
    assert not valid_state({**state, "epoch": "0"}, payload)
    assert not valid_state({**state, "processed_tool_use_ids": ["same", "same"]}, payload)

    base_post = {
        **payload,
        "hook_event_name": "PostToolUse",
        "tool_input": {"filePath": str(DESIGN_FILE)},
        "tool_response": {"content": [{"type": "text", "text": "fake success"}]},
    }
    read_only = {**base_post, "tool_use_id": "read", "tool_input": {"filePath": str(DESIGN_FILE), "input": "Get('target')"}}
    assert next_state(state, read_only) == state
    failed = {**base_post, "tool_use_id": "failed", "tool_input": {"filePath": str(DESIGN_FILE), "input": "Update('target',{})"}, "tool_response": {"isError": True}}
    assert next_state(state, failed) == state
    mutation = {**base_post, "tool_use_id": "mutation-1", "tool_input": {"filePath": str(DESIGN_FILE), "input": "Update('target',{})"}}
    mutated = next_state(state, mutation)
    assert mutated["epoch"] == 1 and mutated["verified_epoch"] is None
    assert next_state(mutated, mutation) == mutated
    fake_image = {"type": "image", "data": "fake-image-content"}
    empty_image = {**base_post, "tool_use_id": "empty-shot", "tool_input": {"filePath": str(DESIGN_FILE), "nodeId": "target"}, "tool_response": {"content": [{"type": "image"}]}}
    assert next_state(mutated, empty_image) == mutated
    screenshot = {**base_post, "tool_use_id": "shot-1", "tool_input": {"filePath": str(DESIGN_FILE), "nodeId": "target"}, "tool_response": {"content": [fake_image]}}
    verified = next_state(mutated, screenshot)
    assert verified["verified_epoch"] == verified["epoch"] == 1
    assert verified["node"] == "target" and verified["design_file"] == str(DESIGN_FILE)
    assert verified["screenshot_tool_use_id"] == "shot-1"
    invalidated = next_state(verified, {**mutation, "tool_use_id": "mutation-2"})
    assert invalidated["epoch"] == 2 and invalidated["verified_epoch"] is None
    assert "node" not in invalidated and "screenshot_tool_use_id" not in invalidated
    assert successful_post(base_post)
    assert not successful_post({**base_post, "hook_event_name": "PreToolUse"})

    original_state_dir = STATE_DIR
    with tempfile.TemporaryDirectory() as directory:
        STATE_DIR = Path(directory)
        gate_path(payload).write_text("not json")
        assert read_state(payload) is None
    STATE_DIR = original_state_dir
    kotlin_patch = """*** Begin Patch
*** Update File: android/shared/src/commonMain/kotlin/x/ui/HomeScreen.kt
@@
 fontSize = 9.sp
+fontSize = 10.5.sp
*** End Patch"""
    android_patch = kotlin_patch.replace("commonMain", "androidMain")
    safe_patch = kotlin_patch.replace("10.5.sp", "11.sp")
    arbitrary_patch = kotlin_patch.replace("10.5.sp", "14.sp")
    function_payload = {"tool_name": "functions.exec", "tool_input": {"input": f"await tools.apply_patch(`{kotlin_patch}`)"}}
    git_diff = """diff --git a/android/shared/src/commonMain/kotlin/x/ui/HomeScreen.kt b/android/shared/src/commonMain/kotlin/x/ui/HomeScreen.kt
--- a/android/shared/src/commonMain/kotlin/x/ui/HomeScreen.kt
+++ b/android/shared/src/commonMain/kotlin/x/ui/HomeScreen.kt
@@ -1,0 +2 @@
+Text("x", fontSize = 9.sp)"""
    assert find_ios_kotlin_violations(kotlin_patch) == ["android/shared/src/commonMain/kotlin/x/ui/HomeScreen.kt: 10.5sp"]
    assert find_ios_kotlin_violations(git_diff) == ["android/shared/src/commonMain/kotlin/x/ui/HomeScreen.kt: 9sp"]
    assert not find_ios_kotlin_violations(git_diff.replace("commonMain", "androidMain"))
    assert not find_ios_kotlin_violations(android_patch)
    assert not find_ios_kotlin_violations(safe_patch)
    assert find_ios_kotlin_violations(arbitrary_patch) == ["android/shared/src/commonMain/kotlin/x/ui/HomeScreen.kt: 14sp"]
    assert is_apply_patch_call(function_payload)
    assert is_design_mutation({"tool_input": {"filePath": str(DESIGN_FILE), "input": "Copy('a','b')"}})
    assert is_design_mutation({"tool_input": {"filePath": str(DESIGN_FILE), "input": "Generate('a',{})"}})
    assert is_design_mutation({"tool_input": {"filePath": str(DESIGN_FILE), "input": "SetVariables({})"}})
    assert classify_patch(kotlin_patch) == (False, True)
    assert classify_patch("*** Update File: backend/README.md\n+설명") == (False, False)
    assert is_project_path(PROJECT_ROOT / "android")
    assert is_project_path("/tmp/example-worktree/android", Path("/tmp/example-worktree"))
    assert not is_project_path(PROJECT_ROOT.parent)
    print("design_system_guard: ok")


def main() -> None:
    if sys.argv[1:] == ["--self-test"]:
        self_test()
        return
    if len(sys.argv) == 3 and sys.argv[1] == "--check-diff":
        raise SystemExit(check_diff(sys.argv[2]))
    try:
        payload = json.load(sys.stdin)
    except Exception:
        return
    if not is_project_path(payload.get("cwd") or Path.cwd()):
        return
    event = payload.get("hook_event_name")
    if event == "UserPromptSubmit":
        prompt = str(payload.get("prompt", ""))
        if UI_PROMPT.search(prompt) and MUTATION_PROMPT.search(prompt):
            record_usage()
            print(json.dumps({"hookSpecificOutput": {
                "hookEventName": "UserPromptSubmit",
                "additionalContext": (
                    "Chaekchaek UI soft nudge: designs.pen의 정확한 대상 node와 "
                    "android/docs/screen-specs.md의 component/state 계약을 확인하세요. "
                    "사용자 표시 텍스트는 HIG 11개 의미 역할의 fontSize만 사용하고, "
                    "구현 전에 변경 후 대상 스크린샷으로 검증합니다."
                ),
            }}, ensure_ascii=False))
        return
    if event == "PostToolUse":
        if targets_design_file(payload):
            record_usage()
            record_successful_post(payload)
        return
    if event != "PreToolUse":
        return
    if is_apply_patch_call(payload):
        command = snippets(payload)
        direct_pen_edit, ui_patch = classify_patch(command)
        hig_violations = find_ios_kotlin_violations(command)
        if direct_pen_edit or ui_patch or hig_violations:
            record_usage()
        if direct_pen_edit:
            deny("designs.pen 직접 편집 차단: Pencil 도구를 사용하세요.")
        elif hig_violations:
            details = "; ".join(hig_violations)
            deny("HIG Typography 변경 차단: iOS 도달 Kotlin 추가행에 의미 역할 밖의 fontSize가 있습니다: " + details)
        elif ui_patch and not has_fresh_screenshot(payload):
            deny("UI 구현 차단: designs.pen 변경 이후 SxMn5/document가 아닌 정확히 한 대상 노드의 성공한 스크린샷을 먼저 검증하세요.")
        return
    if not targets_design_file(payload):
        return
    record_usage()
    violations = find_violations(snippets(payload), check_font_size=is_design_mutation(payload))
    if violations:
        reason = "designs.pen 변경 차단: " + "; ".join(violations)
        deny(reason + ". 기존 디자인 토큰을 사용하거나 사용자 승인 후 디자인 시스템과 guard를 함께 갱신하세요.")


if __name__ == "__main__":
    main()
