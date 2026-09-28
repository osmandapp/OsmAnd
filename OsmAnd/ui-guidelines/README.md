# UI guidelines — OsmAnd Android

Rules for all new UI built with Material 3: screens, dialogs, bottom sheets and components. Existing screens migrate to these rules when they are redesigned.

Where these guidelines and Google's Material 3 guidelines differ, these guidelines win.

The reusable components live in `net.osmand.plus.widgets.ui` (see [Components](#components)). Use them instead of building headers, footers, switches or list rows by hand. Rules for each component are in its KDoc; when the KDoc and this file disagree, the KDoc wins — fix this file.

- Components: [UI Library (AND)](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-)
- Icons: [Icons Lib (AND)](https://www.figma.com/design/LZK01Do6ogNfyCxjMqqlkj/Icons-Lib.--AND-?node-id=2-2)

## Prerequisites

- The screen uses `OsmandMaterialLightTheme` / `OsmandMaterialDarkTheme` (parent `Theme.Material3Expressive.*`).
- Colours come from theme attributes only. No hardcoded hex.
- Dimensions come from existing `@dimen` resources. No hardcoded dp.

## Icons

Source: [Icons Lib (AND)](https://www.figma.com/design/LZK01Do6ogNfyCxjMqqlkj/Icons-Lib.--AND-?node-id=2-2).

- The component name in Figma is the drawable name in code (`ic_action_*`). Use the existing drawable; do not redraw, import or recolour an icon.
- Icons are tinted with theme attributes, like any other colour.
- An icon that is not in the library needs design approval, like a new component.

## Changes need design approval

These components are the shared baseline for every Material 3 screen. A change here changes every screen that uses them.

Do not, without approval from design:

- add new colours — to `colors.xml`, theme attributes, or as hex anywhere;
- add new icons;
- change a component's layout, dimensions, drawables, styles or text appearance;
- change the usage rules in a component's KDoc;
- add a new component to this package.

Allowed without approval: using the components as documented, and setting their text, state and listeners.

If a screen seems to need something the kit does not provide, stop and ask in the issue or PR — do not work around it with a local override or a one-off layout.

## Components

| Component | Layout | Figma | Use for |
|---|---|---|---|
| `ScreenDescriptionView` | `ui_screen_description.xml` | [Screen description](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2329-8656) | One intro text per screen, above all cards |
| `MainSwitchView` | `ui_main_switch.xml` | [Main switch](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2334-3582) | Master toggle for the whole screen |
| `GroupHeaderView` | `ui_group_header.xml` | TODO | Title above a card; optional trailing icon button |
| `SegmentedList` | — | TODO | A group of rows; picks each row's shape from its position |
| `SettingRow` | `item_ui_setting_row.xml` | TODO | One row inside a `SegmentedList` |
| `GroupFooterView` | `ui_group_footer.xml` | [Group footer](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2325-4534) | Explanation below the card it belongs to |

## Screen structure

Top to bottom, every element optional except the groups:

```
ScreenDescriptionView
MainSwitchView
  GroupHeaderView
  SegmentedList
    SettingRow
    SettingRow
  GroupFooterView
  (next group…)
```

## Details

### MainSwitchView

Master toggle at the top of a feature or plugin screen. One per screen, always first; controls everything below it.

- When off, the content below is **hidden** and replaced by an empty state. Never disabled.
- Label mode:
  - `useStateLabel = true` — label reads On / Off, because the app bar already names the feature. The feature name goes to the content description.
  - `useStateLabel = false` — label is the feature name; the footer must state the value in words.
- The whole row is the touch target; the switch itself is not clickable.

Figma: [Main switch](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2334-3582)

### GroupHeaderView

Title above a card, on the surface — never inside the card. Optional trailing icon button.

### GroupFooterView

Explanatory text below a card, on the surface. It belongs to the card directly above it.

- Never inside a card, never between two cards of the same group.
- Never truncated — a cut-off explanation is worse than none.
- Asymmetric padding (8dp top, 16dp bottom) is intentional: it ties the footer to the card above and keeps the gap to the next group.

Figma: [Group footer](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2325-4534)

### ScreenDescriptionView

Intro text at the top of a screen, above all cards.

- One per screen, never inside a card.
- Explains what the screen is for. Never repeats the title, never carries a value, a state or a warning — those go to a footer or a banner.

Looks the same as `GroupFooterView` (14sp, on-surface-variant); only the padding and the scope differ. Screen description is about the whole screen, a footer is about the card above it.

Figma: [Screen description](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2329-8656)

### SegmentedList / SettingRow

`SegmentedList` assigns the row shape from the row's position in the group, so hiding a row keeps the group's corners correct. `SettingRow` binds one row.

## Don't

- Don't wrap a `SegmentedList` in another card — double containment.
- Don't put a `GroupHeaderView` inside a card.
- Don't disable content under a `MainSwitchView` — hide it.
- Don't set row corner radii manually — `SegmentedList` owns them.
- Don't build a custom header, footer or row when a component above fits. If none fits, add a new component here instead of a one-off layout.

## Adding a component

Only after design approval (see above). In the same PR:

1. KDoc on the class with the usage rules.
2. A row in the table above and a section below.
3. The Figma component: `Android: <ClassName> (net.osmand.plus.widgets.ui)` in its description, and a dev resource link to the class on `master`.
