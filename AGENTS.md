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
- `:plugins`: Contains various plugin sub-projects (e.g., `:plugins:Osmand-Nautical`, `:plugins:Osmand-Skimaps`, and APRS driver sources under `plugins/aprs-driver/` compiled into `:OsmAnd`).

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
- **Built-in plugins:** `OsmAnd/src/net/osmand/plus/plugins` (AIS, weather, etc.).
- **APRS driver plugin:** `OsmAnd/plugins/aprs-driver/` (merged into `:OsmAnd` via source sets; requires `aprs-core`).
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
- **Android API compatibility** The app must work on every supported Android version starting from minSdk. Do not call newer platform APIs unless they are guarded by an SDK-version check or accessed through a compatible AndroidX/helper API.

## 8. Common Tasks for Agents
- **Adding a new Setting:** Register it in `OsmandSettings` and add it to the relevant settings fragment.
- **Creating a new Fragment:** Extend `BaseOsmAndFragment` and implement required UI logic using Material Design principles.
- **Modifying Map UI:** Look into `net.osmand.plus.views.OsmandMapTileView` and its layers.
- **Extending Search:** Check `net.osmand.plus.search` and `net.osmand.search.core`.
- **Updating Plugins:** Most plugin-specific code is in `net.osmand.plus.plugins`.

## 9. Restrictions
- **Building Gradle project:** YOU MUST NEVER run Gradle build task by yourself! EVEN for verifying build errors!!!
- **No manual Gradle build emulation:** YOU MUST NEVER reconstruct an Android/Gradle compilation classpath from cached JARs, transformed dependencies, generated classes, or build directories, and MUST NEVER invoke `kotlinc`, `javac`, or similar tools to compile a full Android fragment, source set, module, or project as a substitute for Gradle. Do not spend time resolving cascading classpath errors. Unless the user explicitly requests additional verification, use only fast, targeted checks such as `git diff --check`, XML/resource syntax validation, or isolated tests that have a direct, already-available runtime.
- **New files for git** When creating source, resource, or documentation files intended for the change, add them to VCS. Do not add temporary, generated, local, or diagnostic files. Never change .gitignore file unless explicitly requested. Do not commit unless explicitly requested.

## 10. Language Preference
- **Java / Kotlin / Compose usage**
  - Prefer Kotlin for new Android code: In Kotlin-enabled Android modules such as :OsmAnd, :OsmAnd-shared, and plugin modules, generate new classes, functions, UI components, and Compose-related code in Kotlin by default.
  - Use Java in Java-only modules: In :OsmAnd-java, :OsmAnd-api, and other Java-only/core modules, write new code in Java unless the module is explicitly being migrated to Kotlin.
  - Preserve existing file language: When modifying existing .java files, keep the change in Java unless the PR explicitly migrates that file/class to Kotlin.
  - Follow local architecture: Prefer Kotlin/Compose for future Android UI migration work, but do not introduce Kotlin into Java-only modules just to satisfy the default language preference.

## 11. AI Disclosure in Pull Requests
Every pull request produced with the help of an AI agent MUST end with an **AI disclaimer** section. Do not wait to be asked for it - write it into the PR body when the PR is created.

The disclaimer must contain:
- **Tool and model** that produced the change (e.g. `Claude Code (Opus 5)`).
- **Prompt summary** - a short, faithful summary of the prompts the user actually gave, in order. Summarise the intent and do not invent requirements that were never asked for. Never mention which language the prompt was written in.
- **What the agent decided on its own** - anything not covered by the prompts (design choices, extra files, workarounds), so reviewers can tell human intent from model inference.

The same applies to AI-generated issue comments: mark them `(gen by AI)`.

Rules for the whole PR, not just the disclaimer:
- **English only.** Source code, code comments, commit messages, PR titles and bodies, and issue comments are always written in English, whatever language the conversation with the user happened in. Never note the language of the conversation.
- **No tool advertising.** Do not append "Generated with Claude Code" banners, agent session links, or similar footers to PR bodies or issue comments. The AI disclaimer section above is the only provenance note needed.

Template:

```markdown
## AI disclaimer

Produced with <tool / model>.

Prompts used (summarised):
1. ...
2. ...

Decided by the agent, not requested explicitly: ...
```

*Note: This file is a living document and should be updated as the project evolves.*
