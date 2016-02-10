package net.osmand.plus.dialogs;

import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.RouteLayer;

public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity,
													   final OsmandRasterMapsPlugin.RasterMapType type) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity, false);
		adapter.setDefaultLayoutId(R.layout.drawer_list_material_item);
		createLayersItems(adapter, mapActivity, type);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity,
										  final OsmandRasterMapsPlugin.RasterMapType type) {
		OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
		final MapTileLayer rasterMapLayer;
		final OsmandSettings.CommonPreference<Integer> mapTransparencyPreference;
		final OsmandSettings.CommonPreference<String> mapTypePreference;
		@StringRes final int mapTypeString;
		if (type == OsmandRasterMapsPlugin.RasterMapType.OVERLAY) {
			rasterMapLayer = plugin.getOverlayLayer();
			mapTransparencyPreference = settings.MAP_OVERLAY_TRANSPARENCY;
			mapTypePreference = settings.MAP_OVERLAY;
			mapTypeString = R.string.map_overlay;
		} else if (type == OsmandRasterMapsPlugin.RasterMapType.UNDERLAY){
			rasterMapLayer = plugin.getUnderlayLayer();
			mapTransparencyPreference = settings.MAP_TRANSPARENCY;
			mapTypePreference = settings.MAP_UNDERLAY;
			mapTypeString = R.string.map_underlay;
		} else {
			throw new RuntimeException("Unexpected raster map type");
		}
		final OsmandSettings.CommonPreference<Boolean> hidePolygonsPref =
				mapActivity.getMyApplication().getSettings().getCustomRenderBooleanProperty("noPolygons");
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<?> adapter, View view, int itemId, int pos) {
				Log.v(TAG, "onRowItemClick(" + "adapter=" + adapter + ", view=" + view + ", itemId=" + itemId + ", pos=" + pos + ")");
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<?> adapter,
											  int itemId, int pos, boolean isChecked) {
				Log.v(TAG, "onContextMenuClick(" + "adapter=" + adapter + ", itemId=" + itemId + ", pos=" + pos + ", isChecked=" + isChecked + ")");
				if (itemId == R.string.shared_string_show) {
					MapActivityLayers mapLayers = mapActivity.getMapLayers();
					if (isChecked) {
						mapLayers.getMapControlsLayer().showTransparencyBar(mapTransparencyPreference);
					} else {
						mapLayers.getMapControlsLayer().hideTransparencyBar(mapTransparencyPreference);
					}
					plugin.toggleUnderlayState(mapActivity, type, new OsmandRasterMapsPlugin.OnMapSelectedCallback() {
						@Override
						public void onMapSelected() {
							mapActivity.getDashboard().refreshContent(true);
						}
					});
				} else if (itemId == R.string.show_polygons) {
					hidePolygonsPref.set(!isChecked);
					refreshMapComplete(mapActivity);
				}
				return false;
			}
		};
		int selected = mapTypePreference.get() != null ? 1 : 0;
		contextMenuAdapter.item(R.string.shared_string_show).listen(l).selected(selected).reg();
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
		// Please note this does not modify the transparency of the underlay map, but of the base map, of course!
		contextMenuAdapter.item(R.string.map_transparency).layout(R.layout.progress_list_item)
				.progress(mapTransparencyPreference.get()).listenInteger(integerListener).reg();
		contextMenuAdapter.item(mapTypeString).layout(R.layout.two_line_list_item).description(mapTypePreference.get()).reg();
		contextMenuAdapter.item(R.string.show_polygons).listen(l).selected(hidePolygonsPref.get() ? 0 : 1).reg();
	}

	private static void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		GPXLayer gpx = activity.getMapView().getLayerByClass(GPXLayer.class);
		if(gpx != null) {
			gpx.updateLayerStyle();
		}
		RouteLayer rte = activity.getMapView().getLayerByClass(RouteLayer.class);
		if(rte != null) {
			rte.updateLayerStyle();
		}
		activity.getMapView().refreshMap(true);
	}
}
