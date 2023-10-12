package net.osmand.plus.base.globallistener.implementations;

import androidx.fragment.app.Fragment;

import net.osmand.plus.base.globallistener.BaseGlobalListener;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;

public class GlobalGpxSelectionListener extends BaseGlobalListener implements SelectGpxTaskListener {

	@Override
	public void onGpxSelectionStarted() {
		for (Fragment fragment : getAddedFragments()) {
			if (fragment instanceof SelectGpxTaskListener) {
				((SelectGpxTaskListener) fragment).onGpxSelectionStarted();
			}
		}
	}

	@Override
	public void onGpxSelectionInProgress() {
		for (Fragment fragment : getAddedFragments()) {
			if (fragment instanceof SelectGpxTaskListener) {
				((SelectGpxTaskListener) fragment).onGpxSelectionInProgress();
			}
		}
	}

	@Override
	public void onGpxSelectionFinished() {
		for (Fragment fragment : getAddedFragments()) {
			if (fragment instanceof SelectGpxTaskListener) {
				((SelectGpxTaskListener) fragment).onGpxSelectionFinished();
			}
		}
	}

}
