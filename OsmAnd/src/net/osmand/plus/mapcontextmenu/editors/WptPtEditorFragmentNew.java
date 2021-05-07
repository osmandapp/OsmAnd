package net.osmand.plus.mapcontextmenu.editors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor.OnDismissListener;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

public class WptPtEditorFragmentNew extends PointEditorFragmentNew {

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
	private String iconName;
	@NonNull
	private String backgroundTypeName = DEFAULT_BACKGROUND_TYPE.getTypeName();

	private Map<String, Integer> categoriesMap;
	private OsmandApplication app;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			app = mapActivity.getMyApplication();
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
			SelectFavoriteCategoryBottomSheet selectCategoryDialogFragment = SelectFavoriteCategoryBottomSheet.createInstance(editor.getFragmentTag(), getSelectedCategory());
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
			this.wpt = wpt;
			color = wpt.getColor(0);
			iconName = wpt.getIconNameOrDefault();
			categoriesMap = editor.getGpxFile().getWaypointCategoriesWithColors(false);
			backgroundTypeName = wpt.getBackgroundType();
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
			WptPtEditorFragmentNew fragment = new WptPtEditorFragmentNew();
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null).commit();
		}
	}

	public static void showInstance(final MapActivity mapActivity, boolean skipDialog) {
		WptPtEditor editor = mapActivity.getContextMenu().getWptPtPointEditor();
		if (editor != null) {
			WptPtEditorFragmentNew fragment = new WptPtEditorFragmentNew();
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
				wpt = getWpt();
			} else {
				doUpdateWpt(name, category, description);
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
			wpt.setBackgroundType(backgroundTypeName);
			wpt.setIconName(iconName);
			GPXFile gpx = editor.getGpxFile();
			SavingTrackHelper savingTrackHelper = getSavingTrackHelper();
			GpxSelectionHelper selectedGpxHelper = getSelectedGpxHelper();
			if (gpx != null && savingTrackHelper != null && selectedGpxHelper != null) {
				if (gpx.showCurrentTrack) {
					this.wpt = savingTrackHelper.insertPointData(wpt.getLatitude(), wpt.getLongitude(),
							System.currentTimeMillis(), description, name, category, color, iconName, backgroundTypeName);
					if (!editor.isGpxSelected()) {
						selectedGpxHelper.setGpxFileToDisplay(gpx);
					}
				} else {
					addWpt(gpx, description, name, category, color, iconName, backgroundTypeName);
					saveGpx(getMyApplication(), gpx, editor.isGpxSelected());
				}
				syncGpx(gpx);
			}
		}
	}

	protected void addWpt(GPXFile gpx, String description, String name, String category, int color, String iconName,
	                      String backgroundType) {
		WptPt wpt = getWpt();
		if (wpt != null) {
			this.wpt = gpx.addWptPt(wpt.getLatitude(), wpt.getLongitude(),
					System.currentTimeMillis(), description, name, category, color, iconName, backgroundType);
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
							System.currentTimeMillis(), description, name, category, color, iconName, backgroundTypeName);
					if (!editor.isGpxSelected()) {
						selectedGpxHelper.setGpxFileToDisplay(gpx);
					}
				} else {
					gpx.updateWptPt(wpt, wpt.getLatitude(), wpt.getLongitude(),
							System.currentTimeMillis(), description, name, category, color, iconName, backgroundTypeName);
					saveGpx(getMyApplication(), gpx, editor.isGpxSelected());
				}
				syncGpx(gpx);
			}
		}
	}

	@Override
	protected void delete(final boolean needDismiss) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			final OsmandApplication app = (OsmandApplication) activity.getApplication();
			boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
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
	}

	@Override
	public void setCategory(String name, int color) {
		if (categoriesMap != null) {
			categoriesMap.put(name, color);
		}
		this.color = this.color == 0 ? color : this.color;
		super.setCategory(name, color);
	}

	@Override
	public void setColor(int color) {
		this.color = color;

	}

	@Override
	public void setBackgroundType(BackgroundType backgroundType) {
		this.backgroundTypeName = backgroundType.getTypeName();
	}

	@Override
	public void setIcon(int iconId) {
		this.iconName = getIconNameFromID(iconId).substring(3);
	}

	private String getIconNameFromID(int iconId) {
		return app.getResources().getResourceEntryName(iconId);
	}

	@Override
	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_favorites);
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
	public String getAddressInitValue() {
		return "";
	}

	@Override
	public Drawable getNameIcon() {
		WptPt wptPt = getWpt();
		WptPt point = null;
		if (wptPt != null) {
			point = new WptPt(wptPt);
			point.setColor(getPointColor());
			point.setBackgroundType(backgroundTypeName);
			point.setIconName(iconName);
		}
		return PointImageDrawable.getFromWpt(getMapActivity(), getPointColor(), false, point);
	}

	@Override
	public Drawable getCategoryIcon() {
		return getPaintedIcon(R.drawable.ic_action_folder_stroke, getPointColor());
	}

	@Override
	public int getDefaultColor() {
		return defaultColor;
	}

	@Override
	public int getPointColor() {
		if (color != 0) {
			return color;
		} else {
			return getCategoryColor(getCategoryTextValue());
		}
	}

	@Override
	public BackgroundType getBackgroundType() {
		return BackgroundType.getByTypeName(backgroundTypeName, DEFAULT_BACKGROUND_TYPE);
	}

	@Override
	public int getIconId() {
		return getIconIdFromName(iconName);
	}

	public int getIconIdFromName(String iconName) {
		int iconId = app.getResources().getIdentifier("mx_" + iconName, "drawable", app.getPackageName());
		return iconId != 0 ? iconId : DEFAULT_UI_ICON_ID;
	}

	@Override
	public Set<String> getCategories() {
		return categoriesMap.keySet();
	}

	@Override
	@ColorInt
	public int getCategoryColor(String category) {
		if (categoriesMap != null) {
			Integer color = categoriesMap.get(category);
			if (color != null) {
				return color;
			}
		}
		return defaultColor;
	}

	@Override
	public int getCategoryPointsCount(String category) {
		WptPtEditor editor = getWptPtEditor();
		if (editor != null) {
			List<WptPt> points = editor.getGpxFile().getPointsByCategories().get(category);
			if (points != null) {
				return points.size();
			}
		}
		return 0;
	}

	@Override
	protected boolean isCategoryVisible(String name) {
		WptPtEditor editor = getWptPtEditor();
		if (selectedGpxHelper == null || editor == null || editor.getGpxFile() == null) {
			return true;
		}
		SelectedGpxFile selectedGpxFile;
		if (editor.getGpxFile().showCurrentTrack) {
			selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
		} else {
			selectedGpxFile = selectedGpxHelper.getSelectedFileByPath(editor.getGpxFile().path);
		}
		if (selectedGpxFile != null) {
			Set<String> hiddenGroups = selectedGpxFile.getHiddenGroups();
			return !hiddenGroups.contains(name);
		}
		return true;
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