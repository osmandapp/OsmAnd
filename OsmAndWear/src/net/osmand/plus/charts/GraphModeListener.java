package net.osmand.plus.charts;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxTrackAnalysis;

import java.util.List;

public interface GraphModeListener{
	void onGraphModeChanged(@NonNull GPXDataSetAxisType gpxDataSetAxisType, @NonNull List<GPXDataSetType> gpxDataSetTypes);
	GpxTrackAnalysis getAnalysis();
	GPXDataSetAxisType getSelectedAxisType();
	List<GPXDataSetType> getSelectedDataSetTypes();
}
