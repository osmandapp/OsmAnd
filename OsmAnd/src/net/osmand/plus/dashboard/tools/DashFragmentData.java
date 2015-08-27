package net.osmand.plus.dashboard.tools;

import android.support.annotation.NonNull;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;

public final class DashFragmentData implements Comparable<DashFragmentData> {
	public final String tag;
	public final Class<? extends DashBaseFragment> fragmentClass;
	public final String title;
	public final ShouldShowFunction shouldShowFunction;
	public final boolean customDeletionLogic;
	public final int position;
	public final String rowNumberTag;

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							String title, ShouldShowFunction shouldShowFunction,
							boolean customDeletionLogic, int position, String rowNumberTag) {
		this.tag = tag;
		this.fragmentClass = fragmentClass;
		this.title = title;
		this.shouldShowFunction = shouldShowFunction;
		this.customDeletionLogic = customDeletionLogic;
		this.position = position;
		this.rowNumberTag = rowNumberTag;
	}

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							String title, ShouldShowFunction shouldShowFunction,
							boolean customDeletionLogic, int position) {
		this(tag, fragmentClass, title, shouldShowFunction, customDeletionLogic, position, null);
	}

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							String title, ShouldShowFunction shouldShowFunction, int position) {
		this(tag, fragmentClass, title, shouldShowFunction, false, position, null);
	}

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							String title, int position) {
		this(tag, fragmentClass, title, new DashboardOnMap.DefaultShouldShow(), false, position,
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
