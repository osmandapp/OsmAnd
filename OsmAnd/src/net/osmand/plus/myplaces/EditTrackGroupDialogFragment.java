package net.osmand.plus.myplaces;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.EditFavoriteGroupDialogFragment.FavoriteColorAdapter;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

public class EditTrackGroupDialogFragment extends MenuBottomSheetDialogFragment {
	public static final String TAG = EditTrackGroupDialogFragment.class.getSimpleName();

	private GpxDisplayGroup group;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		if (group == null) {
			return;
		}
		items.add(new TitleItem(getCategoryName(app, group.getName())));

		BaseBottomSheetItem editNameItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
				.setTitle(getString(R.string.edit_name))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Activity activity = getActivity();
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
										TrackActivity trackActivity = getTrackActivity();
										if (trackActivity != null) {
											new UpdateGpxCategoryTask(trackActivity, group, name)
													.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
										}
									}
									dismiss();
								}
							});
							b.show();
						}
					}
				})
				.create();
		items.add(editNameItem);

		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View changeColorView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.change_fav_color, null);
		((ImageView) changeColorView.findViewById(R.id.change_color_icon))
				.setImageDrawable(getContentIcon(R.drawable.ic_action_appearance));
		updateColorView((ImageView) changeColorView.findViewById(R.id.colorImage));
		BaseBottomSheetItem changeColorItem = new BaseBottomSheetItem.Builder()
				.setCustomView(changeColorView)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Activity activity = getActivity();
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
											TrackActivity trackActivity = getTrackActivity();
											if (trackActivity != null) {
												new UpdateGpxCategoryTask(trackActivity, group, color)
														.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
											}
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
		items.add(changeColorItem);

	}

	@Override
	public void onResume() {
		super.onResume();
		if (group == null) {
			dismiss();
		}
	}

	@Nullable
	private TrackActivity getTrackActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof TrackActivity) {
			return (TrackActivity) activity;
		}
		return null;
	}

	private static String getCategoryName(@NonNull Context ctx, String category) {
		return Algorithms.isEmpty(category) ? ctx.getString(R.string.shared_string_waypoints) : category;
	}

	private void updateColorView(ImageView colorImageView) {
		int color = (group.getColor() == 0 ? getResources().getColor(R.color.gpx_color_point) : group.getColor()) | 0xff000000;
		if (color == 0) {
			colorImageView.setImageDrawable(getContentIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(getMyApplication().getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, color));
		}
	}

	public static void showInstance(FragmentManager fragmentManager, GpxDisplayGroup group) {
		EditTrackGroupDialogFragment f = (EditTrackGroupDialogFragment) fragmentManager
				.findFragmentByTag(EditTrackGroupDialogFragment.TAG);
		if (f == null ) {
			f = new EditTrackGroupDialogFragment();
			f.group = group;
			f.show(fragmentManager, EditTrackGroupDialogFragment.TAG);
		}
	}

	private static class UpdateGpxCategoryTask extends AsyncTask<Void, Void, Void> {

		private OsmandApplication app;
		private WeakReference<TrackActivity> activityRef;

		private GpxDisplayGroup group;

		private String newCategory;
		private Integer newColor;

		private ProgressDialog progressDialog;
		private boolean wasUpdated = false;

		private UpdateGpxCategoryTask(@NonNull TrackActivity activity, @NonNull GpxDisplayGroup group) {
			this.app = (OsmandApplication) activity.getApplication();
			activityRef = new WeakReference<>(activity);

			this.group = group;
		}

		UpdateGpxCategoryTask(@NonNull TrackActivity activity, @NonNull GpxDisplayGroup group,
							  @NonNull String newCategory) {
			this(activity, group);
			this.newCategory = newCategory;
		}

		UpdateGpxCategoryTask(@NonNull TrackActivity activity, @NonNull GpxDisplayGroup group,
							  @NonNull Integer newColor) {
			this(activity, group);
			this.newColor = newColor;
		}

		@Override
		protected void onPreExecute() {
			TrackActivity activity = activityRef.get();
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

			TrackActivity activity = activityRef.get();
			if (activity != null) {
				activity.loadGpx();
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
