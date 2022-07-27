package net.osmand.plus.configmap;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

public class ConfigureMapDialogs {

	protected static void showMapModeDialog(@NonNull MapActivity activity, int themeRes, boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		OsmandMapTileView view = activity.getMapView();
		AlertDialog.Builder bld = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		bld.setTitle(R.string.daynight);
		String[] items = new String[DayNightMode.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = DayNightMode.values()[i].toHumanString(app);
		}
		int i = view.getSettings().DAYNIGHT_MODE.get().ordinal();
		bld.setNegativeButton(R.string.shared_string_dismiss, null);
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				items, nightMode, i, app, selectedProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					view.getSettings().DAYNIGHT_MODE.set(DayNightMode.values()[which]);
					if (view.getMapRenderer() == null) {
						activity.refreshMapComplete();
					}
					activity.getDashboard().refreshContent(false);
				}
		);
		bld.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(bld.show());
	}

	protected static void showMapMagnifierDialog(MapActivity activity, int themeRes, boolean nightMode,
	                                             ContextMenuItem item, OnDataChangeUiAdapter uiAdapter) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		OsmandMapTileView view = activity.getMapView();
		OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
		AlertDialog.Builder bld = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		int p = (int) (mapDensity.get() * 100);
		TIntArrayList tlist = new TIntArrayList(new int[] {25, 33, 50, 75, 100, 125, 150, 200, 300, 400});
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

		bld.setTitle(R.string.map_magnifier);
		bld.setNegativeButton(R.string.shared_string_dismiss, null);
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				values.toArray(new String[0]), nightMode, i, app, selectedProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					int value = tlist.get(which);
					mapDensity.set(value / 100.0f);
					view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
					MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
					if (mapContext != null) {
						mapContext.updateMapSettings();
					}
					item.setDescription(
							String.format(Locale.UK, "%.0f", 100f * activity.getMyApplication()
									.getSettings().MAP_DENSITY.get())
									+ " %");
					uiAdapter.onDataSetInvalidated();
				}
		);
		bld.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(bld.show());
	}

	protected static void showTextSizeDialog(MapActivity activity, int themeRes, boolean nightMode,
	                                         ContextMenuItem item, OnDataChangeUiAdapter uiAdapter) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);


		OsmandMapTileView view = activity.getMapView();
		AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		// test old descr as title
		b.setTitle(R.string.text_size);
		Float[] txtValues = {0.33f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f};
		int selected = -1;
		String[] txtNames = new String[txtValues.length];
		for (int i = 0; i < txtNames.length; i++) {
			txtNames[i] = (int) (txtValues[i] * 100) + " %";
			if (Math.abs(view.getSettings().TEXT_SCALE.get() - txtValues[i]) < 0.1f) {
				selected = i;
			}
		}
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				txtNames, nightMode, selected, app, selectedProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					view.getSettings().TEXT_SCALE.set(txtValues[which]);
					activity.refreshMapComplete();
					item.setDescription(ConfigureMapUtils.getScale(activity));
					uiAdapter.onDataSetInvalidated();
				});
		b.setAdapter(dialogAdapter, null);
		b.setNegativeButton(R.string.shared_string_dismiss, null);
		dialogAdapter.setDialog(b.show());
	}

	protected static void showMapLanguageDialog(MapActivity activity, int themeRes, boolean nightMode,
	                                            ContextMenuItem item, OnDataChangeUiAdapter uiAdapter) {

		int[] selectedLanguageIndex = new int[1];
		boolean[] transliterateNames = new boolean[1];

		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);


		OsmandMapTileView view = activity.getMapView();
		AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));

		b.setTitle(activity.getString(R.string.map_locale));

		Map<String, String> mapLanguages = ConfigureMapUtils.getSorterMapLanguages(app);
		String[] mapLanguagesIds = mapLanguages.keySet().toArray(new String[0]);
		String[] mapLanguagesNames = mapLanguages.values().toArray(new String[0]);

		int selected = -1;
		for (int i = 0; i < mapLanguagesIds.length; i++) {
			if (settings.MAP_PREFERRED_LOCALE.get().equals(mapLanguagesIds[i])) {
				selected = i;
				break;
			}
		}
		selectedLanguageIndex[0] = selected;
		transliterateNames[0] = settings.MAP_TRANSLITERATE_NAMES.get();

		OnCheckedChangeListener translitChangdListener = (buttonView, isChecked) -> transliterateNames[0] = isChecked;

		ArrayAdapter<CharSequence> singleChoiceAdapter = new ArrayAdapter<CharSequence>(
				new ContextThemeWrapper(activity, themeRes), R.layout.single_choice_switch_item, R.id.text1, mapLanguagesNames) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				AppCompatCheckedTextView checkedTextView = v.findViewById(R.id.text1);
				UiUtilities.setupCompoundButtonDrawable(app, nightMode, selectedProfileColor, checkedTextView.getCheckMarkDrawable());

				if (position == selectedLanguageIndex[0] && position > 0) {
					checkedTextView.setChecked(true);
					v.findViewById(R.id.topDivider).setVisibility(View.VISIBLE);
					v.findViewById(R.id.bottomDivider).setVisibility(View.VISIBLE);
					v.findViewById(R.id.switchLayout).setVisibility(View.VISIBLE);
					TextView switchText = v.findViewById(R.id.switchText);
					switchText.setText(app.getString(R.string.use_latin_name_if_missing, mapLanguagesNames[position]));
					SwitchCompat check = v.findViewById(R.id.check);
					check.setChecked(transliterateNames[0]);
					check.setOnCheckedChangeListener(translitChangdListener);
					UiUtilities.setupCompoundButton(nightMode, selectedProfileColor, check);
				} else {
					checkedTextView.setChecked(position == selectedLanguageIndex[0]);
					v.findViewById(R.id.topDivider).setVisibility(View.GONE);
					v.findViewById(R.id.bottomDivider).setVisibility(View.GONE);
					v.findViewById(R.id.switchLayout).setVisibility(View.GONE);
				}
				return v;
			}
		};

		b.setAdapter(singleChoiceAdapter, null);
		b.setSingleChoiceItems(mapLanguagesNames, selected, (dialog, which) -> {
			selectedLanguageIndex[0] = which;
			transliterateNames[0] = settings.MAP_TRANSLITERATE_NAMES.isSet()
					? transliterateNames[0]
					: mapLanguagesIds[which].equals("en");
			((AlertDialog) dialog).getListView().setSelection(which);
			singleChoiceAdapter.notifyDataSetChanged();
		});

		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			view.getSettings().MAP_TRANSLITERATE_NAMES.set(selectedLanguageIndex[0] > 0 && transliterateNames[0]);
			AlertDialog dlg = (AlertDialog) dialog;
			int index = dlg.getListView().getCheckedItemPosition();
			view.getSettings().MAP_PREFERRED_LOCALE.set(
					mapLanguagesIds[index]);
			activity.refreshMapComplete();
			String localeDescr = mapLanguagesIds[index];
			localeDescr = localeDescr == null || localeDescr.isEmpty() ? activity
					.getString(R.string.local_map_names) : localeDescr;
			item.setDescription(localeDescr);
			uiAdapter.onDataSetInvalidated();
		});
		b.show();
	}

	protected static void showRenderingPropertyDialog(@NonNull MapActivity activity, @NonNull RenderingRuleProperty p,
	                                                  @NonNull CommonPreference<String> pref, @NonNull ContextMenuItem item,
	                                                  @NonNull OnDataChangeUiAdapter uiAdapter, boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int currentProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		int themeRes = ConfigureMapMenu.getThemeRes(nightMode);
		AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));

		String propertyDescr = AndroidUtils.getRenderingStringPropertyDescription(app, p.getAttrName(), p.getName());

		// test old descr as title
		b.setTitle(propertyDescr);

		int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());
		if (i >= 0) {
			i++;
		} else if (Algorithms.isEmpty(pref.get())) {
			i = 0;
		}

		String[] possibleValuesString = ConfigureMapUtils.getRenderingPropertyPossibleValues(app, p);
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				possibleValuesString, nightMode, i, app, currentProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					if (which == 0) {
						pref.set("");
					} else {
						pref.set(p.getPossibleValues()[which - 1]);
					}
					activity.refreshMapComplete();
					String description = AndroidUtils.getRenderingStringPropertyValue(activity, pref.get());
					item.setDescription(description);
					String id = item.getId();
					if (!Algorithms.isEmpty(id)) {
						uiAdapter.onRefreshItem(id);
					} else {
						uiAdapter.onDataSetChanged();
					}
				}
		);
		b.setNegativeButton(R.string.shared_string_dismiss, null);
		b.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(b.show());
	}

	protected static void showPreferencesDialog(OnDataChangeUiAdapter uiAdapter,
	                                            ContextMenuItem item,
	                                            MapActivity activity,
	                                            String category,
	                                            List<RenderingRuleProperty> ps,
	                                            List<CommonPreference<Boolean>> prefs,
	                                            boolean nightMode,
	                                            @ColorInt int selectedProfileColor) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}

		int themeRes = ConfigureMapMenu.getThemeRes(nightMode);
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		boolean[] checkedItems = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			checkedItems[i] = prefs.get(i).get();
		}
		String[] vals = new String[ps.size()];
		for (int i = 0; i < ps.size(); i++) {
			RenderingRuleProperty p = ps.get(i);
			String propertyName = AndroidUtils.getRenderingStringPropertyName(activity, p.getAttrName(),
					p.getName());
			vals[i] = propertyName;
		}

		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createMultiChoiceAdapter(
				vals, nightMode, checkedItems, activity.getMyApplication(), selectedProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					checkedItems[which] = !checkedItems[which];
				}
		);
		builder.setAdapter(dialogAdapter, null);

		builder.setTitle(category);

		builder.setNegativeButton(R.string.shared_string_cancel, (dialog, whichButton) -> {
			boolean selected = false;
			for (int i = 0; i < prefs.size(); i++) {
				selected |= prefs.get(i).get();
			}
			item.setSelected(selected);
			item.setColor(activity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			uiAdapter.onDataSetInvalidated();
		});

		builder.setPositiveButton(R.string.shared_string_ok, (dialog, whichButton) -> {
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
		AlertDialog dialog = builder.create();
		dialogAdapter.setDialog(dialog);
		dialog.show();
	}
}
