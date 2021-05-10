package net.osmand.plus.mapcontextmenu.editors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor.OnDismissListener;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;

public class WptPtEditorFragment extends PointEditorFragment {

	@Nullable
	protected WptPtEditor editor;
	@Nullable
	protected WptPt wpt;
	@Nullable
	private SavingTrackHelper savingTrackHelper;
	@Nullable
	private GpxSelectionHelper selectedGpxHelper;

	private boolean saved;
	private int color;
	private int defaultColor;
	protected boolean skipDialog;
	private Map<String, Integer> categoriesMap;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			savingTrackHelper = app.getSavingTrackHelper();
			selectedGpxHelper = app.getSelectedGpxHelper();
			assignEditor();
			defaultColor = getResources().getColor(R.color.gpx_color_point);
		}
	}

	@Override
	protected DialogFragment createSelectCategoryDialog() {
		WptPtEditor editor = getWptPtEditor();
		if (editor != null) {
			SelectFavoriteCategoryBottomSheet selectCategoryDialogFragment = SelectFavoriteCategoryBottomSheet.createInstance(editor.getFragmentTag(), "");
			GPXFile gpx = editor.getGpxFile();
			if (gpx != null) {
				selectCategoryDialogFragment.setGpxFile(gpx);
				selectCategoryDialogFragment.setGpxCategories(categoriesMap);
			}
			return selectCategoryDialogFragment;
		} else {
			return null;
		}
	}

	protected void assignEditor() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			editor = mapActivity.getContextMenu().getWptPtPointEditor();
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WptPtEditor editor = getWptPtEditor();
		if (editor != null) {
			WptPt wpt = editor.getWptPt();
			color = wpt.getColor(0);
			this.wpt = wpt;
			categoriesMap = editor.getGpxFile().getWaypointCategoriesWithColors(false);
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (skipDialog) {
			save(true);
		}
	}

	@Override
	public void dismiss(boolean includingMenu) {
		super.dismiss(includingMenu);
		WptPtEditor editor = getWptPtEditor();
		if (editor != null) {
			OnDismissListener listener = editor.getOnDismissListener();
			if (listener != null) {
				listener.onDismiss();
			}
			editor.setNewGpxPointProcessing(false);
			editor.setOnDismissListener(null);
		}
	}

	@Override
	public PointEditor getEditor() {
		return editor;
	}

	public WptPtEditor getWptPtEditor() {
		return editor;
	}

	@Nullable
	public SavingTrackHelper getSavingTrackHelper() {
		return savingTrackHelper;
	}

	@Nullable
	public GpxSelectionHelper getSelectedGpxHelper() {
		return selectedGpxHelper;
	}

	@Nullable
	public WptPt getWpt() {
		return wpt;
	}

	@Override
	public String getToolbarTitle() {
		WptPtEditor editor = getWptPtEditor();
		if (editor != null) {
			if (editor.isNewGpxPointProcessing()) {
				return getString(R.string.save_gpx_waypoint);
			} else {
				if (editor.isNew()) {
					return getString(R.string.context_menu_item_add_waypoint);
				} else {
					return getString(R.string.shared_string_edit);
				}
			}
		}
		return "";
	}

	public static void showInstance(final MapActivity mapActivity) {
		WptPtEditor editor = mapActivity.getContextMenu().getWptPtPointEditor();
		if (editor != null) {
			WptPtEditorFragment fragment = new WptPtEditorFragment();
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null).commit();
		}
	}

	public static void showInstance(final MapActivity mapActivity, boolean skipDialog) {
		WptPtEditor editor = mapActivity.getContextMenu().getWptPtPointEditor();
		if (editor != null) {
			WptPtEditorFragment fragment = new WptPtEditorFragment();
			fragment.skipDialog = skipDialog;
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null).commit();
		}
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Override
	protected void save(final boolean needDismiss) {
		MapActivity mapActivity = getMapActivity();
		WptPtEditor editor = getWptPtEditor();
		WptPt wpt = getWpt();
		if (mapActivity != null && editor != null && wpt != null) {
			String name = Algorithms.isEmpty(getNameTextValue()) ? null : getNameTextValue();
			String category = Algorithms.isEmpty(getCategoryTextValue()) ? null : getCategoryTextValue();
			String description = Algorithms.isEmpty(getDescriptionTextValue()) ? null : getDescriptionTextValue();
			if (editor.isNew()) {
				doAddWpt(name, category, description);
			} else {
				doUpdateWpt(name, category, description);
			}
			mapActivity.refreshMap();
			if (needDismiss) {
				dismiss(false);
			}

			MapContextMenu menu = mapActivity.getContextMenu();
			if (menu.getLatLon() != null && menu.isActive()) {
				LatLon latLon = new LatLon(wpt.getLatitude(), wpt.getLongitude());
				if (menu.getLatLon().equals(latLon)) {
					menu.update(latLon, new WptLocationPoint(wpt).getPointDescription(mapActivity), wpt);
				}
			}
			saved = true;
		}
	}

	private void syncGpx(GPXFile gpxFile) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			MapMarkersHelper helper = app.getMapMarkersHelper();
			MapMarkersGroup group = helper.getMarkersGroup(gpxFile);
			if (group != null) {
				helper.runSynchronization(group);
			}
		}
	}

	private void doAddWpt(String name, String category, String description) {
		WptPt wpt = getWpt();
		WptPtEditor editor = getWptPtEditor();
		if (wpt != null && editor != null) {
			wpt.name = name;
			wpt.category = category;
			wpt.desc = description;
			if (color != 0) {
				wpt.setColor(color);
			} else {
				wpt.removeColor();
			}
			GPXFile gpx = editor.getGpxFile();
			SavingTrackHelper savingTrackHelper = getSavingTrackHelper();
			GpxSelectionHelper selectedGpxHelper = getSelectedGpxHelper();
			if (gpx != null && savingTrackHelper != null && selectedGpxHelper != null) {
				if (gpx.showCurrentTrack) {
					this.wpt = savingTrackHelper.insertPointData(wpt.getLatitude(), wpt.getLongitude(),
							System.currentTimeMillis(), description, name, category, color);
					if (!editor.isGpxSelected()) {
						selectedGpxHelper.setGpxFileToDisplay(gpx);
					}
				} else {
					addWpt(gpx, description, name, category, color);
					saveGpx(getMyApplication(), gpx, editor.isGpxSelected());
				}
				syncGpx(gpx);
			}
		}
	}

	protected void addWpt(GPXFile gpx, String description, String name, String category, int color) {
		WptPt wpt = getWpt();
		if (wpt != null) {
			this.wpt = gpx.addWptPt(wpt.getLatitude(), wpt.getLongitude(),
					System.currentTimeMillis(), description, name, category, color);
			syncGpx(gpx);
		}
	}

	private void doUpdateWpt(String name, String category, String description) {
		WptPt wpt = getWpt();
		WptPtEditor editor = getWptPtEditor();
		SavingTrackHelper savingTrackHelper = getSavingTrackHelper();
		GpxSelectionHelper selectedGpxHelper = getSelectedGpxHelper();
		if (wpt != null && editor != null && savingTrackHelper != null && selectedGpxHelper != null) {
			GPXFile gpx = editor.getGpxFile();
			if (gpx != null) {
				if (gpx.showCurrentTrack) {
					savingTrackHelper.updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(),
							System.currentTimeMillis(), description, name, category, color);
					if (!editor.isGpxSelected()) {
						selectedGpxHelper.setGpxFileToDisplay(gpx);
					}
				} else {
					gpx.updateWptPt(wpt, wpt.getLatitude(), wpt.getLongitude(),
							System.currentTimeMillis(), description, name, category, color);
					saveGpx(getMyApplication(), gpx, editor.isGpxSelected());
				}
				syncGpx(gpx);
			}
		}
	}

	@Override
	protected void delete(final boolean needDismiss) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.context_menu_item_delete_waypoint));
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				WptPt wpt = getWpt();
				WptPtEditor editor = getWptPtEditor();
				SavingTrackHelper savingTrackHelper = getSavingTrackHelper();
				if (wpt != null && editor != null && savingTrackHelper != null) {
					GPXFile gpx = editor.getGpxFile();
					if (gpx != null) {
						if (gpx.showCurrentTrack) {
							savingTrackHelper.deletePointData(wpt);
						} else {
							gpx.deleteWptPt(wpt);
							saveGpx(getMyApplication(), gpx, editor.isGpxSelected());
						}
						syncGpx(gpx);
					}
					saved = true;
				}

				if (needDismiss) {
					dismiss(true);
				} else {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						mapActivity.refreshMap();
					}
				}
			}
		});
		builder.create().show();
	}

	@Override
	public void setCategory(String name, int color) {
		if (categoriesMap != null) {
			categoriesMap.put(name, color);
		}
		this.color = color;
		super.setCategory(name, color);
	}

	@Override
	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_favorites);
	}

	@Override
	public String getHeaderCaption() {
		return getString(R.string.shared_string_waypoint);
	}

	@Override
	public String getNameInitValue() {
		return wpt != null ? wpt.name : "";
	}

	@Override
	public String getCategoryInitValue() {
		return wpt == null || Algorithms.isEmpty(wpt.category) ? "" : wpt.category;
	}

	@Override
	public String getDescriptionInitValue() {
		return wpt != null ? wpt.desc : "";
	}

	@Override
	public Drawable getNameIcon() {
		return PointImageDrawable.getFromWpt(getMapActivity(), getPointColor(), false, wpt);
	}

	@Override
	public Drawable getCategoryIcon() {
		return getPaintedIcon(R.drawable.ic_action_folder_stroke, getPointColor());
	}

	@Override
	public int getPointColor() {
		return color == 0 ? defaultColor : color;
	}

	private void saveGpx(final OsmandApplication app, final GPXFile gpxFile, final boolean gpxSelected) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {

			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (errorMessage == null && !gpxSelected) {
					app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
				}
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
