package net.osmand.plus.routing;

import androidx.annotation.Nullable;

import net.osmand.map.WorldRegion;

import java.util.List;

public interface RouteCalculationProgressListener {

	void onCalculationStart();

	void onUpdateCalculationProgress(int progress);

	void onRequestPrivateAccessRouting();

	void onCalculationFinish();
}
