package net.osmand.plus.download.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivity.FreeVersionDialog;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.IndexItem;

import java.util.ArrayList;
import java.util.List;

public class FreeVersionDialogFragment extends DialogFragment {


	private FreeVersionDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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