#!/usr/bin/env python3
import hashlib
import json
import re
import sys
import tempfile
from pathlib import Path

APPROVED_COLORS = {
    "#00000000", "#00000022", "#00000033", "#00000073", "#1A1A1A",
    "#666666", "#999999", "#A05A27", "#C92A24", "#C9C9C9", "#EEEEEE",
    "#F1E9DE", "#F6F2EC", "#F7F2EC", "#F7F2ECCC", "#FCFAF7", "#FF9800",
    "#FFF4DF", "#FFF4DF80", "#FFFFFF", "#FFFFFFB8",
}
APPROVED_FONTS = {"Funnel Sans", "Geist Mono", "Newsreader"}
HEX_COLOR = re.compile(r"#[0-9A-Fa-f]{3}(?:[0-9A-Fa-f]{3}(?:[0-9A-Fa-f]{2})?)?\b")
FONT_FAMILY = re.compile(r"fontFamily\s*:\s*['\"]([^'\"]+)['\"]")
PATCH_FILE = re.compile(r"^\*\*\* (?:Add|Update|Delete) File: (.+)$", re.MULTILINE)
COMPOSE_UI = re.compile(
    r"@Composable|\bModifier\.|\bMaterialTheme\b|\b(?:Text|Button|Checkbox|Row|Column|Box|Scaffold|Surface|Card|Image)\s*\("
)
FRONTEND_UI_SUFFIXES = {".html", ".css", ".jsx", ".tsx", ".vue", ".svelte"}
STATE_DIR = Path(tempfile.gettempdir()) / "chaekchaek-design-gates"


def find_violations(code: str) -> list[str]:
    colors = sorted({value.upper() for value in HEX_COLOR.findall(code)} - APPROVED_COLORS)
    fonts = sorted({value for value in FONT_FAMILY.findall(code) if not value.startswith("$")} - APPROVED_FONTS)
    violations = []
    if colors:
        violations.append(f"SxMn5에 없는 색상: {', '.join(colors)}")
    if fonts:
        violations.append(f"SxMn5에 없는 서체: {', '.join(fonts)}")
    return violations


def deny(reason: str) -> None:
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }, ensure_ascii=False))


def gate_path(payload: dict) -> Path:
    identity = f'{payload.get("session_id", "")}:{payload.get("turn_id", "")}'
    return STATE_DIR / hashlib.sha256(identity.encode()).hexdigest()


def screenshot_node_ids(payload: dict) -> list[str]:
    tool_input = payload.get("tool_input") or {}
    node_id = str(tool_input.get("nodeId", ""))
    if node_id:
        return [node_id]
    snippets = [str(tool_input.get("input", ""))]
    snippets.extend(str(edit.get("replace", "")) for edit in tool_input.get("edits", []))
    calls = re.findall(r"TakeScreenshot\s*\(\s*\[([^]]*)]", "\n".join(snippets))
    return [node_id for call in calls for node_id in re.findall(r"['\"]([^'\"]+)['\"]", call)]


def record_target_screenshot(payload: dict) -> None:
    tool_input = payload.get("tool_input") or {}
    if Path(str(tool_input.get("filePath", ""))).name != "designs.pen":
        return
    node_id = next((value for value in screenshot_node_ids(payload) if value not in {"document", "SxMn5"}), "")
    if not node_id:
        return
    response = payload.get("tool_response") or {}
    if isinstance(response, dict) and response.get("isError"):
        return
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    gate_path(payload).write_text(node_id)


def classify_patch(command: str) -> tuple[bool, bool]:
    paths = [path.strip() for path in PATCH_FILE.findall(command)]
    direct_pen_edit = any(Path(path).name == "designs.pen" for path in paths)
    for path in paths:
        normalized = path.lower().replace("\\", "/")
        suffix = Path(normalized).suffix
        if "/frontend/" in f"/{normalized}" and suffix in FRONTEND_UI_SUFFIXES:
            return direct_pen_edit, True
        if "/res/layout/" in normalized or "/res/drawable" in normalized or "/res/mipmap" in normalized:
            return direct_pen_edit, True
        if suffix == ".kt" and ("/ui/" in normalized or "/theme/" in normalized or COMPOSE_UI.search(command)):
            return direct_pen_edit, True
    return direct_pen_edit, False


def self_test() -> None:
    assert not find_violations('Update("x",{fill:"#C92A24",fontFamily:"Funnel Sans"})')
    assert find_violations('Update("x",{fill:"#123456"})') == ["SxMn5에 없는 색상: #123456"]
    assert find_violations('Update("x",{fontFamily:"Comic Sans MS"})') == ["SxMn5에 없는 서체: Comic Sans MS"]
    assert classify_patch("*** Update File: android/app/src/main/java/x/ui/HomeScreen.kt\n+Text(\"홈\")") == (False, True)
    assert classify_patch("*** Update File: backend/README.md\n+설명") == (False, False)
    assert classify_patch("*** Update File: designs.pen\n+raw") == (True, False)
    assert screenshot_node_ids({"tool_input": {"input": "TakeScreenshot(['target'])"}}) == ["target"]
    print("design_system_guard: ok")


def main() -> None:
    if "--self-test" in sys.argv:
        self_test()
        return

    payload = json.load(sys.stdin)
    event = payload.get("hook_event_name")
    tool_name = payload.get("tool_name")
    if event == "PostToolUse" and tool_name in {"mcp__pencil__get_screenshot", "mcp__pencil__execute"}:
        record_target_screenshot(payload)
        if tool_name == "mcp__pencil__get_screenshot":
            return

    tool_input = payload.get("tool_input") or {}
    if tool_name == "apply_patch":
        direct_pen_edit, ui_patch = classify_patch(str(tool_input.get("command", "")))
        if direct_pen_edit:
            deny("designs.pen 직접 편집 차단: Pencil 도구를 사용하세요.")
        elif ui_patch and not gate_path(payload).exists():
            deny("UI 구현 차단: 이 작업 턴에서 designs.pen의 대상 시안을 먼저 만들거나 확인하고, SxMn5/document가 아닌 대상 노드 스크린샷을 검증하세요.")
        return

    if tool_name != "mcp__pencil__execute":
        return
    if Path(str(tool_input.get("filePath", ""))).name != "designs.pen":
        return

    snippets = [str(tool_input.get("input", ""))]
    snippets.extend(str(edit.get("replace", "")) for edit in tool_input.get("edits", []))
    violations = find_violations("\n".join(snippets))
    if violations:
        deny("designs.pen 변경 차단: " + "; ".join(violations) + ". 기존 디자인 토큰을 사용하거나 사용자 승인 후 디자인 시스템과 guard를 함께 갱신하세요.")


if __name__ == "__main__":
    main()
