package net.osmand.plus.plugins.osmedit.menu;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class OsmNotesMenu {

	private static final Integer[] zoomIntValues = {8, 9, 10, 11, 12, 13, 14, 15, 16};

	public static ContextMenuAdapter createListAdapter(@NonNull MapActivity mapActivity) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getMyApplication());
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);

		if (plugin == null) {
			return;
		}

		boolean nightMode = isNightMode(app);
		int themeRes = getThemeRes(app);
		int selectedModeColor = settings.getApplicationMode().getProfileColor(nightMode);

		final int osmNotesStringId = R.string.layer_osm_bugs;
		final int showZoomLevelStringId = R.string.show_from_zoom_level;
		final int showClosedNotesStringId = R.string.show_closed_notes;

		String[] zoomStrings = getZoomStrings(mapActivity);

		OnRowItemClick l = new OnRowItemClick() {
			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NotNull ContextMenuItem item,
			                                  boolean isChecked) {
				int itemId = item.getTitleId();
				if (itemId == osmNotesStringId) {
					plugin.SHOW_OSM_BUGS.set(isChecked);
					plugin.updateLayers(mapActivity, mapActivity);
					mapActivity.refreshMap();
					mapActivity.getDashboard().refreshContent(true);
				} else if (itemId == showZoomLevelStringId) {
					int checked = Arrays.asList(zoomIntValues).indexOf(plugin.SHOW_OSM_BUGS_MIN_ZOOM.get());

					DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
							zoomStrings, nightMode, checked, app, selectedModeColor, themeRes, v -> {
								int which = (int) v.getTag();
								plugin.SHOW_OSM_BUGS_MIN_ZOOM.set(zoomIntValues[which]);
								item.setDescription(zoomStrings[which]);
								uiAdapter.onDataSetChanged();
								mapActivity.refreshMap();
							}
					);
					AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(mapActivity, themeRes))
							.setTitle(R.string.show_from_zoom_level)
							.setAdapter(dialogAdapter, null)
							.setNegativeButton(R.string.shared_string_dismiss, null);
					dialogAdapter.setDialog(b.show());
				} else if (itemId == showClosedNotesStringId) {
					plugin.SHOW_CLOSED_OSM_BUGS.set(isChecked);
					mapActivity.refreshMap();
				}
				return false;
			}
		};

		boolean showOsmBugs = plugin.SHOW_OSM_BUGS.get();
		int toggleIconColorId;
		if (showOsmBugs) {
			toggleIconColorId = ColorUtilities.getActiveColorId(nightMode);
		} else {
			toggleIconColorId = ContextMenuItem.INVALID_ID;
		}

		adapter.addItem(new ContextMenuItem(null)
				.setTitleId(osmNotesStringId, mapActivity)
				.setDescription(mapActivity.getString(R.string.switch_osm_notes_visibility_desc))
				.setIcon(R.drawable.ic_action_osm_note)
				.setColor(app, toggleIconColorId)
				.setListener(l)
				.setSelected(showOsmBugs));

		adapter.addItem(new ContextMenuItem(null)
				.setTitleId(showZoomLevelStringId, mapActivity)
				.setDescription(zoomStrings[Arrays.asList(zoomIntValues).indexOf(plugin.SHOW_OSM_BUGS_MIN_ZOOM.get())])
				.setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_magnifier)
				.setListener(l)
				.setClickable(showOsmBugs));

		adapter.addItem(new ContextMenuItem(null)
				.setTitleId(showClosedNotesStringId, mapActivity)
				.setIcon(R.drawable.ic_action_note_dark)
				.setListener(l)
				.setSelected(plugin.SHOW_CLOSED_OSM_BUGS.get())
				.setClickable(showOsmBugs)
				.setHideDivider(true));

		adapter.addItem(new ContextMenuItem(null)
				.setLayout(R.layout.card_bottom_divider)
				.setClickable(false));
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
