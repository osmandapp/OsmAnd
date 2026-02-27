# AGENTS.md - AI Agent Guide for OsmAnd Development

This document provides essential information for AI agents working on the OsmAnd Android project.

## 1. Project Overview
OsmAnd (OSM Automated Navigation Directions) is a powerful, open-source map and navigation application based on OpenStreetMap (OSM) data. It supports offline maps, routing, and a wide range of features for travel and outdoor activities.

- **Main Repository:** Multi-module Gradle project.
- **Languages:** Mix of **Java** and **Kotlin**.
- **Native Core:** `OsmAndCore` (C++) providing high-performance rendering and routing via JNI.

## 2. Project Structure
The project is divided into several sub-projects:

- `:OsmAnd`: The main Android application module.
  - `src/`: Java/Kotlin source code.
  - `res/`: Android resources.
  - `assets/`: App assets.
  - `test/`: Instrumentation and Unit tests.
- `:OsmAnd-java`: Core logic in pure Java. Contains data models, rendering logic, routing, and search. Independent of Android APIs.
- `:OsmAnd-api`: API for external applications to interact with OsmAnd.
- `:OsmAnd-shared`: Kotlin Multiplatform (KMP) library shared between Android, iOS, and server (JVM).
- `:plugins`: Contains various plugin sub-projects (e.g., `:plugins:Osmand-Nautical`, `:plugins:Osmand-Skimaps`).

## 3. Shared Code (Kotlin Multiplatform)
The `:OsmAnd-shared` module is a **Kotlin Multiplatform (KMP)** library designed to share logic across Android, iOS, and JVM platforms.

- **Targets:**
  - `androidTarget`: Android-specific implementations.
  - `iosX64`, `iosArm64`, `iosSimulatorArm64`: iOS-specific implementations.
  - `jvm`: Pure Java/Desktop implementations.
- **Source Sets:**
  - `commonMain`: Contains platform-independent logic (serialization, coroutines, data models).
  - `androidMain`, `iosMain`, `jvmMain`: Contain platform-specific implementations (e.g., SQLite drivers, I/O).
- **Key KMP Technologies:**
  - `kotlinx-serialization`: For JSON handling.
  - `kotlinx-coroutines`: For asynchronous programming.
  - `kotlinx-datetime`: For date and time operations.
  - `okio`: For cross-platform I/O.
  - `stately`: For concurrent collections.

## 4. Architecture & Key Components
### Plugin Architecture
OsmAnd uses an internal plugin system to modularize features. 
- **Base Class:** `net.osmand.plus.plugins.OsmandPlugin`.
- **Location:** `OsmAnd/src/net/osmand/plus/plugins`.
- Plugins can hook into:
  - Map layers (`registerLayers`)
  - Widgets (`createWidgets`)
  - Context menus (`registerMapContextMenuActions`)
  - Settings (`getPreferences`, `getSettingsScreenType`)

### Main Classes
- **Application:** `net.osmand.plus.OsmandApplication` - central entry point, provides access to managers and settings.
- **Main Activity:** `net.osmand.plus.activities.MapActivity` - the main map view.
- **Settings:** `net.osmand.plus.settings.backend.OsmandSettings` - central point for managing preferences.
- **Base Fragment:** `net.osmand.plus.base.BaseOsmAndFragment` - the mandatory base class for new fragments.

### Core Services
- `NavigationService`: Handles background navigation.
- `DownloadService`: Manages map and resource downloads.
- `OsmandAidlService`: Provides an AIDL interface for external apps.

## 5. Key Technologies
- **Target SDK:** 35 (Compile SDK: 35, Min SDK: 24).
- **Kotlin 2.1.10** & **Java 17**.
- **AGP 8.7.3**.
- **UI Framework:** Primarily **standard Android Views** (XML layouts).
- **Logging:** `org.apache.commons.logging` via `PlatformUtil`.
- **SQLDelight:** Used for some database operations.
- **Rhino (JavaScript engine):** Used for voice guidance and other scripts.
- **JTS (JTS Topology Suite):** Used for geometric operations.
- **Picasso:** For image loading.
- **MPAndroidChart:** Custom version used for various charts (e.g., elevation).

## 6. Development Workflow
### Building the Project
Use Gradle to build the project.
- Build Debug APK: `./gradlew :OsmAnd:assembleDebug`
- List of Flavors: `nightlyFree`, `androidFull`, `gplayFree`, `gplayFull`, `huawei`.
- ABI Dimensions: `armv7`, `arm64`, `x86`, `fat`.
- Core Dimensions: `legacy` (no OpenGL), `opengl`.

### Testing
- **Unit & Instrumentation Tests:** Located in `OsmAnd/test/java`.
- Run tests: `./gradlew :OsmAnd:connectedDebugAndroidTest`

### External Resources
Many resources (icons, fonts, voice files) are not in the main `res` folder but are collected from `../../resources` during the build process via the `collectExternalResources` task.

## 7. Coding Standards & Best Practices
- **UI Design:** Follow **Google Material Design** and standard **Android development guidelines** for all new layouts and components.
- **Logging:** Use `PlatformUtil.getLog(YourClass.class)` for logging. Do not use `android.util.Log` directly in core classes to maintain portability.
- **Dependency Injection:** The project uses manual dependency injection. Key services and helpers are accessible via `OsmandApplication`.
- **Theming & Resources:** When creating or modifying UI, ensure consistency by utilizing and updating existing resource files:
  - `dimens.xml` and `sizes.xml` for dimensions and spacing.
  - `colors.xml` for application colors and palette.
  - `attrs.xml` for custom theme attributes.
- **Fragments:** All new fragments must extend `net.osmand.plus.base.BaseOsmAndFragment` (or its specialized subclasses like `BaseFullScreenFragment` or `BaseNestedFragment`) to ensure proper theming, application service access, and consistent lifecycle handling.
- **Strings:** All new user-visible strings must be added to the **beginning** of `OsmAnd/res/values/strings.xml` to support localization and simplify translation management. Avoid hardcoding strings in code or layouts.
- **Prefer Kotlin** for new UI code and modern components.
- **Keep core logic** in `OsmAnd-shared` (KMP) or `OsmAnd-java` where possible to maintain platform independence.
- **Use the Plugin system** for new optional features.
- **Follow existing style:** OsmAnd has a long history, so consistency with existing code is crucial.
- **Resource handling:** Be aware that many resources are dynamically collected; check `OsmAnd/build-common.gradle` for details.

## 8. Common Tasks for Agents
- **Adding a new Setting:** Register it in `OsmandSettings` and add it to the relevant settings fragment.
- **Creating a new Fragment:** Extend `BaseOsmAndFragment` and implement required UI logic using Material Design principles.
- **Modifying Map UI:** Look into `net.osmand.plus.views.OsmandMapTileView` and its layers.
- **Extending Search:** Check `net.osmand.plus.search` and `net.osmand.search.core`.
- **Updating Plugins:** Most plugin-specific code is in `net.osmand.plus.plugins`.

## 9. Restrictions
- **Building Gradle project:** YOU MUST NEVER run Gradle buid task by yourself! EVEN for verifying build errors!!!

---
*Note: This file is a living document and should be updated as the project evolves.*
