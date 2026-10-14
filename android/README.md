# gym assystant — Android

Native Kotlin/Compose Material 3 app, application ID `it.matteogrechi.gymassystant`, version 0.1.0, minimum Android 8/API26 and target API36. `app/` owns Android UI, Room, local media and file adapters; `domain/` owns platform-independent plans, schedules, execution, clocks and statistics. The [specification](../docs/product/spec.md) and [traceability](../docs/product/traceability.md) distinguish actual native verification from the browser reference.

Runtime works entirely offline: no Internet permission, account, cloud sync or remote assets. Fonts/icons/translations are bundled. Starter plans are editable prescriptions; real performance history starts empty. Completed working sets enter statistics immediately, including unfinished sessions; warm-ups remain separate. Finished logs are read-only.

The interface follows the [mockup](../prototypes/web/README.md) through [shared components](../docs/design/native-ui-components.md). Timers and metric selectors use the same component; Plan and Exercises use the same library controls. The app has no simulated phone frame or development view IDs.

## Build and install

To try a built debug APK on your phone, copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and open it in a file manager. If Android asks, allow that file manager to install unknown apps, then install **gym assystant**. This is the native app; the browser mockup is a separate preview. Updates installed with the same debug signing key preserve the local log; uninstalling removes app-private data, so export your JSON/media first.

Install JDK17 and an Android SDK with `platforms;android-36`, `build-tools;35.0.0` and `platform-tools`. Set `JAVA_HOME` and `ANDROID_HOME`, or configure the SDK with an ignored `local.properties`. Open this directory in Android Studio or run:

```sh
./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The wrapper pins Gradle8.13 with its official SHA-256; the [version catalog](gradle/libs.versions.toml) pins compatible AGP8.13.2/Kotlin2.2.20/Compose/Room/Media3 versions. Build dependency acquisition uses connectivity during development; the installed app requires none. Subsequent Gradle builds can use `--offline` after all required dependencies are cached. Debug signing is for local installation; public release signing/publication is a separate decision.

Run device checks with an emulator or connected Android device:

```sh
./gradlew :app:connectedDebugAndroidTest
```

[OfflineWorkflowTest](app/src/androidTest/kotlin/it/matteogrechi/gymassystant/OfflineWorkflowTest.kt) uses an isolated test dataset; do not run it against a personal production dataset. Native visual/device results and unverified platform cases are recorded in [the native review](../docs/reviews/native-implementation-review.md).

## Local files and recovery

Room commits are authoritative and survive relaunch. Settings → Change folder uses Android's system picker and retained permissions for tested local internal/SD shared storage. Other/unverifiable providers are rejected so cloud storage cannot undermine offline operation. A folder failure leaves the private log intact and exposes pending backup/retry.

Full JSON generations retain all raw plans/drafts, original dated workouts, sets/setup, attendance, settings and portable paused timer snapshots. CSV contains derived performance/attendance; TXT describes the dictionary. Imported videos are copied into app-owned files and backed up separately. Removing their original source does not break playback. Use the secondary Exercise video action to select your own local clip; no sample technique clips or streaming are included. Maximum imported video size is 512MiB; portable text exports are bounded to 16MiB.

See [native-file-layout.md](../docs/architecture/native-file-layout.md) for exact schemas, verification and retention. A URI in an export grants no access. No restore/import UI or legacy-data migration is included; the text files remain recoverable with ordinary JSON/CSV tools. Definition editing/deletion, extra execution working sets, timer alerts, history editing/reopening and coaching remain outside the approved scope.
