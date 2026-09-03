import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("private_importer", ROOT / "scripts" / "import-private-content.py")
IMPORTER = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(IMPORTER)


class PrivateImporterTest(unittest.TestCase):
    def test_example_builds_as_exact_two_file_pack(self):
        source = ROOT / "content" / "private-template" / "private-content.example.json"
        content = IMPORTER.content_bytes(source)
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "example.dndpack"
            IMPORTER.write_pack(output, content)
            document = IMPORTER.validate_pack(output)
            self.assertEqual("private.example", document["id"])
            with zipfile.ZipFile(output) as archive:
                self.assertEqual(["manifest.json", "content.json"], archive.namelist())

    def test_unknown_fields_are_rejected(self):
        document = json.loads((ROOT / "content" / "private-template" / "private-content.example.json").read_text(encoding="utf-8"))
        document["legacy"] = True
        with self.assertRaisesRegex(ValueError, "unsupported fields"):
            IMPORTER.validate_document(document)

    def test_broken_parent_reference_is_rejected(self):
        document = json.loads((ROOT / "content" / "private-template" / "private-content.example.json").read_text(encoding="utf-8"))
        document["entries"][1]["mechanics"]["parentClassId"] = "class-missing"
        with self.assertRaisesRegex(ValueError, "reference"):
            IMPORTER.validate_document(document)


if __name__ == "__main__":
    unittest.main()
