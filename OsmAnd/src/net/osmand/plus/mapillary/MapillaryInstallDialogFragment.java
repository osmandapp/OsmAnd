package net.osmand.plus.mapillary;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;

public class MapillaryInstallDialogFragment extends DialogFragment {

	public static final String TAG = "MapillaryInstallDialogFragment";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final MapActivity mapActivity = (MapActivity) getActivity();
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();

		final AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode));
		builder.setCancelable(true);
		builder.setNegativeButton(mapActivity.getString(R.string.shared_string_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setPositiveButton(mapActivity.getString(R.string.shared_string_install), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				MapillaryPlugin.installMapillary(mapActivity, mapActivity.getMyApplication());
			}
		});

		builder.setView(UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.mapillary_install_dialog, null));
		return builder.create();
	}
}
