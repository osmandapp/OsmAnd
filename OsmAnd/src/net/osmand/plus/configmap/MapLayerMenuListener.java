package net.osmand.plus.configmap;

import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TracksTabsFragment;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.transport.TransportLinesMenu;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.jetbrains.annotations.NotNull;

final class MapLayerMenuListener extends OnRowItemClick {

	private final MapActivity mapActivity;
	private final TransportLinesMenu transportLinesMenu;

	MapLayerMenuListener(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.transportLinesMenu = new TransportLinesMenu(mapActivity.getMyApplication());
	}

	@Override
	public boolean onRowItemClick(@NotNull OnDataChangeUiAdapter uiAdapter, @NotNull View view, @NotNull ContextMenuItem item) {
		int itemId = item.getTitleId();
		if (itemId == R.string.layer_poi) {
			showPoiFilterDialog(uiAdapter, item);
			return false;
		} else if (itemId == R.string.layer_gpx_layer) {
			TracksTabsFragment.showInstance(mapActivity.getSupportFragmentManager());
			return false;
		} else if (itemId == R.string.rendering_category_transport) {
			TransportLinesMenu.showTransportsDialog(mapActivity);
			return false;
		} else {
			CompoundButton btn = view.findViewById(R.id.toggle_item);
			if (btn != null && btn.getVisibility() == View.VISIBLE) {
				btn.setChecked(!btn.isChecked());
				item.setColor(mapActivity, btn.isChecked() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				uiAdapter.onDataSetChanged();
				return false;
			} else {
				return onContextMenuClick(uiAdapter, view, item, false);
			}
		}
	}

	@Override
	public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view,
	                                  @NotNull ContextMenuItem item, boolean isChecked) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		PoiFiltersHelper poiFiltersHelper = app.getPoiFilters();
		if (item.getSelected() != null) {
			item.setColor(mapActivity, isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
		}
		int itemId = item.getTitleId();
		if (itemId == R.string.layer_poi) {
			PoiUIFilter wiki = poiFiltersHelper.getTopWikiPoiFilter();
			poiFiltersHelper.clearSelectedPoiFilters(wiki);
			if (isChecked) {
				showPoiFilterDialog(uiAdapter, item);
			} else {
				item.setDescription(poiFiltersHelper.getSelectedPoiFiltersName(wiki));
			}
		} else if (itemId == R.string.layer_amenity_label) {
			settings.SHOW_POI_LABEL.set(isChecked);
		} else if (itemId == R.string.shared_string_favorites) {
			settings.SHOW_FAVORITES.set(isChecked);
		} else if (itemId == R.string.layer_gpx_layer) {
			GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();
			if (selectedGpxHelper.isAnyGpxFileSelected()) {
				selectedGpxHelper.clearAllGpxFilesToShow(true);
				item.setDescription(selectedGpxHelper.getGpxDescription());
			} else {
				TracksTabsFragment.showInstance(mapActivity.getSupportFragmentManager());
			}
		} else if (itemId == R.string.rendering_category_transport) {
			boolean selected = transportLinesMenu.isShowAnyTransport();
			transportLinesMenu.toggleTransportLines(mapActivity, !selected);
		} else if (itemId == R.string.map_markers) {
			settings.SHOW_MAP_MARKERS.set(isChecked);
		} else if (itemId == R.string.layer_map) {
			if (!PluginsHelper.isActive(OsmandRasterMapsPlugin.class)) {
				PluginsFragment.showInstance(mapActivity.getSupportFragmentManager());
				app.showToastMessage(R.string.map_online_plugin_is_not_installed);
			} else if (uiAdapter != null) {
				mapActivity.getMapLayers().selectMapSourceLayer(mapActivity, item, uiAdapter);
			}
			return false;
		} else if (itemId == R.string.show_borders_of_downloaded_maps) {
			settings.SHOW_BORDERS_OF_DOWNLOADED_MAPS.set(isChecked);
		}
		if (uiAdapter != null) {
			uiAdapter.onDataSetChanged();
		}
		mapActivity.updateLayers();
		mapActivity.refreshMap();
		return false;
	}

	private void showPoiFilterDialog(@Nullable OnDataChangeUiAdapter uiAdapter, @NonNull ContextMenuItem item) {
		PoiFiltersHelper poiFiltersHelper = mapActivity.getMyApplication().getPoiFilters();
		PoiUIFilter wiki = poiFiltersHelper.getTopWikiPoiFilter();
		MapLayers.DismissListener dismissListener = () -> {
			PoiFiltersHelper pf = mapActivity.getMyApplication().getPoiFilters();
			boolean selected = pf.isShowingAnyPoi(wiki);
			item.setSelected(selected);
			item.setDescription(pf.getSelectedPoiFiltersName(wiki));
			item.setColor(mapActivity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			if (uiAdapter != null) {
				uiAdapter.onDataSetChanged();
			}
		};
		MapLayers mapLayers = mapActivity.getMapLayers();
		boolean isMultiChoice = poiFiltersHelper.getSelectedPoiFilters(wiki).size() > 1;
		if (isMultiChoice) {
			mapLayers.showMultiChoicePoiFilterDialog(mapActivity, dismissListener);
		} else {
			mapLayers.showSingleChoicePoiFilterDialog(mapActivity, dismissListener);
		}
	}
}
