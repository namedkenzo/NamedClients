# Android signing and recovery

The old alpha workflow tried to cache `~/.android/debug.keystore`, but Gradle used
`~/.config/.android/debug.keystore`. Its private key was not preserved. After the
owner's explicit approval, 1.0.0 starts a new identity; alpha users must copy their
links before a one-time uninstall/reinstall. An APK certificate cannot recover its private key.

## New identity

CI generates a new RSA-3072 PKCS12 identity only for the approved first 1.0.0 build
while no stable 1.0.0 release exists. The private key is never committed, attached
to a public release, printed, or placed in a public Actions cache.
An authenticated AES-256-GCM CMS envelope backs it up to the public recipient
certificate in `.github/signing-backup-recipient.pem`; only the owner's private
workspace holds the corresponding decryption key. The public envelope alone
cannot be used to sign an APK. Candidate branch keys are not published identities.

The owner must retain the recovered **release build's** PKCS12 key, password and
alias in a private offline backup. Future updates require repository Actions secrets:

| Secret | Value |
| --- | --- |
| `NAMEDP_ANDROID_KEYSTORE_BASE64` | Base64 of the recovered `namedP-release.p12` file |
| `NAMEDP_ANDROID_STORE_PASSWORD` | Recovered store password |
| `NAMEDP_ANDROID_KEY_ALIAS` | Recovered alias (`namedp`) |
| `NAMEDP_ANDROID_KEY_PASSWORD` | Same recovered password for the PKCS12 key |

Set these in **Settings → Secrets and variables → Actions → New repository secret**.
Do not paste the key/password in chat, issues, workflow logs or repository files.
The workflow does not have permission to create these owner secrets automatically.
Missing secrets after the first stable release stop the build; it cannot silently
replace the signing identity. Certificate checks compare future APKs with the
stable 1.0.0 asset. Store the backup before discarding the private task workspace.

The fixture upgrade test verifies that profiles survive an in-place update when
both APKs use the same identity. It does not claim that alpha profiles migrate
across the explicitly approved certificate change.
