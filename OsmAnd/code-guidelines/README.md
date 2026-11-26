# Code guidelines — OsmAnd Android

Rules for new Java and Kotlin code in `:OsmAnd`. Existing code moves to them when it is rewritten, not in passing.

## Classes are objects, not bags of static methods

- Do not write a class where everything is static, and no Kotlin `object` that is only a set of functions. Logic is an instance: it gets what it needs (`OsmandApplication`, settings, other services) in the constructor and keeps its own state.
- An instance that holds state for the whole app is created once and owned by a long-lived component, reachable through it (`app.getDownloadThread().getFreeVersionMapDownloads()`), not re-created with `new` at every call site.
- Static is fine for constants and for a fragment's `showInstance(...)`.

## Names say what the class is

- No `Helper` in class names, and no other suffix that names a technical layer instead of the thing: `Utils`, `Manager`, `Api`, `Server`, `Dialogs`. Name what the class represents or does: `FreeVersionMapDownloads`, `OutdatedMapsWarning` — not `FreeRegionHelper`, `FreeRegionApi`.
- One feature, one class: its state, its rules and its server requests stay together (`FreeVersionMapDownloads` keeps the free region, checks the downloads and asks the server for a replace). Split off only what is a thing of its own - a fragment, a dialog - not a layer.
- A class named after one feature holds that feature only. A dialog about something else gets its own class, even if it is shown in the same flow.

## Language

- New classes in `:OsmAnd` are Kotlin; edits of an existing `.java` file stay in Java. See section 10 of `AGENTS.md`.

## Keep `MapActivity` thin

- Do not put feature logic in `MapActivity` and do not reach from it into a specific fragment or feature. A feature hooks in with at most one delegating line, or through an interface the fragments implement.

## One-time actions after an update

- Something done once on the update from an older version (a migration, a one-time dialog) is tied to a version in `AppVersionUpgradeOnInit`: add a `VERSION_x_y_zz` constant, raise `LAST_APP_VERSION`, and check `prevAppVersion < VERSION_x_y_zz`. Do not check a flag on every start of an activity.

## No Compose views in `ListView` rows

- `ListView`, `GridView` and `ExpandableListView` drop surplus rows without detaching them, and a Compose view (`ComposeView`, `AbstractComposeView`) that is never detached keeps its window and activity alive (#26403).
- Put Compose views into a `RecyclerView`, a fragment or dialog layout, or a `PopupWindow`.
