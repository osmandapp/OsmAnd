package net.osmand.plus.download.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivity.FreeVersionDialog;
import net.osmand.util.Algorithms;

public class FreeVersionDialogFragment extends DialogFragment {


	private FreeVersionDialog dialog;

	@SuppressLint("HardwareIds")
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.OsmandDarkTheme));
		builder.setNegativeButton(R.string.later, null);
		View view = getActivity().getLayoutInflater().inflate(R.layout.free_version_banner, null);
		boolean hidePlus = false;
		try {
			String devId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
			if (!Algorithms.isEmpty(devId)) {
				hidePlus = devId.hashCode() % 20 == 8;
			}
		} catch (Exception e) {
			// ignored
		}
		view.findViewById(R.id.osmLiveLayoutTopDivider).setVisibility(hidePlus ? View.GONE : View.VISIBLE);
		view.findViewById(R.id.fullVersionLayout).setVisibility(hidePlus ? View.GONE : View.VISIBLE);
		builder.setView(view);
		dialog = new DownloadActivity.FreeVersionDialog(view, getDownloadActivity(), true);
		dialog.initFreeVersionBanner();
		dialog.expandBanner();
		return builder.create();
	}


	DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

}