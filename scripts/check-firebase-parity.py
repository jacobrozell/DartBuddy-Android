#!/usr/bin/env python3
"""Compare Firebase telemetry allowlists between paired iOS and Android apps."""

from __future__ import annotations

import argparse
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path

QUOTED = re.compile(r'"([a-z][a-z0-9_]*)"')
CODE_PAIR = re.compile(r'"([a-z][a-z0-9_]*)"\s*(?:to|:)\s*(\d+)')


@dataclass(frozen=True)
class AppPairSpec:
    name: str
    ios_analytics_rel: str
    android_analytics_rel: str
    ios_crashlytics_rel: str | None = None
    android_crashlytics_rel: str | None = None
    ios_analytics_markers: tuple[str, ...] = ("allowlistedEvents", "allowlistedLogEvents")


@dataclass(frozen=True)
class AppPair:
    name: str
    ios_analytics: Path
    android_analytics: Path
    ios_analytics_markers: tuple[str, ...]
    ios_crashlytics: Path | None = None
    android_crashlytics: Path | None = None


PAIR_SPECS = [
    AppPairSpec(
        name="Puzzle Buddy",
        ios_analytics_rel="PuzzleBuddy/App/Util/AppLogging.swift",
        android_analytics_rel=(
            "PuzzleBuddy-Android/app/src/main/kotlin/com/jacobrozell/puzzlebuddy/"
            "support/logging/FirebaseAnalyticsEventMapping.kt"
        ),
        ios_crashlytics_rel="PuzzleBuddy/App/Util/FirebaseCrashlyticsEventMapping.swift",
        android_crashlytics_rel=(
            "PuzzleBuddy-Android/app/src/main/kotlin/com/jacobrozell/puzzlebuddy/"
            "support/logging/FirebaseCrashlyticsEventMapping.kt"
        ),
    ),
    AppPairSpec(
        name="Dart Buddy",
        ios_analytics_rel="DartBuddy/Support/Logging/FirebaseAnalyticsEventMapping.swift",
        android_analytics_rel=(
            "DartBuddy-Android/app/src/main/kotlin/com/jacobrozell/dartbuddy/"
            "support/logging/FirebaseAnalyticsEventMapping.kt"
        ),
        ios_crashlytics_rel="DartBuddy/Support/Logging/FirebaseCrashlyticsEventMapping.swift",
        android_crashlytics_rel=(
            "DartBuddy-Android/app/src/main/kotlin/com/jacobrozell/dartbuddy/"
            "support/logging/FirebaseCrashlyticsEventMapping.kt"
        ),
    ),
]

# CI: Android repo checked out at workspace root with iOS nested under PuzzleBuddy/ or DartBuddy/.
CI_ANDROID_ANALYTICS = {
    "Puzzle Buddy": (
        "app/src/main/kotlin/com/jacobrozell/puzzlebuddy/support/logging/"
        "FirebaseAnalyticsEventMapping.kt"
    ),
    "Dart Buddy": (
        "app/src/main/kotlin/com/jacobrozell/dartbuddy/support/logging/"
        "FirebaseAnalyticsEventMapping.kt"
    ),
}
CI_ANDROID_CRASHLYTICS = {
    "Puzzle Buddy": (
        "app/src/main/kotlin/com/jacobrozell/puzzlebuddy/support/logging/"
        "FirebaseCrashlyticsEventMapping.kt"
    ),
    "Dart Buddy": (
        "app/src/main/kotlin/com/jacobrozell/dartbuddy/support/logging/"
        "FirebaseCrashlyticsEventMapping.kt"
    ),
}


def detect_root(explicit: Path | None) -> Path:
    if explicit is not None:
        return explicit
    env_root = os.environ.get("FIREBASE_PARITY_ROOT")
    if env_root:
        return Path(env_root)
    cwd = Path.cwd()
    if (cwd / "PuzzleBuddy").is_dir() and (cwd / "PuzzleBuddy-Android").is_dir():
        return cwd
    if (cwd / "DartBuddy").is_dir() and (cwd / "DartBuddy-Android").is_dir():
        return cwd
    if (cwd / "PuzzleBuddy").is_dir() and (cwd / "app" / "build.gradle.kts").is_file():
        return cwd
    if (cwd / "DartBuddy").is_dir() and (cwd / "app" / "build.gradle.kts").is_file():
        return cwd
    # CI: Android workflow checks out iOS into legacy path names.
    if (cwd / "Dart-Buddy").is_dir() and (cwd / "app" / "build.gradle.kts").is_file():
        return cwd
    if (cwd / "Puzzle-Buddy").is_dir() and (cwd / "app" / "build.gradle.kts").is_file():
        return cwd
    return Path(__file__).resolve().parent.parent


def resolve_ios_path(root: Path, spec: AppPairSpec, monorepo_rel: str, ci_rels: tuple[str, ...]) -> Path:
    monorepo = root / monorepo_rel
    if monorepo.is_file():
        return monorepo
    for rel in ci_rels:
        candidate = root / rel
        if candidate.is_file():
            return candidate
    return monorepo


def resolve_android_path(root: Path, spec: AppPairSpec, monorepo_rel: str) -> Path:
    monorepo = root / monorepo_rel
    if monorepo.is_file():
        return monorepo
    if (root / "app" / "build.gradle.kts").is_file():
        return root / CI_ANDROID_ANALYTICS[spec.name]
    return monorepo


def resolve_crashlytics_path(root: Path, monorepo_rel: str | None, ci_android_rel: str, ci_ios_rels: tuple[str, ...]) -> Path | None:
    if not monorepo_rel:
        return None
    monorepo = root / monorepo_rel
    if monorepo.is_file():
        return monorepo
    if (root / "app" / "build.gradle.kts").is_file():
        return root / ci_android_rel
    for rel in ci_ios_rels:
        candidate = root / rel
        if candidate.is_file():
            return candidate
    return monorepo


def resolve_pair(root: Path, spec: AppPairSpec) -> AppPair:
    ios_analytics = resolve_ios_path(
        root,
        spec,
        spec.ios_analytics_rel,
        (
            "Dart-Buddy/Support/Logging/FirebaseAnalyticsEventMapping.swift",
            "PuzzleBuddy/App/Util/AppLogging.swift",
            "Puzzle-Buddy/Puzzle Buddy/Util/AppLogging.swift",
            "Puzzle Buddy/Util/AppLogging.swift",
            "Support/Logging/FirebaseAnalyticsEventMapping.swift",
        ),
    )
    android_analytics = resolve_android_path(root, spec, spec.android_analytics_rel)
    ios_crashlytics = resolve_crashlytics_path(
        root,
        spec.ios_crashlytics_rel,
        CI_ANDROID_CRASHLYTICS[spec.name],
        (
            "Dart-Buddy/Support/Logging/FirebaseCrashlyticsEventMapping.swift",
            "PuzzleBuddy/App/Util/FirebaseCrashlyticsEventMapping.swift",
            "Puzzle-Buddy/Puzzle Buddy/Util/FirebaseCrashlyticsEventMapping.swift",
            "Puzzle Buddy/Util/FirebaseCrashlyticsEventMapping.swift",
            "Support/Logging/FirebaseCrashlyticsEventMapping.swift",
        ),
    )
    android_crashlytics = None
    if spec.android_crashlytics_rel:
        android_crashlytics = resolve_crashlytics_path(
            root,
            spec.android_crashlytics_rel,
            CI_ANDROID_CRASHLYTICS[spec.name],
            (),
        )
        if not android_crashlytics.is_file() and (root / "app" / "build.gradle.kts").is_file():
            android_crashlytics = root / CI_ANDROID_CRASHLYTICS[spec.name]
    return AppPair(
        name=spec.name,
        ios_analytics=ios_analytics,
        android_analytics=android_analytics,
        ios_analytics_markers=spec.ios_analytics_markers,
        ios_crashlytics=ios_crashlytics,
        android_crashlytics=android_crashlytics,
    )


def read(path: Path) -> str:
    if not path.is_file():
        raise FileNotFoundError(path)
    return path.read_text(encoding="utf-8")


def block_after_marker(content: str, markers: tuple[str, ...]) -> str:
    for marker in markers:
        match = re.search(rf"{re.escape(marker)}\b", content)
        if not match:
            continue
        start = match.end()
        bracket = content.find("[", start)
        setof = content.find("setOf(", start)
        if bracket == -1 and setof == -1:
            continue
        if setof != -1 and (bracket == -1 or setof < bracket):
            opener_index = setof + len("setOf")
            opener = "("
            closer = ")"
        else:
            opener_index = bracket
            opener = "["
            closer = "]"
        depth = 0
        for index in range(opener_index, len(content)):
            char = content[index]
            if char == opener:
                depth += 1
            elif char == closer:
                depth -= 1
                if depth == 0:
                    return content[opener_index : index + 1]
    return ""


def quoted_in_block(content: str, markers: tuple[str, ...]) -> set[str]:
    block = block_after_marker(content, markers)
    return set(QUOTED.findall(block))


def overrides(content: str) -> dict[str, str]:
    block = block_after_marker(content, ("firebaseNameOverrides",))
    pairs = re.findall(r'"([a-z][a-z0-9_]*)"\s*(?:to|:)\s*"([a-z_]+)"', block)
    return dict(pairs)


def crashlytics_events(content: str) -> set[str]:
    return quoted_in_block(content, ("allowlistedLogEvents", "nonFatalEvents"))


def crashlytics_codes(content: str) -> dict[str, int]:
    block = block_after_marker(content, ("eventCodes",))
    return {event: int(code) for event, code in CODE_PAIR.findall(block)}


def diff(left: set[str], right: set[str]) -> tuple[set[str], set[str]]:
    return left - right, right - left


def compare_pair(pair: AppPair) -> list[str]:
    errors: list[str] = []
    ios_analytics_src = read(pair.ios_analytics)
    android_analytics_src = read(pair.android_analytics)

    ios_events = quoted_in_block(ios_analytics_src, pair.ios_analytics_markers)
    android_events = quoted_in_block(android_analytics_src, ("allowlistedEvents", "allowlistedLogEvents"))

    only_ios, only_android = diff(ios_events, android_events)
    if only_ios:
        errors.append(f"{pair.name} analytics: iOS-only events: {sorted(only_ios)}")
    if only_android:
        errors.append(f"{pair.name} analytics: Android-only events: {sorted(only_android)}")

    ios_overrides = overrides(ios_analytics_src)
    android_overrides = overrides(android_analytics_src)
    if ios_overrides != android_overrides:
        errors.append(
            f"{pair.name} analytics: firebaseNameOverrides differ "
            f"(iOS={ios_overrides}, Android={android_overrides})"
        )

    if pair.ios_crashlytics and pair.android_crashlytics:
        ios_crash_src = read(pair.ios_crashlytics)
        android_crash_src = read(pair.android_crashlytics)

        ios_nf = crashlytics_events(ios_crash_src)
        android_nf = crashlytics_events(android_crash_src)
        only_ios, only_android = diff(ios_nf, android_nf)
        if only_ios:
            errors.append(f"{pair.name} crashlytics: iOS-only non-fatals: {sorted(only_ios)}")
        if only_android:
            errors.append(f"{pair.name} crashlytics: Android-only non-fatals: {sorted(only_android)}")

        ios_codes = crashlytics_codes(ios_crash_src)
        android_codes = crashlytics_codes(android_crash_src)
        if ios_codes != android_codes:
            errors.append(
                f"{pair.name} crashlytics: eventCodes differ "
                f"(iOS={ios_codes}, Android={android_codes})"
            )

    return errors


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=None,
        help="Workspace root containing iOS + Android repos (default: auto-detect)",
    )
    parser.add_argument(
        "--pair",
        action="append",
        dest="pairs",
        help='Only check named pair(s), e.g. --pair "Puzzle Buddy"',
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    root = detect_root(args.root)
    specs = PAIR_SPECS
    if args.pairs:
        wanted = set(args.pairs)
        specs = [spec for spec in PAIR_SPECS if spec.name in wanted]
        missing = wanted - {spec.name for spec in specs}
        if missing:
            print(f"Unknown pair(s): {sorted(missing)}", file=sys.stderr)
            return 2

    all_errors: list[str] = []
    resolved: list[AppPair] = [resolve_pair(root, spec) for spec in specs]
    for pair in resolved:
        missing = [p for p in (pair.ios_analytics, pair.android_analytics) if not p.is_file()]
        if missing:
            all_errors.append(f"{pair.name}: missing mapping file(s): {missing}")
            continue
        all_errors.extend(compare_pair(pair))

    if all_errors:
        print("Firebase cross-platform parity check FAILED:\n", file=sys.stderr)
        for error in all_errors:
            print(f"  - {error}", file=sys.stderr)
        print(
            "\nFix both platforms and update docs/telemetry.md. "
            "See workspace/firebase-cross-platform-parity.md",
            file=sys.stderr,
        )
        return 1

    print("Firebase cross-platform parity check passed.")
    for pair in resolved:
        if pair.ios_analytics.is_file() and pair.android_analytics.is_file():
            print(f"  OK  {pair.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
