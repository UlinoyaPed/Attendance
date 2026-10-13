#!/usr/bin/env bash
# Sign the already-built, aligned release APK. Private inputs exist only in this step.
set -euo pipefail

# These are generated outputs; a rerun must not mistake an old signature for this build.
rm -f dist/Attendance-nightly.apk dist/SIGNING-CERTIFICATE.txt

secrets=(SIGN_KEY_BASE64 SIGN_KEY_ALIAS SIGN_KEY_PWD)
configured=0
missing=()
for name in "${secrets[@]}"; do
  if [[ -n "${!name:-}" ]]; then
    configured=$((configured + 1))
  else
    case "$name" in
      SIGN_KEY_BASE64) missing+=(SIGNING_KEY) ;;
      SIGN_KEY_ALIAS) missing+=(KEY_ALIAS) ;;
      SIGN_KEY_PWD) missing+=(KEY_PASSWORD) ;;
    esac
  fi
done
if [[ "$configured" == 0 ]]; then
  echo "No signing secrets configured; publishing unsigned APK only."
  exit 0
fi
if [[ ${#missing[@]} != 0 ]]; then
  printf 'Incomplete signing configuration; missing Secret: %s\n' "${missing[@]}" >&2
  exit 1
fi

sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
if [[ -z "$sdk_root" || ! -d "$sdk_root/build-tools" ]]; then
  echo "Android SDK build-tools not found." >&2
  exit 1
fi
apksigner=$(find "$sdk_root/build-tools" -type f -name apksigner | sort -V | tail -n 1)
if [[ ! -x "$apksigner" ]]; then
  echo "apksigner not found in installed build-tools." >&2
  exit 1
fi

umask 077
signing_dir=$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/attendance-sign.XXXXXX")
trap 'rm -rf "$signing_dir"' EXIT
printf '%s' "$SIGN_KEY_BASE64" | base64 --decode > "$signing_dir/signing.keystore"

# Passwords are read from the environment, never passed as command-line text.
"$apksigner" sign --ks "$signing_dir/signing.keystore" --ks-key-alias "$SIGN_KEY_ALIAS" \
  --ks-pass env:SIGN_KEY_PWD --key-pass env:SIGN_KEY_PWD \
  --out "$signing_dir/Attendance-nightly.apk" dist/Attendance-nightly-unsigned.apk
"$apksigner" verify --verbose "$signing_dir/Attendance-nightly.apk"
"$apksigner" verify --print-certs "$signing_dir/Attendance-nightly.apk" \
  | awk '/certificate SHA-256 digest:/ { print }' > "$signing_dir/SIGNING-CERTIFICATE.txt"

# Only verified APKs and public certificate fingerprints enter the artifact directory.
cp "$signing_dir/Attendance-nightly.apk" dist/Attendance-nightly.apk
cp "$signing_dir/SIGNING-CERTIFICATE.txt" dist/SIGNING-CERTIFICATE.txt
