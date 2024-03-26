package net.osmand.plus.configmap.tracks.appearance.tasks;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.gpx.GpxParameter.WIDTH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;

import java.io.File;
import java.util.Set;

public class ChangeAppearanceTask extends BaseLoadAsyncTask<Void, File, Void> {

	private final GpxDbHelper gpxDbHelper;
	private final GpxSelectionHelper selectionHelper;

	private final Set<TrackItem> trackItems;
	private final AppearanceData appearanceData;
	private final CallbackWithObject<Void> callback;

	public ChangeAppearanceTask(@NonNull FragmentActivity activity,
								@NonNull AppearanceData appearanceData,
	                            @NonNull Set<TrackItem> trackItems,
	                            @Nullable CallbackWithObject<Void> callback) {
		super(activity);
		this.trackItems = trackItems;
		this.appearanceData = appearanceData;
		this.callback = callback;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.selectionHelper = app.getSelectedGpxHelper();
	}

	@Override
	protected Void doInBackground(Void... params) {
		GpxDataItemCallback callback = getGpxDataItemCallback();
		for (TrackItem trackItem : trackItems) {
			File file = trackItem.getFile();
			if (file != null) {
				GpxDataItem item = gpxDbHelper.getItem(file, callback);
				if (item != null) {
					updateTrackAppearance(item);
				}
			} else if (trackItem.isShowCurrentTrack()) {
				updateCurrentTrackAppearance();
			}
		}
		return null;
	}

	private void updateTrackAppearance(@NonNull GpxDataItem item) {
		ColoringStyle coloringStyle = appearanceData.getColoringStyle();
		setParameterIfEdited(item, COLORING_TYPE, coloringStyle != null ? coloringStyle.getId() : null);
		setParameterIfEdited(item, COLOR, appearanceData.getCustomColor());
		setParameterIfEdited(item, WIDTH, appearanceData.getWidthValue());
		setParameterIfEdited(item, SHOW_ARROWS, appearanceData.shouldShowArrows());
		setParameterIfEdited(item, SHOW_START_FINISH, appearanceData.shouldShowStartFinish());

//		item.setParameter(SPLIT_TYPE, GpxSplitType.getSplitTypeByTypeId(trackDrawInfo.getSplitType()).getType());
//		item.setParameter(SPLIT_INTERVAL, trackDrawInfo.getSplitInterval());
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
		Integer customColor = appearanceData.getCustomColor();
		if (customColor != null) {
			settings.CURRENT_TRACK_COLOR.set(customColor);
		}
		ColoringStyle coloringStyle = appearanceData.getColoringStyle();
		if (coloringStyle != null) {
			settings.CURRENT_TRACK_COLORING_TYPE.set(coloringStyle.getType());
			settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.set(coloringStyle.getRouteInfoAttribute());
		}
		String widthValue = appearanceData.getWidthValue();
		if (widthValue != null) {
			settings.CURRENT_TRACK_WIDTH.set(widthValue);
		}
		Boolean shouldShowArrows = appearanceData.shouldShowArrows();
		if (shouldShowArrows != null) {
			settings.CURRENT_TRACK_SHOW_ARROWS.set(shouldShowArrows);
		}
		Boolean shouldShowStartFinish = appearanceData.shouldShowStartFinish();
		if (shouldShowStartFinish != null) {
			settings.CURRENT_TRACK_SHOW_START_FINISH.set(shouldShowStartFinish);
		}
	}

	@NonNull
	private GpxDataItemCallback getGpxDataItemCallback() {
		return new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return ChangeAppearanceTask.this.isCancelled();
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

	public static void execute(@NonNull FragmentActivity activity,
	                           @NonNull AppearanceData appearanceData,
	                           @NonNull Set<TrackItem> trackItems,
	                           @Nullable CallbackWithObject<Void> callback) {
		new ChangeAppearanceTask(activity, appearanceData, trackItems, callback).execute();
	}
}