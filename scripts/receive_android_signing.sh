#!/usr/bin/env bash
# Sourced by prepare_android_signing.sh. Private material remains on this runner;
# the repository carries only an ephemeral public recipient and encrypted payload.
set -euo pipefail
task_handoff_branch=signing-handoff
task_request=".signing-handoff/request-${GITHUB_RUN_ID:?}.pem"
task_payload=".signing-handoff/payload-${GITHUB_RUN_ID}.pem"
openssl req -x509 -newkey rsa:3072 -noenc -quiet -keyout "$task_signing_dir/recipient-private.pem" -out "$task_signing_dir/recipient-public.pem" -subj "/CN=namedP-signing-run-$GITHUB_RUN_ID" -days 1
if ! gh api "repos/$GITHUB_REPOSITORY/git/ref/heads/$task_handoff_branch" >/dev/null 2>&1; then
  gh api --method POST "repos/$GITHUB_REPOSITORY/git/refs" -f "ref=refs/heads/$task_handoff_branch" -f "sha=$GITHUB_SHA" >/dev/null
fi
gh api --method PUT "repos/$GITHUB_REPOSITORY/contents/$task_request" -f "branch=$task_handoff_branch" -f "message=Ephemeral signing recipient for run $GITHUB_RUN_ID ($GITHUB_SHA)" -f "content=$(base64 -w0 "$task_signing_dir/recipient-public.pem")" >/dev/null
echo "Signing handoff ready: branch=$task_handoff_branch request=$task_request run=$GITHUB_RUN_ID sha=$GITHUB_SHA"
task_received=false
for ((task_attempt=0;task_attempt<96;task_attempt++)); do
  if gh api "repos/$GITHUB_REPOSITORY/contents/$task_payload?ref=$task_handoff_branch" --jq .content > "$task_signing_dir/payload.b64" 2>/dev/null; then task_received=true;break;fi
  sleep 5
done
if [[ "$task_received" != true ]]; then echo '::error::Owner signing secrets or an encrypted owner handoff are required. No replacement identity was generated.';exit 1;fi
base64 --decode "$task_signing_dir/payload.b64" > "$task_signing_dir/payload.pem"
openssl cms -decrypt -binary -inform PEM -in "$task_signing_dir/payload.pem" -recip "$task_signing_dir/recipient-public.pem" -inkey "$task_signing_dir/recipient-private.pem" -out "$task_signing_dir/incoming.tar"
task_names=$(tar -tf "$task_signing_dir/incoming.tar" | sort)
[[ "$task_names" == $'key-alias.txt\nnamedP-release.p12\nstore-password.txt' ]]
tar -tvf "$task_signing_dir/incoming.tar" | awk 'substr($0,1,1)!="-" {exit 1}'
tar -xf "$task_signing_dir/incoming.tar" -C "$task_signing_dir" namedP-release.p12 store-password.txt key-alias.txt
export NAMEDP_STORE_PASSWORD="$(tr -d '\r\n' < "$task_signing_dir/store-password.txt")"
export NAMEDP_KEY_PASSWORD="$NAMEDP_STORE_PASSWORD"
export NAMEDP_KEY_ALIAS="$(tr -d '\r\n' < "$task_signing_dir/key-alias.txt")"
echo "::add-mask::$NAMEDP_STORE_PASSWORD"
keytool -list -keystore "$task_store" -storetype PKCS12 -storepass:env NAMEDP_STORE_PASSWORD -alias "$NAMEDP_KEY_ALIAS" >/dev/null
echo 'Existing owner signing identity recovered through the encrypted one-run handoff.'
