package net.osmand.aidl;

import static net.osmand.aidl.OsmandAidlApi.WIDGET_ID_PREFIX;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONFIGURE_MAP_ITEM_ID_SCHEME;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.AidlMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

import java.util.List;
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

	private final OsmandApplication app;

	private final Map<String, AidlMapWidgetWrapper> widgets = new ConcurrentHashMap<>();
	private final Map<String, TextInfoWidget> widgetControls = new ConcurrentHashMap<>();

	private final Map<String, AidlMapLayerWrapper> layers = new ConcurrentHashMap<>();
	private final Map<String, OsmandMapLayer> mapLayers = new ConcurrentHashMap<>();

	private final CommonPreference<Boolean> layersPref;

	private final String pack;
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

	void registerMapLayers(@NonNull Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (AidlMapLayerWrapper layer : layers.values()) {
			OsmandMapLayer mapLayer = mapLayers.get(layer.getId());
			if (mapLayer != null) {
				mapView.removeLayer(mapLayer);
			}
			mapLayer = new AidlMapLayer(context, layer, pack);
			mapView.addLayer(mapLayer, layer.getZOrder());
			mapLayers.put(layer.getId(), mapLayer);
		}
	}

	void registerLayerContextMenu(ContextMenuAdapter menuAdapter, MapActivity mapActivity) {
		ItemClickListener listener = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                              @NonNull View view, @NonNull ContextMenuItem item) {
				CompoundButton btn = view.findViewById(R.id.toggle_item);
				if (btn != null && btn.getVisibility() == View.VISIBLE) {
					btn.setChecked(!btn.isChecked());
					item.setColor(app, btn.isChecked() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					uiAdapter.onDataSetChanged();
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NotNull ContextMenuItem item,
			                                  boolean isChecked) {
				if (layersPref.set(isChecked)) {
					item.setColor(app, isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					item.setSelected(isChecked);
					uiAdapter.onDataSetChanged();
					mapActivity.refreshMap();
				}
				return false;
			}
		};
		boolean layersEnabled = layersPref.get();
		menuAdapter.addItem(new ContextMenuItem(CONFIGURE_MAP_ITEM_ID_SCHEME + AIDL_LAYERS_PREFIX + pack)
				.setTitle(name)
				.setListener(listener)
				.setSelected(layersEnabled)
				.setIcon(R.drawable.ic_extension_dark)
				.setColor(app, layersEnabled ? R.color.osmand_orange : ContextMenuItem.INVALID_ID));
	}

	void createWidgetControls(@NonNull MapActivity mapActivity,
	                          @NonNull List<MapWidgetInfo> widgetsInfos,
	                          @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);
		for (AidlMapWidgetWrapper widgetData : widgets.values()) {
			String baseWidgetId = widgetData.getId();
			String widgetKey = WIDGET_ID_PREFIX + baseWidgetId;
			WidgetsPanel defaultPanel = widgetData.isRightPanelByDefault() ? WidgetsPanel.RIGHT : WidgetsPanel.LEFT;
			TextInfoWidget widget = createWidgetControl(mapActivity, baseWidgetId, defaultPanel);
			MapWidgetInfo widgetInfo = createWidgetInfo(creator, widgetData, widget, widgetKey, defaultPanel);
			widgetsInfos.add(widgetInfo);
			widgetControls.put(baseWidgetId, widget);
		}
	}

	@Nullable
	public MapWidgetInfo askCreateWidgetInfo(@NonNull WidgetInfoCreator creator,
	                                         @NonNull MapWidget widget,
	                                         @NonNull String widgetId,
	                                         @NonNull WidgetsPanel panel) {
		for (AidlMapWidgetWrapper widgetData : widgets.values()) {
			String widgetKey = WIDGET_ID_PREFIX + widgetData.getId();
			if (widgetId.startsWith(widgetKey)) {
				return createWidgetInfo(creator, widgetData, widget, widgetId, panel);
			}
		}
		return null;
	}

	private MapWidgetInfo createWidgetInfo(@NonNull WidgetInfoCreator creator, @NonNull AidlMapWidgetWrapper widgetData,
	                                       @NonNull MapWidget widget, @NonNull String widgetId,
	                                       @NonNull WidgetsPanel widgetsPanel) {
		int iconId = AndroidUtils.getDrawableId(app, widgetData.getMenuIconName());
		int menuIconId = iconId != 0 ? iconId : ContextMenuItem.INVALID_ID;

		String title = widgetData.getMenuTitle();
		MapWidgetInfo widgetInfo = creator.createExternalWidget(
				widgetId, widget, menuIconId, title, widgetsPanel, widgetData.getOrder()
		);
		widgetInfo.setExternalProviderPackage(pack);
		return widgetInfo;
	}

	@Nullable
	public TextInfoWidget askCreateWidgetControl(@NonNull MapActivity mapActivity,
	                                             @Nullable String widgetId,
	                                             @Nullable WidgetsPanel panel) {
		if (widgetId != null) {
			for (AidlMapWidgetWrapper widgetData : widgets.values()) {
				String widgetKey = WIDGET_ID_PREFIX + widgetData.getId();
				if (widgetId.startsWith(widgetKey)) {
					return createWidgetControl(mapActivity, widgetId, panel);
				}
			}
		}
		return null;
	}

	public AidlMapWidgetWrapper getWidgetData(@Nullable String widgetId) {
		String dataId = null;
		if (widgetId != null) {
			dataId = WidgetType.getDefaultWidgetId(widgetId);
			dataId = dataId.replace(WIDGET_ID_PREFIX, "");
		}
		return widgets.get(dataId);
	}

	@NonNull
	TextInfoWidget createWidgetControl(@NonNull MapActivity mapActivity, @NonNull String widgetId,
	                                   @Nullable WidgetsPanel widgetsPanel) {
		return new AidlTextInfoWidget(mapActivity, widgetId, widgetsPanel);
	}

	class AidlTextInfoWidget extends SimpleWidget {

		private final String widgetId;
		private String cachedTxt;
		private String cachedSubtext;
		private Boolean cachedNight;
		private Integer cachedIcon;
		private boolean init = true;

		public AidlTextInfoWidget(@NonNull MapActivity mapActivity, @NonNull String widgetId, @Nullable WidgetsPanel widgetsPanel) {
			super(mapActivity, WidgetType.AIDL_WIDGET, widgetId, widgetsPanel);
			this.widgetId = widgetId;

			updateInfo(null);
			setOnClickListener(getOnClickListener());
		}

		@Override
		protected OnClickListener getOnClickListener() {
			return v -> {
				AidlMapWidgetWrapper widget = getWidgetData(widgetId);
				if (widget != null && widget.getIntentOnClick() != null) {
					AndroidUtils.startActivityIfSafe(app, widget.getIntentOnClick());
				}
			};
		}

		@Override
		public void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
			AidlMapWidgetWrapper widget = getWidgetData(widgetId);
			if (widget != null) {
				String txt = widget.getText();
				String subtext = widget.getDescription();
				boolean nightMode = drawSettings != null && drawSettings.isNightMode();
				int icon = AndroidUtils.getDrawableId(app, nightMode ? widget.getDarkIconName() : widget.getLightIconName());
				if (init || !Algorithms.objectEquals(txt, cachedTxt) || !Algorithms.objectEquals(subtext, cachedSubtext)
						|| !Algorithms.objectEquals(nightMode, cachedNight) || !Algorithms.objectEquals(icon, cachedIcon)) {
					init = false;
					cachedTxt = txt;
					cachedSubtext = subtext;
					cachedNight = nightMode;
					cachedIcon = icon;

					setText(txt, subtext);
					if (icon != 0) {
						setImageDrawable(icon);
					} else {
						setImageDrawable(null);
					}
				}
			} else {
				setText(null, null);
				setImageDrawable(null);
			}
		}

		@Nullable
		protected String getWidgetName() {
			return getName();
		}
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