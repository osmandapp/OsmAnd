package net.osmand.plus.dialogs;

import android.content.DialogInterface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import androidx.appcompat.app.AlertDialog;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.activities.PluginsFragment;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.transport.TransportLinesMenu;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;
import java.util.Map;

final class MapLayerMenuListener extends OnRowItemClick {

	private MapActivity mapActivity;
	private ContextMenuAdapter menuAdapter;

	MapLayerMenuListener(MapActivity mapActivity, ContextMenuAdapter menuAdapter) {
		this.mapActivity = mapActivity;
		this.menuAdapter = menuAdapter;
	}

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
	public boolean onRowItemClick(final ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int pos) {
		if (itemId == R.string.layer_poi) {
			showPoiFilterDialog(adapter, adapter.getItem(pos));
			return false;
		} else if (itemId == R.string.layer_gpx_layer && menuAdapter.getItem(pos).getSelected()) {
			showGpxSelectionDialog(adapter, adapter.getItem(pos));
			return false;
		} else if (itemId == R.string.rendering_category_transport) {
			final ContextMenuItem item = adapter.getItem(pos);
			TransportLinesMenu.showTransportsDialog(mapActivity, new CallbackWithObject<Boolean>() {
				@Override
				public boolean processResult(Boolean result) {
					if (item != null) {
						item.setSelected(result);
						item.setColor(mapActivity, result ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						adapter.notifyDataSetChanged();
					}
					return true;
				}
			});
			boolean selected = TransportLinesMenu.isShowLines(mapActivity.getMyApplication());
			if (!selected && item != null) {
				item.setSelected(true);
				item.setColor(mapActivity, R.color.osmand_orange);
				adapter.notifyDataSetChanged();
			}
			return false;
		} else {
			CompoundButton btn = (CompoundButton) view.findViewById(R.id.toggle_item);
			if (btn != null && btn.getVisibility() == View.VISIBLE) {
				btn.setChecked(!btn.isChecked());
				menuAdapter.getItem(pos).setColor(mapActivity, btn.isChecked() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				adapter.notifyDataSetChanged();
				return false;
			} else {
				return onContextMenuClick(adapter, itemId, pos, false, null);
			}
		}
	}

	@Override
	public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId,
	                                  final int pos, boolean isChecked, int[] viewCoordinates) {
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		final PoiFiltersHelper poiFiltersHelper = mapActivity.getMyApplication().getPoiFilters();
		final ContextMenuItem item = menuAdapter.getItem(pos);
		if (item.getSelected() != null) {
			item.setColor(mapActivity, isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
		}
		if (itemId == R.string.layer_poi) {
			PoiUIFilter wiki = poiFiltersHelper.getTopWikiPoiFilter();
			poiFiltersHelper.clearSelectedPoiFilters(wiki);
			if (isChecked) {
				showPoiFilterDialog(adapter, adapter.getItem(pos));
			} else {
				adapter.getItem(pos).setDescription(
						poiFiltersHelper.getSelectedPoiFiltersName(wiki));
			}
		} else if (itemId == R.string.layer_amenity_label) {
			settings.SHOW_POI_LABEL.set(isChecked);
		} else if (itemId == R.string.shared_string_favorites) {
			settings.SHOW_FAVORITES.set(isChecked);
		} else if (itemId == R.string.layer_gpx_layer) {
			final GpxSelectionHelper selectedGpxHelper = mapActivity.getMyApplication().getSelectedGpxHelper();
			if (selectedGpxHelper.isShowingAnyGpxFiles()) {
				selectedGpxHelper.clearAllGpxFilesToShow(true);
				adapter.getItem(pos).setDescription(selectedGpxHelper.getGpxDescription());
			} else {
				showGpxSelectionDialog(adapter, adapter.getItem(pos));
			}
		} else if (itemId == R.string.rendering_category_transport) {
			boolean selected = TransportLinesMenu.isShowLines(mapActivity.getMyApplication());
			TransportLinesMenu.toggleTransportLines(mapActivity, !selected, new CallbackWithObject<Boolean>() {
				@Override
				public boolean processResult(Boolean result) {
					item.setSelected(result);
					item.setColor(mapActivity, result ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
					return true;
				}
			});
		} else if (itemId == R.string.map_markers) {
			settings.SHOW_MAP_MARKERS.set(isChecked);
		} else if (itemId == R.string.layer_map) {
			if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
				PluginsFragment.showInstance(mapActivity.getSupportFragmentManager());
			} else {
				ContextMenuItem it = adapter.getItem(pos);
				mapActivity.getMapLayers().selectMapLayer(mapActivity.getMapView(), it, adapter);
			}
		}
		adapter.notifyDataSetChanged();
		mapActivity.getMapLayers().updateLayers(mapActivity.getMapView());
		mapActivity.getMapView().refreshMap();
		return false;
	}

	private void showGpxSelectionDialog(final ArrayAdapter<ContextMenuItem> adapter,
	                                    final ContextMenuItem item) {
		AlertDialog dialog = mapActivity.getMapLayers().showGPXFileLayer(getAlreadySelectedGpx(),
				mapActivity.getMapView());
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				OsmandApplication app = mapActivity.getMyApplication();
				boolean selected = app.getSelectedGpxHelper().isShowingAnyGpxFiles();
				item.setSelected(selected);
				item.setDescription(app.getSelectedGpxHelper().getGpxDescription());
				item.setColor(mapActivity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				adapter.notifyDataSetChanged();
			}
		});
	}

	protected void showPoiFilterDialog(final ArrayAdapter<ContextMenuItem> adapter,
	                                   final ContextMenuItem item) {
		final PoiFiltersHelper poiFiltersHelper = mapActivity.getMyApplication().getPoiFilters();
		final PoiUIFilter wiki = poiFiltersHelper.getTopWikiPoiFilter();
		MapActivityLayers.DismissListener dismissListener =
				new MapActivityLayers.DismissListener() {
					@Override
					public void dismiss() {
						PoiFiltersHelper pf = mapActivity.getMyApplication().getPoiFilters();
						boolean selected = pf.isShowingAnyPoi(wiki);
						item.setSelected(selected);
						item.setDescription(pf.getSelectedPoiFiltersName(wiki));
						item.setColor(mapActivity, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						adapter.notifyDataSetChanged();
					}
				};
		boolean isMultichoose = poiFiltersHelper.getSelectedPoiFilters(wiki).size() > 1;
		if (isMultichoose) {
			mapActivity.getMapLayers().showMultichoicePoiFilterDialog(mapActivity.getMapView(),
					dismissListener);
		} else {
			mapActivity.getMapLayers().showSingleChoicePoiFilterDialog(mapActivity.getMapView(),
					dismissListener);
		}
	}
}