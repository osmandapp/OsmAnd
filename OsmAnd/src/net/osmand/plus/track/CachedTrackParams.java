package net.osmand.plus.track;

import net.osmand.util.Algorithms;

public record CachedTrackParams(long modifiedTime, boolean useFilteredGpx, boolean useJoinSegments, long pointsCount) {

	@Override
	public int hashCode() {
		return Algorithms.hash(modifiedTime, useFilteredGpx, useJoinSegments);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		CachedTrackParams params = (CachedTrackParams) obj;
		return modifiedTime == params.modifiedTime
				&& useFilteredGpx == params.useFilteredGpx
				&& useJoinSegments == params.useJoinSegments
				&& pointsCount == params.pointsCount;
	}

	@Override
	public String toString() {
		return "CachedTrackParams{" +
				"modifiedTime=" + modifiedTime +
				", useFilteredGpx=" + useFilteredGpx +
				", useJoinSegments=" + useJoinSegments +
				", pointsCount=" + pointsCount;
	}
}