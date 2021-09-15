package net.osmand.plus.dialogs;

import android.content.DialogInterface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.AndroidUtils;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.enums.DayNightMode;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import gnu.trove.list.array.TIntArrayList;

public class ConfigureMapDialogs {

	protected static void showMapModeDialog(@NonNull MapActivity activity, int themeRes, boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		final OsmandMapTileView view = activity.getMapView();
		AlertDialog.Builder bld = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		bld.setTitle(R.string.daynight);
		final String[] items = new String[DayNightMode.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = DayNightMode.values()[i].toHumanString(app);
		}
		int i = view.getSettings().DAYNIGHT_MODE.get().ordinal();
		bld.setNegativeButton(R.string.shared_string_dismiss, null);
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				items, nightMode, i, app, selectedProfileColor, themeRes, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int which = (int) v.getTag();
						view.getSettings().DAYNIGHT_MODE.set(DayNightMode.values()[which]);
						activity.refreshMapComplete();
						activity.getDashboard().refreshContent(true);
					}
				}
		);
		bld.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(bld.show());
	}

	protected static void showMapMagnifierDialog(MapActivity activity, ContextMenuAdapter adapter,
												 int themeRes, boolean nightMode,
												 int pos, ArrayAdapter<ContextMenuItem> ad) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		final OsmandMapTileView view = activity.getMapView();
		final OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
		AlertDialog.Builder bld = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		int p = (int) (mapDensity.get() * 100);
		final TIntArrayList tlist = new TIntArrayList(new int[] {25, 33, 50, 75, 100, 125, 150, 200, 300, 400});
		final List<String> values = new ArrayList<>();
		int i = -1;
		for (int k = 0; k <= tlist.size(); k++) {
			final boolean end = k == tlist.size();
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
				values.toArray(new String[0]), nightMode, i, app, selectedProfileColor, themeRes, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int which = (int) v.getTag();
						int p = tlist.get(which);
						mapDensity.set(p / 100.0f);
						view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
						MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
						if (mapContext != null) {
							mapContext.updateMapSettings();
						}
						adapter.getItem(pos).setDescription(
								String.format(Locale.UK, "%.0f", 100f * activity.getMyApplication()
										.getSettings().MAP_DENSITY.get())
										+ " %");
						ad.notifyDataSetInvalidated();
					}
				}
		);
		bld.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(bld.show());
	}

	protected static void showTextSizeDialog(MapActivity activity, ContextMenuAdapter adapter,
											 int themeRes, boolean nightMode,
											 int pos, ArrayAdapter<ContextMenuItem> ad) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);


		final OsmandMapTileView view = activity.getMapView();
		AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		// test old descr as title
		b.setTitle(R.string.text_size);
		final Float[] txtValues = new Float[] {0.33f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f};
		int selected = -1;
		final String[] txtNames = new String[txtValues.length];
		for (int i = 0; i < txtNames.length; i++) {
			txtNames[i] = (int) (txtValues[i] * 100) + " %";
			if (Math.abs(view.getSettings().TEXT_SCALE.get() - txtValues[i]) < 0.1f) {
				selected = i;
			}
		}
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				txtNames, nightMode, selected, app, selectedProfileColor, themeRes, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int which = (int) v.getTag();
						view.getSettings().TEXT_SCALE.set(txtValues[which]);
						activity.refreshMapComplete();
						adapter.getItem(pos).setDescription(ConfigureMapUtils.getScale(activity));
						ad.notifyDataSetInvalidated();
					}
				});
		b.setAdapter(dialogAdapter, null);
		b.setNegativeButton(R.string.shared_string_dismiss, null);
		dialogAdapter.setDialog(b.show());
	}

	protected static void showMapLanguageDialog(MapActivity activity, ContextMenuAdapter adapter, int themeRes, boolean nightMode, int pos, ArrayAdapter<ContextMenuItem> ad) {

		int[] selectedLanguageIndex = new int[1];
		boolean[] transliterateNames = new boolean[1];

		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);


		final OsmandMapTileView view = activity.getMapView();
		AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));

		b.setTitle(activity.getString(R.string.map_locale));

		final String[] txtIds = ConfigureMapUtils.getSortedMapNamesIds(activity, ConfigureMapUtils.MAP_NAMES_IDS,
				ConfigureMapUtils.getMapNamesValues(activity, ConfigureMapUtils.MAP_NAMES_IDS));
		final String[] txtValues = ConfigureMapUtils.getMapNamesValues(activity, txtIds);
		int selected = -1;
		for (int i = 0; i < txtIds.length; i++) {
			if (view.getSettings().MAP_PREFERRED_LOCALE.get().equals(txtIds[i])) {
				selected = i;
				break;
			}
		}
		selectedLanguageIndex[0] = selected;
		transliterateNames[0] = settings.MAP_TRANSLITERATE_NAMES.get();

		final OnCheckedChangeListener translitChangdListener = (buttonView, isChecked) -> transliterateNames[0] = isChecked;

		final ArrayAdapter<CharSequence> singleChoiceAdapter = new ArrayAdapter<CharSequence>(
				new ContextThemeWrapper(activity, themeRes), R.layout.single_choice_switch_item, R.id.text1, txtValues) {
			@NonNull
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				AppCompatCheckedTextView checkedTextView = v.findViewById(R.id.text1);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					UiUtilities.setupCompoundButtonDrawable(app, nightMode, selectedProfileColor, checkedTextView.getCheckMarkDrawable());
				}
				if (position == selectedLanguageIndex[0] && position > 0) {
					checkedTextView.setChecked(true);
					v.findViewById(R.id.topDivider).setVisibility(View.VISIBLE);
					v.findViewById(R.id.bottomDivider).setVisibility(View.VISIBLE);
					v.findViewById(R.id.switchLayout).setVisibility(View.VISIBLE);
					TextView switchText = v.findViewById(R.id.switchText);
					switchText.setText(activity.getString(R.string.translit_name_if_miss, txtValues[position]));
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
		b.setSingleChoiceItems(txtValues, selected, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				selectedLanguageIndex[0] = which;
				transliterateNames[0] = settings.MAP_TRANSLITERATE_NAMES.isSet() ? transliterateNames[0] : txtIds[which].equals("en");
				((AlertDialog) dialog).getListView().setSelection(which);
				singleChoiceAdapter.notifyDataSetChanged();
			}
		});

		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				view.getSettings().MAP_TRANSLITERATE_NAMES.set(selectedLanguageIndex[0] > 0 && transliterateNames[0]);
				AlertDialog dlg = (AlertDialog) dialog;
				int index = dlg.getListView().getCheckedItemPosition();
				view.getSettings().MAP_PREFERRED_LOCALE.set(
						txtIds[index]);
				activity.refreshMapComplete();
				String localeDescr = txtIds[index];
				localeDescr = localeDescr == null || localeDescr.isEmpty() ? activity
						.getString(R.string.local_map_names) : localeDescr;
				adapter.getItem(pos).setDescription(localeDescr);
				ad.notifyDataSetInvalidated();
			}
		});
		b.show();
	}

	protected static void showRenderingPropertyDialog(MapActivity activity, ContextMenuAdapter adapter, RenderingRuleProperty p,
													  CommonPreference<String> pref, int pos, boolean nightMode) {
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
					adapter.getItem(pos).setDescription(description);
				}
		);
		b.setNegativeButton(R.string.shared_string_dismiss, null);
		b.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(b.show());
	}

	protected static void showPreferencesDialog(ContextMenuAdapter adapter,
												ArrayAdapter<?> a,
												int pos,
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
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		boolean[] checkedItems = new boolean[prefs.size()];
		boolean[] tempPrefs = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			tempPrefs[i] = checkedItems[i] = prefs.get(i).get();
		}
		String[] vals = new String[ps.size()];
		for (int i = 0; i < ps.size(); i++) {
			RenderingRuleProperty p = ps.get(i);
			String propertyName = AndroidUtils.getRenderingStringPropertyName(activity, p.getAttrName(),
					p.getName());
			vals[i] = propertyName;
		}

		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createMultiChoiceAdapter(
				vals, nightMode, checkedItems, activity.getMyApplication(), selectedProfileColor, themeRes, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int which = (int) v.getTag();
						tempPrefs[which] = !tempPrefs[which];
					}
				}
		);
		builder.setAdapter(dialogAdapter, null);

		builder.setTitle(category);

		builder.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				boolean selected = false;
				for (int i = 0; i < prefs.size(); i++) {
					selected |= prefs.get(i).get();
				}
				adapter.getItem(pos).setSelected(selected);
				adapter.getItem(pos).setColor(activity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				a.notifyDataSetInvalidated();
			}
		});

		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				boolean selected = false;
				for (int i = 0; i < prefs.size(); i++) {
					prefs.get(i).set(tempPrefs[i]);
					selected |= tempPrefs[i];
				}
				if (adapter != null) {
					adapter.getItem(pos).setSelected(selected);
					adapter.getItem(pos).setColor(activity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				}
				a.notifyDataSetInvalidated();
				activity.refreshMapComplete();
				activity.getMapLayers().updateLayers(activity);
			}
		});
		AlertDialog dialog = builder.create();
		dialogAdapter.setDialog(dialog);
		dialog.show();
	}
}
