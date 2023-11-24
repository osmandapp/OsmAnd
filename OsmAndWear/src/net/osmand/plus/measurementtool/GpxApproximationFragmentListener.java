package net.osmand.plus.measurementtool;

import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;

import java.util.List;

public interface GpxApproximationFragmentListener {

	void onGpxApproximationDone(List<GpxRouteApproximation> gpxApproximations, List<List<WptPt>> pointsList, ApplicationMode mode);

	void onApplyGpxApproximation();

	void onCancelGpxApproximation();

}
