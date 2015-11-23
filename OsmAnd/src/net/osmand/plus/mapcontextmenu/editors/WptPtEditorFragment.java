package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;

import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
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

public class WptPtEditorFragment extends PointEditorFragment {
	private WptPtEditor editor;
	private WptPt wpt;
	private SavingTrackHelper savingTrackHelper;
	private GpxSelectionHelper selectedGpxHelper;

	private boolean saved;
	private int color;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		savingTrackHelper = getMapActivity().getMyApplication().getSavingTrackHelper();
		selectedGpxHelper = getMapActivity().getMyApplication().getSelectedGpxHelper();
		editor = getMapActivity().getContextMenu().getWptPtPointEditor();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		wpt = editor.getWptPt();
		int defaultColor = getResources().getColor(R.color.gpx_color_point);
		color = wpt.getColor(defaultColor);
	}

	@Override
	public PointEditor getEditor() {
		return editor;
	}

	@Override
	public String getToolbarTitle() {
		if (editor.isNew()) {
			return getMapActivity().getResources().getString(R.string.context_menu_item_add_waypoint);
		} else {
			return getMapActivity().getResources().getString(R.string.shared_string_edit);
		}
	}

	public static void showInstance(final MapActivity mapActivity) {
		WptPtEditor editor = mapActivity.getContextMenu().getWptPtPointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		WptPtEditorFragment fragment = new WptPtEditorFragment();
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
	protected void save(final boolean needDismiss) {
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
		LatLon latLon = new LatLon(wpt.getLatitude(), wpt.getLongitude());
		if (menu.getLatLon().equals(latLon)) {
			menu.update(latLon, wpt.getPointDescription(getMapActivity()), wpt);
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
				wpt = gpx.addWptPt(wpt.getLatitude(), wpt.getLongitude(),
						System.currentTimeMillis(), description, name, category, color);
				new SaveGpxAsyncTask(getMyApplication(), gpx, editor.isGpxSelected()).execute();
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
				new SaveGpxAsyncTask(getMyApplication(), gpx, editor.isGpxSelected()).execute();
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
						new SaveGpxAsyncTask(getMyApplication(), gpx, editor.isGpxSelected()).execute();
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
		FavoriteGroup group = getMyApplication().getFavorites().getGroup(name);
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
		return FavoriteImageDrawable.getOrCreate(getMapActivity(), color, false);
	}

	@Override
	public Drawable getCategoryIcon() {
		return getPaintedIcon(R.drawable.ic_action_folder_stroke, color);
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
