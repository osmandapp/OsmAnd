package net.osmand.plus.download.ui;

import static net.osmand.plus.Version.FULL_VERSION_NAME;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.utils.AndroidUtils;

public class InstallPaidVersionDialogFragment extends BaseAlertDialogFragment {

	public static final String TAG = InstallPaidVersionDialogFragment.class.getSimpleName();

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		AlertDialog.Builder builder = createDialogBuilder();
		builder.setTitle(R.string.free_version_title);
		builder.setMessage(DownloadValidationManager.getFreeVersionMessage(app));

		if (Version.isMarketEnabled()) {
			builder.setPositiveButton(R.string.install_paid, (dialog, which) -> {
				Context context = getContext();
				if (context != null) {
					Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, FULL_VERSION_NAME));
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					AndroidUtils.startActivityIfSafe(context, intent);
				}
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
		} else {
			builder.setNeutralButton(R.string.shared_string_ok, null);
		}
		return builder.create();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			new InstallPaidVersionDialogFragment().show(fragmentManager, TAG);
		}
	}
}
