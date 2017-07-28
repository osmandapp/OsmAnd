package net.osmand.core.samples.android.sample1;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;

public class InstallOsmandAppDialog extends AppCompatDialogFragment {
	private static final String TAG = "InstallOsmandAppDialog";
	private static final String OSMAND_PLUS_PACKAGE_NAME = "net.osmand.plus";
	private static final String OSMAND_PACKAGE_NAME = "net.osmand";
	private static boolean processed = false;
	private static boolean shown = false;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.install_osmand_title);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(getString(R.string.restart_app), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				SampleUtils.doRestart(getActivity());
			}
		});

		View view = getActivity().getLayoutInflater().inflate(R.layout.install_osmand_dialog, null);
		view.findViewById(R.id.install_osmand_btn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean success = execOsmAndInstall("market://details?id=");
				if (!success) {
					success = execOsmAndInstall("https://play.google.com/store/apps/details?id=");
				}
				if (!success) {
					execOsmAndInstall("http://osmand.net/apps?id=");
				}
			}
		});
		builder.setView(view);
		return builder.create();
	}

	private boolean execOsmAndInstall(String prefix) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(prefix + OSMAND_PACKAGE_NAME));
		try {
			getActivity().startActivity(intent);
			return true;
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean wasShown() {
		return shown;
	}

	public static boolean show(FragmentManager manager, Context ctx) {
		if (processed) {
			return false;
		}
		processed = true;
		if (!SampleUtils.isPackageInstalled(OSMAND_PACKAGE_NAME, ctx)
				&& !SampleUtils.isPackageInstalled(OSMAND_PLUS_PACKAGE_NAME, ctx)) {
			new InstallOsmandAppDialog().show(manager, TAG);
			shown = true;
		}
		return shown;
	}
}
