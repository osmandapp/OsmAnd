package net.osmand.plus.myplaces.ui;

import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.dialogs.CopyTrackGroupToFavoritesBottomSheet;
import net.osmand.plus.dialogs.EditTrackGroupBottomSheet.OnGroupNameChangeListener;
import net.osmand.plus.dialogs.RenameTrackGroupBottomSheet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.measurementtool.OptionsDividerItem;
import net.osmand.plus.myplaces.DeletePointsTask;
import net.osmand.plus.myplaces.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.myplaces.UpdateGpxCategoryTask;
import net.osmand.plus.myplaces.ui.EditFavoriteGroupDialogFragment.FavoriteColorAdapter;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.HashSet;
import java.util.Set;

public class EditTrackGroupDialogFragment extends MenuBottomSheetDialogFragment implements OnPointsDeleteListener, OnGroupNameChangeListener {

	public static final String TAG = EditTrackGroupDialogFragment.class.getSimpleName();

	private OsmandApplication app;
	private GpxSelectionHelper selectedGpxHelper;
	private MapMarkersHelper mapMarkersHelper;

	private GpxDisplayGroup group;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (group == null) {
			return;
		}
		app = requiredMyApplication();
		selectedGpxHelper = app.getSelectedGpxHelper();
		mapMarkersHelper = app.getMapMarkersHelper();
		items.add(new TitleItem(getCategoryName(app, group.getName())));

		GPXFile gpxFile = group.getGpx();

		boolean currentTrack = group.getGpx().showCurrentTrack;

		SelectedGpxFile selectedGpxFile;
		if (currentTrack) {
			selectedGpxFile = selectedGpxHelper.getSelectedCurrentRecordingTrack();
		} else {
			selectedGpxFile = selectedGpxHelper.getSelectedFileByPath(gpxFile.path);
		}
		boolean trackPoints = group.getType() == GpxDisplayItemType.TRACK_POINTS;
		if (trackPoints && selectedGpxFile != null) {
			items.add(createShowOnMapItem(selectedGpxFile));
		}
		items.add(createEditNameItem());
		if (trackPoints) {
			items.add(createChangeColorItem());
		}
		items.add(new OptionsDividerItem(app));

		if (!currentTrack) {
			items.add(createCopyToMarkersItem(gpxFile));
		}
		items.add(createCopyToFavoritesItem());
		items.add(new OptionsDividerItem(app));

		items.add(createDeleteGroupItem());
	}

	private BaseBottomSheetItem createShowOnMapItem(final SelectedGpxFile selectedGpxFile) {
		boolean checked = !selectedGpxFile.isGroupHidden(group.getName());
		final ApplicationMode mode = app.getSettings().getApplicationMode();
		final BottomSheetItemWithCompoundButton[] showOnMapItem = new BottomSheetItemWithCompoundButton[1];
		showOnMapItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCompoundButtonColor(mode.getProfileColor(nightMode))
				.setChecked(checked)
				.setTitle(getString(R.string.shared_string_show_on_map))
				.setCustomView(getCustomButtonView(app, mode, checked, nightMode))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean checked = !showOnMapItem[0].isChecked();
						if (checked) {
							selectedGpxFile.removeHiddenGroups(group.getName());
						} else {
							selectedGpxFile.addHiddenGroups(group.getName());
						}
						app.getSelectedGpxHelper().updateSelectedGpxFile(selectedGpxFile);

						showOnMapItem[0].setChecked(checked);
						updateCustomButtonView(app, mode, v, checked, nightMode);

						FragmentActivity activity = getActivity();
						if (activity instanceof MapActivity) {
							((MapActivity) activity).refreshMap();
						}
						Fragment fragment = getTargetFragment();
						if (fragment instanceof TrackMenuFragment) {
							((TrackMenuFragment) fragment).updateContent();
						}
					}
				})
				.create();
		return showOnMapItem[0];
	}

	private BaseBottomSheetItem createEditNameItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_name_field))
				.setTitle(getString(R.string.shared_string_rename))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							FragmentManager fragmentManager = activity.getSupportFragmentManager();
							RenameTrackGroupBottomSheet.showInstance(fragmentManager, EditTrackGroupDialogFragment.this, group);
						}
					}
				})
				.create();
	}

	private BaseBottomSheetItem createCopyToMarkersItem(final GPXFile gpxFile) {
		MapMarkersGroup markersGroup = mapMarkersHelper.getMarkersGroup(gpxFile);
		final boolean synced = markersGroup != null && (Algorithms.isEmpty(markersGroup.getWptCategories())
				|| markersGroup.getWptCategories().contains(group.getName()));

		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(synced ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_copy))
				.setTitle(getString(synced ? R.string.remove_group_from_markers : R.string.add_group_to_markers))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						updateGroupWptCategory(gpxFile, synced);
						dismiss();
					}
				})
				.create();
	}

	private void updateGroupWptCategory(GPXFile gpxFile, boolean synced) {
		SelectedGpxFile selectedGpxFile = selectedGpxHelper.getSelectedFileByPath(gpxFile.path);
		if (selectedGpxFile == null) {
			selectedGpxHelper.selectGpxFile(gpxFile, true, false, false, false, false);
		}
		boolean groupCreated = false;
		MapMarkersGroup markersGroup = mapMarkersHelper.getMarkersGroup(gpxFile);
		if (markersGroup == null) {
			groupCreated = true;
			markersGroup = mapMarkersHelper.addOrEnableGroup(gpxFile);
		}
		Set<String> categories = markersGroup.getWptCategories();
		Set<String> selectedCategories = new HashSet<>();
		if (categories != null) {
			selectedCategories.addAll(categories);
		}
		if (synced) {
			selectedCategories.remove(group.getName());
		} else {
			selectedCategories.add(group.getName());
		}
		if (Algorithms.isEmpty(selectedCategories)) {
			mapMarkersHelper.removeMarkersGroup(markersGroup);
		} else {
			mapMarkersHelper.updateGroupWptCategories(markersGroup, selectedCategories);
			if (!groupCreated) {
				mapMarkersHelper.runSynchronization(markersGroup);
			}
		}
	}

	private BaseBottomSheetItem createCopyToFavoritesItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setTitle(getString(R.string.copy_to_map_favorites))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							FragmentManager fragmentManager = activity.getSupportFragmentManager();
							CopyTrackGroupToFavoritesBottomSheet.showInstance(fragmentManager, EditTrackGroupDialogFragment.this, group);
						}
					}
				})
				.create();
	}

	private BaseBottomSheetItem createDeleteGroupItem() {
		String delete = app.getString(R.string.shared_string_delete);
		Typeface typeface = FontCache.getRobotoMedium(app);
		return new SimpleBottomSheetItem.Builder()
				.setTitleColorId(R.color.color_osm_edit_delete)
				.setIcon(getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete))
				.setTitle(UiUtilities.createCustomFontSpannable(typeface, delete, delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						showDeleteConfirmationDialog(activity);
					}
				})
				.create();
	}

	private void showDeleteConfirmationDialog(@NonNull FragmentActivity activity) {
		Context themedCtx = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder b = new AlertDialog.Builder(themedCtx);
		b.setTitle(app.getString(R.string.are_you_sure));
		b.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> deleteGroupItems());
		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();
	}

	private void deleteGroupItems() {
		Set<GpxDisplayItem> items = new HashSet<>(group.getModifiableList());
		new DeletePointsTask(app, group.getGpx(), items, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private BaseBottomSheetItem createChangeColorItem() {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View changeColorView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.change_fav_color, null);
		ImageView icon = ((ImageView) changeColorView.findViewById(R.id.change_color_icon));
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_appearance));
		int margin = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin_large);
		UiUtilities.setMargins(icon, 0, 0, margin, 0);
		updateColorView((ImageView) changeColorView.findViewById(R.id.colorImage));
		return new BaseBottomSheetItem.Builder()
				.setCustomView(changeColorView)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final FragmentActivity activity = getActivity();
						if (activity != null) {
							final ListPopupWindow popup = new ListPopupWindow(activity);
							popup.setAnchorView(v);
							popup.setContentWidth(AndroidUtils.dpToPx(app, 200f));
							popup.setModal(true);
							popup.setDropDownGravity(Gravity.END | Gravity.TOP);
							if (AndroidUiHelper.isOrientationPortrait(activity)) {
								popup.setVerticalOffset(AndroidUtils.dpToPx(app, 48f));
							} else {
								popup.setVerticalOffset(AndroidUtils.dpToPx(app, -48f));
							}
							popup.setHorizontalOffset(AndroidUtils.dpToPx(app, -6f));

							final FavoriteColorAdapter colorAdapter = new FavoriteColorAdapter(activity);
							popup.setAdapter(colorAdapter);
							popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
								@Override
								public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
									Integer color = colorAdapter.getItem(position);
									if (color != null) {
										if (color != group.getColor()) {
											new UpdateGpxCategoryTask(activity, group, color)
													.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
										}
									}
									popup.dismiss();
									dismiss();
								}
							});
							popup.show();
						}
					}
				})
				.create();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (group == null) {
			dismiss();
		}
	}

	public static String getCategoryName(@NonNull Context ctx, String category) {
		return Algorithms.isEmpty(category) ? ctx.getString(R.string.shared_string_waypoints) : category;
	}

	private void updateColorView(ImageView colorImageView) {
		int color = (group.getColor() == 0 ? getResources().getColor(R.color.gpx_color_point) : group.getColor()) | 0xff000000;
		if (color == 0) {
			colorImageView.setImageDrawable(getContentIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, color));
		}
	}

	public static void showInstance(FragmentManager fragmentManager, GpxDisplayGroup group, Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			EditTrackGroupDialogFragment fragment = new EditTrackGroupDialogFragment();
			fragment.group = group;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void onPointsDeletionStarted() {

	}

	@Override
	public void onPointsDeleted() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof TrackMenuFragment) {
			((TrackMenuFragment) fragment).updateContent();
		}
		dismiss();
	}

	@Override
	public void onTrackGroupChanged() {
		dismiss();
	}
}
