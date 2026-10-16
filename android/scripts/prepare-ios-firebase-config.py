#!/usr/bin/env python3
"""로컬 환경변수로 Git에서 제외한 Firebase iOS 설정을 생성한다."""

import argparse
import os
from pathlib import Path
import plistlib
import sys


def main():
    android_root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--template",
        type=Path,
        default=android_root / "iosApp/Chaekchaek/GoogleService-Info.plist.example",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=android_root / "iosApp/Chaekchaek/GoogleService-Info.plist",
    )
    args = parser.parse_args()
    api_key = os.environ.get("FIREBASE_IOS_API_KEY", "").strip()
    if not api_key:
        parser.error("FIREBASE_IOS_API_KEY 환경변수를 설정하세요. 값은 출력하지 않습니다.")
    try:
        config = plistlib.loads(args.template.read_bytes())
        config["API_KEY"] = api_key
        rendered = plistlib.dumps(config, sort_keys=False)
        # Existing local credentials must survive an accidental second invocation.
        with args.output.open("xb") as output:
            os.chmod(args.output, 0o600)
            output.write(rendered)
    except FileExistsError:
        parser.exit(1, "기존 설정을 유지합니다. 교체가 필요하면 먼저 로컬 파일을 별도로 보관하세요.\n")
    except (OSError, ValueError, TypeError, plistlib.InvalidFileException):
        parser.exit(1, "설정을 생성하지 못했습니다. 템플릿과 출력 경로를 확인하세요.\n")
    print("Firebase iOS 로컬 설정을 생성했습니다. 이 파일은 커밋에서 제외됩니다.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
