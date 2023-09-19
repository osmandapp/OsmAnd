package net.osmand.plus.download.local.dialogs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.LocalItemsHolder;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;

public abstract class LocalBaseFragment extends BaseOsmAndFragment implements OperationListener {

	@Nullable
	public abstract LocalItemsHolder getItemsHolder();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	protected void updateProgressVisibility(boolean visible) {
		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	@Nullable
	protected DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	@NonNull
	protected DownloadActivity requireDownloadActivity() {
		return (DownloadActivity) requireActivity();
	}
}
