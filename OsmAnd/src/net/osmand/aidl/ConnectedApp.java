package net.osmand.aidl;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import net.osmand.AndroidUtils;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AidlMapLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ConnectedApp implements Comparable<ConnectedApp> {

	public static final String AIDL_LAYERS_PREFIX = "aidl_layers_";
	public static final String AIDL_WIDGETS_PREFIX = "aidl_widgets_";

	static final String AIDL_OBJECT_ID = "aidl_object_id";
	static final String AIDL_PACKAGE_NAME = "aidl_package_name";

	static final String AIDL_ADD_MAP_WIDGET = "aidl_add_map_widget";
	static final String AIDL_REMOVE_MAP_WIDGET = "aidl_remove_map_widget";

	static final String AIDL_ADD_MAP_LAYER = "aidl_add_map_layer";
	static final String AIDL_REMOVE_MAP_LAYER = "aidl_remove_map_layer";

	static final String PACK_KEY = "pack";
	static final String ENABLED_KEY = "enabled";

	private OsmandApplication app;

	private Map<String, AidlMapWidgetWrapper> widgets = new ConcurrentHashMap<>();
	private Map<String, TextInfoWidget> widgetControls = new ConcurrentHashMap<>();

	private Map<String, AidlMapLayerWrapper> layers = new ConcurrentHashMap<>();
	private Map<String, OsmandMapLayer> mapLayers = new ConcurrentHashMap<>();

	private OsmandSettings.CommonPreference<Boolean> layersPref;

	private String pack;
	private String name;

	private Drawable icon;

	private boolean enabled;

	ConnectedApp(OsmandApplication app, String pack, boolean enabled) {
		this.app = app;
		this.pack = pack;
		this.enabled = enabled;
		layersPref = app.getSettings().registerBooleanPreference(AIDL_LAYERS_PREFIX + pack, true).cache();
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@NonNull
	public String getPack() {
		return pack;
	}

	@Nullable
	public Drawable getIcon() {
		return icon;
	}

	@NonNull
	public Map<String, AidlMapWidgetWrapper> getWidgets() {
		return widgets;
	}

	@NonNull
	public Map<String, TextInfoWidget> getWidgetControls() {
		return widgetControls;
	}

	@NonNull
	public Map<String, AidlMapLayerWrapper> getLayers() {
		return layers;
	}

	@NonNull
	public Map<String, OsmandMapLayer> getMapLayers() {
		return mapLayers;
	}

	void switchEnabled() {
		enabled = !enabled;
	}

	void registerMapLayers(@NonNull MapActivity mapActivity) {
		for (AidlMapLayerWrapper layer : layers.values()) {
			OsmandMapLayer mapLayer = mapLayers.get(layer.getId());
			if (mapLayer != null) {
				mapActivity.getMapView().removeLayer(mapLayer);
			}
			mapLayer = new AidlMapLayer(mapActivity, layer, pack);
			mapActivity.getMapView().addLayer(mapLayer, layer.getZOrder());
			mapLayers.put(layer.getId(), mapLayer);
		}
	}

	void registerLayerContextMenu(final ContextMenuAdapter menuAdapter, final MapActivity mapActivity) {
		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.OnRowItemClick() {

			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
				CompoundButton btn = view.findViewById(R.id.toggle_item);
				if (btn != null && btn.getVisibility() == View.VISIBLE) {
					btn.setChecked(!btn.isChecked());
					menuAdapter.getItem(position).setColorRes(btn.isChecked() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId,
			                                  int position, boolean isChecked, int[] viewCoordinates) {
				if (layersPref.set(isChecked)) {
					ContextMenuItem item = adapter.getItem(position);
					if (item != null) {
						item.setColorRes(isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						item.setSelected(isChecked);
						adapter.notifyDataSetChanged();
					}
					mapActivity.refreshMap();
				}
				return false;
			}
		};
		boolean layersEnabled = layersPref.get();
		menuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(AIDL_LAYERS_PREFIX + pack)
				.setTitle(name)
				.setListener(listener)
				.setSelected(layersEnabled)
				.setIcon(R.drawable.ic_extension_dark)
				.setColor(layersEnabled ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.createItem());
	}

	void registerWidgetControls(MapActivity mapActivity) {
		for (AidlMapWidgetWrapper widget : widgets.values()) {
			MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
			if (layer != null) {
				TextInfoWidget control = createWidgetControl(mapActivity, widget.getId());
				widgetControls.put(widget.getId(), control);
				int menuIconId = AndroidUtils.getDrawableId(mapActivity.getMyApplication(), widget.getMenuIconName());
				MapWidgetRegistry.MapWidgetRegInfo widgetInfo = layer.registerSideWidget(control, menuIconId,
						widget.getMenuTitle(), "aidl_widget_" + widget.getId(), false, widget.getOrder());
				if (!mapActivity.getMapLayers().getMapWidgetRegistry().isVisible(widgetInfo.key)) {
					mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(widgetInfo, true, false);
				}
			}
		}
	}

	TextInfoWidget createWidgetControl(final MapActivity mapActivity, final String widgetId) {
		TextInfoWidget control = new TextInfoWidget(mapActivity) {
			@Override
			public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
				AidlMapWidgetWrapper widget = widgets.get(widgetId);
				if (widget != null) {
					String txt = widget.getText();
					String subtext = widget.getDescription();
					boolean night = drawSettings != null && drawSettings.isNightMode();
					int icon = AndroidUtils.getDrawableId(mapActivity.getMyApplication(), night ? widget.getDarkIconName() : widget.getLightIconName());
					setText(txt, subtext);
					if (icon != 0) {
						setImageDrawable(icon);
					} else {
						setImageDrawable(null);
					}
					return true;
				}
				return false;
			}
		};
		control.updateInfo(null);

		control.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AidlMapWidgetWrapper widget = widgets.get(widgetId);
				if (widget != null && widget.getIntentOnClick() != null) {
					app.startActivity(widget.getIntentOnClick());
				}
			}
		});
		return control;
	}

	boolean addMapWidget(AidlMapWidgetWrapper widget) {
		if (widget != null) {
			if (widgets.containsKey(widget.getId())) {
				updateMapWidget(widget);
			} else {
				widgets.put(widget.getId(), widget);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_MAP_WIDGET);
				intent.putExtra(AIDL_OBJECT_ID, widget.getId());
				intent.putExtra(AIDL_PACKAGE_NAME, pack);
				app.sendBroadcast(intent);
			}
			app.getAidlApi().reloadMap();
			return true;
		}
		return false;
	}

	boolean removeMapWidget(String widgetId) {
		if (!Algorithms.isEmpty(widgetId) && widgets.containsKey(widgetId)) {
			widgets.remove(widgetId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_MAP_WIDGET);
			intent.putExtra(AIDL_OBJECT_ID, widgetId);
			intent.putExtra(AIDL_PACKAGE_NAME, pack);
			app.sendBroadcast(intent);
			return true;
		}
		return false;
	}

	boolean updateMapWidget(AidlMapWidgetWrapper widget) {
		if (widget != null && widgets.containsKey(widget.getId())) {
			widgets.put(widget.getId(), widget);
			app.getAidlApi().reloadMap();
			return true;
		}
		return false;
	}

	boolean addMapLayer(AidlMapLayerWrapper layer) {
		if (layer != null) {
			if (layers.containsKey(layer.getId())) {
				updateMapLayer(layer);
			} else {
				layers.put(layer.getId(), layer);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_MAP_LAYER);
				intent.putExtra(AIDL_OBJECT_ID, layer.getId());
				intent.putExtra(AIDL_PACKAGE_NAME, pack);
				app.sendBroadcast(intent);
			}
			app.getAidlApi().reloadMap();
			return true;
		}
		return false;
	}

	boolean removeMapLayer(String layerId) {
		if (!Algorithms.isEmpty(layerId) && layers.containsKey(layerId)) {
			layers.remove(layerId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_MAP_LAYER);
			intent.putExtra(AIDL_OBJECT_ID, layerId);
			intent.putExtra(AIDL_PACKAGE_NAME, pack);
			app.sendBroadcast(intent);
			return true;
		}
		return false;
	}

	boolean updateMapLayer(AidlMapLayerWrapper layer) {
		if (layer != null) {
			AidlMapLayerWrapper existingLayer = layers.get(layer.getId());
			if (existingLayer != null) {
				for (AidlMapPointWrapper point : layer.getPoints()) {
					existingLayer.putPoint(point);
				}
				existingLayer.copyZoomBounds(layer);
				app.getAidlApi().reloadMap();
				return true;
			}
		}
		return false;
	}

	boolean putMapPoint(String layerId, AidlMapPointWrapper point) {
		AidlMapLayerWrapper layer = layers.get(layerId);
		if (layer != null) {
			layer.putPoint(point);
			app.getAidlApi().reloadMap();
			return true;
		}
		return false;
	}

	boolean updateMapPoint(String layerId, AidlMapPointWrapper point, boolean updateOpenedMenuAndMap) {
		AidlMapLayerWrapper layer = layers.get(layerId);
		if (layer != null) {
			layer.putPoint(point);
			app.getAidlApi().reloadMap();
			if (updateOpenedMenuAndMap && app.getAidlApi().getAMapPointUpdateListener() != null) {
				app.getAidlApi().getAMapPointUpdateListener().onAMapPointUpdated(point, layerId);
			}
			return true;
		}
		return false;
	}

	boolean removeMapPoint(String layerId, String pointId) {
		AidlMapLayerWrapper layer = layers.get(layerId);
		if (layer != null) {
			layer.removePoint(pointId);
			app.getAidlApi().reloadMap();
			return true;
		}
		return false;
	}

	boolean updateApplicationInfo(PackageManager pm) {
		try {
			ApplicationInfo ai = pm.getPackageInfo(pack, 0).applicationInfo;
			name = ai.loadLabel(pm).toString();
			icon = ai.loadIcon(pm);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			// ignore
		}
		return false;
	}

	@Override
	public int compareTo(@NonNull ConnectedApp app) {
		if (name != null && app.name != null) {
			return name.compareTo(app.name);
		}
		return 0;
	}
}