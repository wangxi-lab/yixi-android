#!/usr/bin/env bash
set -euo pipefail

apk_path="${1:-app/build/outputs/apk/debug/app-debug.apk}"

if [[ ! -f "$apk_path" ]]; then
  echo "APK not found: $apk_path" >&2
  exit 1
fi

permissions="$(apkanalyzer manifest permissions "$apk_path")"
echo "$permissions"

for forbidden in \
  android.permission.INTERNET \
  android.permission.ACCESS_NETWORK_STATE \
  android.permission.PACKAGE_USAGE_STATS \
  android.permission.QUERY_ALL_PACKAGES \
  android.permission.SYSTEM_ALERT_WINDOW \
  android.permission.READ_EXTERNAL_STORAGE \
  android.permission.WRITE_EXTERNAL_STORAGE; do
  if grep -qF "$forbidden" <<<"$permissions"; then
    echo "Forbidden permission found in APK: $forbidden" >&2
    exit 1
  fi
done

echo "APK privacy permission check passed."
