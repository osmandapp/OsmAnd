package net.osmand.plus.dialogs;

import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.RouteLayer;

public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity,
													   final OsmandRasterMapsPlugin.RasterMapType type) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity, false);
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		createLayersItems(adapter, mapActivity, type);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity,
										  final OsmandRasterMapsPlugin.RasterMapType type) {
		OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
		assert plugin != null;
		final OsmandSettings.CommonPreference<Integer> mapTransparencyPreference;
		final OsmandSettings.CommonPreference<String> mapTypePreference;
		final OsmandSettings.CommonPreference<String> exMapTypePreference;
		@StringRes final int mapTypeString;
		@StringRes final int mapTypeStringTransparency;
		if (type == OsmandRasterMapsPlugin.RasterMapType.OVERLAY) {
			mapTransparencyPreference = settings.MAP_OVERLAY_TRANSPARENCY;
			mapTypePreference = settings.MAP_OVERLAY;
			exMapTypePreference = settings.MAP_OVERLAY_PREVIOUS;
			mapTypeString = R.string.map_overlay;
			mapTypeStringTransparency = R.string.overlay_transparency;
		} else if (type == OsmandRasterMapsPlugin.RasterMapType.UNDERLAY) {
			mapTransparencyPreference = settings.MAP_TRANSPARENCY;
			mapTypePreference = settings.MAP_UNDERLAY;
			exMapTypePreference = settings.MAP_UNDERLAY_PREVIOUS;
			mapTypeString = R.string.map_underlay;
			mapTypeStringTransparency = R.string.map_transparency;
		} else {
			throw new RuntimeException("Unexpected raster map type");
		}
		final OsmandSettings.CommonPreference<Boolean> hidePolygonsPref =
				mapActivity.getMyApplication().getSettings().getCustomRenderBooleanProperty("noPolygons");

		String mapTypeDescr = mapTypePreference.get();
		final boolean selected = mapTypeDescr != null;
		final int toggleActionStringId = selected ? R.string.shared_string_enabled
				: R.string.shared_string_disabled;

		final OsmandRasterMapsPlugin.OnMapSelectedCallback onMapSelectedCallback =
				new OsmandRasterMapsPlugin.OnMapSelectedCallback() {
					@Override
					public void onMapSelected() {
						mapActivity.getDashboard().refreshContent(true);
					}
				};
		final MapActivityLayers mapLayers = mapActivity.getMapLayers();
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<?> adapter, View view, int itemId, int pos) {
				if (itemId == mapTypeString) {
					if (selected) {
						plugin.selectMapOverlayLayer(mapActivity.getMapView(), mapTypePreference,
								exMapTypePreference, true, mapActivity, onMapSelectedCallback);
					}
					return false;
				}
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<?> adapter,
											  int itemId, int pos, boolean isChecked) {
				if (itemId == toggleActionStringId) {
					if (isChecked) {
						mapLayers.getMapControlsLayer().showTransparencyBar(mapTransparencyPreference);
					} else {
						mapLayers.getMapControlsLayer().hideTransparencyBar(mapTransparencyPreference);
					}
					mapLayers.getMapControlsLayer().setTransparencyBarEnabled(isChecked);
					plugin.toggleUnderlayState(mapActivity, type, onMapSelectedCallback);
					if (type == OsmandRasterMapsPlugin.RasterMapType.UNDERLAY && !isChecked) {
						hidePolygonsPref.set(false);
						mapActivity.getDashboard().refreshContent(true);
					}
					refreshMapComplete(mapActivity);
				} else if (itemId == R.string.show_polygons) {
					hidePolygonsPref.set(!isChecked);
					refreshMapComplete(mapActivity);
				} else if (itemId == R.string.show_transparency_seekbar) {
					settings.SHOW_LAYER_TRANSPARENCY_SEEKBAR.set(isChecked);
					mapLayers.getMapControlsLayer().setTransparencyBarEnabled(isChecked);
					if (isChecked) {
						mapLayers.getMapControlsLayer().showTransparencyBar(mapTransparencyPreference);
					}
				}
				return false;
			}
		};
		mapTypeDescr = selected ? mapTypeDescr : mapActivity.getString(R.string.shared_string_none);
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.setListener(l)
				.setSelected(selected).createItem());
		if (selected) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(mapTypeString, mapActivity)
					.setListener(l)
					.setLayout(R.layout.two_line_list_item)
					.setDescription(mapTypeDescr).createItem());
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
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(mapTypeStringTransparency, mapActivity)
					.setLayout(R.layout.progress_list_item)
					.setColorIcon(R.drawable.ic_action_opacity)
					.setProgress(mapTransparencyPreference.get())
					.setListener(l)
					.setIntegerListener(integerListener).createItem());
			if (type == OsmandRasterMapsPlugin.RasterMapType.UNDERLAY) {
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.show_polygons, mapActivity)
						.setListener(l)
						.setSelected(hidePolygonsPref.get()).createItem());
			}
			Boolean transparencySwitchState = settings.SHOW_LAYER_TRANSPARENCY_SEEKBAR.get()
					&& mapLayers.getMapControlsLayer().isTransparencyBarInitialized();
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.show_transparency_seekbar, mapActivity)
					.setListener(l)
					.setSelected(transparencySwitchState).createItem());
		}
	}

	private static void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		GPXLayer gpx = activity.getMapView().getLayerByClass(GPXLayer.class);
		if (gpx != null) {
			gpx.updateLayerStyle();
		}
		RouteLayer rte = activity.getMapView().getLayerByClass(RouteLayer.class);
		if (rte != null) {
			rte.updateLayerStyle();
		}
		activity.getMapView().refreshMap(true);
	}
}
