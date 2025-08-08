package net.osmand.plus.plugins.mapillary;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;

public class MapillaryInstallDialogFragment extends BaseAlertDialogFragment {

	public static final String TAG = MapillaryInstallDialogFragment.class.getSimpleName();

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		AlertDialog.Builder builder = createDialogBuilder();
		builder.setCancelable(true);
		builder.setNegativeButton(getString(R.string.shared_string_cancel), (dialog, which) -> dialog.dismiss());
		builder.setPositiveButton(getString(R.string.shared_string_install), (dialog, which) -> {
			dialog.dismiss();
			MapillaryPlugin.installMapillary(app);
		});
		builder.setView(inflate(R.layout.mapillary_install_dialog));
		return builder.create();
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}
}
