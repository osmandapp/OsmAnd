package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.util.Algorithms;

import java.io.File;

public class RtePtEditorFragment extends PointEditorFragment{
	private WptPtEditor editor;
	private WptPt wpt;
	private SavingTrackHelper savingTrackHelper;
	private GpxSelectionHelper selectedGpxHelper;

	private boolean saved;
	private int color;
	private int defaultColor;
	private boolean skipDialog;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		savingTrackHelper = getMapActivity().getMyApplication().getSavingTrackHelper();
		selectedGpxHelper = getMapActivity().getMyApplication().getSelectedGpxHelper();
		editor = getMapActivity().getContextMenu().getWptPtPointEditor();
		defaultColor = getResources().getColor(R.color.gpx_color_point);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		wpt = editor.getWptPt();

		FavouritesDbHelper.FavoriteGroup group = getMyApplication().getFavorites().getGroup(wpt.category);

		if (group == null) {
			color = wpt.getColor(0);
		} else {
			color = group.color;
		}
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (skipDialog) {
			save(true);
		}
	}

	@Override
	public PointEditor getEditor() {
		return editor;
	}

	@Override
	public String getToolbarTitle() {
		return getMapActivity().getResources().getString(R.string.save_route_point);
	}

	public static void showInstance(final MapActivity mapActivity) {
		RtePtEditor editor = mapActivity.getContextMenu().getRtePtPointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		RtePtEditorFragment fragment = new RtePtEditorFragment();
		mapActivity.getSupportFragmentManager().beginTransaction()
				//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
				.addToBackStack(null).commit();
	}

	public static void showInstance(final MapActivity mapActivity, boolean skipDialog) {
		RtePtEditor editor = mapActivity.getContextMenu().getRtePtPointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		RtePtEditorFragment fragment = new RtePtEditorFragment();
		fragment.skipDialog = skipDialog;

		mapActivity.getSupportFragmentManager().beginTransaction()
				//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
				.addToBackStack(null).commit();
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Override
	protected void save(boolean needDismiss) {
		String name = Algorithms.isEmpty(getNameTextValue()) ? null : getNameTextValue();
		String category = Algorithms.isEmpty(getCategoryTextValue()) ? null : getCategoryTextValue();
		String description = Algorithms.isEmpty(getDescriptionTextValue()) ? null : getDescriptionTextValue();
		if (editor.isNew()) {
			doAddWpt(name, category, description);
		} else {
			doUpdateWpt(name, category, description);
		}
		getMapActivity().refreshMap();
		if (needDismiss) {
			dismiss(false);
		}

		MapContextMenu menu = getMapActivity().getContextMenu();

		if (menu.getLatLon() != null) {

			LatLon latLon = new LatLon(wpt.getLatitude(), wpt.getLongitude());

			if (menu.getLatLon().equals(latLon)) {
				menu.update(latLon, wpt.getPointDescription(getMapActivity()), wpt);
			}
		}

		saved = true;
	}

	private void doAddWpt(String name, String category, String description) {
		wpt.name = name;
		wpt.category = category;
		wpt.desc = description;
		if (color != 0) {
			wpt.setColor(color);
		}

		GPXFile gpx = editor.getGpxFile();
		if (gpx != null) {
			if (gpx.showCurrentTrack) {
				wpt = savingTrackHelper.insertPointData(wpt.getLatitude(), wpt.getLongitude(),
						System.currentTimeMillis(), description, name, category, color);
				if (!editor.isGpxSelected()) {
					selectedGpxHelper.setGpxFileToDisplay(gpx);
				}
			} else {
				wpt = gpx.addRtePt(wpt.getLatitude(), wpt.getLongitude(),
						System.currentTimeMillis(), description, name, category, color);
				new RtePtEditorFragment.SaveGpxAsyncTask(getMyApplication(), gpx, editor.isGpxSelected()).execute();
			}
		}
	}

	private void doUpdateWpt(String name, String category, String description) {
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
				new RtePtEditorFragment.SaveGpxAsyncTask(getMyApplication(), gpx, editor.isGpxSelected()).execute();
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

				GPXFile gpx = editor.getGpxFile();
				if (gpx != null) {
					if (gpx.showCurrentTrack) {
						savingTrackHelper.deletePointData(wpt);
					} else {
						gpx.deleteWptPt(wpt);
						new RtePtEditorFragment.SaveGpxAsyncTask(getMyApplication(), gpx, editor.isGpxSelected()).execute();
					}
				}
				saved = true;

				if (needDismiss) {
					dismiss(true);
				} else {
					getMapActivity().refreshMap();
				}
			}
		});
		builder.create().show();
	}

	@Override
	public void setCategory(String name) {
		FavouritesDbHelper.FavoriteGroup group = getMyApplication().getFavorites().getGroup(name);
		if (group != null) {
			color = group.color;
		}
		super.setCategory(name);
	}

	@Override
	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_favorites);
	}

	@Override
	public String getHeaderCaption() {
		return getMapActivity().getResources().getString(R.string.gpx_wpt);
	}

	@Override
	public String getNameInitValue() {
		return wpt.name;
	}

	@Override
	public String getCategoryInitValue() {
		return Algorithms.isEmpty(wpt.category) ? "" : wpt.category;
	}

	@Override
	public String getDescriptionInitValue() {
		return wpt.desc;
	}

	@Override
	public Drawable getNameIcon() {
		return FavoriteImageDrawable.getOrCreate(getMapActivity(), color == 0 ? defaultColor : color, false);
	}

	@Override
	public Drawable getCategoryIcon() {
		return getPaintedIcon(R.drawable.ic_action_folder_stroke, color == 0 ? defaultColor : color);
	}

	private static class SaveGpxAsyncTask extends AsyncTask<Void, Void, Void> {
		private final OsmandApplication app;
		private final GPXFile gpx;
		private final boolean gpxSelected;

		public SaveGpxAsyncTask(OsmandApplication app, GPXFile gpx, boolean gpxSelected) {
			this.app = app;
			this.gpx = gpx;
			this.gpxSelected = gpxSelected;
		}

		@Override
		protected Void doInBackground(Void... params) {
			GPXUtilities.writeGpxFile(new File(gpx.path), gpx, app);
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			if (!gpxSelected) {
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpx);
			}
		}
	}
}
