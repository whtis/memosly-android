#!/usr/bin/env python3
"""Check that the release metadata scattered across the repo agrees with itself.

A release touches four places, and nothing but care keeps them in sync:

  app/build.gradle.kts                                versionCode / versionName
  fastlane/metadata/android/{locale}/changelogs/N.txt store changelog, named by versionCode
  version.json                                        what the in-app update check reads
  CHANGELOG.md                                        the human history

Getting one wrong is quiet: a missing changelogs/N.txt just means the store shows
nothing, and a stale version.json prompts every user to "update" to what they have.

Run with no arguments from the repo root. Exits non-zero on the first failure.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

# Both F-Droid and Play cap a changelog at 500 characters and truncate past it.
CHANGELOG_LIMIT = 500

LOCALES = ("en-US", "zh-CN")

REPO = Path(__file__).resolve().parent.parent


class Failures(list):
    def check(self, ok: bool, message: str) -> bool:
        if not ok:
            self.append(message)
        return ok


def read_version() -> tuple[int, str]:
    text = (REPO / "app/build.gradle.kts").read_text()
    code = re.search(r"versionCode\s*=\s*(\d+)", text)
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    if not code or not name:
        sys.exit("could not parse versionCode/versionName out of app/build.gradle.kts")
    return int(code.group(1)), name.group(1)


def main() -> int:
    version_code, version_name = read_version()
    print(f"versionCode={version_code} versionName={version_name}")
    f = Failures()

    # Store changelogs are named by versionCode, so a bump needs a new file per locale.
    for locale in LOCALES:
        path = REPO / f"fastlane/metadata/android/{locale}/changelogs/{version_code}.txt"
        if not f.check(path.exists(), f"missing {path.relative_to(REPO)} for versionCode {version_code}"):
            continue
        body = path.read_text()
        f.check(body.strip() != "", f"{path.relative_to(REPO)} is empty")
        f.check(
            len(body) <= CHANGELOG_LIMIT,
            f"{path.relative_to(REPO)} is {len(body)} chars, over the {CHANGELOG_LIMIT} store limit",
        )

    # version.json is fetched from main by the in-app update check — if it disagrees
    # with versionName, users are told to update to a version that does not exist.
    version_json = REPO / "version.json"
    try:
        data = json.loads(version_json.read_text())
    except (OSError, json.JSONDecodeError) as e:
        f.append(f"version.json is unreadable: {e}")
    else:
        f.check(
            data.get("version") == version_name,
            f"version.json says {data.get('version')!r}, app/build.gradle.kts says {version_name!r}",
        )
        for field in ("release_notes", "release_notes_zh"):
            f.check(bool(data.get(field, "").strip()), f"version.json has no {field}")

    changelog = (REPO / "CHANGELOG.md").read_text()
    f.check(
        f"## [{version_name}]" in changelog,
        f"CHANGELOG.md has no '## [{version_name}]' section",
    )
    f.check(
        re.search(rf"^\[{re.escape(version_name)}\]:\s*http", changelog, re.M) is not None,
        f"CHANGELOG.md has no '[{version_name}]: <url>' link reference at the bottom",
    )

    if f:
        print("\nrelease metadata is out of sync:", file=sys.stderr)
        for problem in f:
            print(f"  - {problem}", file=sys.stderr)
        return 1

    print("release metadata is consistent")
    return 0


if __name__ == "__main__":
    sys.exit(main())
