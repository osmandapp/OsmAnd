package net.osmand.plus.configmap;

import static net.osmand.plus.configmap.ConfigureMapMenu.nonEmptyStringOrDefault;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import org.threeten.bp.Duration;

import java.util.*;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.InitializePreferenceFragmentWithFragmentBeforeOnCreate;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighterProvider;
import gnu.trove.list.array.TIntArrayList;

public class ConfigureMapDialogs {

	public static void showMapMagnifierDialog(@NonNull OsmandMapTileView view) {
		OsmandPreference<Float> density = view.getSettings().MAP_DENSITY;
		int p = (int) (density.get() * 100);
		TIntArrayList tlist = new TIntArrayList(new int[]{25, 33, 50, 75, 100, 125, 150, 200, 300, 400});
		List<String> values = new ArrayList<>();
		int i = -1;
		for (int k = 0; k <= tlist.size(); k++) {
			boolean end = k == tlist.size();
			if (i == -1) {
				if ((end || p < tlist.get(k))) {
					values.add(p + " %");
					i = k;
				} else if (p == tlist.get(k)) {
					i = k;
				}
			}
			if (k < tlist.size()) {
				values.add(tlist.get(k) + " %");
			}
		}
		if (values.size() != tlist.size()) {
			tlist.insert(i, p);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(view.requireMapActivity());
		builder.setTitle(R.string.map_magnifier);
		builder.setSingleChoiceItems(values.toArray(new String[0]), i, (dialog, which) -> {
			int p1 = tlist.get(which);
			density.set(p1 / 100.0f);
			view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				mapContext.updateMapSettings(true);
			}
			dialog.dismiss();
		});
		builder.show();
	}

	protected static void showMapMagnifierDialog(@NonNull MapActivity activity, boolean nightMode,
												 @NonNull ContextMenuItem item, @NonNull OnDataChangeUiAdapter adapter) {
		OsmandApplication app = activity.getMyApplication();
		int profileColor = ColorUtilities.getAppModeColor(app, nightMode);
		OsmandSettings settings = app.getSettings();

		OsmandMapTileView view = activity.getMapView();
		OsmandPreference<Float> mapDensity = settings.MAP_DENSITY;
		int p = (int) (mapDensity.get() * 100);
		TIntArrayList tlist = new TIntArrayList(new int[]{25, 33, 50, 75, 100, 125, 150, 200, 300, 400});
		List<String> values = new ArrayList<>();
		int i = -1;
		for (int k = 0; k <= tlist.size(); k++) {
			boolean end = k == tlist.size();
			if (i == -1) {
				if ((end || p < tlist.get(k))) {
					values.add(p + " %");
					i = k;
				} else if (p == tlist.get(k)) {
					i = k;
				}
			}
			if (k < tlist.size()) {
				values.add(tlist.get(k) + " %");
			}
		}
		if (values.size() != tlist.size()) {
			tlist.insert(i, p);
		}

		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(R.string.map_magnifier)
				.setControlsColor(profileColor)
				.setNegativeButton(R.string.shared_string_dismiss, null);

		CustomAlert.showSingleSelection(dialogData, values.toArray(new String[0]), i, v -> {
			int which = (int) v.getTag();
			int value = tlist.get(which);
			mapDensity.set(value / 100.0f);
			view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				mapContext.updateMapSettings(true);
			}
			item.setDescription(String.format(Locale.UK, "%.0f", 100f * settings.MAP_DENSITY.get()) + " %");
			adapter.onDataSetInvalidated();
		});
	}

	protected static void showTextSizeDialog(
			@NonNull MapActivity activity, boolean nightMode,
			@NonNull ContextMenuItem item, @NonNull OnDataChangeUiAdapter uiAdapter
	) {
		OsmandApplication app = activity.getMyApplication();
		int profileColor = ColorUtilities.getAppModeColor(app, nightMode);

		OsmandMapTileView view = activity.getMapView();
		Float[] txtValues = {0.33f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f};
		int selected = -1;
		String[] txtNames = new String[txtValues.length];
		for (int i = 0; i < txtNames.length; i++) {
			txtNames[i] = (int) (txtValues[i] * 100) + " %";
			if (Math.abs(view.getSettings().TEXT_SCALE.get() - txtValues[i]) < 0.1f) {
				selected = i;
			}
		}
		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(R.string.text_size)
				.setControlsColor(profileColor)
				.setNegativeButton(R.string.shared_string_dismiss, null);

		CustomAlert.showSingleSelection(dialogData, txtNames, selected, v -> {
			int which = (int) v.getTag();
			view.getSettings().TEXT_SCALE.set(txtValues[which]);
			activity.refreshMapComplete();
			item.setDescription(ConfigureMapUtils.getScale(activity));
			uiAdapter.onDataSetInvalidated();
		});
	}

	public static MapLanguageDialog createMapLanguageDialog(final @NonNull MapActivity activity,
															final boolean nightMode,
															final @NonNull ContextMenuItem item,
															final @NonNull OnDataChangeUiAdapter uiAdapter) {
		final OsmandApplication app = activity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final int profileColor = ColorUtilities.getAppModeColor(app, nightMode);
		final OsmandMapTileView view = activity.getMapView();
		final Context ctx = UiUtilities.getThemedContext(activity, nightMode);
		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(activity.getString(R.string.map_locale));
		final Map<String, String> mapLanguageNameById = ConfigureMapUtils.getSortedMapLanguageNameById(app);
		final List<String> mapLanguagesIds = new ArrayList<>(mapLanguageNameById.keySet());
		final List<String> mapLanguagesNames = new ArrayList<>(mapLanguageNameById.values());
		final MapLanguageDialog.DialogState dialogState =
				new MapLanguageDialog.DialogState(
						getSelected(mapLanguagesIds, settings.MAP_PREFERRED_LOCALE.get()),
						settings.MAP_TRANSLITERATE_NAMES.get());
		final OnCheckedChangeListener translitChangeListener = (buttonView, isChecked) -> dialogState.transliterateNames = isChecked;
		final BaseAdapter singleChoiceAdapter =
				new ArrayAdapter<>(ctx, R.layout.single_choice_switch_item, R.id.text1, mapLanguagesNames) {

					@NonNull
					@Override
					public View getView(int position, View convertView, @NonNull ViewGroup parent) {
						final View view = super.getView(position, convertView, parent);
						final AppCompatCheckedTextView checkedTextView = view.findViewById(R.id.text1);
						UiUtilities.setupCompoundButtonDrawable(app, nightMode, profileColor, checkedTextView.getCheckMarkDrawable());

						if (position == dialogState.selectedLanguageIndex && position > 0) {
							checkedTextView.setChecked(true);
							view.findViewById(R.id.topDivider).setVisibility(View.VISIBLE);
							view.findViewById(R.id.bottomDivider).setVisibility(View.VISIBLE);
							view.findViewById(R.id.switchLayout).setVisibility(View.VISIBLE);
							final TextView switchText = view.findViewById(R.id.switchText);
							switchText.setText(app.getString(R.string.use_latin_name_if_missing, mapLanguagesNames.get(position)));
							final SwitchCompat check = view.findViewById(R.id.check);
							check.setChecked(dialogState.transliterateNames);
							check.setOnCheckedChangeListener(translitChangeListener);
							UiUtilities.setupCompoundButton(nightMode, profileColor, check);
						} else {
							checkedTextView.setChecked(position == dialogState.selectedLanguageIndex);
							view.findViewById(R.id.topDivider).setVisibility(View.GONE);
							view.findViewById(R.id.bottomDivider).setVisibility(View.GONE);
							view.findViewById(R.id.switchLayout).setVisibility(View.GONE);
						}
						return view;
					}
				};
		builder.setAdapter(singleChoiceAdapter, null);
		builder.setSingleChoiceItems(
				mapLanguagesNames.toArray(new String[0]),
				dialogState.selectedLanguageIndex,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						dialogState.selectedLanguageIndex = which;
						dialogState.transliterateNames =
								settings.MAP_TRANSLITERATE_NAMES.isSet() ?
										dialogState.transliterateNames :
										mapLanguagesIds.get(which).equals("en");
						((AlertDialog) dialog).getListView().setSelection(which);
						singleChoiceAdapter.notifyDataSetChanged();
					}
				});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(
				R.string.shared_string_apply,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						view.getSettings().MAP_TRANSLITERATE_NAMES.set(dialogState.selectedLanguageIndex > 0 && dialogState.transliterateNames);
						final int index = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
						view.getSettings().MAP_PREFERRED_LOCALE.set(mapLanguagesIds.get(index));
						activity.refreshMapComplete();
						// FK-FIXME: 1. search for "Afrikaans", 2. click search result to open dialog, 3. click Afrikaans, 4. click Apply to close the dialog, then the following item erroneously does not show af (for Afrikaans) in it's description, but displays the "old" description.
						item.setDescription(getLocaleDescr(index));
						uiAdapter.onDataSetInvalidated();
					}

					private String getLocaleDescr(final int index) {
						return nonEmptyStringOrDefault(mapLanguagesIds.get(index), () -> activity.getString(R.string.local_map_names));
					}
				});
		return new MapLanguageDialog(
				builder.create(),
				mapLanguageNameById,
				dialogState,
				settings.MAP_TRANSLITERATE_NAMES,
				settings.MAP_PREFERRED_LOCALE);
	}

	private static int getSelected(final List<String> haystack, final String needle) {
		for (int i = 0; i < haystack.size(); i++) {
			if (Objects.equals(needle, haystack.get(i))) {
				return i;
			}
		}
		return -1;
	}

	// FK-TODO: DRY with CustomAlert.SingleSelectionDialogFragment
	public static class MapLanguageDialog extends DialogFragment implements SettingHighlighterProvider {

		public static class DialogState {

			public int selectedLanguageIndex;
			public boolean transliterateNames;

			public DialogState(final int selectedLanguageIndex, final boolean transliterateNames) {
				this.selectedLanguageIndex = selectedLanguageIndex;
				this.transliterateNames = transliterateNames;
			}
		}

		private final AlertDialog alertDialog;
		private final Map<String, String> mapLanguageNameById;
		private final DialogState dialogState;
		private final OsmandPreference<Boolean> MAP_TRANSLITERATE_NAMES;
		private final OsmandPreference<String> MAP_PREFERRED_LOCALE;

		public MapLanguageDialog(final AlertDialog alertDialog,
								 final Map<String, String> mapLanguageNameById,
								 final DialogState dialogState,
								 final OsmandPreference<Boolean> MAP_TRANSLITERATE_NAMES,
								 final OsmandPreference<String> MAP_PREFERRED_LOCALE) {
			this.alertDialog = alertDialog;
			this.mapLanguageNameById = mapLanguageNameById;
			this.dialogState = dialogState;
			this.MAP_TRANSLITERATE_NAMES = MAP_TRANSLITERATE_NAMES;
			this.MAP_PREFERRED_LOCALE = MAP_PREFERRED_LOCALE;
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
			return alertDialog;
		}

		public void updateDialogStateFromPreferences() {
			dialogState.selectedLanguageIndex = getSelected(getKeys(), MAP_PREFERRED_LOCALE.get());
			dialogState.transliterateNames = MAP_TRANSLITERATE_NAMES.get();
		}

		@Override
		public SettingHighlighter getSettingHighlighter() {
			return new ViewOfSettingHighlighter(
					this::getView,
					Duration.ofSeconds(1));
		}

		private View getView(final Setting setting) {
			return getViewByPosition(getListView(), getIndexedOf(setting));
		}

		// aapted from https://stackoverflow.com/a/24864536
		public static View getViewByPosition(final ListView listView, final int pos) {
			final int firstListItemPosition = listView.getFirstVisiblePosition();
			final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;
			return firstListItemPosition <= pos && pos <= lastListItemPosition ?
					listView.getChildAt(pos - firstListItemPosition) :
					listView.getAdapter().getView(pos, null, listView);
		}

		public ListView getListView() {
			return ((AlertDialog) getDialog()).getListView();
		}

		public int getIndexedOf(final Setting setting) {
			return getKeys().indexOf(setting.getKey());
		}

		private List<String> getKeys() {
			return new ArrayList<>(mapLanguageNameById.keySet());
		}

		public static class PreferenceFragment extends PreferenceFragmentCompat implements InitializePreferenceFragmentWithFragmentBeforeOnCreate<MapLanguageDialog> {

			private MapLanguageDialog mapLanguageDialog;
			private Map<String, String> mapLanguageNameById;

			@Override
			public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final MapLanguageDialog mapLanguageDialog) {
				this.mapLanguageDialog = mapLanguageDialog;
				mapLanguageNameById = mapLanguageDialog.mapLanguageNameById;
			}

			public MapLanguageDialog getPrincipal() {
				return mapLanguageDialog;
			}

			@Override
			public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
				final Context context = getPreferenceManager().getContext();
				final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
				screen.setTitle("screen title");
				screen.setSummary("screen summary");
				PreferenceFragment
						.asPreferences(mapLanguageNameById, context)
						.forEach(screen::addPreference);
				setPreferenceScreen(screen);
			}

			private static Collection<Preference> asPreferences(final Map<String, String> mapLanguageNameById,
																final Context context) {
				return mapLanguageNameById
						.entrySet()
						.stream()
						.map(id_mapLanguageName_entry -> asPreference(id_mapLanguageName_entry.getKey(), id_mapLanguageName_entry.getValue(), context))
						.collect(Collectors.toUnmodifiableList());
			}

			private static Preference asPreference(final String id, final String mapLanguageName, final Context context) {
				final Preference preference = new Preference(context);
				preference.setKey(id);
				preference.setTitle(mapLanguageName);
				return preference;
			}
		}
	}

	public static CustomAlert.SingleSelectionDialogFragment createRenderingPropertyDialog(
			final @NonNull MapActivity activity,
			final @NonNull RenderingRuleProperty property,
			final @NonNull ContextMenuItem item,
			final @NonNull OnDataChangeUiAdapter uiAdapter,
			final boolean nightMode) {
		return CustomAlert
				.createSingleSelectionDialogFragment(
						new AlertDialogData(activity, nightMode)
								.setTitle(AndroidUtils.getRenderingStringPropertyDescription(activity.getMyApplication(), property.getAttrName(), property.getName()))
								.setControlsColor(ColorUtilities.getAppModeColor(activity.getMyApplication(), nightMode))
								.setNegativeButton(R.string.shared_string_dismiss, null),
						ConfigureMapUtils.getSortedItemByKey(property, activity.getMyApplication()),
						getSelectedIndex(activity, property),
						v -> {
							final int which = (int) v.getTag();
							final CommonPreference<String> preference = getCustomRenderProperty(activity, property);
							preference.set(which == 0 ? "" : property.getPossibleValues()[which - 1]);
							activity.refreshMapComplete();
							item.setDescription(AndroidUtils.getRenderingStringPropertyValue(activity, preference.get()));
							final String id = item.getId();
							if (!Algorithms.isEmpty(id)) {
								uiAdapter.onRefreshItem(id);
							} else {
								uiAdapter.onDataSetChanged();
							}
						});
	}

	public static int getSelectedIndex(final MapActivity activity, final RenderingRuleProperty property) {
		return getSelectedIndex(Arrays.asList(property.getPossibleValues()), getCustomRenderProperty(activity, property).get());
	}

	private static CommonPreference<String> getCustomRenderProperty(final MapActivity activity,
																	final RenderingRuleProperty property) {
		return ConfigureMapMenu.getCustomRenderProperty(property, activity.getMyApplication().getSettings());
	}

	private static int getSelectedIndex(final List<String> haystack, final String needle) {
		final int selectedIndex = haystack.indexOf(needle);
		if (selectedIndex >= 0) {
			return selectedIndex + 1;
		} else if (Algorithms.isEmpty(needle)) {
			return 0;
		} else {
			return selectedIndex;
		}
	}

	protected static void showPreferencesDialog(
			@NonNull OnDataChangeUiAdapter uiAdapter, @NonNull ContextMenuItem item,
			@NonNull MapActivity activity, @NonNull String category,
			@NonNull List<RenderingRuleProperty> properties,
			@NonNull List<CommonPreference<Boolean>> prefs, boolean nightMode
	) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		OsmandApplication app = activity.getMyApplication();
		boolean[] checkedItems = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			checkedItems[i] = prefs.get(i).get();
		}
		String[] propertyNames = new String[properties.size()];
		for (int i = 0; i < properties.size(); i++) {
			RenderingRuleProperty p = properties.get(i);
			String propertyName = AndroidUtils.getRenderingStringPropertyName(activity, p.getAttrName(),
					p.getName());
			propertyNames[i] = propertyName;
		}

		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(category)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode))
				.setNegativeButton(R.string.shared_string_cancel, (dialog, whichButton) -> {
					boolean selected = false;
					for (int i = 0; i < prefs.size(); i++) {
						selected |= prefs.get(i).get();
					}
					item.setSelected(selected);
					item.setColor(activity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					uiAdapter.onDataSetInvalidated();
				})
				.setPositiveButton(R.string.shared_string_ok, (dialog, whichButton) -> {
					boolean selected = false;
					for (int i = 0; i < prefs.size(); i++) {
						prefs.get(i).set(checkedItems[i]);
						selected |= checkedItems[i];
					}
					item.setSelected(selected);
					item.setColor(activity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					uiAdapter.onDataSetInvalidated();
					activity.refreshMapComplete();
					activity.getMapLayers().updateLayers(activity);
				});

		CustomAlert.showMultiSelection(dialogData, propertyNames, checkedItems, v -> {
			int which = (int) v.getTag();
			checkedItems[which] = !checkedItems[which];
		});
	}
}
