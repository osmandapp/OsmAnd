package net.osmand.plus.download.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivity.FreeVersionDialog;

import static net.osmand.plus.OsmandApplication.SHOW_PLUS_VERSION_INAPP_PARAM;

public class FreeVersionDialogFragment extends DialogFragment {
	public static final String TAG = "FreeVersionDialogFragment";
	private FreeVersionDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		OsmandApplication app = (OsmandApplication) getActivity().getApplication();
		app.activateFetchedRemoteParams();

		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.OsmandDarkTheme));
		builder.setNegativeButton(R.string.later, null);
		View view = getActivity().getLayoutInflater().inflate(R.layout.free_version_banner, null);
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