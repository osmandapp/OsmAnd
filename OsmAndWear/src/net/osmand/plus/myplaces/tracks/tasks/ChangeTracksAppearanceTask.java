package net.osmand.plus.myplaces.tracks.tasks;

import static net.osmand.shared.gpx.ColoringPurpose.TRACK;
import static net.osmand.shared.gpx.GpxParameter.COLOR_PALETTE;
import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.shared.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.shared.gpx.GpxParameter.TRACK_VISUALIZATION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.WIDTH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.shared.io.KFile;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDbHelper.GpxDataItemCallback;;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxParameter;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Set;

public class ChangeTracksAppearanceTask extends BaseLoadAsyncTask<Void, File, Void> {

	private static final Log LOG = PlatformUtil.getLog(ChangeTracksAppearanceTask.class);

	private final GpxDbHelper gpxDbHelper;
	private final GpxSelectionHelper selectionHelper;

	private final AppearanceData data;
	private final Set<TrackItem> items;
	private final CallbackWithObject<Void> callback;

	public ChangeTracksAppearanceTask(@NonNull FragmentActivity activity, @NonNull AppearanceData data,
	                                  @NonNull Set<TrackItem> items, @Nullable CallbackWithObject<Void> callback) {
		super(activity);
		this.data = data;
		this.items = items;
		this.callback = callback;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.selectionHelper = app.getSelectedGpxHelper();
	}

	@Override
	protected Void doInBackground(Void... params) {
		boolean resetAnything = data.shouldResetAnything();
		for (TrackItem item : items) {
			KFile file = item.getFile();
			if (file != null) {
				GpxFile gpxFile = resetAnything ? getGpxFile(file) : null;
				updateTrackAppearance(file, gpxFile);
			} else if (item.isShowCurrentTrack()) {
				updateCurrentTrackAppearance();
			}
		}
		return null;
	}

	private void updateTrackAppearance(@NonNull KFile file, @Nullable GpxFile gpxFile) {
		GpxDataItemCallback callback = getGpxDataItemCallback(gpxFile);
		GpxDataItem item = gpxDbHelper.getItem(file, callback);
		if (item != null) {
			updateTrackAppearance(item, gpxFile);
		}
	}

	@Nullable
	private GpxFile getGpxFile(@NonNull KFile file) {
		SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedFileByPath(file.absolutePath());
		GpxFile gpxFile = selectedGpxFile != null ? selectedGpxFile.getGpxFile() : SharedUtil.loadGpxFile(file);
		if (gpxFile.getError() != null) {
			LOG.error("Failed read gpx file", SharedUtil.jException(gpxFile.getError()));
		}
		return gpxFile.getError() == null ? gpxFile : null;
	}

	private void updateTrackAppearance(@NonNull GpxDataItem item, @Nullable GpxFile gpxFile) {
		for (GpxParameter parameter : GpxParameter.Companion.getAppearanceParameters()) {
			if (data.shouldResetParameter(parameter)) {
				if (gpxFile != null) {
					item.readGpxAppearanceParameter(gpxFile, parameter);
				}
			} else {
				Object value = data.getParameter(parameter);
				if (value != null) {
					item.setParameter(parameter, value);
				}
			}
		}
		app.getGpxDbHelper().updateDataItem(item);

		SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedFileByPath(item.getFile().getParentFile().absolutePath());
		if (selectedGpxFile != null) {
			selectedGpxFile.resetSplitProcessed();
		}
	}

	private void updateCurrentTrackAppearance() {
		Integer color = data.getParameter(COLOR);
		if (color != null) {
			settings.CURRENT_TRACK_COLOR.set(color);
		}
		String coloringType = data.getParameter(COLORING_TYPE);
		if (coloringType != null) {
			settings.CURRENT_TRACK_COLORING_TYPE.set(ColoringType.Companion.requireValueOf(TRACK, coloringType));
			settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.set(ColoringType.Companion.getRouteInfoAttribute(coloringType));
		}
		String width = data.getParameter(WIDTH);
		if (width != null) {
			settings.CURRENT_TRACK_WIDTH.set(width);
		}
		Boolean showArrows = data.getParameter(SHOW_ARROWS);
		if (showArrows != null) {
			settings.CURRENT_TRACK_SHOW_ARROWS.set(showArrows);
		}
		Boolean showStartFinish = data.getParameter(SHOW_START_FINISH);
		if (showStartFinish != null) {
			settings.CURRENT_TRACK_SHOW_START_FINISH.set(showStartFinish);
		}
		String trackVisualizationType = data.getParameter(TRACK_VISUALIZATION_TYPE);
		if (trackVisualizationType != null) {
			settings.CURRENT_TRACK_3D_VISUALIZATION_TYPE.set(trackVisualizationType);
		}
		String gradientPalette = data.getParameter(COLOR_PALETTE);
		if (gradientPalette != null) {
			settings.CURRENT_GRADIENT_PALETTE.set(gradientPalette);
		}
	}

	@NonNull
	private GpxDataItemCallback getGpxDataItemCallback(@Nullable GpxFile gpxFile) {
		return new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return ChangeTracksAppearanceTask.this.isCancelled();
			}

			@Override
			public void onGpxDataItemReady(@NonNull GpxDataItem item) {
				updateTrackAppearance(item, gpxFile);
			}
		};
	}

	@Override
	protected void onPostExecute(Void result) {
		hideProgress();

		if (callback != null) {
			callback.processResult(null);
		}
	}
}

