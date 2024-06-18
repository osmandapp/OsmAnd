package net.osmand.plus.track;

import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public class CachedTrackParams {

	public final long prevModifiedTime;
	public final boolean useFilteredGpx;
	public final boolean useJoinSegments;

	public CachedTrackParams(long prevModifiedTime, boolean useFilteredGpx, boolean useJoinSegments) {
		this.prevModifiedTime = prevModifiedTime;
		this.useFilteredGpx = useFilteredGpx;
		this.useJoinSegments = useJoinSegments;
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(prevModifiedTime, useFilteredGpx, useJoinSegments);
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
		return prevModifiedTime == params.prevModifiedTime
				&& useFilteredGpx == params.useFilteredGpx
				&& useJoinSegments == params.useJoinSegments;
	}

	@Override
	public String toString() {
		return "CachedTrackParams{" +
				"prevModifiedTime=" + prevModifiedTime +
				", useFilteredGpx=" + useFilteredGpx +
				", useJoinSegments=" + useJoinSegments;
	}
}