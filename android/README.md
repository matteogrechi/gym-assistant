# Android project — implementation planning

The user has requested native **gym assystant** implementation with entirely offline operation and a detailed design presentation before code. That plan is in [android-implementation-plan.md](../docs/architecture/android-implementation-plan.md). The proposed stack is Kotlin, Compose/Material 3, Room, Media3 local playback and open text recovery files.

Native code has not started: this directory contains no sources, manifest, resources or Gradle scaffold. The first build milestone will establish the documented app/domain modules and toolchain after the plan presentation. Product choices explicitly marked pending remain proposals, not approved extra features.

Use [specification revision 33](../docs/product/spec.md), [traceability](../docs/product/traceability.md), [draft native data contract](../docs/architecture/native-data-v3.md) and [repository layout](../docs/architecture/repository-layout.md). The browser mockup remains a separate [design reference](../prototypes/web/README.md).
