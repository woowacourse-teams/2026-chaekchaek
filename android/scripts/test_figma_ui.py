import base64
import copy
from pathlib import Path
import tempfile
import shutil
import unittest
from types import SimpleNamespace
from unittest.mock import patch
import xml.etree.ElementTree as ET

from cases import chaekchaek as gate


class GenericReceiptAdapterTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.out = self.root / "build/figma-ui"
        self.out.mkdir(parents=True)
        self.artifact = self.root / "app/build/outputs/apk/release/app-release.apk"
        self.artifact.parent.mkdir(parents=True)
        self.artifact.write_bytes(b"fixture-artifact")
        for name in ("page-loaded-390.png", "page-loaded-390.xml", "page-loaded-390.json", "build.json"):
            (self.out / name).write_text(name)
        self.job = SimpleNamespace(path=self.root / "visual-contracts/job.json")
        self.build = {"apk": gate.digest(self.artifact), "guard_context": "source-at-build",
                      "guard_implementation": "implementation-at-build"}
        for name, value in (("ROOT", self.root), ("OUT", self.out)):
            context = patch.object(gate, name, value)
            context.start()
            self.addCleanup(context.stop)

    def test_receipt_preserves_build_identity_and_capture_evidence(self):
        gate.publish_guard_receipt(self.job, "page-loaded-390", self.build)
        receipt = gate.read(self.root / "build/design-guard/page-loaded-390.receipt.json")
        self.assertEqual(receipt["context"], "source-at-build")
        self.assertEqual(receipt["implementation"], "implementation-at-build")
        self.assertEqual(receipt["scenario"], "page-loaded-390")
        self.assertEqual(set(receipt["roles"]), {"image", "artifact", "observation"})
        self.assertEqual(len(receipt["files"]), 5)

    def test_replaced_artifact_cannot_be_registered_as_capture_evidence(self):
        self.artifact.write_bytes(b"different-artifact")
        with self.assertRaisesRegex(ValueError, "산출물과 다릅니다"):
            gate.publish_guard_receipt(self.job, "page-loaded-390", self.build)


class NavigationAssetTest(unittest.TestCase):
    def test_all_eight_assets_and_tab_bindings_match_figma(self):
        self.assertEqual(gate.verify_icon_assets(), [])

    def test_wrong_stroke_and_filled_icon_are_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            for folder in ("visual-contracts/assets", "shared/src/commonMain/composeResources/drawable"):
                shutil.copytree(gate.ROOT / folder, root / folder)
            callsite = Path("shared/src/commonMain/kotlin/com/chaekchaek/app/ui/RootScreen.kt")
            (root / callsite).parent.mkdir(parents=True)
            shutil.copy(gate.ROOT / callsite, root / callsite)
            icon = root / "shared/src/commonMain/composeResources/drawable/ic_nav_home_active.xml"
            icon.write_text(icon.read_text().replace('strokeWidth="2.10833"', 'strokeWidth="1"').replace('#00000000', '#191919'))
            failures = gate.verify_icon_assets(root)
            self.assertIn("home_active: strokeWidth 불일치", failures)
            self.assertIn("home_active: 원본에 없는 채움", failures)


class CompletionGateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.root_patch = patch.object(gate, "ROOT", self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)
        png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a/J8AAAAASUVORK5CYII=")
        (self.root / "reference.png").write_bytes(png)
        (self.root / "actual.png").write_bytes(png)
        self.spec = {"widths": [390], "screens": [{
            "id": "home", "reference": "reference.png", "checks": [
                {"id": "home.shadow", "expected": "그림자 표시", "states": ["reading"]},
            ],
        }]}
        gate.write(self.root / "capture.json", {
            "source": "current", "screen": "home", "state": "reading", "width": 390,
            "image": "actual.png", "sha256": gate.digest(self.root / "actual.png"),
        })
        self.review = {"home.shadow@390": {
            "status": "pass", "note": "원본과 표지 바깥 그림자 비교", "source": "current",
            "reference": gate.digest(self.root / "reference.png"),
            "captures": [{"metadata": "capture.json", "metadata_sha256": gate.digest(self.root / "capture.json"),
                          "sha256": gate.digest(self.root / "actual.png")}],
        }}

    def verdict(self, review=None, source="current"):
        return gate.evaluate(self.spec, self.review if review is None else review, source, self.root)[0]["status"]

    def mutate_capture(self, **changes):
        data = gate.read(self.root / "capture.json")
        data.update(changes)
        gate.write(self.root / "capture.json", data)
        self.review["home.shadow@390"]["captures"][0]["metadata_sha256"] = gate.digest(self.root / "capture.json")

    def test_complete_review_passes(self):
        self.assertEqual(self.verdict(), "pass")

    def test_missing_review_blocks_completion(self):
        self.assertEqual(self.verdict({}), "pending")

    def test_build_success_cannot_replace_visual_review(self):
        self.review["home.shadow@390"]["captures"] = []
        self.assertEqual(self.verdict(), "pending")

    def test_source_change_invalidates_pass(self):
        self.assertEqual(self.verdict(source="new-code"), "stale")

    def test_reference_change_invalidates_pass(self):
        (self.root / "reference.png").write_bytes(b"changed")
        self.assertEqual(self.verdict(), "stale")

    def test_overwritten_screenshot_invalidates_pass(self):
        (self.root / "actual.png").write_bytes(b"not a screenshot")
        self.assertEqual(self.verdict(), "stale")

    def test_empty_state_cannot_prove_reading_shadow(self):
        self.mutate_capture(state="empty")
        self.assertEqual(self.verdict(), "pending")

    def test_other_screen_cannot_prove_home(self):
        self.mutate_capture(screen="library")
        self.assertEqual(self.verdict(), "stale")

    def test_wrong_width_cannot_pass(self):
        self.mutate_capture(width=320)
        self.assertEqual(self.verdict(), "stale")

    def test_new_required_item_blocks_previous_complete_review(self):
        item = copy.deepcopy(self.spec["screens"][0]["checks"][0])
        item["id"] = "home.indicator"
        self.spec["screens"][0]["checks"].append(item)
        rows = gate.evaluate(self.spec, self.review, "current", self.root)
        self.assertEqual([r["status"] for r in rows], ["pass", "pending"])

    def test_failure_remains_failure(self):
        self.review["home.shadow@390"]["status"] = "fail"
        self.assertEqual(self.verdict(), "fail")

    def test_no_observation_cannot_pass(self):
        self.review["home.shadow@390"]["note"] = " "
        self.assertEqual(self.verdict(), "pending")

    def test_metadata_edit_invalidates_pass(self):
        gate.write(self.root / "capture.json", {"state": "reading"})
        self.assertEqual(self.verdict(), "stale")

    def test_loading_screen_rejected(self):
        root = ET.fromstring('<hierarchy><node text="책췍"/></hierarchy>')
        with self.assertRaises(ValueError):
            gate.validate_screen(root, {"id": "home", "anchors": ["최근"]})

    def test_shared_brand_header_cannot_prove_feed_tab(self):
        root = ET.fromstring('<hierarchy><node text="책췍"/><node selected="true"><node text="홈"/></node></hierarchy>')
        with self.assertRaises(ValueError):
            gate.validate_screen(root, {"id": "feed", "anchors": ["책췍"]})

    def test_empty_library_cannot_be_captured_as_populated(self):
        root = ET.fromstring('<hierarchy><node text="아직 서재가 비어 있어요"/></hierarchy>')
        with self.assertRaises(ValueError):
            gate.validate_state(root, "library", "populated")

    def test_missing_reading_card_cannot_be_captured_as_reading(self):
        root = ET.fromstring('<hierarchy><node text="지금 읽고 있는 책이 있으세요?"/></hierarchy>')
        with self.assertRaises(ValueError):
            gate.validate_state(root, "home", "reading")


if __name__ == "__main__":
    unittest.main()
