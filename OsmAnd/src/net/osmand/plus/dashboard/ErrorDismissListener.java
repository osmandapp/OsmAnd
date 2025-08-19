package net.osmand.plus.dashboard;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashBaseFragment.DismissListener;

class ErrorDismissListener implements DismissListener {

	private final View parentView;
	private final DashboardOnMap dashboardOnMap;
	private final String fragmentTag;
	private final View fragmentView;

	public ErrorDismissListener(View parentView, DashboardOnMap dashboardOnMap,
	                            String fragmentTag, View fragmentView) {
		this.parentView = parentView;
		this.dashboardOnMap = dashboardOnMap;
		this.fragmentTag = fragmentTag;
		this.fragmentView = fragmentView;
	}

	@Override
	public void onDismiss() {
		dashboardOnMap.hideFragmentByTag(fragmentTag);
		fragmentView.setTranslationX(0);
		fragmentView.setAlpha(1);
		Snackbar.make(parentView, dashboardOnMap.getMyApplication().getResources()
						.getString(R.string.shared_string_card_was_hidden), Snackbar.LENGTH_LONG)
				.setAction(R.string.shared_string_undo, view -> ErrorDismissListener.this.onUndo())
				.show();
	}

	public void onUndo() {
		dashboardOnMap.unhideFragmentByTag(fragmentTag);
		fragmentView.setTranslationX(0);
		fragmentView.setAlpha(1);
	}
}
