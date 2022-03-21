package net.osmand.plus.dialogs;

import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.map.ITileSource;
import net.osmand.map.ParameterType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.OnMapSelectedCallback;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.RasterMapType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.ctxmenu.callback.OnIntegerValueChangedListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;

import static net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode.OVERLAY;
import static net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode.UNDERLAY;


public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity,
	                                                   final RasterMapType type) {
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getMyApplication());
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.setProfileDependent(true);
		createLayersItems(adapter, mapActivity, type);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
	                                      final MapActivity mapActivity,
	                                      final RasterMapType type) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final OsmandRasterMapsPlugin plugin = OsmandPlugin.getPlugin(OsmandRasterMapsPlugin.class);
		assert plugin != null;
		final CommonPreference<Integer> mapTransparencyPreference;
		final CommonPreference<String> mapTypePreference;
		final CommonPreference<String> exMapTypePreference;
		final LayerTransparencySeekbarMode currentMode = type == RasterMapType.OVERLAY ? OVERLAY : UNDERLAY;
		@StringRes final int mapTypeString;
		@StringRes final int mapTypeStringTransparency;
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

		CommonPreference<Boolean> hidePolygonsPref = settings.getCustomRenderBooleanProperty("noPolygons");
		CommonPreference<Boolean> hideWaterPolygonsPref = settings.getCustomRenderBooleanProperty("hideWaterPolygons");

		String mapTypeDescr = mapTypePreference.get();
		if (mapTypeDescr != null && mapTypeDescr.contains(".sqlitedb")) {
			mapTypeDescr = mapTypeDescr.replaceFirst(".sqlitedb", "");
		}

		final boolean mapSelected = mapTypeDescr != null;
		final int toggleActionStringId = mapSelected ? R.string.shared_string_on
				: R.string.shared_string_off;

		final OnMapSelectedCallback onMapSelectedCallback =
				new OnMapSelectedCallback() {
					@Override
					public void onMapSelected(boolean canceled) {
						mapActivity.getDashboard().refreshContent(true);
						boolean refreshToHidePolygons = type == RasterMapType.UNDERLAY;
						if (refreshToHidePolygons) {
							mapActivity.refreshMapComplete();
						}
					}
				};
		final MapLayers mapLayers = mapActivity.getMapLayers();
		OnRowItemClick l = new OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
			                              View view, int itemId, int pos) {
				if (itemId == mapTypeString) {
					if (mapSelected) {
						plugin.selectMapOverlayLayer(mapTypePreference,
								exMapTypePreference, true, mapActivity, onMapSelectedCallback);
					}
					return false;
				}
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
			                                  final int itemId, final int pos, final boolean isChecked, int[] viewCoordinates) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(() -> {
						plugin.toggleUnderlayState(mapActivity, type, onMapSelectedCallback);
						mapActivity.refreshMapComplete();
					});
				} else if (itemId == R.string.show_polygons) {
					hidePolygonsPref.set(!isChecked);
					hideWaterPolygonsPref.set(!isChecked);
					mapActivity.refreshMapComplete();
				} else if (itemId == R.string.show_transparency_seekbar) {
					updateTransparencyBarVisibility(isChecked);
				} else if (itemId == R.string.show_parameter_seekbar) {
					if (isChecked) {
						settings.SHOW_MAP_LAYER_PARAMETER.set(true);
						MapTileLayer overlayLayer = plugin.getOverlayLayer();
						if (overlayLayer != null) {
							mapLayers.getMapControlsLayer().showParameterBar(overlayLayer);
						}
					} else {
						settings.SHOW_MAP_LAYER_PARAMETER.set(false);
						mapLayers.getMapControlsLayer().hideParameterBar();
						updateTransparencyBarVisibility(isSeekbarVisible(app, RasterMapType.OVERLAY));
					}
				}
				return false;
			}

			private void updateTransparencyBarVisibility(boolean visible) {
				if (visible) {
					settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(currentMode);
					mapLayers.getMapControlsLayer().showTransparencyBar(mapTransparencyPreference);
				} else // if(settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == currentMode)
				{
					settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(LayerTransparencySeekbarMode.OFF);
					mapLayers.getMapControlsLayer().hideTransparencyBar();
				}
			}
		};

		mapTypeDescr = mapSelected ? mapTypeDescr : mapActivity.getString(R.string.shared_string_none);
		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(toggleActionStringId, mapActivity)
				.hideDivider(true)
				.setListener(l)
				.setSelected(mapSelected));
		if (mapSelected) {
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(mapTypeString, mapActivity)
					.hideDivider(true)
					.setListener(l)
					.setLayout(R.layout.list_item_icon_and_menu_wide)
					.setDescription(mapTypeDescr));
			OnIntegerValueChangedListener integerListener =
					newValue -> {
						mapTransparencyPreference.set(newValue);
						mapActivity.getMapLayers().getMapControlsLayer().updateTransparencySliderValue();
						mapActivity.refreshMap();
						return false;
					};
			// android:max="255" in layout is expected
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(mapTypeStringTransparency, mapActivity)
					.hideDivider(true)
					.setLayout(R.layout.list_item_progress)
					.setIcon(R.drawable.ic_action_opacity)
					.setProgress(mapTransparencyPreference.get())
					.setListener(l)
					.setIntegerListener(integerListener));
			if (type == RasterMapType.UNDERLAY) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitleId(R.string.show_polygons, mapActivity)
						.hideDivider(true)
						.setListener(l)
						.setSelected(!hidePolygonsPref.get()));
			}
			Boolean transparencySwitchState = isSeekbarVisible(app, type);
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(R.string.show_transparency_seekbar, mapActivity)
					.hideDivider(true)
					.setListener(l)
					.setSelected(transparencySwitchState));
			ITileSource oveplayMap = plugin.getOverlayLayer().getMap();
			if (type == RasterMapType.OVERLAY && oveplayMap != null && oveplayMap.getParamType() != ParameterType.UNDEFINED) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitleId(R.string.show_parameter_seekbar, mapActivity)
						.hideDivider(true)
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
