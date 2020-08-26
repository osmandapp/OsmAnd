# Building OsmAnd

This document describes how to set up your development environment to build and run OsmAnd.

- [Prerequisite Software](#prerequisite-software)
- [Getting the Sources](#getting-the-sources)
- [Building](#building)

## Prerequisite Software

Before you can build and run OsmAnd, you must install and configure the
following products on your development machine:

- [Git](http://git-scm.com) and/or the **GitHub app** (for [Mac](http://mac.github.com) or
  [Windows](http://windows.github.com)); [GitHub's Guide to Installing
  Git](https://help.github.com/articles/set-up-git) is a good source of information.

- [git-repo](https://gerrit.googlesource.com/git-repo) to fetch all dependencies required to build OsmAnd.

- [Android Studio](https://developer.android.com/studio), Android IDE.

## Getting the Sources

Clone required repositories using `repo`:

```shell
repo init -u https://github.com/osmandapp/OsmAnd-manifest -m readonly.xml
repo sync
```

## Building

### Simplest solution (no native code)

The simplest way to build OsmAnd is by skipping NDK usage and native code (C++) compilation.

To build and install OsmAnd run:

```sh
unset ANDROID_NDK
cd android
# Either build for all archs (big APK)
./gradlew -Dorg.gradle.jvmargs="-Xmx4096m" OsmAnd:cleanNoTranslate OsmAnd:installFreedevLegacyFatDebug
# Or for emulator only
./gradlew -Dorg.gradle.jvmargs="-Xmx4096m" OsmAnd:cleanNoTranslate OsmAnd:installFreedevLegacyX86Debug
# Or select proper arch for real device (Arm64, Armv7 or Armonly)
./gradlew -Dorg.gradle.jvmargs="-Xmx4096m" OsmAnd:cleanNoTranslate OsmAnd:installFreedevLegacyArm64Debug
```

This will generate an APK (located in `OsmAnd/build/outputs/apk` folder) and install it on any plugged in device/emulator.

### With NDK

To build with NDK (heavier but improves performance), you have to:

1. [Download the Android NDK](https://developer.android.com/ndk/downloads). Ensure to download the same version as in [build.gradle](https://github.com/osmandapp/OsmAnd/blob/master/OsmAnd/build.gradle#L27).
2. Export `ANDROID_NDK` environment variable eg: `export ANDROID_NDK=/path/to/android-ndk-r17b`
3. Run `./gradlew -Dorg.gradle.jvmargs="-Xmx4096m" OsmAnd:cleanNoTranslate OsmAnd:installFreedevLegacyFatDebug` or similar (see above).
