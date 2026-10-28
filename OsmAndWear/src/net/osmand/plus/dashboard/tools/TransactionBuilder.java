package net.osmand.plus.dashboard.tools;

import android.util.Log;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by GaidamakUA on 8/6/15.
 */
public final class TransactionBuilder {
	private static final String TAG = "TransactionBuilder";
	private final FragmentManager manager;
	private final List<DashFragmentData> fragments;
	private final OsmandSettings settings;
	private final MapActivity mapActivity;

	public TransactionBuilder(FragmentManager manager, OsmandSettings settings,
							  MapActivity mapActivity) {
		this.manager = manager;
		this.settings = settings;
		this.mapActivity = mapActivity;
		fragments = new ArrayList<>();
	}

	public TransactionBuilder addFragmentsData(DashFragmentData... dashFragmentsData) {
		fragments.addAll(Arrays.asList(dashFragmentsData));
		return this;
	}

	public TransactionBuilder addFragmentsData(Collection<DashFragmentData> dashFragmentsData) {
		fragments.addAll(dashFragmentsData);
		return this;
	}

	public TransactionBuilder addFragment(DashFragmentData fragmentData) {
		fragments.add(fragmentData);
		return this;
	}

	public FragmentTransaction getFragmentTransaction() {
		FragmentTransaction fragmentTransaction = manager.beginTransaction();
		Collections.sort(fragments);
		for (DashFragmentData dashFragmentData : fragments) {
			DashBaseFragment fragment =
					(DashBaseFragment) manager.findFragmentByTag(dashFragmentData.tag);
			if (fragment == null) {
				if (dashFragmentData.shouldShowFunction.shouldShow(settings, mapActivity, dashFragmentData.tag)) {
					try {
						DashBaseFragment newInstance = dashFragmentData.fragmentClass.newInstance();
						// XXX hardcoded value
						fragmentTransaction.add(R.id.content, newInstance, dashFragmentData.tag);
					} catch (InstantiationException e) {
						Log.v(TAG, "");
						mapActivity.getMyApplication()
								.showToastMessage("Error showing dashboard " + dashFragmentData.tag);
					} catch (IllegalAccessException e) {
						Log.v(TAG, "");
						mapActivity.getMyApplication()
								.showToastMessage("Error showing dashboard " + dashFragmentData.tag);
					}
				}
			} else {
				if (!dashFragmentData.shouldShowFunction.shouldShow(settings, mapActivity, dashFragmentData.tag)) {
					fragmentTransaction.remove(fragment);
				} else if (fragment.getView() != null) {
					if (fragment.isHidden()) {
						fragmentTransaction.show(fragment);
					}
					fragment.onOpenDash();
				}
			}
		}
		return fragmentTransaction;
	}
}
