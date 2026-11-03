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
| `ui_material_app_bar` (include, no class) | `ui_material_app_bar.xml` | [Top app bar](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=1958-4725) | Small top app bar on every Material 3 screen |
| `ScreenDescriptionView` | `ui_screen_description.xml` | [Screen description](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2329-8656) | One intro text per screen, above all cards |
| `MainSwitchView` | `ui_main_switch.xml` | [Main switch](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2334-3582) | Master toggle for the whole screen |
| `GroupHeaderView` | `ui_group_header.xml` | TODO | Title above a card; optional trailing icon button |
| `SegmentedList` | — | TODO | A group of rows; picks each row's shape from its position |
| `SettingRow` | `item_ui_setting_row.xml` | TODO | One row inside a `SegmentedList` |
| `SliderListItem` (planned) | `item_ui_slider_list_item.xml` | [List item slider](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2502-8148) | A row with a title, a value and a slider, inside a `SegmentedList` |
| `GroupFooterView` | `ui_group_footer.xml` | [Group footer](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2325-4534) | Explanation below the card it belongs to |
| `MaterialButton` (native, no OsmAnd class) | — | [Button](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2349-6407) | Filled, Tonal, Outlined and Text actions |
| `MaterialButtonToggleGroup` (native, no OsmAnd class) | — | [Connected button group](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2229-4243) | A single choice between 2–4 short options, e.g. TCP / UDP |
| `MaterialAlertDialogBuilder` (native, no OsmAnd class) | — | [Dialog](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2401-11323) | A dialog on a Material 3 screen |
| `TextInputLayout` (native, no OsmAnd class) | — | [Text field](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2371-8949) | Text or number input |

## Screen structure

Top to bottom, every element optional except the groups:

```
ui_material_app_bar
ScreenDescriptionView
MainSwitchView
  GroupHeaderView
  SegmentedList
    SettingRow
    SliderListItem
  GroupFooterView
  (next group…)
```

## Details

### Top app bar

Small top app bar at the top of every Material 3 screen. Include it with `<include layout="@layout/ui_material_app_bar" />`; there is no view class — the layout only configures the native `AppBarLayout` and `MaterialToolbar`.

- Background is the screen background (`colorSurface`). It does not change colour on scroll (`liftOnScroll="false"`): in the light theme `colorSurfaceContainer` is white and would merge with the cards scrolling under it. Same behaviour as Android system Settings.
- Only the small size is implemented. Medium and large app bars need design approval.

Figma: [Top app bar](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=1958-4725)

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

### Buttons

Native Material 3 Expressive buttons in four types. Colours, pressed / focused / disabled states and corners come from the theme — a screen only picks the type.

| Type | Use for |
|---|---|
| Filled | The main action of a screen or card. One per screen or card. |
| Tonal | An important action that is not the main one, or the main action of a card when the screen already has a Filled button. |
| Outlined | A secondary action next to a Filled or Tonal one (e.g. Cancel next to Save). |
| Text | The lowest-emphasis action; dialog buttons. |

- Don't set `backgroundTint`, `textColor`, `cornerRadius` or a custom style on a button.
- How to use each type on a screen: TODO — examples are added by the Buttons task.

Figma: [Button](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2349-6407)

### Connected button group

Native `MaterialButtonToggleGroup`; in the Expressive theme its default style is the M3 connected button group (the old segmented button is deprecated). There is no OsmAnd class.

- For a single choice set `app:singleSelection="true"` and `app:selectionRequired="true"`.
- Colours are an intentional deviation from native M3: selected `colorPrimary`, unselected `colorPrimaryContainer` (native uses a neutral container, which disappears on white cards). They come from the theme — do not set `backgroundTint`, `textColor` or a style on the buttons of a screen.
- Outer corners are round (the default). The square variant needs design approval.

Figma: [Connected button group](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2229-4243)

### SegmentedList / SettingRow

`SegmentedList` assigns the row shape from the row's position in the group, so hiding a row keeps the group's corners correct. `SettingRow` binds one row.

### SliderListItem

Planned — replaces `ui_slider_card.xml`, which is a standalone card and can't be part of a group.

A row of a `SegmentedList`: title, value on the right, slider below. It has no background of its own; `SegmentedList` gives it its shape, like any other row, so a slider can follow the switch it depends on in the same group.

- Title is regular weight, the same as `SettingRow`.
- The value is committed immediately. No Save button.
- The screen supplies the value format and units; the component hardcodes neither. TalkBack reads the formatted value, never the handle index.
- Stops (`tickVisible`) only for a discrete scale — a fixed set of values. A continuous scale has no stops.
- A non-linear scale maps the handle to an index in a list of values. A stored value that is not on the scale falls back to the first stop and is saved.
- Two handles (`Slider=Two` in Figma, native `RangeSlider`) are not implemented yet; they need design approval and code.

Figma: [List item slider](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2502-8148)

### Text field

Native `TextInputLayout` with `TextInputEditText`. Outlined (`Widget.Material3.TextInputLayout.OutlinedBox`) by default; filled (`Widget.Material3.TextInputLayout.FilledBox`) only where the design asks for it.

- Clear button on every text field: `app:endIconMode="clear_text"` with `app:endIconDrawable="@drawable/ic_action_clear_field"`. It shows only when the field has text.
- Label is the `android:hint`; no separate label above the field.
- Errors use the native `error` text below the field — never a toast or a dialog. Validate when the field loses focus and on save; clear the error as soon as the value is valid again, without waiting for focus to leave.
- No helper text by default. Add it only when the field needs an explanation that the label can't carry.
- Restrict input with `inputType`, `digits` and `maxLength` where the value has a known format (e.g. port, MMSI), instead of rejecting it afterwards.

Figma: [Text field](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2371-8949)

### Dialogs

Native `MaterialAlertDialogBuilder` — title, buttons, corners and paddings come from the Material 3 style. Custom content only through `setView`, with `@dimen/ui_dialog_padding` (24dp) on the sides.

- Background is `colorSurfaceContainer` — an intentional deviation from native M3 (`colorSurfaceContainerHigh`), set once in the theme.
- No Material 2 dialogs (`AlertDialog.Builder`) on Material 3 screens.

Figma: [Dialog](https://www.figma.com/design/lrvcZk05PlOpwYJyZTHcPK/UI-Library--AND-?node-id=2401-11323)

## Don't

- Don't wrap a `SegmentedList` in another card — double containment.
- Don't put a slider in a card of its own — it is a `SliderListItem` row of a `SegmentedList`.
- Don't put a `GroupHeaderView` inside a card.
- Don't disable content under a `MainSwitchView` — hide it.
- Don't set row corner radii manually — `SegmentedList` owns them.
- Don't colour or restyle a button on a screen — pick one of the four types.
- Don't colour or restyle the buttons of a connected button group on a screen — the theme owns them.
- Don't override a text field's colours, shape or icons on a screen — use the native styles.
- Don't set a dialog's background, corners or button styles per dialog — the theme owns them.
- Don't build an app bar by hand or override its colours on a screen — include `ui_material_app_bar`.
- Don't build a custom header, footer or row when a component above fits. If none fits, add a new component here instead of a one-off layout.

## Adding a component

Only after design approval (see above). In the same PR:

1. KDoc on the class with the usage rules.
2. A row in the table above and a section below.
3. The Figma component: `Android: <ClassName> (net.osmand.plus.widgets.ui)` in its description — or `Android: <layout>.xml (layout include, no class)` for a component without a class, or `Android: <MaterialClass> (native)` for a native Material component — and a dev resource link to the class or layout on `master` (for a native component, to its Material Components docs).
