from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("import-private-content.py")
SPEC = importlib.util.spec_from_file_location("private_importer", MODULE_PATH)
assert SPEC and SPEC.loader
IMPORTER = importlib.util.module_from_spec(SPEC)
sys.modules["private_importer"] = IMPORTER
SPEC.loader.exec_module(IMPORTER)


class PrivateImporterTests(unittest.TestCase):
    def test_recognizes_language_entries(self) -> None:
        self.assertEqual("language", IMPORTER.guess_kind("Lunar Language", "A spoken language of moonfolk."))

    def test_rejects_urls(self) -> None:
        with self.assertRaises(SystemExit):
            IMPORTER.validate_paths(Path("https://example.invalid/book.pdf"), Path("private-local/out.dndpack"), False)

    def test_rejects_public_pack_output(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.md"
            source.write_text("# Fictional Feature\nA project-authored test rule.", encoding="utf-8")
            with self.assertRaises(SystemExit):
                IMPORTER.validate_paths(source, root / "content" / "packs" / "out.dndpack", False)

    def test_pack_is_review_gated_and_contains_no_network_setting(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.md"
            source.write_text("# Fictional Feature\nRoll 1d6 for a project-authored test effect.", encoding="utf-8")
            output = root / "private-local" / "test.dndpack"
            source, output = IMPORTER.validate_paths(source, output, False)
            candidates = IMPORTER.candidates_from_pages("test-pack", IMPORTER.extract_source(source))
            IMPORTER.write_pack(source, output, "test-pack", "en", candidates)

            with zipfile.ZipFile(output) as archive:
                manifest = json.loads(archive.read("manifest.json"))
                packed_candidates = json.loads(archive.read("candidates.json"))
                names = set(archive.namelist())
                packed_values = {item["path"]: archive.read(item["path"]) for item in manifest["files"]}

            self.assertEqual(1, manifest["schemaVersion"])
            self.assertEqual("review-candidates", manifest["containerKind"])
            self.assertFalse(manifest["privacy"]["distributionReady"])
            self.assertFalse(manifest["privacy"]["networkAccess"])
            self.assertEqual(0, manifest["review"]["automationEligibleCount"])
            self.assertEqual({"manifest.json", "candidates.json", "REVIEW.md"}, names)
            for packed_file in manifest["files"]:
                value = packed_values[packed_file["path"]]
                self.assertEqual(len(value), packed_file["size"])
                self.assertEqual(__import__("hashlib").sha256(value).hexdigest(), packed_file["sha256"])
            self.assertTrue(packed_candidates)
            self.assertTrue(all(item["review"]["status"] == "needs_review" for item in packed_candidates))
            self.assertTrue(all(not item["automation"]["eligible"] for item in packed_candidates))


if __name__ == "__main__":
    unittest.main()
