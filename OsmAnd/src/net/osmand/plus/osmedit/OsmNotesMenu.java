package net.osmand.plus.osmedit;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.Arrays;

public class OsmNotesMenu {

	private static Integer[] zoomIntValues = {8, 9, 10, 11, 12, 13, 14, 15, 16};

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getMyApplication());
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter adapter, final MapActivity mapActivity) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);

		if (plugin == null) {
			return;
		}

		final boolean nightMode = isNightMode(app);
		final int themeRes = getThemeRes(app);
		final int selectedModeColor = settings.getApplicationMode().getProfileColor(nightMode);

		final int osmNotesStringId = R.string.layer_osm_bugs;
		final int showZoomLevelStringId = R.string.show_from_zoom_level;
		final int showClosedNotesStringId = R.string.show_closed_notes;

		final OsmandPreference<Boolean> showOsmBugsPref = settings.SHOW_OSM_BUGS;
		final CommonPreference<Boolean> showClosedOsmBugsPref = settings.SHOW_CLOSED_OSM_BUGS;
		final CommonPreference<Integer> showOsmBugsZoomPref = settings.SHOW_OSM_BUGS_MIN_ZOOM;

		final String[] zoomStrings = getZoomStrings(mapActivity);

		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId,
											  final int position, boolean isChecked, int[] viewCoordinates) {
				if (itemId == osmNotesStringId) {
					showOsmBugsPref.set(isChecked);
					plugin.updateLayers(mapActivity.getMapView(), mapActivity);
					mapActivity.refreshMap();
					mapActivity.getDashboard().refreshContent(true);
				} else if (itemId == showZoomLevelStringId) {
					int checked = Arrays.asList(zoomIntValues).indexOf(showOsmBugsZoomPref.get());

					DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
							zoomStrings, nightMode, checked, app, selectedModeColor, themeRes, new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									int which = (int) v.getTag();
									showOsmBugsZoomPref.set(zoomIntValues[which]);
									ContextMenuItem item = adapter.getItem(position);
									if (item != null) {
										item.setDescription(zoomStrings[which]);
										adapter.notifyDataSetChanged();
									}
									mapActivity.refreshMap();
								}
							}
					);
					AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(mapActivity, themeRes))
							.setTitle(R.string.show_from_zoom_level)
							.setAdapter(dialogAdapter, null)
							.setNegativeButton(R.string.shared_string_dismiss, null);
					dialogAdapter.setDialog(b.show());
				} else if (itemId == showClosedNotesStringId) {
					showClosedOsmBugsPref.set(isChecked);
					mapActivity.refreshMap();
				}
				return false;
			}
		};

		boolean showOsmBugs = showOsmBugsPref.get();
		int toggleIconColorId;
		if (showOsmBugs) {
			toggleIconColorId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		} else {
			toggleIconColorId = ContextMenuItem.INVALID_ID;
		}

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(osmNotesStringId, mapActivity)
				.setDescription(mapActivity.getString(R.string.switch_osm_notes_visibility_desc))
				.setIcon(R.drawable.ic_action_osm_note)
				.setColor(app, toggleIconColorId)
				.setListener(l)
				.setSelected(showOsmBugs)
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(showZoomLevelStringId, mapActivity)
				.setDescription(zoomStrings[Arrays.asList(zoomIntValues).indexOf(showOsmBugsZoomPref.get())])
				.setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_magnifier)
				.setListener(l)
				.setClickable(showOsmBugs)
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(showClosedNotesStringId, mapActivity)
				.setIcon(R.drawable.ic_action_note_dark)
				.setListener(l)
				.setSelected(showClosedOsmBugsPref.get())
				.setClickable(showOsmBugs)
				.hideDivider(true)
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setLayout(R.layout.card_bottom_divider)
				.setClickable(false)
				.createItem());
	}

	private static String[] getZoomStrings(Context context) {
		String[] res = new String[zoomIntValues.length];
		for (int i = 0; i < zoomIntValues.length; i++) {
			String strVal = String.valueOf(zoomIntValues[i]);
			res[i] = i == 0 ? context.getString(R.string.rendering_value_default_name) + " (" + strVal + ")" : strVal;
		}
		return res;
	}
	
	public static boolean isNightMode(OsmandApplication app) {
		if (app == null) {
			return false;
		}
		return app.getDaynightHelper().isNightModeForMapControls();
	}
	
	public static int getThemeRes(OsmandApplication app) {
		return isNightMode(app) ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}
}
