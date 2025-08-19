package net.osmand.plus.dashboard;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashBaseFragment.DismissListener;

public class DefaultDismissListener implements DismissListener {

	private final View parentView;
	private final DashboardOnMap dashboard;
	private final String fragmentTag;
	private final View fragmentView;

	public DefaultDismissListener(View parentView, DashboardOnMap dashboard,
	                              String fragmentTag, View fragmentView) {
		this.parentView = parentView;
		this.dashboard = dashboard;
		this.fragmentTag = fragmentTag;
		this.fragmentView = fragmentView;
	}

	@Override
	public void onDismiss() {
		dashboard.blacklistFragmentByTag(fragmentTag);
		fragmentView.setTranslationX(0);
		fragmentView.setAlpha(1);
		Snackbar.make(parentView, dashboard.getMyApplication().getResources()
						.getString(R.string.shared_string_card_was_hidden), Snackbar.LENGTH_LONG)
				.setAction(R.string.shared_string_undo, view -> DefaultDismissListener.this.onUndo())
				.show();
	}

	public void onUndo() {
		dashboard.unblacklistFragmentClass(fragmentTag);
		fragmentView.setTranslationX(0);
		fragmentView.setAlpha(1);
	}
}
