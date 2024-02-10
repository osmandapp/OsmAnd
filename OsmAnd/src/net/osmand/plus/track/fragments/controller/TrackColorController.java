package net.osmand.plus.track.fragments.controller;

import static net.osmand.gpx.GpxParameter.COLOR;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;

import java.util.List;

public class TrackColorController {

	public static void saveCustomColorsToTracks(@NonNull OsmandApplication app, int prevColor, int newColor) {
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		List<GpxDataItem> gpxDataItems = gpxDbHelper.getItems();
		for (GpxDataItem dataItem : gpxDataItems) {
			int color = dataItem.getParameter(COLOR);
			if (prevColor == color) {
				dataItem.setParameter(COLOR, newColor);
				gpxDbHelper.updateDataItem(dataItem);
			}
		}
		List<SelectedGpxFile> files = app.getSelectedGpxHelper().getSelectedGPXFiles();
		for (SelectedGpxFile selectedGpxFile : files) {
			if (prevColor == selectedGpxFile.getGpxFile().getColor(0)) {
				selectedGpxFile.getGpxFile().setColor(newColor);
			}
		}
	}

}
