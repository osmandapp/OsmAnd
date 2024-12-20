package net.osmand.plus.configmap;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.SwitchCompat;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

public class ConfigureMapDialogs {

	public static void showMapMagnifierDialog(@NonNull OsmandMapTileView view) {
		OsmandPreference<Float> density = view.getSettings().MAP_DENSITY;
		int p = (int) (density.get() * 100);
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

	protected static void showMapLanguageDialog(
			@NonNull MapActivity activity, boolean nightMode,
			@NonNull ContextMenuItem item, @NonNull OnDataChangeUiAdapter uiAdapter
	) {

		int[] selectedLanguageIndex = new int[1];
		boolean[] transliterateNames = new boolean[1];

		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int profileColor = ColorUtilities.getAppModeColor(app, nightMode);


		OsmandMapTileView view = activity.getMapView();
		Context ctx = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
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
				ctx, R.layout.single_choice_switch_item, R.id.text1, mapLanguagesNames) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				AppCompatCheckedTextView checkedTextView = v.findViewById(R.id.text1);
				UiUtilities.setupCompoundButtonDrawable(app, nightMode, profileColor, checkedTextView.getCheckMarkDrawable());

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
					UiUtilities.setupCompoundButton(nightMode, profileColor, check);
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

	protected static void showRenderingPropertyDialog(
			@NonNull MapActivity activity, @NonNull RenderingRuleProperty p,
			@NonNull CommonPreference<String> pref, @NonNull ContextMenuItem item,
			@NonNull OnDataChangeUiAdapter uiAdapter, boolean nightMode
	) {
		OsmandApplication app = activity.getMyApplication();
		String title = AndroidUtils.getRenderingStringPropertyDescription(app, p.getAttrName(), p.getName());
		String[] possibleValuesString = ConfigureMapUtils.getRenderingPropertyPossibleValues(app, p);
		int selectedIndex = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());
		if (selectedIndex >= 0) {
			selectedIndex++;
		} else if (Algorithms.isEmpty(pref.get())) {
			selectedIndex = 0;
		}

		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(title)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode))
				.setNegativeButton(R.string.shared_string_dismiss, null);

		CustomAlert.showSingleSelection(dialogData, possibleValuesString, selectedIndex, v -> {
			int which = (int) v.getTag();
			if (which == 0) {
				pref.set("");
			} else {
				pref.set(p.getPossibleValues()[which - 1]);
			}
			activity.refreshMapComplete();
			item.setDescription(AndroidUtils.getRenderingStringPropertyValue(activity, pref.get()));
			String id = item.getId();
			if (!Algorithms.isEmpty(id)) {
				uiAdapter.onRefreshItem(id);
			} else {
				uiAdapter.onDataSetChanged();
			}
		});
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
