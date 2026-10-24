# Repository split

The clients are maintained at **namedkenzo/NamedClients**; the web panel and Linux server remain at **namedkenzo/NamedICMP**. Protocol compatibility, not repository layout, connects the projects.

## Windows

- Original UI, mascot, VPN guardian and client engine extracted from server release 0.3.0.
- Standalone `namedp` Go module. No server/panel/store dependency or server compression dependency.
- Windows source remains version 0.1.0-alpha. Future releases use this repository.
- Existing releases in the old repository remain historical downloads; they are not deleted or silently replaced.

## Validation

Go unit/security/reliability tests and Windows compile/render checks run here. For cross-repository compatibility, CI checks out server commit `19843b1aab9b1ebaefd031d54e0892ceb69bd8f4` into an ignored `.compat-server` directory and overlays the current client/codec/reliability sources. The server reference's original real-core integration suite then exercises numeric/AES/ChaCha20, TCP loss/ordering/half-close and multiple UDP destinations.

No reference-server source is shipped in client releases. The checkout is a test fixture only. Public VPS and actual Windows VPN acceptance remain separate, uncompleted checks.

## Android

Development location reserved; no app/APK implemented yet. The public Windows workflow does not build or publish Android. The owner's earlier personal-delivery request remains unresolved for Android because this destination repository is public.
