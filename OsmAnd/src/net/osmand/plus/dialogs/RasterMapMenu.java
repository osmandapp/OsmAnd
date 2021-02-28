package net.osmand.plus.dialogs;

import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.rastermaps.LayerTransparencySeekbarMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin.OnMapSelectedCallback;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin.RasterMapType;


public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";
	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity,
													   final RasterMapType type) {
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getMyApplication());
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);
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
		final CommonPreference<Integer> mapTransparencyPreference;
		final CommonPreference<String> mapTypePreference;
		final CommonPreference<String> exMapTypePreference;
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

		final CommonPreference<Boolean> hidePolygonsPref =
				mapActivity.getMyApplication().getSettings().getCustomRenderBooleanProperty("noPolygons");
		final CommonPreference<Boolean> hideWaterPolygonsPref =
				mapActivity.getMyApplication().getSettings().getCustomRenderBooleanProperty("hideWaterPolygons");


		String mapTypeDescr = mapTypePreference.get();
		if (mapTypeDescr!=null && mapTypeDescr.contains(".sqlitedb")) {
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
		final MapActivityLayers mapLayers = mapActivity.getMapLayers();
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
										  View view, int itemId, int pos) {
				if (itemId == mapTypeString) {
					if (mapSelected) {
						plugin.selectMapOverlayLayer(mapActivity.getMapView(), mapTypePreference,
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
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							plugin.toggleUnderlayState(mapActivity, type, onMapSelectedCallback);
							mapActivity.refreshMapComplete();
						}
					});
				} else if (itemId == R.string.show_polygons) {
					hidePolygonsPref.set(!isChecked);
					hideWaterPolygonsPref.set(!isChecked);
					mapActivity.refreshMapComplete();
				} else if (itemId == R.string.show_transparency_seekbar) {
					if (isChecked) {
						settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(currentMapTypeSeekbarMode);
						mapLayers.getMapControlsLayer().showTransparencyBar(mapTransparencyPreference, true);
					} else // if(settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == currentMapTypeSeekbarMode)
					{
						settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(LayerTransparencySeekbarMode.OFF);
						mapLayers.getMapControlsLayer().hideTransparencyBar();
					}
				}
				return false;
			}
		};

		mapTypeDescr = mapSelected ? mapTypeDescr : mapActivity.getString(R.string.shared_string_none);
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.hideDivider(true)
				.setListener(l)
				.setSelected(mapSelected).createItem());
		if (mapSelected) {
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
							mapActivity.getMapLayers().getMapControlsLayer().updateTransparencySlider();
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
		final LayerTransparencySeekbarMode currentMapTypeSeekbarMode =
				type == RasterMapType.OVERLAY ? LayerTransparencySeekbarMode.OVERLAY : LayerTransparencySeekbarMode.UNDERLAY;
		LayerTransparencySeekbarMode seekbarMode = app.getSettings().LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		return seekbarMode == LayerTransparencySeekbarMode.UNDEFINED || seekbarMode == currentMapTypeSeekbarMode;
	}
}
