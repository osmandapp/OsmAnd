package net.osmand.plus.charts;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXTrackAnalysis;

import java.util.List;

public interface GraphModeListener{
	void onGraphModeChanged(@NonNull GPXDataSetAxisType gpxDataSetAxisType, @NonNull List<GPXDataSetType> gpxDataSetTypes);
	GPXTrackAnalysis getAnalysis();
	GPXDataSetAxisType getSelectedAxisType();
	List<GPXDataSetType> getSelectedDataSetTypes();
}
