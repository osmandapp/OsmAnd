package net.osmand.plus.avoidroads;

import androidx.annotation.Nullable;

public interface AvoidRoadsCallback {

	void onAddImpassableRoad(boolean success, @Nullable AvoidRoadInfo roadInfo);

	boolean isCancelled();
}
