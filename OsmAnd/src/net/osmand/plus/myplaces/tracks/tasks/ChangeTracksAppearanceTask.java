package net.osmand.plus.myplaces.tracks.tasks;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.gpx.GpxParameter.WIDTH;
import static net.osmand.plus.card.color.ColoringPurpose.TRACK;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;

import java.io.File;
import java.util.Set;

public class ChangeTracksAppearanceTask extends BaseLoadAsyncTask<Void, File, Void> {

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
		GpxDataItemCallback callback = getGpxDataItemCallback();
		for (TrackItem item : items) {
			File file = item.getFile();
			if (file != null) {
				GpxDataItem dataItem = gpxDbHelper.getItem(file, callback);
				if (dataItem != null) {
					updateTrackAppearance(dataItem);
				}
			} else if (item.isShowCurrentTrack()) {
				updateCurrentTrackAppearance();
			}
		}
		return null;
	}

	private void updateTrackAppearance(@NonNull GpxDataItem item) {
		for (GpxParameter parameter : GpxParameter.getAppearanceParameters()) {
			setParameterIfEdited(item, parameter, data.getParameter(parameter));
		}
		app.getGpxDbHelper().updateDataItem(item);

		SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedFileByPath(item.getFile().getAbsolutePath());
		if (selectedGpxFile != null) {
			selectedGpxFile.resetSplitProcessed();
		}
	}

	private void setParameterIfEdited(@NonNull GpxDataItem item, @NonNull GpxParameter parameter, @Nullable Object value) {
		if (value != null) {
			item.setParameter(parameter, value);
		}
	}

	private void updateCurrentTrackAppearance() {
		Integer color = data.getParameter(COLOR);
		if (color != null) {
			settings.CURRENT_TRACK_COLOR.set(color);
		}
		String coloringType = data.getParameter(COLORING_TYPE);
		if (coloringType != null) {
			settings.CURRENT_TRACK_COLORING_TYPE.set(ColoringType.requireValueOf(TRACK, coloringType));
			settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.set(ColoringType.getRouteInfoAttribute(coloringType));
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
	}

	@NonNull
	private GpxDataItemCallback getGpxDataItemCallback() {
		return new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return ChangeTracksAppearanceTask.this.isCancelled();
			}

			@Override
			public void onGpxDataItemReady(@NonNull GpxDataItem item) {
				updateTrackAppearance(item);
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

