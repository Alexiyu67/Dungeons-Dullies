#!/usr/bin/env python3
"""Build a review-only .dndpack from a user-supplied local PDF, text, or JSON file.

The importer has no network code, never writes into public content/packs, and does not print source
text. Candidates remain informational until a human marks them reviewed in candidates.json.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import zipfile
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

MAX_SOURCE_BYTES = 50 * 1024 * 1024
MAX_TEXT_BYTES = 16 * 1024 * 1024
MAX_PDF_PAGES = 1_000
MAX_EXTRACTED_CHARACTERS = 5_000_000
ALLOWED_SUFFIXES = {".pdf", ".txt", ".md", ".json"}
PRIVATE_OUTPUT_PARTS = {"privatecontent", "private-local", "local-content"}
FORMULA_PATTERN = re.compile(r"\b\d+d\d+(?:\s*[+-]\s*\d+)?\b", re.IGNORECASE)
MEDIA_TYPES = {
    ".pdf": "application/pdf",
    ".txt": "text/plain",
    ".md": "text/markdown",
    ".json": "application/json",
}


@dataclass(frozen=True)
class SourcePage:
    page: int
    text: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Create a private review pack from a local source file.")
    parser.add_argument("--input", required=True, type=Path, help="Local .pdf, .txt, .md, or .json source")
    parser.add_argument("--output", required=True, type=Path, help="Output path inside privateContent, private-local, or local-content")
    parser.add_argument("--pack-id", required=True, help="Stable private pack ID")
    parser.add_argument("--language", choices=("en", "de"), default="en")
    parser.add_argument("--force", action="store_true", help="Replace an existing output pack")
    return parser.parse_args()


def fail(message: str) -> "NoReturn":
    print(f"error: {message}", file=sys.stderr)
    raise SystemExit(2)


def validate_paths(source: Path, output: Path, force: bool) -> tuple[Path, Path]:
    source_text = str(source)
    if re.match(r"^https?:[\\/]", source_text, re.IGNORECASE):
        fail("URLs are not accepted; provide a file you already have locally")
    source = source.expanduser().resolve()
    output = output.expanduser().resolve()
    if not source.is_file():
        fail("input is not a readable local file")
    if source.suffix.lower() not in ALLOWED_SUFFIXES:
        fail("input must be PDF, TXT, MD, or JSON")
    if source.stat().st_size > MAX_SOURCE_BYTES:
        fail("input exceeds the 50 MB local-import limit")
    if source.suffix.lower() != ".pdf" and source.stat().st_size > MAX_TEXT_BYTES:
        fail("text and JSON input exceeds the 16 MB local-import limit")
    if not any(part.lower() in PRIVATE_OUTPUT_PARTS for part in output.parts):
        fail("output must be inside privateContent, private-local, or local-content")
    if output.suffix.lower() != ".dndpack":
        output = output.with_suffix(".dndpack")
    if output.exists() and not force:
        fail("output already exists; pass --force to replace that exact pack")
    return source, output


def extract_pdf(source: Path) -> list[SourcePage]:
    try:
        import pdfplumber  # type: ignore

        with pdfplumber.open(source) as document:
            if not 1 <= len(document.pages) <= MAX_PDF_PAGES:
                fail("PDF page count exceeds the local-import limit")
            pages = []
            total = 0
            for index, page in enumerate(document.pages):
                text = page.extract_text() or ""
                total += len(text)
                if total > MAX_EXTRACTED_CHARACTERS:
                    fail("extracted PDF text exceeds the local-import limit")
                pages.append(SourcePage(index + 1, text))
    except ImportError:
        try:
            from pypdf import PdfReader  # type: ignore

            reader = PdfReader(str(source))
            if not 1 <= len(reader.pages) <= MAX_PDF_PAGES:
                fail("PDF page count exceeds the local-import limit")
            pages = []
            total = 0
            for index, page in enumerate(reader.pages):
                text = page.extract_text() or ""
                total += len(text)
                if total > MAX_EXTRACTED_CHARACTERS:
                    fail("extracted PDF text exceeds the local-import limit")
                pages.append(SourcePage(index + 1, text))
        except ImportError:
            fail("PDF import needs the local pdfplumber or pypdf package")
    if not any(page.text.strip() for page in pages):
        fail("no text was extracted; OCR the scan explicitly before importing it")
    return pages


def extract_json(source: Path) -> list[SourcePage]:
    try:
        value = json.loads(source.read_text(encoding="utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError, RecursionError):
        fail("JSON input is not valid UTF-8 JSON")

    fragments: list[str] = []

    extracted_characters = 0

    def append_fragment(value: str) -> None:
        nonlocal extracted_characters
        remaining = MAX_EXTRACTED_CHARACTERS - extracted_characters
        if remaining <= 0:
            return
        fragment = value.strip()[:remaining]
        if fragment:
            fragments.append(fragment)
            extracted_characters += len(fragment)

    def visit(node: Any, depth: int = 0) -> None:
        if depth > 12 or len(fragments) >= 10_000:
            return
        if isinstance(node, str) and node.strip():
            append_fragment(node)
        elif isinstance(node, dict):
            for key, child in node.items():
                if isinstance(key, str) and key.strip():
                    append_fragment(key)
                visit(child, depth + 1)
        elif isinstance(node, list):
            for child in node:
                visit(child, depth + 1)

    visit(value)
    return [SourcePage(1, "\n".join(fragments)[:MAX_EXTRACTED_CHARACTERS])]


def extract_source(source: Path) -> list[SourcePage]:
    if source.suffix.lower() == ".pdf":
        return extract_pdf(source)
    if source.suffix.lower() == ".json":
        return extract_json(source)
    try:
        return [SourcePage(1, source.read_text(encoding="utf-8")[:MAX_EXTRACTED_CHARACTERS])]
    except UnicodeDecodeError:
        fail("text input must use UTF-8")


def looks_like_heading(line: str) -> bool:
    markdown_heading = line.lstrip().startswith("#")
    line = line.strip().strip("#*:—-")
    if not 2 <= len(line) <= 100 or len(line.split()) > 12:
        return False
    if FORMULA_PATTERN.fullmatch(line):
        return False
    alpha = [char for char in line if char.isalpha()]
    return bool(alpha) and (markdown_heading or line.isupper() or line.istitle() or line.endswith(("Feature", "Spell", "Weapon", "Action")))


def guess_kind(title: str, body: str) -> str:
    text = f"{title} {body}".lower()
    routes = (
        ("spell", ("spell", "cantrip", "zauber")),
        ("weapon", ("weapon", "attack", "waffe", "angriff")),
        ("condition", ("condition", "zustand")),
        ("feat", ("feat", "talent")),
        ("species", ("species", "ancestry", "spezies", "abstammung")),
        ("background", ("background", "hintergrund")),
        ("subclass", ("subclass", "subklasse")),
        ("class", ("class", "klasse")),
        ("item", ("item", "equipment", "gear", "gegenstand", "ausrüstung")),
        ("resource", ("uses", "charges", "rest", "verwendungen", "rast")),
    )
    return next((kind for kind, words in routes if any(word in text for word in words)), "rule")


def stable_id(pack_id: str, title: str, page: int, index: int) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")[:60] or "candidate"
    digest = hashlib.sha256(f"{pack_id}:{page}:{index}:{title}".encode()).hexdigest()[:8]
    return f"{slug}-{digest}"


def candidates_from_pages(pack_id: str, pages: Iterable[SourcePage]) -> list[dict[str, Any]]:
    candidates: list[dict[str, Any]] = []
    for page in pages:
        lines = [re.sub(r"\s+", " ", line).strip() for line in page.text.splitlines()]
        headings = [index for index, line in enumerate(lines) if looks_like_heading(line)]
        for local_index, line_index in enumerate(headings):
            title = lines[line_index].strip("#*:—- ")
            next_heading = headings[local_index + 1] if local_index + 1 < len(headings) else len(lines)
            body_lines = [line for line in lines[line_index + 1 : next_heading] if line][:8]
            summary = " ".join(body_lines)[:600]
            if not summary:
                continue
            formulas = sorted(set(FORMULA_PATTERN.findall(summary)))[:8]
            candidates.append(
                {
                    "id": stable_id(pack_id, title, page.page, local_index),
                    "kind": guess_kind(title, summary),
                    "name": title,
                    "summaryCandidate": summary,
                    "formulas": formulas,
                    "sourcePage": page.page,
                    "review": {"status": "needs_review", "approvedBy": None, "reviewedAt": None},
                    "automation": {"level": "informational", "eligible": False, "reason": "human review required"},
                }
            )
    return candidates[:5_000]


def write_pack(source: Path, output: Path, pack_id: str, language: str, candidates: list[dict[str, Any]]) -> None:
    if not re.fullmatch(r"[a-z0-9][a-z0-9._-]{1,79}", pack_id):
        fail("pack ID must use 2-80 lowercase letters, numbers, dots, underscores, or hyphens")
    output.parent.mkdir(parents=True, exist_ok=True)
    source_digest = hashlib.sha256()
    with source.open("rb") as source_stream:
        for block in iter(lambda: source_stream.read(64 * 1024), b""):
            source_digest.update(block)
    source_hash = source_digest.hexdigest()
    review_note = """# Local pack review

Every candidate begins as `needs_review` and `informational`. Check the named source page, correct
the mechanical fields, and record approval before enabling automation. Do not move this pack into
the public `content/packs` directory. Missing or ambiguous fields must remain informational.
"""
    candidates_bytes = json.dumps(candidates, ensure_ascii=False, indent=2).encode("utf-8")
    review_bytes = review_note.encode("utf-8")

    def packed_file(path: str, media_type: str, value: bytes) -> dict[str, Any]:
        return {
            "path": path,
            "mediaType": media_type,
            "size": len(value),
            "sha256": hashlib.sha256(value).hexdigest(),
        }

    manifest = {
        "schemaVersion": 1,
        "containerKind": "review-candidates",
        "id": pack_id,
        "version": "0.0.1-local",
        "locale": language,
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "source": {
            "fileName": source.name,
            "sha256": source_hash,
            "mediaType": MEDIA_TYPES[source.suffix.lower()],
        },
        "payload": {"primary": "candidates.json"},
        "files": [
            packed_file("candidates.json", "application/json", candidates_bytes),
            packed_file("REVIEW.md", "text/markdown", review_bytes),
        ],
        "privacy": {
            "containsPrivateContent": True,
            "distributionReady": False,
            "networkAccess": False,
        },
        "review": {"status": "needs-review", "automationEligibleCount": 0},
    }
    partial = output.with_name(f"{output.name}.part")
    try:
        partial.unlink(missing_ok=True)
        with zipfile.ZipFile(partial, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            archive.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
            archive.writestr("candidates.json", candidates_bytes)
            archive.writestr("REVIEW.md", review_bytes)
        with partial.open("rb+") as staged_pack:
            os.fsync(staged_pack.fileno())
        partial.replace(output)
    finally:
        partial.unlink(missing_ok=True)


def main() -> None:
    args = parse_args()
    source, output = validate_paths(args.input, args.output, args.force)
    pages = extract_source(source)
    candidates = candidates_from_pages(args.pack_id, pages)
    if not candidates:
        fail("no structured candidates were found; use the in-app manual editor for this source")
    write_pack(source, output, args.pack_id, args.language, candidates)
    print(f"created review pack: {output}")
    print(f"candidates: {len(candidates)}; automation eligible: 0")


if __name__ == "__main__":
    main()
