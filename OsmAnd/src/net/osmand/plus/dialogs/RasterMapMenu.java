package net.osmand.plus.dialogs;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.views.MapTileLayer;

public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity,
													   final OsmandRasterMapsPlugin.RasterMapType type) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity, false);
		adapter.setDefaultLayoutId(R.layout.drawer_list_material_item);
		createLayersItems(adapter, mapActivity, type);
		return adapter;
	}

	private static void createLayersItems(ContextMenuAdapter adapter,
										  final MapActivity mapActivity,
										  final OsmandRasterMapsPlugin.RasterMapType type) {
		OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
		final MapTileLayer rasterMapLayer;
		final OsmandSettings.CommonPreference<Integer> mapTransparencyPreference;
		if (type == OsmandRasterMapsPlugin.RasterMapType.OVERLAY) {
			rasterMapLayer = plugin.getOverlayLayer();
			mapTransparencyPreference = settings.MAP_OVERLAY_TRANSPARENCY;
		} else {
			rasterMapLayer = plugin.getUnderlayLayer();
			mapTransparencyPreference = settings.MAP_TRANSPARENCY;
		}
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<?> adapter, View view, int itemId, int pos) {
				Log.v(TAG, "onRowItemClick(" + "adapter=" + adapter + ", view=" + view + ", itemId=" + itemId + ", pos=" + pos + ")");
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				Log.v(TAG, "onContextMenuClick(" + "adapter=" + adapter + ", itemId=" + itemId + ", pos=" + pos + ", isChecked=" + isChecked + ")");
				if (itemId == R.string.shared_string_show) {
					plugin.toggleUnderlayState(mapActivity, type);
				}
				return false;
			}
		};
		adapter.item(R.string.shared_string_show).listen(l).selected(rasterMapLayer != null && rasterMapLayer.getMap() != null ? 1 : 0).reg();
		// String appMode = " [" + settings.getApplicationMode().toHumanString(view.getApplication()) +"] ";
		ContextMenuAdapter.OnIntegerValueChangedListener integerListener =
				new ContextMenuAdapter.OnIntegerValueChangedListener() {
					@Override
					public boolean onIntegerValueChangedListener(int newValue) {
						mapTransparencyPreference.set(newValue);
						mapActivity.getMapView().refreshMap();
						return false;
					}
				};
		// android:max="255" in layout is expected
		//adapter.item(R.string.underlay_transparency).layout(R.layout.progress_list_item)
		// Please note this does not modify the transparency of the underlay map, but of the base map, of course!
		adapter.item(R.string.map_transparency).layout(R.layout.progress_list_item)
				.progress(mapTransparencyPreference.get()).listenInteger(integerListener).reg();
		adapter.item(R.string.map_underlay).layout(R.layout.two_line_list_item).listen(l).reg();
		adapter.item(R.string.show_polygons).listen(l).reg();
	}
}
