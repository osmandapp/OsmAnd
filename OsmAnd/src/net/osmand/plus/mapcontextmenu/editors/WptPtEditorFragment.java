package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.PointsGroup;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor.OnDismissListener;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SavingTrackHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class WptPtEditorFragment extends PointEditorFragment {

	public static final String TAG = WptPtEditorFragment.class.getSimpleName();

	private SavingTrackHelper savingTrackHelper;
	private GpxSelectionHelper gpxSelectionHelper;

	@Nullable
	private WptPtEditor editor;
	@Nullable
	private WptPt wpt;
	private final Map<String, PointsGroup> pointsGroups = new LinkedHashMap<>();

	private boolean saved;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		editor = requireMapActivity().getContextMenu().getWptPtPointEditor();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		savingTrackHelper = app.getSavingTrackHelper();
		gpxSelectionHelper = app.getSelectedGpxHelper();

		WptPtEditor editor = getWptPtEditor();
		if (editor != null) {
			WptPt wpt = editor.getWptPt();
			this.wpt = wpt;
			pointsGroups.putAll(editor.getPointsGroups());
			selectedGroup = pointsGroups.get(Algorithms.isEmpty(wpt.category) ? "" : wpt.category);

			setColor(getPointColor());
			setIconName(getInitialIconName(wpt));
			setBackgroundType(BackgroundType.getByTypeName(wpt.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
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
			editor.setProcessingOrdinaryPoint();
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
	public WptPt getWpt() {
		return wpt;
	}

	@NonNull
	@Override
	public String getToolbarTitle() {
		WptPtEditor editor = getWptPtEditor();
		if (editor != null) {
			if (editor.isProcessingTemplate()) {
				return getString(R.string.waypoint_template);
			} else if (editor.isNewGpxPointProcessing()) {
				return getString(R.string.save_gpx_waypoint);
			} else if (editor.isNew()) {
				return getString(R.string.context_menu_item_add_waypoint);
			} else {
				return getString(R.string.shared_string_edit);
			}
		}
		return "";
	}

	private String getInitialIconName(WptPt wpt) {
		String iconName = wpt.getIconName();
		return iconName != null ? iconName : getDefaultIconName();
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Override
	protected void save(boolean needDismiss) {
		MapActivity mapActivity = getMapActivity();
		WptPtEditor editor = getWptPtEditor();
		WptPt wpt = getWpt();
		if (mapActivity != null && editor != null && wpt != null) {
			String name = Algorithms.isEmpty(getNameTextValue()) ? null : getNameTextValue();
			String address = Algorithms.isEmpty(getAddressTextValue()) ? null : getAddressTextValue();
			String category = Algorithms.isEmpty(getCategoryTextValue()) ? null : getCategoryTextValue();
			String description = Algorithms.isEmpty(getDescriptionTextValue()) ? null : getDescriptionTextValue();

			if (editor.isProcessingTemplate()) {
				doAddWaypointTemplate(name, address, category, description);
			} else if (editor.isNew()) {
				doAddWpt(name, category, description, wpt.getExtensionsToRead());
				wpt = getWpt();
			} else {
				doUpdateWpt(name, category, description);
			}

			if (wpt != null && !Algorithms.isEmpty(wpt.getIconName())) {
				addLastUsedIcon(wpt.getIconName());
			}

			mapActivity.refreshMap();
			if (needDismiss) {
				dismiss(false);
			}

			MapContextMenu menu = mapActivity.getContextMenu();
			if (menu.getLatLon() != null && menu.isActive() && wpt != null) {
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

	private void doAddWaypointTemplate(String name, String address, String category, String description) {
		WptPt wpt = getWpt();
		WptPtEditor editor = getWptPtEditor();
		if (wpt != null && editor != null && editor.getOnWaypointTemplateAddedListener() != null) {
			wpt.name = name;
			wpt.setAddress(address);
			wpt.category = category;
			wpt.desc = description;
			if (getColor() != 0) {
				wpt.setColor(getColor());
			} else {
				wpt.removeColor();
			}
			wpt.setBackgroundType(getBackgroundType().getTypeName());
			wpt.setIconName(getIconName());

			PointsGroup group = pointsGroups.get(category);
			int categoryColor = group != null ? group.color : 0;

			editor.getOnWaypointTemplateAddedListener().onAddWaypointTemplate(wpt, categoryColor);
		}
	}

	private void doAddWpt(String name, String category, String description, Map<String, String> extensions) {
		WptPt wpt = getWpt();
		WptPtEditor editor = getWptPtEditor();
		if (wpt != null && editor != null) {
			wpt.name = name;
			wpt.category = category;
			wpt.desc = description;
			if (getColor() != 0) {
				wpt.setColor(getColor());
			} else {
				wpt.removeColor();
			}
			wpt.setBackgroundType(getBackgroundType().getTypeName());
			wpt.setIconName(getIconName());
			wpt.getExtensionsToWrite().putAll(extensions);

			GPXFile gpx = editor.getGpxFile();
			if (gpx != null) {
				if (gpx.showCurrentTrack) {
					this.wpt = savingTrackHelper.insertPointData(wpt.getLatitude(), wpt.getLongitude(),
							description, name, category, getColor(), getIconName(), getBackgroundType().getTypeName());
					this.wpt.getExtensionsToWrite().putAll(extensions);
					if (!editor.isGpxSelected()) {
						gpxSelectionHelper.setGpxFileToDisplay(gpx);
					}
				} else {
					addWpt(gpx, description, name, category, getColor(), getIconName(), getBackgroundType().getTypeName(), extensions);
					saveGpx(getMyApplication(), gpx, editor.isGpxSelected());
				}
				syncGpx(gpx);
			}
		}
	}

	protected void addWpt(GPXFile gpx, String description, String name, String category, int color, String iconName,
	                      String backgroundType, Map<String, String> extensions) {
		WptPt wpt = getWpt();
		if (wpt != null) {
			this.wpt = WptPt.createAdjustedPoint(wpt.getLatitude(), wpt.getLongitude(), description,
					name, category, color, iconName, backgroundType, wpt.getAmenityOriginName(), extensions);
			gpx.addPoint(wpt);
		}
	}

	private void doUpdateWpt(String name, String category, String description) {
		WptPt wpt = getWpt();
		WptPtEditor editor = getWptPtEditor();
		if (wpt != null && editor != null) {
			GPXFile gpx = editor.getGpxFile();
			if (gpx != null) {
				if (gpx.showCurrentTrack) {
					savingTrackHelper.updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(), description,
							name, category, getColor(), getIconName(), getBackgroundType().getTypeName());
					if (!editor.isGpxSelected()) {
						gpxSelectionHelper.setGpxFileToDisplay(gpx);
					}
				} else {
					gpx.updateWptPt(wpt, wpt.getLatitude(), wpt.getLongitude(), description, name,
							category, getColor(), getIconName(), getBackgroundType().getTypeName());
					saveGpx(getMyApplication(), gpx, editor.isGpxSelected());
				}
				syncGpx(gpx);
			}
		}
	}

	@Override
	protected void delete(boolean needDismiss) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
			builder.setMessage(getString(R.string.context_menu_item_delete_waypoint));
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
				WptPt wpt = getWpt();
				WptPtEditor editor = getWptPtEditor();
				if (wpt != null && editor != null) {
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
			});
			builder.create().show();
		}
	}

	@Override
	public void setPointsGroup(@NonNull PointsGroup group, boolean updateAppearance) {
		pointsGroups.put(group.name, group);
		super.setPointsGroup(group, updateAppearance);
	}

	@Override
	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_favorites);
	}

	@Nullable
	@Override
	public String getNameInitValue() {
		return wpt != null ? wpt.name : "";
	}

	@Override
	public String getDescriptionInitValue() {
		return wpt != null ? wpt.desc : "";
	}

	@Override
	public String getAddressInitValue() {
		return "";
	}

	@Override
	public Drawable getNameIcon() {
		WptPt wptPt = getWpt();
		WptPt point = null;
		if (wptPt != null) {
			point = new WptPt(wptPt);
			point.setColor(getColor());
			point.setBackgroundType(getBackgroundType().getTypeName());
			point.setIconName(getIconName());
		}
		return PointImageDrawable.getFromWpt(getMapActivity(), getColor(), false, point);
	}

	@ColorInt
	@Override
	public int getDefaultColor() {
		return ContextCompat.getColor(app, R.color.gpx_color_point);
	}

	@ColorInt
	public int getPointColor() {
		WptPt wptPt = getWpt();
		int color = wptPt != null ? wptPt.getColor() : 0;
		if (wptPt != null && color == 0) {
			PointsGroup group = pointsGroups.get(wptPt.category);
			if (group != null && group.color != 0) {
				color = group.color;
			}
		}
		if (color == 0) {
			color = getDefaultColor();
		}
		return color;
	}

	@NonNull
	@Override
	public Map<String, PointsGroup> getPointsGroups() {
		return pointsGroups;
	}

	@Override
	protected boolean isCategoryVisible(String categoryName) {
		WptPtEditor editor = getWptPtEditor();
		if (editor == null || editor.getGpxFile() == null) {
			return true;
		}
		SelectedGpxFile selectedGpxFile;
		if (editor.getGpxFile().showCurrentTrack) {
			selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
		} else {
			selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(editor.getGpxFile().path);
		}
		if (selectedGpxFile != null) {
			return !selectedGpxFile.isGroupHidden(categoryName);
		}
		return true;
	}

	private void saveGpx(OsmandApplication app, GPXFile gpxFile, boolean gpxSelected) {
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

	@Override
	protected void showSelectCategoryDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			SelectGpxGroupBottomSheet.showInstance(manager, getSelectedCategory(), null);
		}
	}

	@Override
	protected void showAddNewCategoryFragment() {
		FragmentActivity activity = getActivity();
		WptPtEditor editor = getWptPtEditor();
		if (activity != null && editor != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			GpxGroupEditorFragment.showInstance(manager, editor.getGpxFile(), null, null);
		}
	}

	public static void showInstance(@NonNull MapActivity mapActivity) {
		showInstance(mapActivity, false);
	}

	public static void showInstance(@NonNull MapActivity mapActivity, boolean skipConfirmationDialog) {
		WptPtEditor editor = mapActivity.getContextMenu().getWptPtPointEditor();
		if (editor != null) {
			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			String tag = editor.getFragmentTag();
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
				WptPtEditorFragment fragment = new WptPtEditorFragment();
				fragment.skipConfirmationDialog = skipConfirmationDialog;
				fragmentManager.beginTransaction()
						.add(R.id.fragmentContainer, fragment, tag)
						.addToBackStack(null)
						.commitAllowingStateLoss();
			}
		}
	}
}