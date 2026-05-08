package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.TitleButtonController;
import net.osmand.plus.mapcontextmenu.builders.SelectedGpxMenuBuilder;
import net.osmand.plus.myplaces.tracks.tasks.OpenGpxDetailsTask;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

public class SelectedGpxMenuController extends MenuController {

	private SelectedGpxPoint selectedGpxPoint;

	public SelectedGpxMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription,
	                                 @NonNull SelectedGpxPoint selectedGpxPoint) {
		super(new SelectedGpxMenuBuilder(mapActivity, selectedGpxPoint), pointDescription, mapActivity);
		this.selectedGpxPoint = selectedGpxPoint;
		builder.setShowOnlinePhotos(false);

		leftTitleButtonController = new TitleButtonController(this) {
			@Override
			public void buttonPressed() {
				if (mapContextMenu != null) {
					mapContextMenu.close();
				}
				SelectedGpxFile selectedGpxFile = selectedGpxPoint.getSelectedGpxFile();
				if (selectedGpxFile != null) {
					TrackMenuFragment.showInstance(mapActivity, selectedGpxFile, selectedGpxPoint);
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_open_track);
		leftTitleButtonController.startIconId = R.drawable.ic_action_folder;

		rightTitleButtonController = new TitleButtonController(this) {
			@Override
			public void buttonPressed() {
				WptPt selectedPoint = selectedGpxPoint.getSelectedPoint();
				SelectedGpxFile selectedGpxFile = selectedGpxPoint.getSelectedGpxFile();
				if (selectedGpxFile == null) {
					return;
				}
				GpxFile gpxFile = selectedGpxFile.getGpxFile();

				OpenGpxDetailsTask detailsTask = new OpenGpxDetailsTask(mapActivity, gpxFile, selectedPoint, null);
				OsmAndTaskManager.executeTask(detailsTask);
			}
		};
		rightTitleButtonController.caption = mapActivity.getString(R.string.analyze_on_map);
		rightTitleButtonController.startIconId = R.drawable.ic_action_analyze_intervals;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof SelectedGpxPoint) {
			this.selectedGpxPoint = (SelectedGpxPoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return selectedGpxPoint;
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (getMenuType() == MenuType.MULTI_LINE) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null && selectedGpxPoint.getSelectedGpxFile() != null) {
				SelectedGpxFile selectedGpxFile = selectedGpxPoint.getSelectedGpxFile();
				GpxTrackAnalysis analysis = selectedGpxFile.getTrackAnalysis(mapActivity.getApp());
				if (analysis != null) {
					String path = selectedGpxFile.getGpxFile().getPath();
					KFile file = selectedGpxFile.isShowCurrentTrack() || Algorithms.isEmpty(path)
							? null
							: new KFile(path);
					String description = GpxUiHelper.getTrackShortDescription(mapActivity, analysis, file, true);
					if (!Algorithms.isEmpty(description)) {
						return description;
					}
				}
			}
		}
		return getPointDescription().getTypeName();
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.shared_string_gpx_track);
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getRightIcon() {
		if (getPointDescription() != null && getPointDescription().getIconDrawable() != null) {
			return getPointDescription().getIconDrawable();
		}
		int color = ColorUtilities.getActiveColorId(!isLight());
		return getIcon(R.drawable.ic_action_polygom_dark, color);
	}

	public static class SelectedGpxPoint {

		@Nullable
		private final SelectedGpxFile selectedGpxFile;
		private final WptPt selectedPoint;
		private final WptPt prevPoint;
		private final WptPt nextPoint;
		private final float bearing;
		private final boolean showTrackPointMenu;

		public SelectedGpxPoint(@Nullable SelectedGpxFile selectedGpxFile, WptPt selectedPoint) {
			this(selectedGpxFile, selectedPoint, null, null, Float.NaN, false);
		}

		public SelectedGpxPoint(@Nullable SelectedGpxFile selectedGpxFile, WptPt selectedPoint, WptPt prevPoint,
		                        WptPt nextPoint, float bearing, boolean showTrackPointMenu) {
			this.prevPoint = prevPoint;
			this.nextPoint = nextPoint;
			this.selectedPoint = selectedPoint;
			this.selectedGpxFile = selectedGpxFile;
			this.bearing = bearing;
			this.showTrackPointMenu = showTrackPointMenu;
		}

		@Nullable
		public SelectedGpxFile getSelectedGpxFile() {
			return selectedGpxFile;
		}

		public WptPt getSelectedPoint() {
			return selectedPoint;
		}

		public float getBearing() {
			return bearing;
		}

		public WptPt getPrevPoint() {
			return prevPoint;
		}

		public WptPt getNextPoint() {
			return nextPoint;
		}

		public boolean shouldShowTrackPointMenu() {
			return showTrackPointMenu;
		}
	}
}