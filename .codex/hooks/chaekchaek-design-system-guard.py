#!/usr/bin/env python3
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


PROJECT_ROOT = Path("/Users/ujeonghyeon/Desktop/dev/myDev/2026-chaekchaek")
DESIGN_FILE = Path("/Users/ujeonghyeon/Downloads/designs.pen")
STATE_VERSION = 1
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
COMPOSE_UI = re.compile(r"@Composable|\bModifier\.|\bMaterialTheme\b|\b(?:Text|Button|Checkbox|Row|Column|Box|Scaffold|Surface|Card|Image)\s*\(")
PENCIL_MUTATION = re.compile(r"\b(?:Update|Set|Insert|Delete|Move|Clone|Replace)\s*\(")
UI_PROMPT = re.compile(r"(?:\bui\b|화면|시안|컴포넌트|레이아웃|스타일|디자인)", re.IGNORECASE)
MUTATION_PROMPT = re.compile(r"(?:수정|변경|구현|추가|삭제|적용|만들|fix|change|implement|add|delete|update)", re.IGNORECASE)
FRONTEND_UI_SUFFIXES = {".html", ".css", ".jsx", ".tsx", ".vue", ".svelte"}
STATE_DIR = Path(tempfile.gettempdir()) / "chaekchaek-design-gates"


def record_usage() -> None:
    subprocess.run(
        ["python3", str(Path.home() / ".agents/skills/usage-stats/scripts/usage_stats.py"), "record", "hook", "chaekchaek-design-system-guard"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False,
    )


def is_project_path(value) -> bool:
    try:
        path = Path(value).resolve()
        return path == PROJECT_ROOT or PROJECT_ROOT in path.parents
    except (OSError, TypeError):
        return False


def snippets(payload: dict) -> str:
    tool_input = payload.get("tool_input") or {}
    values = [str(tool_input.get("input", ""))]
    values.extend(str(edit.get("replace", "")) for edit in tool_input.get("edits", []) if isinstance(edit, dict))
    return "\n".join(values)


def targets_design_file(payload: dict) -> bool:
    tool_input = payload.get("tool_input") or {}
    path = str(tool_input.get("filePath", ""))
    if path:
        try:
            return Path(path).resolve() == DESIGN_FILE
        except OSError:
            return False
    return str(DESIGN_FILE) in snippets(payload)


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
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": reason,
    }}, ensure_ascii=False))


def identity(payload: dict) -> dict:
    return {"session_id": str(payload.get("session_id", "")), "turn_id": str(payload.get("turn_id", ""))}


def gate_path(payload: dict) -> Path:
    key = f'{payload.get("session_id", "")}:{payload.get("turn_id", "")}'
    return STATE_DIR / hashlib.sha256(key.encode()).hexdigest()


def read_state(payload: dict):
    try:
        state = json.loads(gate_path(payload).read_text())
    except (OSError, ValueError):
        return None
    if not valid_state(state, payload):
        return None
    return state


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
    tool_input = payload.get("tool_input") or {}
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
    global STATE_DIR
    assert not find_violations('Update("x",{fill:"#C92A24",fontFamily:"Funnel Sans"})')
    assert find_violations('Update("x",{fill:"#123456"})') == ["SxMn5에 없는 색상: #123456"]
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
    mutation_2 = {**mutation, "tool_use_id": "mutation-2"}
    invalidated = next_state(verified, mutation_2)
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
    assert classify_patch("*** Update File: android/app/src/main/java/x/ui/HomeScreen.kt\n+Text(\"홈\")") == (False, True)
    assert classify_patch("*** Update File: backend/README.md\n+설명") == (False, False)
    assert is_project_path(PROJECT_ROOT / "android")
    assert not is_project_path(PROJECT_ROOT.parent)
    print("design_system_guard: ok")


def main() -> None:
    if sys.argv[1:] == ["--self-test"]:
        self_test()
        return
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
                    "의미 일치는 자동 판정하지 않으며 구현 전에 변경 후 대상 스크린샷으로 검증합니다."
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
    tool_name = payload.get("tool_name")
    tool_input = payload.get("tool_input") or {}
    if tool_name == "apply_patch":
        direct_pen_edit, ui_patch = classify_patch(str(tool_input.get("command", "")))
        if direct_pen_edit or ui_patch:
            record_usage()
        if direct_pen_edit:
            deny("designs.pen 직접 편집 차단: Pencil 도구를 사용하세요.")
        elif ui_patch and not has_fresh_screenshot(payload):
            deny("UI 구현 차단: designs.pen 변경 이후 SxMn5/document가 아닌 정확히 한 대상 노드의 성공한 스크린샷을 먼저 검증하세요.")
        return
    if not targets_design_file(payload):
        return
    record_usage()
    violations = find_violations(snippets(payload))
    if violations:
        deny("designs.pen 변경 차단: " + "; ".join(violations) + ". 기존 디자인 토큰을 사용하거나 사용자 승인 후 디자인 시스템과 guard를 함께 갱신하세요.")


if __name__ == "__main__":
    main()
