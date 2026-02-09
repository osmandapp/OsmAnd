package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.data.PointDescription.POINT_TYPE_MAP_MARKER;
import static net.osmand.plus.helpers.MapFragmentsHelper.CLOSE_ALL_FRAGMENTS;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoritesTreeFragment.IMPORT_FAVOURITES_REQUEST;
import static net.osmand.plus.utils.UiUtilities.getThemedContext;
import static net.osmand.shared.gpx.GpxFile.DEFAULT_WPT_GROUP_NAME;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.mapcontextmenu.editors.FavoriteAppearanceFragment;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.FavouriteGroupEditorFragment;
import net.osmand.plus.mapcontextmenu.editors.SelectFavouriteGroupBottomSheet;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.mapcontextmenu.other.SharePoiParams;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoriteMenu {
	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final MyPlacesActivity activity;

	public FavoriteMenu(@NonNull OsmandApplication app, @NonNull UiUtilities uiUtilities, @NonNull MyPlacesActivity activity) {
		this.app = app;
		this.activity = activity;
		this.uiUtilities = uiUtilities;
	}

	public void showPointOptionsMenu(@NonNull View view, @NonNull FavouritePoint favouritePoint, boolean nightMode,
	                                 @NonNull CategorySelectionListener selectionListener, @NonNull FavoriteActionListener actionListener,
	                                 @NonNull Fragment targetFragment) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(favouritePoint.getDisplayName(app))
				.setTitleColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
				.setTitleSize(14)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_edit)
				.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
				.setOnClickListener(v -> {
					FavoriteGroup group = app.getFavoritesHelper().getGroup(favouritePoint);
					if (group != null) {
						FavoritePointEditor editor = new FavoritePointEditor(app);
						editor.edit(favouritePoint, activity, targetFragment);
						if (targetFragment instanceof DialogFragment dialogFragment) {
							dialogFragment.dismiss();
						}
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_move)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_move))
				.setOnClickListener(v -> {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					SelectFavouriteGroupBottomSheet.showInstance(fragmentManager, favouritePoint.getCategory(), selectionListener);
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> {
					LatLon latLon = new LatLon(favouritePoint.getLatitude(), favouritePoint.getLongitude());
					SharePoiParams params = new SharePoiParams(latLon);
					params.addName(favouritePoint.getName());
					ShareMenu.show(latLon, favouritePoint.getName(), favouritePoint.getAddress(), ShareMenu.buildOsmandPoiUri(params), app, activity);
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_add_to_map_markers)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_add_to_markers))
				.setOnClickListener(v -> addToMarkers(Collections.singleton(favouritePoint)))
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_to_track)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_track_add))
				.setOnClickListener(v -> {
					SelectTrackTabsFragment.GpxFileSelectionListener gpxFileSelectionListener = gpxFile -> {
						WptPt wptPt = favouritePoint.toWpt(app);
						wptPt.setCategory(DEFAULT_WPT_GROUP_NAME);
						gpxFile.addPoint(wptPt);
						saveGpx(app, gpxFile);
						syncGpx(gpxFile);
					};
					SelectTrackTabsFragment.showInstance(activity.getSupportFragmentManager(), gpxFileSelectionListener);

				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_to_navigation)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_navigation_outlined))
				.setOnClickListener(v -> {

					app.getTargetPointsHelper().navigateToPoint(new LatLon(favouritePoint.getLatitude(), favouritePoint.getLongitude()), true, -1);
					app.getSettings().navigateDialog();
					Bundle args = new Bundle();
					args.putBoolean(CLOSE_ALL_FRAGMENTS, true);

					if (targetFragment instanceof FragmentStateHolder fragmentStateHolder) {
						Bundle bundle = fragmentStateHolder.storeState();
						MapActivity.launchMapActivityMoveToTop(activity, bundle, null, args);
					}

				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_delete)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> {
					OsmandApplication app = (OsmandApplication) activity.getApplication();
					AlertDialog.Builder builder = new AlertDialog.Builder(getThemedContext(activity, nightMode));
					builder.setMessage(app.getString(R.string.favourites_remove_dialog_msg, favouritePoint.getName()));
					builder.setNegativeButton(R.string.shared_string_no, null);
					builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
						app.getFavoritesHelper().deleteFavourite(favouritePoint);
						actionListener.onActionFinish();
					});
					builder.create().show();
				})
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.popup_menu_item_full_divider;
		PopUpMenu.show(displayData);
	}

	private void saveGpx(OsmandApplication app, GpxFile gpxFile) {
		SaveGpxHelper.saveGpx(new File(gpxFile.getPath()), gpxFile, errorMessage -> {
			if (errorMessage == null) {
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
			}
		});
	}

	private void syncGpx(GpxFile gpxFile) {
		MapMarkersHelper helper = app.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(gpxFile);
		if (group != null) {
			helper.runSynchronization(group);
		}
	}

	public void showFolderOptionsMenu(@NonNull MyPlacesActivity activity, @NonNull View view, @NonNull FavoriteGroup selectedGroup,
	                                  boolean nightMode, @NonNull BaseFavoriteListFragment fragment) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_select)
				.setIcon(getContentIcon(R.drawable.ic_action_deselect_all))
				.setOnClickListener(v -> {
					fragment.setSelectionMode(!fragment.selectionMode);
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.change_appearance)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_outlined))
				.setOnClickListener(v -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					PointsGroup pointsGroup = selectedGroup.toPointsGroup(app);
					FavoriteAppearanceFragment.showInstance(manager, pointsGroup, fragment);
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_delete)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> {
					AlertDialog.Builder b = new AlertDialog.Builder(getThemedContext(app, nightMode));
					b.setTitle(R.string.favorite_delete_group);
					String groupName = Algorithms.isEmpty(selectedGroup.getName()) ? app.getString(R.string.shared_string_favorites) : selectedGroup.getName();
					b.setMessage(app.getString(R.string.favorite_confirm_delete_group, groupName, selectedGroup.getPoints().size()));
					b.setNeutralButton(R.string.shared_string_cancel, null);
					b.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
						FavouritesHelper helper = app.getFavoritesHelper();
						helper.deleteGroup(selectedGroup, false);
						helper.saveCurrentPointsIntoFile(true);
						FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
						sortModesHelper.onFavoriteFolderDeleted(selectedGroup);
					});
					b.show();
				})
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	public void showFolderSelectionOptionsMenu(@NonNull MyPlacesActivity activity, @NonNull View view, @NonNull Set<FavoriteGroup> groups,
	                                           boolean nightMode, @NonNull BaseFavoriteListFragment fragment) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> {
					fragment.shareFavorites(new ArrayList<>(groups));
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_delete)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> {
					AlertDialog.Builder b = new AlertDialog.Builder(getThemedContext(activity, nightMode));
					b.setTitle(R.string.favorite_delete_group);
					b.setMessage(app.getString(R.string.delete_groups_confirmation));
					b.setNeutralButton(R.string.shared_string_cancel, null);
					b.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
						FavouritesHelper helper = app.getFavoritesHelper();
						FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
						for (FavoriteGroup group : groups) {
							helper.deleteGroup(group, false);
							sortModesHelper.onFavoriteFolderDeleted(group);
						}
						helper.saveCurrentPointsIntoFile(true);
						fragment.onActionFinish();
					});
					b.show();
				})
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	public void showPointsSelectOptionsMenu(@NonNull View view, @NonNull Set<FavouritePoint> points, @Nullable FavoriteGroup selectedGroup, boolean nightMode,
	                                        @NonNull CategorySelectionListener selectionListener, @NonNull FavoriteActionListener actionListener,
	                                        @NonNull FragmentStateHolder fragmentStateHolder) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_move)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_move))
				.setOnClickListener(v -> {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					SelectFavouriteGroupBottomSheet.showInstance(
							fragmentManager,
							selectedGroup != null ? selectedGroup.getName() : null,
							selectionListener);
				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_add_to_map_markers)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_add_to_markers))
				.setOnClickListener(v -> addToMarkers(points))
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_to_track)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_track_add))
				.setOnClickListener(v -> {
					SelectTrackTabsFragment.GpxFileSelectionListener gpxFileSelectionListener = gpxFile -> {
						for (FavouritePoint point : points) {
							WptPt wptPt = point.toWpt(app);
							wptPt.setCategory(DEFAULT_WPT_GROUP_NAME);
							gpxFile.addPoint(wptPt);
						}
						saveGpx(app, gpxFile);
						syncGpx(gpxFile);
					};
					SelectTrackTabsFragment.showInstance(activity.getSupportFragmentManager(), gpxFileSelectionListener);

				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_to_navigation)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_navigation_outlined))
				.setOnClickListener(v -> {
					List<TargetPoint> targetPoints = new ArrayList<>();
					for (FavouritePoint point : points) {
						TargetPoint targetPoint = new TargetPoint(new LatLon(point.getLatitude(), point.getLongitude()), point.getPointDescription(app));
						targetPoints.add(targetPoint);
					}
					app.getTargetPointsHelper().reorderAllTargetPoints(targetPoints, true);
					app.getSettings().navigateDialog();
					Bundle args = new Bundle();
					args.putBoolean(CLOSE_ALL_FRAGMENTS, true);

					Bundle bundle = fragmentStateHolder.storeState();
					MapActivity.launchMapActivityMoveToTop(activity, bundle, null, args);
				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_delete)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> {
					OsmandApplication app = (OsmandApplication) activity.getApplication();
					AlertDialog.Builder builder = new AlertDialog.Builder(getThemedContext(activity, nightMode));
					builder.setMessage(app.getString(R.string.favourites_context_menu_delete));
					builder.setNegativeButton(R.string.shared_string_no, null);
					builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
						Set<FavouritePoint> favouritePoints = new HashSet<>(points);
						app.getFavoritesHelper().delete(null, favouritePoints);
						actionListener.onActionFinish();
					});
					builder.create().show();
				})
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	public void showFoldersOptionsMenu(@NonNull MyPlacesActivity activity, @NonNull View view, boolean nightMode,
	                                   @NonNull CategorySelectionListener selectionListener, @NonNull BaseFavoriteListFragment fragment) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_select)
				.setIcon(getContentIcon(R.drawable.ic_action_deselect_all))
				.setOnClickListener(v -> {
					fragment.setSelectionMode(!fragment.selectionMode);
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_new_folder)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_add_outlined))
				.setOnClickListener(v -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					FavouriteGroupEditorFragment.showInstance(manager, null, selectionListener, false);
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_import)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_import))
				.setOnClickListener(v -> importFavourites(fragment))
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void importFavourites(@NonNull Fragment fragment) {
		Intent intent = ImportHelper.getImportFileIntent();
		AndroidUtils.startActivityForResultIfSafe(fragment, intent, IMPORT_FAVOURITES_REQUEST);
	}

	private void addToMarkers(@NonNull Set<FavouritePoint> favouritePoints) {
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<LatLon> points = new ArrayList<>();
		List<PointDescription> names = new ArrayList<>();

		for (FavouritePoint point : favouritePoints) {
			points.add(new LatLon(point.getLatitude(), point.getLongitude()));
			names.add(new PointDescription(POINT_TYPE_MAP_MARKER, point.getName()));
		}
		markersHelper.addMapMarkers(points, names, null);
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int id) {
		return uiUtilities.getThemedIcon(id);
	}

	public interface FavoriteActionListener {
		void onActionFinish();
	}
}

