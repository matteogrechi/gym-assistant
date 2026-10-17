# Android distribution for gym assystant

GYM-029: `.github/workflows/android-release.yml` runs on every push to `main`, without path filters or cancellation. Manual dispatch is accepted only on `main`. It checks out the triggering SHA, runs `:domain:test`, `:app:testReleaseUnitTest` and `:app:lintRelease`, then builds and verifies signed release APK/AAB files. The installed app remains entirely offline; CI needs network access for build tools and dependencies.

## Validate locally before a workflow push

Use the same workflow with `act` and rootless Podman before pushing workflow changes. The shared `.github/scripts/publish-release.py` is used locally and in CI; its seven standard-library regressions include fresh release creation, real CLI-style draft discovery, partial uploads, retry preservation, conflicting commits, bad checksums and the read-only dry-run boundary.

Install `act` (the checked execution used version 0.2.89) and start a rootless Podman API socket. Set `DOCKER_HOST` to its Unix socket, `ANDROID_HOME` to an installed SDK with platform36/build-tools35.0.0, and `GRADLE_USER_HOME` to a writable cache. Supply a private file outside the repository with `GITHUB_TOKEN`, `ANDROID_SIGNING_KEYSTORE_BASE64` and `ANDROID_SIGNING_KEYSTORE_PASSWORD`; directory mode700/file mode600. Use a token authorized for this repository. Never print or commit its values. The launcher builds `.github/local/Containerfile` if its image is absent, then runs:

```sh
.github/scripts/check-local-release.sh /private/secrets.env 4
```

The numeric argument is the simulated workflow run number; choose an unused number to exercise fresh-tag handling. It sets the matching release code, runs native tests/lint and actual signed APK/AAB builds, verifies their signatures, and executes real GitHub reads with publication forced to dry-run. It creates no remote release or tag. The SDK/cache are mounted into the local container; the private key directory and host GitHub configuration are not mounted. The local image makes `/var/run` a real directory because Podman's archive API rejects `act` copies through Ubuntu's default symlink.

Locally, `act` optimizes the standard checkout step by copying the current working tree, retaining unpublished fixes. Hosted checkout uses its documented default: the ref/SHA that triggered the workflow. The pinned checkout action already succeeded in hosted runs and the earlier unoptimized local attempt, which correctly restored older remote source. Local containers differ from GitHub VMs and use the supplied token rather than a job-scoped token; a successful hosted run remains the final verification of runner and token-policy behavior. Retain that limitation rather than claiming local validation guarantees all GitHub behavior.

## Version and signing contract

CI uses Ubuntu 24.04, Temurin JDK 17, the checked-in Gradle 8.13 wrapper, SDK platform 36 and build tools 35.0.0. Only official checkout/setup-java actions are used, pinned to the verified release commits of [checkout v7.0.1](https://github.com/actions/checkout/releases/tag/v7.0.1) and [setup-java v6.0.1](https://github.com/actions/setup-java/releases/tag/v6.0.1). There are no dependency cache or Actions artifact uploads.

| Input | Contract |
| --- | --- |
| Gradle `-PreleaseVersionCode=N` | Integer 1001–2100000000; release code `N`, name `0.1.N`. CI sets `N = 1000 + github.run_number`. Debug remains code 1/name 0.1.0. |
| `ANDROID_RELEASE_KEYSTORE` | Absolute path to the production PKCS12 keystore. |
| `ANDROID_RELEASE_KEYSTORE_PASSWORD` | Same password for the store and private key; alias is exactly `gym-assystant`. |
| GitHub `ANDROID_SIGNING_KEYSTORE_BASE64` | Base64 encoding of that keystore, provided as a repository Actions secret. Base64 is encoding, not encryption. |
| GitHub `ANDROID_SIGNING_KEYSTORE_PASSWORD` | Repository Actions secret mapped to the Gradle password environment variable only in the signing step. |

The owner has configured the dedicated production key and both GitHub secrets. CI decodes the key into a runner temporary file with owner-only permissions, removes it on step exit, and does not include it in release assets. Never print secrets, enable shell tracing, commit the key, or pass the password as a command-line argument. Debug builds work without release credentials; without them a local release build is unsigned and must not be distributed. Both signing environment variables must be supplied together.

The local key is `.signing/gym-assystant.p12` and its password file is `.signing/keystore-password.txt`, under the ignored repository-root `/.signing/` directory, with owner-only permissions. Keep an encrypted offline backup of the keystore and password in separate protected locations. The dedicated certificate SHA-256 is `0A:0C:4F:DE:6A:AE:33:E0:4E:17:B4:79:4D:9C:0E:EC:D7:0B:D8:3E:3F:0C:7B:7A:DA:65:AA:0A:FB:AB:0B:2F`. The owner reports adding this fingerprint in Play Console; this does not establish that package registration or Play App Signing enrollment is complete. Verify its certificate SHA-256 fingerprint using `keytool -list -v -storetype PKCS12 -keystore <path> -alias gym-assystant` (interactive password prompt). Losing the production key prevents future compatible sideloaded updates.

For a local signed build, set the two environment variables securely, choose an unused increasing code, and run from `android/`:

```sh
./gradlew :domain:test :app:testReleaseUnitTest :app:lintRelease \
  :app:assembleRelease :app:bundleRelease -PreleaseVersionCode=1001
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

Do not independently publish a local code that a future CI run will reuse. Keep this workflow's run-number sequence: replacing it with a new workflow can reset numbering; coordinate an increased offset before doing so. Rerunning an existing run keeps its code; a new manual dispatch gets a new one.

## Releases and safe retry

The workflow has only `contents: write` token permissions and disables checkout credential persistence. Version tag `v0.1.N` points to the exact triggering SHA, even when `main` advances during the build. APK/AAB assets are named `gym-assystant-0.1.N.apk` / `.aab`, with `SHA256SUMS`. The release body records the full SHA. Publishing proceeds through a draft after all checks pass, with a final signature and commit-identity check. No Play upload occurs.

A retry refuses a tag or release targeting another SHA. It reuses existing asset bytes, verifies their signatures/checksums, uploads only missing assets, and publishes a completed draft. It never overwrites remote assets or moves tags; a mismatch fails for owner investigation. A failed upload may leave a tag/draft that the same run can resume. Concurrent distinct runs use distinct versions; there is no concurrency group that discards queued pushes. The workflow leaves latest-release selection to GitHub's automatic date/version rules rather than forcing an out-of-order completion to become latest. GitHub release/tag administration remains privileged: do not manually retarget these tags.

## Hosting visibility and installation

The owner explicitly authorized public visibility, verified on 2026-10-14. The repository is public; the workflow uses free standard Ubuntu hosted runners and public GitHub Release downloads. This workflow never changes visibility. [GitHub's billing documentation](https://docs.github.com/en/billing/concepts/product-billing/github-actions) states that standard hosted runners are free for public repositories. Private repositories consume the owner's monthly allowance (currently 2,000 minutes on GitHub Free); further usage can be billed or blocked depending on billing settings. To keep private operation free, monitor usage and configure a zero-spend budget that stops usage. No Actions artifact storage is used. [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases) hosts these download assets separately; private releases require repository read access, so they are not public distribution. The public visibility choice exposes source and Git history; it leaves encrypted Actions secret values private.

Download the APK and `SHA256SUMS` from the desired release. On a computer, place the release assets together and run `sha256sum --check SHA256SUMS` (also download the AAB for both lines to pass). Copy the APK to the phone, open it from a file manager, allow that app to install unknown apps when Android asks, and install **gym assystant**. The AAB is for Play submission, not direct installation. Alternatively use `adb install -r <release.apk>`.

An update needs the same application ID/signing identity and a compatible version code. A debug-signed installation cannot update to this production signature. Before uninstalling debug, export and protect the full JSON generations and copied media outside app-private storage, verify the files exist and are readable, and retain originals. Uninstall removes private data. **There is no restore/import UI:** exports remain readable with ordinary tools but cannot currently refill the app, so switching from debug means starting fresh in the release app. Do not uninstall expecting automatic restoration.

## Manual Google Play submission

The owner already has a Play account. Submit the release AAB manually after completing Play Console's required listings, declarations and testing/review steps; CI does not automate them. Version codes must increase relative to every previously uploaded Play build, including test tracks.

For compatible updates between the sideloaded APK and Play, provide this **own production app signing key during the first Play App Signing enrollment**, before a release rolls out to open testing or production. Do not accept a different Google-generated app signing identity by default. Follow Play's encrypted key-transfer instructions and compare the Play **app signing certificate** SHA-256 fingerprint with the production keystore/APK fingerprint. [Google's Play App Signing documentation](https://support.google.com/googleplay/android-developer/answer/9842756) explains own-key enrollment and cross-store distribution; console defaults and signing options can change, so confirm the identity before rolling out.

An **upload key** authenticates submitted AABs; the **app signing key** signs APKs installed from Play. Merely registering this keystore as an upload key does not establish the same installed identity. A separate upload key is recommended by Google; if adopted, sign manual Play submissions with it while keeping this CI's sideloaded APK production key unchanged. No separate upload-key contract is added here.
