#!/usr/bin/env bash
set -euo pipefail
# Never cache a private signing key in a public repository's Actions cache.
# First release bootstrap was approved by the owner after the legacy key loss.
task_signing_dir=$(mktemp -d "${RUNNER_TEMP:?}/namedp-signing-XXXXXX")
chmod 700 "$task_signing_dir"
task_store="$task_signing_dir/namedP-release.p12"
if [[ -n "${NAMEDP_KEYSTORE_BASE64:-}" ]]; then
  test -n "${NAMEDP_STORE_PASSWORD:-}"
  printf '%s' "$NAMEDP_KEYSTORE_BASE64" | base64 --decode > "$task_store"
  export NAMEDP_KEY_ALIAS="${NAMEDP_KEY_ALIAS:-namedp}"
  export NAMEDP_KEY_PASSWORD="${NAMEDP_KEY_PASSWORD:-$NAMEDP_STORE_PASSWORD}"
else
  task_version=$(node -e 'process.stdout.write(require("fs").readFileSync("android/app/build.gradle.kts","utf8").match(/versionName = "([^"]+)"/)[1])')
  if [[ "$task_version" != 1.0.0 ]] || gh release view namedp-v1.0.0 >/dev/null 2>&1; then
    echo '::error::A published signing identity already exists or bootstrap is not version 1.0.0. Configure the owner signing secrets; refusing to generate an incompatible key.'
    exit 1
  fi
  export NAMEDP_STORE_PASSWORD=$(openssl rand -base64 36)
  export NAMEDP_KEY_PASSWORD="$NAMEDP_STORE_PASSWORD"
  export NAMEDP_KEY_ALIAS=namedp
  echo "::add-mask::$NAMEDP_STORE_PASSWORD"
  keytool -genkeypair -keystore "$task_store" -storetype PKCS12 -storepass:env NAMEDP_STORE_PASSWORD -keypass:env NAMEDP_KEY_PASSWORD -alias "$NAMEDP_KEY_ALIAS" -dname 'CN=namedP, O=namedkenzo' -keyalg RSA -keysize 3072 -validity 10000
  # Only an authenticated encrypted envelope leaves the runner. The recipient's
  # private key is held in the owner's task workspace, never in this repository.
  printf '%s' "$NAMEDP_STORE_PASSWORD" > "$task_signing_dir/store-password.txt"
  printf '%s' "$NAMEDP_KEY_ALIAS" > "$task_signing_dir/key-alias.txt"
  tar -cf "$task_signing_dir/backup.tar" -C "$task_signing_dir" namedP-release.p12 store-password.txt key-alias.txt
  openssl cms -encrypt -aes-256-gcm -binary -in "$task_signing_dir/backup.tar" -outform PEM -out "$task_signing_dir/backup.cms.pem" .github/signing-backup-recipient.pem
  echo "NAMEDP_ENCRYPTED_SIGNING_BACKUP=$(base64 -w0 "$task_signing_dir/backup.cms.pem")"
  echo 'Generated approved 1.0.0 signing identity; encrypted recovery envelope recorded for the owner.'
fi
chmod 600 "$task_store"
echo "::add-mask::$NAMEDP_STORE_PASSWORD"
echo "::add-mask::$NAMEDP_KEY_PASSWORD"
{
  echo "NAMEDP_SIGNING_STORE=$task_store"
  echo "NAMEDP_STORE_PASSWORD=$NAMEDP_STORE_PASSWORD"
  echo "NAMEDP_KEY_PASSWORD=$NAMEDP_KEY_PASSWORD"
  echo "NAMEDP_KEY_ALIAS=$NAMEDP_KEY_ALIAS"
} >> "$GITHUB_ENV"
