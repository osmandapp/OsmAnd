package net.osmand.plus.dialogs;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class UnderlayMapMenu {
	private static final String TAG = "UnderlayMapMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity ma) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(ma, false);
		adapter.setDefaultLayoutId(R.layout.drawer_list_material_item);
		createLayersItems(adapter, ma);
		return adapter;
	}

	private static void createLayersItems(ContextMenuAdapter adapter , MapActivity activity) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<?> adapter, View view, int itemId, int pos) {
				Log.v(TAG, "onRowItemClick(" + "adapter=" + adapter + ", view=" + view + ", itemId=" + itemId + ", pos=" + pos + ")");
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				Log.v(TAG, "onContextMenuClick(" + "adapter=" + adapter + ", itemId=" + itemId + ", pos=" + pos + ", isChecked=" + isChecked + ")");
				return false;
			}
		};
		adapter.item(R.string.shared_string_show).listen(l).reg();
		// String appMode = " [" + settings.getApplicationMode().toHumanString(view.getApplication()) +"] ";
		adapter.item(R.string.underlay_transparency).layout(R.layout.progress_list_item).reg();
		adapter.item(R.string.map_underlay).layout(R.layout.two_line_list_item).listen(l).reg();
		adapter.item(R.string.show_polygons).listen(l).reg();

//		.selected(overlayLayer != null && overlayLayer.getMap() != null ? 1 : 0)

//		if(underlayLayer.getMap() != null){
//			settings.MAP_UNDERLAY.set(null);
//			updateMapLayers(mapView, null, layers);
//			layers.getMapControlsLayer().hideTransparencyBar(settings.MAP_TRANSPARENCY);
//		} else {
//			selectMapOverlayLayer(mapView, settings.MAP_UNDERLAY,settings.MAP_TRANSPARENCY,
//					mapActivity);
//		}
	}
}
