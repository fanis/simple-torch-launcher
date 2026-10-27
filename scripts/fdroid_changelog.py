"""Writes F-Droid's release notes for one version from its CHANGELOG.md section.

Usage: python3 scripts/fdroid_changelog.py VERSION VERSION_CODE

F-Droid shows fastlane/metadata/android/en-US/changelogs/<versionCode>.txt as plain text and
asks for at most 500 characters, so Markdown is stripped and long notes are cut at a line.
"""
import re
import sys

LIMIT = 500
MORE = "More in the release notes on GitHub."

version, code = sys.argv[1], sys.argv[2]
with open("CHANGELOG.md", encoding="utf-8") as f:
    changelog = f.read()

section = re.search(
    r"^## \[" + re.escape(version) + r"\][^\n]*\n(.*?)(?=^## \[|\Z)", changelog, re.M | re.S
)
if not section:
    sys.exit(f"CHANGELOG.md has no section for {version}")

lines = []
for line in section.group(1).strip().splitlines():
    line = line.strip()
    if not line:
        continue
    line = re.sub(r"^###\s*(.+)$", r"\1:", line)  # "### Added" -> "Added:"
    line = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", line)  # links -> their text
    line = line.replace("**", "").replace("`", "")
    lines.append(line)

notes = "\n".join(lines)
if len(notes) > LIMIT:
    kept = []
    for line in lines:
        if len("\n".join(kept + [line, MORE])) > LIMIT:
            break
        kept.append(line)
    notes = "\n".join(kept + [MORE])

with open(f"fastlane/metadata/android/en-US/changelogs/{code}.txt", "w", encoding="utf-8") as f:
    f.write(notes + "\n")
print(notes)
