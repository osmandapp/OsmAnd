package net.osmand.plus.mapillary;

import android.app.Activity;
import android.widget.ArrayAdapter;

import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

public class MapillaryPlugin extends OsmandPlugin {
	public static final String ID = "osmand.mapillary";
	private OsmandSettings settings;
	private OsmandApplication app;

	private MapillaryLayer rasterLayer;

	public MapillaryPlugin(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_mapillary;
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.online_maps;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.mapillary);
	}

	@Override
	public String getName() {
		return app.getString(R.string.mapillary);
	}

	@Override
	public void registerLayers(MapActivity activity) {
		createLayers();
	}

	private void createLayers() {
		rasterLayer = new MapillaryLayer();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		updateMapLayers(mapView, activity.getMapLayers());
	}

	public void updateMapLayers(OsmandMapTileView mapView, final MapActivityLayers layers) {
		if (rasterLayer == null) {
			createLayers();
		}
		if (isActive()) {
			updateLayer(mapView, rasterLayer, 0.6f);
		} else {
			mapView.removeLayer(rasterLayer);
			rasterLayer.setMap(null);
		}
		layers.updateMapSource(mapView, null);
	}

	public void updateLayer(OsmandMapTileView mapView, MapTileLayer layer, float layerOrder) {
		ITileSource mapillarySource = null;
		if (settings.SHOW_MAPILLARY.get()) {
			mapillarySource = settings.getTileSourceByName(TileSourceManager.getMapillarySource().getName(), false);
		}
		if (!Algorithms.objectEquals(mapillarySource, layer.getMap())) {
			if (mapillarySource == null) {
				mapView.removeLayer(layer);
			} else if (mapView.getMapRenderer() == null) {
				mapView.addLayer(layer, layerOrder);
			}
			layer.setMap(mapillarySource);
			mapView.refreshMap();
		}
	}

	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}

	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView,
												ContextMenuAdapter adapter,
												final MapActivity mapActivity) {
		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.OnRowItemClick() {

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId, final int pos, boolean isChecked) {
				final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
				if (itemId == R.string.mapillary) {
					OsmandMapTileView mapView = mapActivity.getMapView();
					MapActivityLayers mapLayers = mapActivity.getMapLayers();
					settings.SHOW_MAPILLARY.set(!settings.SHOW_MAPILLARY.get());
					updateMapLayers(mapView, mapLayers);
					ContextMenuItem item = adapter.getItem(pos);
					if (item != null) {
						item.setSelected(settings.SHOW_MAPILLARY.get());
						item.setColorRes(settings.SHOW_MAPILLARY.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						adapter.notifyDataSetChanged();
					}
				}
				return false;
			}
		};

		if (rasterLayer.getMap() == null) {
			settings.SHOW_MAPILLARY.set(false);
		}
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.mapillary, mapActivity)
				.setSelected(settings.SHOW_MAPILLARY.get())
				.setColor(settings.SHOW_MAPILLARY.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_mapillary)
				.setListener(listener)
				.setPosition(11)
				.createItem());
	}
}
