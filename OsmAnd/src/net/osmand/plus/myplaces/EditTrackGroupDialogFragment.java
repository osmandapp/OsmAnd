package net.osmand.plus.myplaces;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.EditFavoriteGroupDialogFragment.FavoriteColorAdapter;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.measurementtool.OptionsDividerItem;
import net.osmand.plus.myplaces.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.TrackMenuFragment;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;

public class EditTrackGroupDialogFragment extends MenuBottomSheetDialogFragment implements OnPointsDeleteListener {

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
		final String name = Algorithms.isEmpty(group.getName()) ? null : group.getName();
		boolean checked = !selectedGpxFile.getHiddenGroups().contains(name);
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
							selectedGpxFile.removeHiddenGroups(name);
						} else {
							selectedGpxFile.addHiddenGroups(name);
						}
						app.getSelectedGpxHelper().updateSelectedGpxFile(selectedGpxFile);

						showOnMapItem[0].setChecked(checked);
						updateCustomButtonView(app, mode, v, checked, nightMode);

						FragmentActivity activity = getActivity();
						if (activity instanceof MapActivity) {
							((MapActivity) activity).refreshMap();
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
						final FragmentActivity activity = getActivity();
						if (activity != null) {
							AlertDialog.Builder b = new AlertDialog.Builder(activity);
							b.setTitle(R.string.favorite_group_name);
							final EditText nameEditText = new EditText(activity);
							nameEditText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
							nameEditText.setText(group.getName());
							LinearLayout container = new LinearLayout(activity);
							int sidePadding = AndroidUtils.dpToPx(activity, 24f);
							int topPadding = AndroidUtils.dpToPx(activity, 4f);
							container.setPadding(sidePadding, topPadding, sidePadding, topPadding);
							container.addView(nameEditText);
							b.setView(container);
							b.setNegativeButton(R.string.shared_string_cancel, null);
							b.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									String name = nameEditText.getText().toString();
									boolean nameChanged = !Algorithms.objectEquals(group.getName(), name);
									if (nameChanged) {
										new UpdateGpxCategoryTask(activity, group, name)
												.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
									}
									dismiss();
								}
							});
							b.show();
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
				.setTitle(getString(synced ? R.string.remove_from_map_markers : R.string.copy_to_map_markers))
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
						saveGroupToFavorites();
					}
				})
				.create();
	}

	private void saveGroupToFavorites() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AlertDialog.Builder b = new AlertDialog.Builder(activity);
			final EditText editText = new EditText(activity);
			String name = group.getModifiableList().iterator().next().group.getName();
			if (name.indexOf('\n') > 0) {
				name = name.substring(0, name.indexOf('\n'));
			}
			editText.setText(name);
			int leftMargin = AndroidUtils.dpToPx(activity, 16f);
			int topMargin = AndroidUtils.dpToPx(activity, 8f);
			editText.setPadding(leftMargin, topMargin, leftMargin, topMargin);
			b.setTitle(R.string.save_as_favorites_points);
			b.setView(editText);
			b.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String category = editText.getText().toString();
					FavouritesDbHelper favouritesDbHelper = app.getFavorites();
					for (GpxDisplayItem item : group.getModifiableList()) {
						if (item.locationStart != null) {
							FavouritePoint fp = FavouritePoint.fromWpt(item.locationStart, app, category);
							if (!Algorithms.isEmpty(item.description)) {
								fp.setDescription(item.description);
							}
							favouritesDbHelper.addFavourite(fp, false);
						}
					}
					favouritesDbHelper.saveCurrentPointsIntoFile();
					dismiss();
				}
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	private BaseBottomSheetItem createDeleteGroupItem() {
		String delete = app.getString(R.string.shared_string_delete);
		Typeface typeface = FontCache.getRobotoMedium(app);
		return new SimpleBottomSheetItem.Builder()
				.setTitleColorId(R.color.color_osm_edit_delete)
				.setIcon(getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete))
				.setTitle(UiUtilities.createCustomFontSpannable(typeface, delete, delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						deleteGroupItems();
					}
				})
				.create();
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

	private static String getCategoryName(@NonNull Context ctx, String category) {
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
		if (!fragmentManager.isStateSaved() && fragmentManager.findFragmentByTag(TAG) == null) {
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

	private static class UpdateGpxCategoryTask extends AsyncTask<Void, Void, Void> {

		private OsmandApplication app;
		private WeakReference<FragmentActivity> activityRef;

		private GpxDisplayGroup group;

		private String newCategory;
		private Integer newColor;

		private ProgressDialog progressDialog;
		private boolean wasUpdated = false;

		private UpdateGpxCategoryTask(@NonNull FragmentActivity activity, @NonNull GpxDisplayGroup group) {
			this.app = (OsmandApplication) activity.getApplication();
			activityRef = new WeakReference<>(activity);

			this.group = group;
		}

		UpdateGpxCategoryTask(@NonNull FragmentActivity activity, @NonNull GpxDisplayGroup group,
							  @NonNull String newCategory) {
			this(activity, group);
			this.newCategory = newCategory;
		}

		UpdateGpxCategoryTask(@NonNull FragmentActivity activity, @NonNull GpxDisplayGroup group,
							  @NonNull Integer newColor) {
			this(activity, group);
			this.newColor = newColor;
		}

		@Override
		protected void onPreExecute() {
			FragmentActivity activity = activityRef.get();
			if (activity != null) {
				progressDialog = new ProgressDialog(activity);
				progressDialog.setTitle(EditTrackGroupDialogFragment.getCategoryName(app, group.getName()));
				progressDialog.setMessage(newCategory != null ? "Changing name" : "Changing color");
				progressDialog.setCancelable(false);
				progressDialog.show();

				GPXFile gpxFile = group.getGpx();
				if (gpxFile != null) {
					SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
					List<GpxDisplayItem> items = group.getModifiableList();
					String prevCategory = group.getName();
					boolean emptyCategory = TextUtils.isEmpty(prevCategory);
					for (GpxDisplayItem item : items) {
						WptPt wpt = item.locationStart;
						if (wpt != null) {
							boolean update = false;
							if (emptyCategory) {
								if (TextUtils.isEmpty(wpt.category)) {
									update = true;
								}
							} else if (prevCategory.equals(wpt.category)) {
								update = true;
							}
							if (update) {
								wasUpdated = true;
								String category = newCategory != null ? newCategory : wpt.category;
								int color = newColor != null ? newColor : wpt.colourARGB;
								if (gpxFile.showCurrentTrack) {
									savingTrackHelper.updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(),
											System.currentTimeMillis(), wpt.desc, wpt.name, category, color);
								} else {
									gpxFile.updateWptPt(wpt, wpt.getLatitude(), wpt.getLongitude(),
											System.currentTimeMillis(), wpt.desc, wpt.name, category, color,
											wpt.getIconName(), wpt.getBackgroundType());
								}
							}
						}
					}
				}
			}
		}

		@Override
		protected Void doInBackground(Void... voids) {
			GPXFile gpxFile = group.getGpx();
			if (gpxFile != null && !gpxFile.showCurrentTrack && wasUpdated) {
				GPXUtilities.writeGpxFile(new File(gpxFile.path), gpxFile);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			GPXFile gpxFile = group.getGpx();
			if (gpxFile != null && wasUpdated) {
				syncGpx(gpxFile);
			}

			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}

			FragmentActivity activity = activityRef.get();
			if (activity instanceof TrackActivity) {
				((TrackActivity) activity).loadGpx();
			} else if (activity instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) activity;
				TrackMenuFragment fragment = mapActivity.getTrackMenuFragment();
				if (fragment != null) {
					fragment.updateContent();
				}
			}
		}

		private void syncGpx(GPXFile gpxFile) {
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			MapMarkersGroup group = markersHelper.getMarkersGroup(gpxFile);
			if (group != null) {
				markersHelper.runSynchronization(group);
			}
		}
	}
}
