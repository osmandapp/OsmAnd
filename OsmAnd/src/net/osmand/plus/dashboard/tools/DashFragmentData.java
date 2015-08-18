package net.osmand.plus.dashboard.tools;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;

public final class DashFragmentData {
	public final String tag;
	public final Class<? extends DashBaseFragment> fragmentClass;
	public final String title;
	public final ShouldShowFunction shouldShowFunction;
	public final CanHideFunction canHideFunction;

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							String title, ShouldShowFunction shouldShowFunction,
							CanHideFunction canHideFunction) {
		this.tag = tag;
		this.fragmentClass = fragmentClass;
		this.title = title;
		this.shouldShowFunction = shouldShowFunction;
		this.canHideFunction = canHideFunction;
	}

	public DashFragmentData(String tag, Class<? extends DashBaseFragment> fragmentClass,
							String title, ShouldShowFunction shouldShowFunction) {
		this.tag = tag;
		this.fragmentClass = fragmentClass;
		this.title = title;
		this.shouldShowFunction = shouldShowFunction;
		this.canHideFunction = null;
	}

	public interface ShouldShowFunction {
		boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag);
	}

	public interface CanHideFunction {
		boolean canHide();
	}
}
