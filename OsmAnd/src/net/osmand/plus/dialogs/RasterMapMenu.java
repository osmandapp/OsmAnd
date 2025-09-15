package net.osmand.plus.dialogs;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.map.ITileSource;
import net.osmand.map.ParameterType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.OnMapSelectedCallback;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.RasterMapType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnIntegerValueChangedListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.jetbrains.annotations.NotNull;

import static net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode.OVERLAY;
import static net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode.UNDERLAY;


public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";

	public static ContextMenuAdapter createListAdapter(MapActivity mapActivity,
	                                                   RasterMapType type) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getApp());
		createLayersItems(adapter, mapActivity, type);
		return adapter;
	}

	private static void createLayersItems(ContextMenuAdapter contextMenuAdapter,
	                                      MapActivity mapActivity,
	                                      RasterMapType type) {
		OsmandApplication app = mapActivity.getApp();
		OsmandSettings settings = app.getSettings();
		OsmandRasterMapsPlugin plugin = PluginsHelper.getPlugin(OsmandRasterMapsPlugin.class);
		assert plugin != null;
		CommonPreference<Integer> mapTransparencyPreference;
		CommonPreference<String> mapTypePreference;
		CommonPreference<String> exMapTypePreference;
		LayerTransparencySeekbarMode currentMode = type == RasterMapType.OVERLAY ? OVERLAY : UNDERLAY;
		@StringRes int mapTypeString;
		@StringRes int mapTypeStringTransparency;
		if (type == RasterMapType.OVERLAY) {
			mapTransparencyPreference = settings.MAP_OVERLAY_TRANSPARENCY;
			mapTypePreference = settings.MAP_OVERLAY;
			exMapTypePreference = settings.MAP_OVERLAY_PREVIOUS;
			mapTypeString = R.string.map_overlay;
			mapTypeStringTransparency = R.string.overlay_transparency;
		} else if (type == RasterMapType.UNDERLAY) {
			mapTransparencyPreference = settings.MAP_TRANSPARENCY;
			mapTypePreference = settings.MAP_UNDERLAY;
			exMapTypePreference = settings.MAP_UNDERLAY_PREVIOUS;
			mapTypeString = R.string.map_underlay;
			mapTypeStringTransparency = R.string.map_transparency;
		} else {
			throw new RuntimeException("Unexpected raster map type");
		}

		String mapSourceTitle = mapTypePreference.get();
		if (mapSourceTitle != null) {
			mapSourceTitle = settings.getTileSourceTitle(mapSourceTitle);
		}

		boolean mapSelected = mapSourceTitle != null;
		int toggleActionStringId = mapSelected ? R.string.shared_string_on : R.string.shared_string_off;

		OnMapSelectedCallback onMapSelectedCallback =
				canceled -> {
					mapActivity.getDashboard().refreshContent(true);
					if (type == RasterMapType.UNDERLAY && !canceled) {
						mapActivity.refreshMapComplete();
					}
				};
		MapLayers mapLayers = mapActivity.getMapLayers();
		OnRowItemClick l = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                              @NonNull View view, @NonNull ContextMenuItem item) {
				if (item.getTitleId() == mapTypeString) {
					if (mapSelected) {
						plugin.selectMapOverlayLayer(mapTypePreference,
								exMapTypePreference, true, mapActivity, onMapSelectedCallback);
					}
					return false;
				}
				return super.onRowItemClick(uiAdapter, view, item);
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NotNull ContextMenuItem item,
			                                  boolean isChecked) {
				int itemId = item.getTitleId();
				if (itemId == toggleActionStringId) {
					app.runInUIThread(() -> {
						plugin.toggleUnderlayState(mapActivity, type, onMapSelectedCallback);
					});
				} else if (itemId == R.string.show_polygons) {
					settings.SHOW_POLYGONS_WHEN_UNDERLAY_IS_ON.set(isChecked);
					mapActivity.refreshMapComplete();
				} else if (itemId == R.string.show_transparency_seekbar) {
					updateTransparencyBarVisibility(isChecked);
				} else if (itemId == R.string.show_map_symbols) {
					settings.KEEP_MAP_LABELS_VISIBLE.set(isChecked);
					mapActivity.getMapView().refreshMap();
				} else if (itemId == R.string.show_parameter_seekbar) {
					if (isChecked) {
						settings.SHOW_MAP_LAYER_PARAMETER.set(true);
						MapTileLayer overlayLayer = plugin.getOverlayLayer();
						if (overlayLayer != null) {
							mapLayers.getMapControlsLayer().getMapTransparencyHelper().showParameterBar(overlayLayer);
						}
					} else {
						settings.SHOW_MAP_LAYER_PARAMETER.set(false);
						mapLayers.getMapControlsLayer().getMapTransparencyHelper().hideParameterBar();
						updateTransparencyBarVisibility(isSeekbarVisible(app, RasterMapType.OVERLAY));
					}
				}
				return false;
			}

			private void updateTransparencyBarVisibility(boolean visible) {
				if (visible) {
					settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(currentMode);
					mapLayers.getMapControlsLayer().getMapTransparencyHelper().showTransparencyBar(mapTransparencyPreference);
				} else // if(settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == currentMode)
				{
					settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(LayerTransparencySeekbarMode.OFF);
					mapLayers.getMapControlsLayer().getMapTransparencyHelper().hideTransparencyBar();
				}
			}
		};

		mapSourceTitle = mapSelected ? mapSourceTitle : mapActivity.getString(R.string.shared_string_none);
		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(toggleActionStringId, mapActivity)
				.setHideDivider(true)
				.setListener(l)
				.setSelected(mapSelected));
		if (mapSelected) {
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(mapTypeString, mapActivity)
					.setHideDivider(true)
					.setListener(l)
					.setLayout(R.layout.list_item_icon_and_menu_wide)
					.setDescription(mapSourceTitle));
			OnIntegerValueChangedListener integerListener =
					newValue -> {
						mapTransparencyPreference.set(newValue);
						mapActivity.getMapLayers().getMapControlsLayer().getMapTransparencyHelper().updateTransparencySliderValue();
						mapActivity.refreshMap();
						return false;
					};
			// android:max="255" in layout is expected
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(mapTypeStringTransparency, mapActivity)
					.setHideDivider(true)
					.setLayout(R.layout.list_item_progress)
					.setIcon(R.drawable.ic_action_opacity)
					.setProgress(mapTransparencyPreference.get())
					.setListener(l)
					.setIntegerListener(integerListener));
			if (type == RasterMapType.UNDERLAY) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitleId(R.string.show_polygons, mapActivity)
						.setHideDivider(true)
						.setListener(l)
						.setSelected(settings.SHOW_POLYGONS_WHEN_UNDERLAY_IS_ON.get()));
			}
			Boolean transparencySwitchState = isSeekbarVisible(app, type);
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(R.string.show_transparency_seekbar, mapActivity)
					.setHideDivider(true)
					.setListener(l)
					.setSelected(transparencySwitchState));
			ITileSource oveplayMap = plugin.getOverlayLayer().getMap();
			if (type == RasterMapType.OVERLAY) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitleId(R.string.show_map_symbols, mapActivity)
						.setDescription(app.getString(R.string.show_map_symbols_desc))
						.setHideDivider(true)
						.setListener(l)
						.setSelected(settings.KEEP_MAP_LABELS_VISIBLE.get()));
			}
			if (type == RasterMapType.OVERLAY && oveplayMap != null && oveplayMap.getParamType() != ParameterType.UNDEFINED) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitleId(R.string.show_parameter_seekbar, mapActivity)
						.setHideDivider(true)
						.setListener(l)
						.setSelected(settings.SHOW_MAP_LAYER_PARAMETER.get()));
			}
		}
	}

	@NonNull
	public static Boolean isSeekbarVisible(@NonNull OsmandApplication app, @NonNull RasterMapType type) {
		LayerTransparencySeekbarMode currentMode = type == RasterMapType.OVERLAY ? OVERLAY : UNDERLAY;
		LayerTransparencySeekbarMode seekbarMode = app.getSettings().LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		return seekbarMode == LayerTransparencySeekbarMode.UNDEFINED || seekbarMode == currentMode;
	}
}
