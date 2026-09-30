"""작업별 원본 전체 목록을 구현 파일과 실제 렌더링 검토에 연결한다.

제품, 플랫폼, Figma 파일 또는 화면 이름을 알지 못한다. 픽셀 일치 자동 판독기가
아니며, 원본 수집 누락과 근거 없는 완료를 차단하는 검토 커버리지 게이트다.

입력 계약:
- job: schema=1, id, implementation_root, implementation_inputs(glob 목록),
  sources(id/identity/path/roots), scenarios(id/source/root/state/viewport/reference),
  reviews, output, unresolved(id/reason). 모든 경로는 job 파일 기준이다.
- source: schema=1, source(identity), roots, discovered(독립 수집 노드 ID 목록),
  records(id/parent/children/declaredProperties/properties/unavailable).
  수집기는 실패를 unavailable에 보존한다. hidden 노드와 빈 속성도 생략하지 않는다.
- receipt: context, implementation(context 명령 결과), scenario, roles,
  files(작업 파일 기준 경로 -> SHA256). roles는 image/artifact/observation 각각 다른 파일.
  캡처 어댑터가 빌드 당시 구현 해시, 실행 산출물 일치, 실제 화면/상태/뷰포트를
  확인한 뒤 생성한다. 현재 파일 해시를 과거 캡처에 덧붙여 소급 승인하지 않는다.
- review 명령은 사람이 실제로 대조한 항목 키를 명시적으로 받는다. 전체 pass,
  속성 자동 제외 또는 수집 오류 무시 옵션은 제공하지 않는다.

신뢰 경계: 원본 수집기와 캡처 어댑터 자체를 위조하는 공격을 막는 서명 시스템이
아니다. 제공하지 않은 화면, 외부 변수 정의, 이미지 바이트, 인터랙션 상태의 존재를
추측할 수 없다. 원본 범위 확정과 실제 시각 비교는 반드시 별도로 수행한다.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import sys


class IncompleteSource(ValueError):
    """원본을 보충해야 검토 범위를 확정할 수 있다."""


def read(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def fingerprint(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, ensure_ascii=False,
                                    separators=(",", ":")).encode()).hexdigest()


def file_hash(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def unique(values, description):
    if not values or len(values) != len(set(values)):
        raise IncompleteSource(f"{description}: 비었거나 중복됨")


def validate_snapshot(snapshot, expected_roots):
    """수집기가 발견한 목록과 전달받은 트리/속성을 독립적으로 대조한다."""
    if snapshot.get("schema") != 1:
        raise IncompleteSource("지원하지 않는 원본 스키마")
    unique(expected_roots, "루트")
    if snapshot.get("roots") != expected_roots:
        raise IncompleteSource("요청한 루트와 원본이 다름")
    records = snapshot["records"]
    ids = [record["id"] for record in records]
    unique(ids, "원본 노드")
    unique(snapshot["discovered"], "발견 노드")
    if set(ids) != set(snapshot["discovered"]):
        raise IncompleteSource("발견 노드와 전달된 노드 불일치: 응답 잘림 또는 누락")
    nodes = {record["id"]: record for record in records}
    visited = set()

    def visit(node_id, parent):
        if node_id not in nodes or node_id in visited:
            raise IncompleteSource("트리의 누락, 중복 참조 또는 순환")
        visited.add(node_id)
        node = nodes[node_id]
        if node["parent"] != parent:
            raise IncompleteSource("부모 관계 불일치")
        declared = node["declaredProperties"]
        unique(declared, f"{node_id} 선언 속성")
        if any(name.startswith("@") for name in declared):
            raise IncompleteSource("내부 구조 검사와 충돌하는 속성 이름")
        available, unavailable = set(node["properties"]), set(node["unavailable"])
        if available & unavailable or set(declared) != available | unavailable:
            raise IncompleteSource(f"{node_id}: 선언한 속성이 응답에서 빠짐")
        for child in node["children"]:
            visit(child, node_id)

    for root in expected_roots:
        visit(root, None)
    if visited != set(ids):
        raise IncompleteSource("어느 루트에도 속하지 않는 노드")
    return nodes


def unavailable_value(value):
    if isinstance(value, dict):
        return "$unavailable" in value or "$mixed" in value or any(unavailable_value(v) for v in value.values())
    return isinstance(value, list) and any(unavailable_value(v) for v in value)


def inventory(snapshot, roots):
    nodes = validate_snapshot(snapshot, roots)
    result = {}

    def visit(node_id, root):
        node = nodes[node_id]
        properties = {"@structure": {"parent": node["parent"], "children": node["children"]},
                      **node["properties"]}
        for name, value in node["unavailable"].items():
            properties[name] = {"$unavailable": value}
        for name, expected in properties.items():
            key = json.dumps([node_id, name], separators=(",", ":"))
            result[key] = {"node": node_id, "property": name, "root": root,
                           "expected": expected, "fingerprint": fingerprint(expected),
                           "source_available": not unavailable_value(expected)}
        for child in node["children"]:
            visit(child, root)

    for root in roots:
        visit(root, root)
    return result


class DesignJob:
    def __init__(self, path):
        self.path = Path(path).resolve()
        self.spec = read(self.path)
        if self.spec.get("schema") != 1:
            raise ValueError("지원하지 않는 작업 스키마")
        self.root = self.resolve(self.spec["implementation_root"])
        unique([s["id"] for s in self.spec["sources"]], "원본 설정")
        unique([s["id"] for s in self.spec["scenarios"]], "시나리오")
        self.items = {}
        self.snapshots = {}
        for source in self.spec["sources"]:
            snapshot = read(self.resolve(source["path"]))
            if snapshot["source"] != source["identity"]:
                raise IncompleteSource("다른 디자인 원본")
            self.snapshots[source["id"]] = snapshot
            self.items[source["id"]] = inventory(snapshot, source["roots"])
        for scenario in self.spec["scenarios"]:
            if not scenario.get("state") or not scenario.get("viewport"):
                raise ValueError("시나리오 상태와 뷰포트 필요")
            if scenario["source"] not in self.items or scenario["root"] not in self.snapshots[scenario["source"]]["roots"]:
                raise IncompleteSource("시나리오의 원본 루트 없음")
        covered = {(s["source"], s["root"]) for s in self.spec["scenarios"]}
        expected = {(s["id"], root) for s in self.spec["sources"] for root in s["roots"]}
        if covered != expected:
            raise IncompleteSource("검증 시나리오가 없는 원본 루트")

    def resolve(self, path):
        return (self.path.parent / path).resolve()

    def implementation_files(self):
        files = set()
        for pattern in self.spec["implementation_inputs"]:
            matches = {p for p in self.root.glob(pattern) if p.is_file()}
            if not matches:
                raise ValueError(f"구현 입력이 비어 있음: {pattern}")
            files.update(matches)
        if not files:
            raise ValueError("구현 입력 없음")
        return files

    def implementation_hash(self):
        return fingerprint({str(p.relative_to(self.root)): file_hash(p) for p in sorted(self.implementation_files())})

    def context_hash(self):
        references = {s["id"]: file_hash(self.resolve(s["reference"])) for s in self.spec["scenarios"]}
        sources = {s["id"]: file_hash(self.resolve(s["path"])) for s in self.spec["sources"]}
        return fingerprint({"job": file_hash(self.path), "sources": sources, "references": references})

    def requirements(self):
        for scenario in self.spec["scenarios"]:
            items = [item for item in self.items[scenario["source"]].values() if item["root"] == scenario["root"]]
            # 개별 속성을 모두 확인해도 전체 화면의 균형과 고정 영역 검토는 별개다.
            items.append({"node": scenario["root"], "property": "@composition",
                          "expected": scenario, "fingerprint": fingerprint(scenario),
                          "source_available": True})
            for item in items:
                key = json.dumps([scenario["id"], item["node"], item["property"]], separators=(",", ":"))
                yield key, {**item, "scenario": scenario["id"]}


def evidence_files_current(job, files, cache=None):
    # 한 캡처의 수천 속성을 검토해도 같은 APK를 수천 번 읽지 않는다.
    cache = {} if cache is None else cache
    for name, digest in files.items():
        path = job.resolve(name)
        if path not in cache:
            cache[path] = file_hash(path) if path.is_file() else None
        if cache[path] != digest:
            return False
    return bool(files)


def validate_receipt(job, path, scenario, context, implementation, cache=None):
    receipt = read(job.resolve(path))
    if receipt.get("context") != context or receipt.get("implementation") != implementation:
        raise ValueError("캡처 당시의 원본/구현과 현재 입력이 다름")
    if receipt.get("scenario") != scenario:
        raise ValueError("다른 화면, 상태 또는 뷰포트의 근거")
    files, roles = receipt.get("files", {}), receipt.get("roles", {})
    if set(roles) != {"image", "artifact", "observation"} or len(set(roles.values())) != 3:
        raise ValueError("이미지, 실행 산출물, 화면/상태 관찰 근거 필요")
    if not all(path in files for path in roles.values()) or not evidence_files_current(job, files, cache):
        raise ValueError("실행 근거 없음 또는 변경")
    return files


def evaluate(job, reviews):
    context, implementation = job.context_hash(), job.implementation_hash()
    implementation_files = job.implementation_files()
    rows = []
    expected_keys = set()
    file_cache, receipt_cache = {}, {}
    for key, item in job.requirements():
        expected_keys.add(key)
        review = reviews.get(key)
        status, reason = "pending", "검토 기록 없음"
        if not item["source_available"]:
            status, reason = "blocked", "원본 속성 읽기 실패: 보충 원본 필요"
        elif review:
            if review.get("context") != context or review.get("implementation") != implementation:
                status, reason = "stale", "원본, 작업 범위 또는 구현 변경"
            elif review.get("fingerprint") != item["fingerprint"]:
                status, reason = "stale", "기대 속성 변경"
            elif not evidence_files_current(job, review.get("mapping", {}), file_cache):
                status, reason = "stale", "구현 위치 없음 또는 변경"
            elif any(job.resolve(path) not in implementation_files for path in review["mapping"]):
                status, reason = "stale", "매핑한 파일이 작업의 구현 입력에 속하지 않음"
            elif not evidence_files_current(job, review.get("evidence", {}), file_cache):
                status, reason = "stale", "실제 렌더링 근거 없음 또는 변경"
            elif not review.get("note", "").strip() or not review.get("reviewer", "").strip():
                status, reason = "pending", "관찰 내용과 검토자 필요"
            elif review.get("status") in ("pass", "fail"):
                status, reason = review["status"], review["note"]
                try:
                    path = review["receipt"]
                    if path not in review["evidence"]:
                        raise ValueError("검토에 캡처 영수증 해시 없음")
                    receipt_key = (path, item["scenario"])
                    if receipt_key not in receipt_cache:
                        try:
                            validate_receipt(job, path, item["scenario"], context, implementation, file_cache)
                            receipt_cache[receipt_key] = None
                        except (OSError, ValueError, KeyError, TypeError) as error:
                            receipt_cache[receipt_key] = str(error)
                    if receipt_cache[receipt_key]:
                        raise ValueError(receipt_cache[receipt_key])
                except (OSError, ValueError, KeyError, TypeError) as error:
                    status, reason = "stale", str(error)
        rows.append({"key": key, **item, "status": status, "reason": reason})
    for key in sorted(set(reviews) - expected_keys):
        rows.append({"key": key, "status": "stale", "reason": "삭제되거나 다른 작업의 검토 항목"})
    for issue in job.spec.get("unresolved", []):
        rows.append({"key": issue["id"], "status": "blocked", "reason": issue["reason"]})
    return rows


def record_review(job, args):
    requirements = dict(job.requirements())
    receipt_path = job.resolve(args.receipt)
    context, implementation = job.context_hash(), job.implementation_hash()
    scenario = requirements[args.items[0]]["scenario"]
    evidence = validate_receipt(job, args.receipt, scenario, context, implementation)
    evidence = {**evidence, args.receipt: file_hash(receipt_path)}
    mappings = {path: file_hash(job.resolve(path)) for path in args.mapping}
    if any(job.resolve(path) not in job.implementation_files() for path in mappings):
        raise ValueError("매핑한 파일이 작업의 구현 입력에 속하지 않음")
    if not args.note.strip() or not args.reviewer.strip():
        raise ValueError("실제 관찰 내용과 검토자 필요")
    path = job.resolve(job.spec["reviews"])
    reviews = read(path) if path.exists() else {}
    for key in args.items:
        item = requirements[key]
        if not item["source_available"]:
            raise ValueError("읽지 못한 원본을 통과시킬 수 없음")
        if scenario != item["scenario"]:
            raise ValueError("다른 화면, 상태 또는 뷰포트의 근거")
        reviews[key] = {"context": context, "implementation": implementation,
                        "fingerprint": item["fingerprint"], "status": args.status,
                        "note": args.note, "reviewer": args.reviewer,
                        "mapping": mappings, "evidence": evidence, "receipt": args.receipt}
    write(path, reviews)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--job", type=Path, required=True)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("inventory", help="원본에서 전체 검토 항목을 다시 생성")
    commands.add_parser("verify", help="미검토/실패/낡음/수집 실패가 하나라도 있으면 실패")
    commands.add_parser("context", help="캡처 어댑터가 기록할 원본 및 구현 해시")
    review = commands.add_parser("review", help="실제로 대조한 명시적 항목만 기록")
    review.add_argument("--items", nargs="+", required=True)
    review.add_argument("--mapping", nargs="+", required=True)
    review.add_argument("--receipt", required=True)
    review.add_argument("--status", choices=("pass", "fail"), required=True)
    review.add_argument("--note", required=True)
    review.add_argument("--reviewer", required=True)
    args = parser.parse_args(argv)
    try:
        job = DesignJob(args.job)
        if args.command == "context":
            print(json.dumps({"context": job.context_hash(), "implementation": job.implementation_hash()}))
        elif args.command == "review":
            record_review(job, args)
        else:
            path = job.resolve(job.spec["reviews"])
            rows = evaluate(job, read(path) if path.exists() else {})
            output = job.resolve(job.spec["output"])
            write(output, {"job": job.spec["id"], "summary": dict(Counter(r["status"] for r in rows)), "items": rows})
            print(json.dumps(dict(Counter(r["status"] for r in rows))), output)
            if args.command == "verify" and any(row["status"] != "pass" for row in rows):
                return 1
        return 0
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"디자인 검증 차단: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
