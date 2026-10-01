import argparse
import base64
import copy
import json
from pathlib import Path
import tempfile
import unittest
import struct

import design_guard as guard
from import_design_chunks import decode_chunks


def node(node_id, parent=None, children=None, **properties):
    return {"id": node_id, "parent": parent, "children": children or [],
            "declaredProperties": list(properties), "properties": properties, "unavailable": {}}


def snapshot(source="arbitrary-design", root="panel"):
    records = [node(root, children=["hidden-card"], type="FRAME", width=800),
               node("hidden-card", parent=root, type="RECTANGLE", visible=False,
                    effects=[{"type": "DROP_SHADOW", "radius": 9}], futureProperty={"value": 12})]
    return {"schema": 1, "source": source, "roots": [root],
            "discovered": [n["id"] for n in records], "records": records}


class SourceCoverageTest(unittest.TestCase):
    def test_unrelated_designs_require_no_engine_changes(self):
        checkout = snapshot("commerce", "checkout")
        diagram = {"schema": 1, "source": "flow-editor", "roots": ["canvas"],
                   "discovered": ["canvas", "connection"], "records": [
                       node("canvas", children=["connection"], type="SECTION", background="#ffffff"),
                       node("connection", parent="canvas", type="CONNECTOR", endpoints=[[0, 10], [80, 90]],
                            arrowhead="TRIANGLE", dashPattern=[4, 2], label="승인 후 진행")
                   ]}
        checkout_items = guard.inventory(checkout, ["checkout"])
        diagram_items = guard.inventory(diagram, ["canvas"])
        self.assertIn('["hidden-card","effects"]', checkout_items)
        self.assertIn('["connection","endpoints"]', diagram_items)
        self.assertIn('["connection","arrowhead"]', diagram_items)
        self.assertFalse(any(i["property"] == "effects" for i in diagram_items.values()))

    def test_hidden_nodes_and_unknown_properties_are_not_filtered(self):
        items = guard.inventory(snapshot(), ["panel"])
        self.assertIn('["hidden-card","futureProperty"]', items)
        self.assertFalse(items['["hidden-card","visible"]']["expected"])

    def test_new_shadow_on_any_node_adds_a_requirement(self):
        source = snapshot()
        before = guard.inventory(source, ["panel"])
        source["records"][0]["properties"]["effects"] = [{"radius": 20}]
        source["records"][0]["declaredProperties"].append("effects")
        after = guard.inventory(source, ["panel"])
        self.assertEqual(set(after) - set(before), {'["panel","effects"]'})

    def test_dropped_node_cannot_silently_reduce_scope(self):
        source = snapshot()
        source["records"].pop()
        with self.assertRaises(guard.IncompleteSource):
            guard.inventory(source, ["panel"])

    def test_dropped_property_cannot_silently_reduce_scope(self):
        source = snapshot()
        del source["records"][1]["properties"]["effects"]
        with self.assertRaises(guard.IncompleteSource):
            guard.inventory(source, ["panel"])

    def test_duplicate_and_disconnected_nodes_are_rejected(self):
        for change in ("duplicate", "disconnected", "wrong-parent", "missing-child"):
            with self.subTest(change=change):
                source = snapshot()
                if change == "duplicate":
                    source["records"].append(source["records"][0])
                elif change == "disconnected":
                    source["records"][0]["children"] = []
                elif change == "wrong-parent":
                    source["records"][1]["parent"] = "other"
                else:
                    source["records"][0]["children"].append("missing")
                with self.assertRaises(guard.IncompleteSource):
                    guard.inventory(source, ["panel"])

    def test_source_read_errors_remain_visible_and_blocked(self):
        source = snapshot()
        source["records"][0]["unavailable"]["unsupported"] = "not supported"
        source["records"][0]["declaredProperties"].append("unsupported")
        item = guard.inventory(source, ["panel"])['["panel","unsupported"]']
        self.assertFalse(item["source_available"])

    def test_nested_undefined_is_not_treated_as_a_known_value(self):
        self.assertTrue(guard.unavailable_value({"fills": [{"$unavailable": "undefined"}]}))

    def test_mixed_text_styles_require_range_level_source(self):
        self.assertTrue(guard.unavailable_value({"$mixed": True}))

    def test_layer_order_change_changes_structure_fingerprint(self):
        source = snapshot()
        source["records"].append(node("second", parent="panel", type="VECTOR"))
        source["discovered"].append("second")
        source["records"][0]["children"].append("second")
        before = guard.inventory(source, ["panel"])['["panel","@structure"]']
        source["records"][0]["children"].reverse()
        after = guard.inventory(source, ["panel"])['["panel","@structure"]']
        self.assertNotEqual(before["fingerprint"], after["fingerprint"])


class EvidenceCoverageTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.path = self.root / "job.json"
        for path in ("implementation.kt", "reference.png", "actual.png", "artifact.apk", "observation.xml"):
            (self.root / path).write_text(path)
        guard.write(self.root / "source.json", snapshot())
        self.spec = {"schema": 1, "id": "any-product", "implementation_root": ".",
                     "implementation_inputs": ["*.kt"], "reviews": "reviews.json", "output": "report.json",
                     "sources": [{"id": "design", "identity": "arbitrary-design", "path": "source.json", "roots": ["panel"]}],
                     "scenarios": [{"id": "tablet-empty", "source": "design", "root": "panel",
                                    "state": "empty", "viewport": {"width": 768}, "reference": "reference.png"}]}
        guard.write(self.path, self.spec)
        self.job = guard.DesignJob(self.path)
        self.receipt = {"context": self.job.context_hash(), "implementation": self.job.implementation_hash(),
                        "scenario": "tablet-empty", "roles": {"image": "actual.png", "artifact": "artifact.apk",
                                                                 "observation": "observation.xml"},
                        "files": {p: guard.file_hash(self.root / p) for p in ("actual.png", "artifact.apk", "observation.xml")}}
        guard.write(self.root / "receipt.json", self.receipt)

    def review_all(self):
        args = argparse.Namespace(items=list(dict(self.job.requirements())), mapping=["implementation.kt"],
                                  receipt="receipt.json", status="pass", note="원본과 해당 속성을 비교한 시험 기록", reviewer="test")
        guard.record_review(self.job, args)
        return guard.read(self.root / "reviews.json")

    def test_explicit_complete_evidence_can_pass(self):
        self.assertEqual({row["status"] for row in guard.evaluate(self.job, self.review_all())}, {"pass"})

    def test_deleting_a_review_item_does_not_delete_requirement(self):
        reviews = self.review_all()
        del reviews[next(iter(reviews))]
        self.assertIn("pending", {row["status"] for row in guard.evaluate(self.job, reviews)})

    def test_whole_page_comparison_is_always_required(self):
        self.assertTrue(any(item["property"] == "@composition" for _, item in self.job.requirements()))

    def test_added_property_becomes_pending_without_editing_checklist(self):
        reviews = self.review_all()
        source = guard.read(self.root / "source.json")
        source["records"][0]["properties"]["newEffect"] = {"blur": 20}
        source["records"][0]["declaredProperties"].append("newEffect")
        guard.write(self.root / "source.json", source)
        rows = guard.evaluate(guard.DesignJob(self.path), reviews)
        self.assertEqual(next(r for r in rows if r.get("property") == "newEffect")["status"], "pending")
        self.assertIn("stale", {r["status"] for r in rows})

    def test_changed_code_reference_capture_or_artifact_invalidates_evidence(self):
        reviews = self.review_all()
        for file in ("implementation.kt", "reference.png", "actual.png", "artifact.apk", "observation.xml", "receipt.json"):
            with self.subTest(file=file):
                path = self.root / file
                original = path.read_bytes()
                path.write_bytes(original + b"changed")
                self.assertEqual({row["status"] for row in guard.evaluate(self.job, reviews)}, {"stale"})
                path.write_bytes(original)

    def test_other_state_cannot_supply_evidence(self):
        self.receipt["scenario"] = "tablet-loaded"
        guard.write(self.root / "receipt.json", self.receipt)
        with self.assertRaisesRegex(ValueError, "다른 화면"):
            self.review_all()

    def test_reviews_cannot_hide_wrong_scenario_by_updating_file_hash(self):
        reviews = self.review_all()
        self.receipt["scenario"] = "tablet-loaded"
        guard.write(self.root / "receipt.json", self.receipt)
        for review in reviews.values():
            review["evidence"]["receipt.json"] = guard.file_hash(self.root / "receipt.json")
        self.assertEqual({r["status"] for r in guard.evaluate(self.job, reviews)}, {"stale"})

    def test_missing_source_or_empty_implementation_fails_closed(self):
        for mutation in ("source", "implementation"):
            with self.subTest(mutation=mutation):
                if mutation == "source":
                    self.spec["sources"][0]["path"] = "missing.json"
                else:
                    self.spec["sources"][0]["path"] = "source.json"
                    self.spec["implementation_inputs"] = ["missing.kt"]
                guard.write(self.path, self.spec)
                self.assertEqual(guard.main(["--job", str(self.path), "verify"]), 2)

    def test_unresolved_menu_or_state_blocks_completion(self):
        self.spec["unresolved"] = [{"id": "open-menu", "reason": "해당 상태 원본 없음"}]
        guard.write(self.path, self.spec)
        rows = guard.evaluate(guard.DesignJob(self.path), {})
        self.assertEqual(rows[-1]["status"], "blocked")

    def test_uncovered_source_root_is_rejected(self):
        self.spec["scenarios"][0]["root"] = "not-the-requested-root"
        guard.write(self.path, self.spec)
        with self.assertRaises(guard.IncompleteSource):
            guard.DesignJob(self.path)

    def test_actual_entrypoint_returns_failure_for_pending(self):
        self.assertEqual(guard.main(["--job", str(self.path), "verify"]), 1)

    def test_actual_entrypoint_returns_success_only_after_all_reviews(self):
        self.review_all()
        self.assertEqual(guard.main(["--job", str(self.path), "verify"]), 0)

    def test_source_changes_during_build_invalidate_same_job_object(self):
        before = self.job.context_hash()
        source = guard.read(self.root / "source.json")
        source["records"][1]["properties"]["effects"] = []
        guard.write(self.root / "source.json", source)
        self.assertNotEqual(before, self.job.context_hash())

    def test_mapping_must_point_inside_declared_implementation_inputs(self):
        args = argparse.Namespace(items=list(dict(self.job.requirements())), mapping=["reference.png"],
                                  receipt="receipt.json", status="pass", note="시험", reviewer="test")
        with self.assertRaisesRegex(ValueError, "구현 입력"):
            guard.record_review(self.job, args)

    def test_unknown_source_property_cannot_be_manually_passed(self):
        source = guard.read(self.root / "source.json")
        source["records"][0]["unavailable"]["unknown"] = "unsupported"
        source["records"][0]["declaredProperties"].append("unknown")
        guard.write(self.root / "source.json", source)
        self.job = guard.DesignJob(self.path)
        self.receipt["context"] = self.job.context_hash()
        guard.write(self.root / "receipt.json", self.receipt)
        with self.assertRaisesRegex(ValueError, "읽지 못한 원본"):
            self.review_all()

    def test_orphaned_review_cannot_disappear_from_report(self):
        reviews = self.review_all()
        reviews["old-screen"] = {}
        self.assertEqual(guard.evaluate(self.job, reviews)[-1]["status"], "stale")


class TransportCoverageTest(unittest.TestCase):
    def test_lossless_unicode_source_import(self):
        original = snapshot("디자인 원본", "루트")
        raw = json.dumps(original, ensure_ascii=False).encode()
        encoded = base64.b64encode(b"".join(struct.pack(">H", byte) for byte in raw)).decode()
        checksum = 2166136261
        for char in encoded:
            checksum = ((checksum ^ ord(char)) * 16777619) & 0xffffffff
        chunk = {"offset": 0, "total": len(encoded), "checksum": checksum, "nodes": 2,
                 "encoding": "lzw16-base64", "chunk": encoded}
        self.assertEqual(decode_chunks([chunk]), original)

    def test_truncated_or_inconsistent_chunks_are_rejected(self):
        with self.assertRaises(ValueError):
            decode_chunks([])
        chunk = {"offset": 0, "total": 8, "checksum": 10, "nodes": 1, "encoding": "lzw16-base64", "chunk": "abcd"}
        with self.assertRaisesRegex(ValueError, "잘린"):
            decode_chunks([chunk])
        with self.assertRaisesRegex(ValueError, "누락 또는 중복"):
            decode_chunks([chunk, copy.deepcopy(chunk)])
        other = {**chunk, "offset": 4, "checksum": 11}
        with self.assertRaisesRegex(ValueError, "원본 변경"):
            decode_chunks([chunk, other])


if __name__ == "__main__":
    unittest.main()
