package net.osmand.plus.dashboard.tools;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;

public final class DashFragmentData implements Comparable<DashFragmentData> {
	public final String tag;
	public final Class<? extends DashBaseFragment> fragmentClass;
	public final ShouldShowFunction shouldShowFunction;
	public final int position;
	public final String rowNumberTag;

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
			ShouldShowFunction shouldShowFunction, int position, String rowNumberTag) {
		this.tag = tag;
		this.fragmentClass = fragmentClass;
		this.shouldShowFunction = shouldShowFunction;
		this.position = position;
		this.rowNumberTag = rowNumberTag;
	}

	@Override
	public int compareTo(@NonNull DashFragmentData another) {
		return position - another.position;
	}

	public boolean hasRows() {
		return rowNumberTag != null;
	}

	public boolean canBeDisabled() {
		return shouldShowFunction.getTitleId() != -1;
	}

	public abstract static class ShouldShowFunction {
		public abstract boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag);

		public int getTitleId() {
			return -1;
		}
	}

}
