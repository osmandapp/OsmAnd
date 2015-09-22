package net.osmand.plus.dashboard.tools;

import android.support.annotation.NonNull;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;

public final class DashFragmentData implements Comparable<DashFragmentData> {
	public final String tag;
	public final Class<? extends DashBaseFragment> fragmentClass;
	public final int titleStringId;
	public final ShouldShowFunction shouldShowFunction;
	public final boolean customDeletionLogic;
	public final int position;
	public final String rowNumberTag;

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							int titleStringId, ShouldShowFunction shouldShowFunction,
							boolean customDeletionLogic, int position, String rowNumberTag) {
		this.tag = tag;
		this.fragmentClass = fragmentClass;
		this.titleStringId = titleStringId;
		this.shouldShowFunction = shouldShowFunction;
		this.customDeletionLogic = customDeletionLogic;
		this.position = position;
		this.rowNumberTag = rowNumberTag;
	}

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							int titleStringId, ShouldShowFunction shouldShowFunction,
							boolean customDeletionLogic, int position) {
		this(tag, fragmentClass, titleStringId, shouldShowFunction, customDeletionLogic, position, null);
	}

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							int titleStringId, ShouldShowFunction shouldShowFunction, int position) {
		this(tag, fragmentClass, titleStringId, shouldShowFunction, false, position, null);
	}

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							int titleStringId, int position) {
		this(tag, fragmentClass, titleStringId, new DashboardOnMap.DefaultShouldShow(), false, position,
				null);
	}

	@Override
	public int compareTo(@NonNull DashFragmentData another) {
		return position - another.position;
	}

	public interface ShouldShowFunction {
		boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag);
	}

}
