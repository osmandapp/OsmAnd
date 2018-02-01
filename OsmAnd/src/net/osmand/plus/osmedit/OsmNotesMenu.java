package net.osmand.plus.osmedit;

import android.widget.ArrayAdapter;
import android.widget.Toast;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class OsmNotesMenu {

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter adapter, final MapActivity mapActivity) {
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		final OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);

		if (plugin == null) {
			return;
		}

		final int osmNotesStringId = R.string.layer_osm_bugs;
		final int showZoomLevelStringId = R.string.show_from_zoom_level;
		final int showClosedNotesStringId = R.string.show_closed_notes;

		final OsmandPreference<Boolean> showOsmBugsPref = settings.SHOW_OSM_BUGS;
		final CommonPreference<Boolean> showClosedOsmBugsPref = settings.SHOW_CLOSED_OSM_BUGS;

		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId,
											  int position, boolean isChecked, int[] viewCoordinates) {
				if (itemId == osmNotesStringId) {
					showOsmBugsPref.set(isChecked);
					plugin.updateLayers(mapActivity.getMapView(), mapActivity);
					mapActivity.refreshMap();
				} else if (itemId == showZoomLevelStringId) {
					Toast.makeText(mapActivity, "show zoom level", Toast.LENGTH_SHORT).show(); //todo
				} else if (itemId == showClosedNotesStringId) {
					showClosedOsmBugsPref.set(isChecked);
					mapActivity.refreshMap();
				}
				return false;
			}
		};

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(osmNotesStringId, mapActivity)
				.setDescription(mapActivity.getString(R.string.switch_osm_notes_visibility_desc))
				.setIcon(R.drawable.ic_action_bug_dark)
				.setListener(l)
				.setSelected(showOsmBugsPref.get())
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(showZoomLevelStringId, mapActivity)
				.setDescription("11") //todo
				.setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_magnifier)
				.setListener(l)
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(showClosedNotesStringId, mapActivity)
				.setIcon(R.drawable.ic_action_note_dark)
				.setListener(l)
				.setSelected(showClosedOsmBugsPref.get())
				.hideDivider(true)
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setLayout(R.layout.card_bottom_divider)
				.setClickable(false)
				.createItem());
	}
}
