package net.osmand.plus.mapillary;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import net.osmand.AndroidUtils;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.text.MessageFormat;
import java.util.List;

import static android.content.Intent.ACTION_VIEW;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAPILLARY;

public class MapillaryPlugin extends OsmandPlugin {
	public static final String ID = "osmand.mapillary";
	private static final String MAPILLARY_PACKAGE_ID = "app.mapillary";
	private OsmandSettings settings;
	private OsmandApplication app;

	private MapillaryRasterLayer rasterLayer;
	private MapillaryVectorLayer vectorLayer;
	private TextInfoWidget mapillaryControl;
	private MapWidgetRegInfo mapillaryWidgetRegInfo;

	public MapillaryPlugin(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_mapillary;
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.mapillary;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.plugin_mapillary_descr);
	}

	@Override
	public String getName() {
		return app.getString(R.string.mapillary);
	}

	@Override
	public void registerLayers(MapActivity activity) {
		createLayers();
		registerWidget(activity);
	}

	private void createLayers() {
		rasterLayer = new MapillaryRasterLayer();
		vectorLayer = new MapillaryVectorLayer();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		updateMapLayers(mapView, activity.getMapLayers(), false);
	}

	public void updateLayers(OsmandMapTileView mapView, MapActivity activity, boolean force) {
		updateMapLayers(mapView, activity.getMapLayers(), force);
	}

	private void updateMapLayers(OsmandMapTileView mapView, final MapActivityLayers layers, boolean force) {
		if (rasterLayer == null || vectorLayer == null) {
			createLayers();
		}
		if (isActive()) {
			ITileSource rasterSource = null;
			ITileSource vectorSource = null;
			if (settings.SHOW_MAPILLARY.get() || force) {
				rasterSource = settings.getTileSourceByName(TileSourceManager.getMapillaryRasterSource().getName(), false);
				vectorSource = settings.getTileSourceByName(TileSourceManager.getMapillaryVectorSource().getName(), false);
			}
			updateLayer(mapView, rasterSource, rasterLayer, 0.61f);
			updateLayer(mapView, vectorSource, vectorLayer, 0.62f);
		} else {
			mapView.removeLayer(rasterLayer);
			rasterLayer.setMap(null);
			mapView.removeLayer(vectorLayer);
			vectorLayer.setMap(null);
		}
		layers.updateMapSource(mapView, null);
	}

	private void updateLayer(OsmandMapTileView mapView, ITileSource mapillarySource, MapTileLayer layer, float layerOrder) {
		if (!Algorithms.objectEquals(mapillarySource, layer.getMap()) || !mapView.isLayerVisible(layer)) {
			if (mapView.getMapRenderer() == null && !mapView.isLayerVisible(layer)) {
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
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.OnRowItemClick() {

			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
				if (itemId == R.string.mapillary) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAPILLARY, AndroidUtils.getCenterViewCoordinates(view));
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId, final int pos, boolean isChecked, int[] viewCoordinates) {
				final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
				if (itemId == R.string.mapillary) {
					OsmandMapTileView mapView = mapActivity.getMapView();
					MapActivityLayers mapLayers = mapActivity.getMapLayers();
					settings.SHOW_MAPILLARY.set(!settings.SHOW_MAPILLARY.get());
					updateMapLayers(mapView, mapLayers, false);
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
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(MAPILLARY)
				.setTitleId(R.string.mapillary, mapActivity)
				.setSelected(settings.SHOW_MAPILLARY.get())
				.setColor(settings.SHOW_MAPILLARY.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_mapillary)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener)
				.setPosition(11)
				.createItem());
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
		mapillaryControl = createMonitoringControl(activity);
		mapillaryWidgetRegInfo = layer.registerSideWidget(mapillaryControl,
				R.drawable.ic_action_mapillary, R.string.mapillary, "mapillary", false, 19);
		layer.recreateControls();
	}

	private TextInfoWidget createMonitoringControl(final MapActivity map) {
		mapillaryControl = new TextInfoWidget(map);
		mapillaryControl.setText(map.getString(R.string.mapillary), "");
		mapillaryControl.setIcons(R.drawable.widget_mapillary_day, R.drawable.widget_mapillary_night);
		mapillaryControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openMapillary(map, null);
			}
		});

		return mapillaryControl;
	}

	private void setWidgetVisible(MapActivity mapActivity, boolean visible) {
		if (mapillaryWidgetRegInfo != null) {
			final List<ApplicationMode> allModes = ApplicationMode.allPossibleValues();
			for (ApplicationMode mode : allModes) {
				mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(mode, mapillaryWidgetRegInfo, visible, false);
			}
			MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
			if (mil != null) {
				mil.recreateControls();
			}
			mapActivity.refreshMap();
		}
	}

	public static boolean openMapillary(FragmentActivity activity, String imageKey) {
		boolean success = false;
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if (isPackageInstalled(MAPILLARY_PACKAGE_ID, app)) {
			try {
				if (imageKey != null) {
					Intent intent = new Intent(ACTION_VIEW, Uri.parse(MessageFormat.format("mapillary://mapillary/photo/{0}?image_key={0}", imageKey)));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					app.startActivity(intent);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mapillary://mapillary/capture"));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					app.startActivity(intent);
				}
			} catch (ActivityNotFoundException e) {
				new MapillaryInstallDialogFragment().show(activity.getSupportFragmentManager(), MapillaryInstallDialogFragment.TAG);
			}
			success = true;
		} else {
			new MapillaryInstallDialogFragment().show(activity.getSupportFragmentManager(), MapillaryInstallDialogFragment.TAG);
		}
		return success;
	}

	public static boolean installMapillary(Activity activity, OsmandApplication app) {
		app.logEvent("install_mapillary");
		boolean success = execInstall(app, Version.getUrlWithUtmRef(app, MAPILLARY_PACKAGE_ID));
		if (!success) {
			success = execInstall(app, "https://play.google.com/store/apps/details?id=" + MAPILLARY_PACKAGE_ID);
		}
		return success;
	}

	private static boolean execInstall(OsmandApplication app, String url) {
		Intent intent = new Intent(ACTION_VIEW, Uri.parse(url));
		try {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			app.startActivity(intent);
			return true;
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static class MapillaryFirstDialogFragment extends BottomSheetDialogFragment {
		public static final String TAG = "MapillaryFirstDialogFragment";

		private static final String KEY_SHOW_WIDGET = "key_show_widget";
		private boolean showWidget = true;

		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			if (savedInstanceState != null) {
				showWidget = savedInstanceState.getBoolean(KEY_SHOW_WIDGET, true);
			}

			View view = inflater.inflate(R.layout.mapillary_first_dialog, container, false);
			final SwitchCompat widgetSwitch = (SwitchCompat) view.findViewById(R.id.widget_switch);
			widgetSwitch.setChecked(showWidget);
			widgetSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					showWidget(isChecked);
				}
			});
			view.findViewById(R.id.actionButton).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showWidget(widgetSwitch.isChecked());
					dismiss();
				}
			});
			showWidget(showWidget);
			return view;
		}

		private void showWidget(boolean show) {
			FragmentActivity activity = getActivity();
			MapillaryPlugin plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
			if (plugin != null && activity instanceof MapActivity) {
				plugin.setWidgetVisible((MapActivity) activity, show);
			}
		}

		@Override
		public void onSaveInstanceState(@NonNull Bundle outState) {
			outState.putBoolean(KEY_SHOW_WIDGET, showWidget);
		}
	}
}
