package net.osmand.plus.measurementtool;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.routing.GpxApproximator;
import net.osmand.router.GpxRouteApproximation;

import java.util.List;

public interface GpxApproximationListener {
	default void onNewApproximation() { }

	default void onApproximationStarted() { }

	default void onSegmentApproximationStarted(@NonNull GpxApproximator approximator) { }

	default void onSegmentApproximationFinished(@NonNull GpxApproximator approximator) { }

	default void updateApproximationProgress(@NonNull GpxApproximator approximator, int progress) { }

	default void processApproximationResults(@NonNull List<GpxRouteApproximation> approximations,
	                                         @NonNull List<List<WptPt>> points) { }
}
