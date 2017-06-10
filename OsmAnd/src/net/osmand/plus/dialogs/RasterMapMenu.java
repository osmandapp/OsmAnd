package net.osmand.plus.dialogs;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.LayerTransparencySeekbarMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin.OnMapSelectedCallback;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin.RasterMapType;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.RouteLayer;

public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity,
													   final RasterMapType type) {
		ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		createLayersItems(adapter, mapActivity, type);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity,
										  final RasterMapType type) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
		assert plugin != null;
		final OsmandSettings.CommonPreference<Integer> mapTransparencyPreference;
		final OsmandSettings.CommonPreference<String> mapTypePreference;
		final OsmandSettings.CommonPreference<String> exMapTypePreference;
		final LayerTransparencySeekbarMode currentMapTypeSeekbarMode =
				type == RasterMapType.OVERLAY ? LayerTransparencySeekbarMode.OVERLAY : LayerTransparencySeekbarMode.UNDERLAY;
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
		final OsmandSettings.CommonPreference<Boolean> hidePolygonsPref =
				mapActivity.getMyApplication().getSettings().getCustomRenderBooleanProperty("noPolygons");

		String mapTypeDescr = mapTypePreference.get();
		final boolean selected = mapTypeDescr != null;
		final int toggleActionStringId = selected ? R.string.shared_string_enabled
				: R.string.shared_string_disabled;

		final OnMapSelectedCallback onMapSelectedCallback =
				new OnMapSelectedCallback() {
					@Override
					public void onMapSelected(boolean canceled) {
						if (type == RasterMapType.UNDERLAY && !canceled && !selected) {
							hidePolygonsPref.set(true);
							refreshMapComplete(mapActivity);
						} else if (type == RasterMapType.UNDERLAY && !canceled && mapTypePreference.get() == null) {
							hidePolygonsPref.set(false);
							refreshMapComplete(mapActivity);
						}
						mapActivity.getDashboard().refreshContent(true);
					}
				};
		final MapActivityLayers mapLayers = mapActivity.getMapLayers();
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
										  View view, int itemId, int pos) {
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
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
											  final int itemId, final int pos, final boolean isChecked) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							plugin.toggleUnderlayState(mapActivity, type, onMapSelectedCallback);
							refreshMapComplete(mapActivity);
						}
					});
				} else if (itemId == R.string.show_polygons) {
					hidePolygonsPref.set(!isChecked);
					refreshMapComplete(mapActivity);
				} else if (itemId == R.string.show_transparency_seekbar) {
					settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(
							isChecked ? currentMapTypeSeekbarMode : LayerTransparencySeekbarMode.OFF);
					if (isChecked) {
						mapLayers.getMapControlsLayer().showTransparencyBar(mapTransparencyPreference);
					} else {
						mapLayers.getMapControlsLayer().hideTransparencyBar(mapTransparencyPreference);
					}
					mapLayers.getMapControlsLayer().setTransparencyBarEnabled(isChecked);
				}
				return false;
			}
		};
		mapTypeDescr = selected ? mapTypeDescr : mapActivity.getString(R.string.shared_string_none);
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.hideDivider(true)
				.setListener(l)
				.setSelected(selected).createItem());
		if (selected) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(mapTypeString, mapActivity)
					.hideDivider(true)
					.setListener(l)
					.setLayout(R.layout.list_item_icon_and_menu_wide)
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
					.hideDivider(true)
					.setLayout(R.layout.list_item_progress)
					.setIcon(R.drawable.ic_action_opacity)
					.setProgress(mapTransparencyPreference.get())
					.setListener(l)
					.setIntegerListener(integerListener).createItem());
			if (type == RasterMapType.UNDERLAY) {
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.show_polygons, mapActivity)
						.hideDivider(true)
						.setListener(l)
						.setSelected(!hidePolygonsPref.get()).createItem());
			}
			Boolean transparencySwitchState = isSeekbarVisible(app, type);
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.show_transparency_seekbar, mapActivity)
					.hideDivider(true)
					.setListener(l)
					.setSelected(transparencySwitchState).createItem());
		}
	}

	@NonNull
	public static Boolean isSeekbarVisible(OsmandApplication app, RasterMapType type) {
		final OsmandSettings.LayerTransparencySeekbarMode currentMapTypeSeekbarMode =
				type == RasterMapType.OVERLAY ? OsmandSettings.LayerTransparencySeekbarMode.OVERLAY : OsmandSettings.LayerTransparencySeekbarMode.UNDERLAY;
		LayerTransparencySeekbarMode seekbarMode = app.getSettings().LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		return seekbarMode == LayerTransparencySeekbarMode.UNDEFINED || seekbarMode == currentMapTypeSeekbarMode;
	}

	public static void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		activity.getMapView().refreshMap(true);
	}
}
