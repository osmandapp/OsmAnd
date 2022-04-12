package net.osmand.plus.configmap;

import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.transport.TransportLinesMenu;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;

final class MapLayerMenuListener extends OnRowItemClick {

	private final MapActivity mapActivity;

	MapLayerMenuListener(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@NonNull
	private List<String> getAlreadySelectedGpx() {
		GpxSelectionHelper selectedGpxHelper = mapActivity.getMyApplication().getSelectedGpxHelper();
		List<SelectedGpxFile> selectedGpxFiles = selectedGpxHelper.getSelectedGPXFiles();
		List<String> files = GpxUiHelper.getSelectedTrackPaths(mapActivity.getMyApplication());
		if (selectedGpxFiles.isEmpty()) {
			Map<GPXFile, Long> fls = selectedGpxHelper.getSelectedGpxFilesBackUp();
			for (Map.Entry<GPXFile, Long> f : fls.entrySet()) {
				if (!Algorithms.isEmpty(f.getKey().path)) {
					File file = new File(f.getKey().path);
					if (file.exists() && !file.isDirectory()) {
						files.add(f.getKey().path);
					}
				}
			}
		}
		return files;
	}

	@Override
	public boolean onRowItemClick(@NotNull OnDataChangeUiAdapter uiAdapter,
	                              @NotNull View view, ContextMenuItem item) {
		int itemId = item.getTitleId();
		if (itemId == R.string.layer_poi) {
			showPoiFilterDialog(uiAdapter, item);
			return false;
		} else if (itemId == R.string.layer_gpx_layer && item.getSelected()) {
			showGpxSelectionDialog(uiAdapter, item);
			return false;
		} else if (itemId == R.string.rendering_category_transport) {
			TransportLinesMenu.showTransportsDialog(mapActivity, result -> {
				item.setSelected(result);
				item.setColor(mapActivity, result ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				uiAdapter.onDataSetChanged();
				return true;
			});
			boolean selected = TransportLinesMenu.isShowLines(mapActivity.getMyApplication());
			if (!selected) {
				item.setSelected(true);
				item.setColor(mapActivity, R.color.osmand_orange);
				uiAdapter.onDataSetChanged();
			}
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
	public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		final PoiFiltersHelper poiFiltersHelper = mapActivity.getMyApplication().getPoiFilters();
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
			final GpxSelectionHelper selectedGpxHelper = mapActivity.getMyApplication().getSelectedGpxHelper();
			if (selectedGpxHelper.isShowingAnyGpxFiles()) {
				selectedGpxHelper.clearAllGpxFilesToShow(true);
				item.setDescription(selectedGpxHelper.getGpxDescription());
			} else {
				showGpxSelectionDialog(uiAdapter, item);
			}
		} else if (itemId == R.string.rendering_category_transport) {
			boolean selected = TransportLinesMenu.isShowLines(mapActivity.getMyApplication());
			TransportLinesMenu.toggleTransportLines(mapActivity, !selected, result -> {
				item.setSelected(result);
				item.setColor(mapActivity, result ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				uiAdapter.onDataSetChanged();
				return true;
			});
		} else if (itemId == R.string.map_markers) {
			settings.SHOW_MAP_MARKERS.set(isChecked);
		} else if (itemId == R.string.layer_map) {
			if (!OsmandPlugin.isActive(OsmandRasterMapsPlugin.class)) {
				PluginsFragment.showInstance(mapActivity.getSupportFragmentManager());
			} else {
				mapActivity.getMapLayers().selectMapLayer(mapActivity, item, uiAdapter);
			}
			return false;
		}
		uiAdapter.onDataSetChanged();
		mapActivity.updateLayers();
		mapActivity.refreshMap();
		return false;
	}

	private void showGpxSelectionDialog(OnDataChangeUiAdapter uiAdapter, ContextMenuItem item) {
		AlertDialog dialog = mapActivity.getMapLayers().showGPXFileLayer(getAlreadySelectedGpx(), mapActivity);
		dialog.setOnDismissListener(dlg -> {
			OsmandApplication app = mapActivity.getMyApplication();
			boolean selected = app.getSelectedGpxHelper().isShowingAnyGpxFiles();
			item.setSelected(selected);
			item.setDescription(app.getSelectedGpxHelper().getGpxDescription());
			item.setColor(mapActivity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			uiAdapter.onDataSetChanged();
		});
	}

	protected void showPoiFilterDialog(OnDataChangeUiAdapter uiAdapter,
	                                   ContextMenuItem item) {
		final PoiFiltersHelper poiFiltersHelper = mapActivity.getMyApplication().getPoiFilters();
		final PoiUIFilter wiki = poiFiltersHelper.getTopWikiPoiFilter();
		MapLayers.DismissListener dismissListener =
				() -> {
					PoiFiltersHelper pf = mapActivity.getMyApplication().getPoiFilters();
					boolean selected = pf.isShowingAnyPoi(wiki);
					item.setSelected(selected);
					item.setDescription(pf.getSelectedPoiFiltersName(wiki));
					item.setColor(mapActivity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					uiAdapter.onDataSetChanged();
				};
		boolean isMultiChoice = poiFiltersHelper.getSelectedPoiFilters(wiki).size() > 1;
		if (isMultiChoice) {
			mapActivity.getMapLayers().showMultiChoicePoiFilterDialog(mapActivity, dismissListener);
		} else {
			mapActivity.getMapLayers().showSingleChoicePoiFilterDialog(mapActivity, dismissListener);
		}
	}
}
