package net.osmand.plus.dashboard.tools;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;

public final class DashFragmentData implements Comparable<DashFragmentData> {
	public final String tag;
	public final Class<? extends DashBaseFragment> fragmentClass;
	public final int titleStringId;
	public final ShouldShowFunction shouldShowFunction;
	public final int position;
	public final String rowNumberTag;

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							@StringRes int titleStringId, ShouldShowFunction shouldShowFunction,
							int position, String rowNumberTag) {
		this.tag = tag;
		this.fragmentClass = fragmentClass;
		this.titleStringId = titleStringId;
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
		return titleStringId != -1;
	}
	public interface ShouldShowFunction {
		boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag);
	}

}
