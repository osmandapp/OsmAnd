package net.osmand.plus.dialogs;

import static net.osmand.plus.AppInitializer.LATEST_CHANGES_URL;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.datastorage.SharedStorageWarningFragment;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

public class WhatsNewDialogFragment extends DialogFragment {

	public static final String TAG = WhatsNewDialogFragment.class.getSimpleName();

	private static boolean notShown = true;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final OsmandApplication app = requireMyApplication();

		String appVersion = Version.getAppVersion(app);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setTitle(getString(R.string.whats_new) + " " + appVersion)
				.setMessage(getString(R.string.release_4_1))
				.setNegativeButton(R.string.shared_string_close, (dialog, which) -> showSharedStorageWarningIfRequired());
		builder.setPositiveButton(R.string.read_more, (dialog, which) -> {
			showArticle();
			dismiss();
		});
		return builder.create();
	}

	private void showArticle() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = requireMyApplication();
			Uri uri = Uri.parse(LATEST_CHANGES_URL);
			boolean nightMode = !app.getSettings().isLightContent();
			WikipediaDialogFragment.showFullArticle(mapActivity, uri, nightMode);
		}
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		super.onCancel(dialog);
		showSharedStorageWarningIfRequired();
	}

	private void showSharedStorageWarningIfRequired() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = requireMyApplication();
			if (mapActivity.getFragment(SharedStorageWarningFragment.TAG) == null
					&& SharedStorageWarningFragment.dialogShowRequired(app)) {
				SharedStorageWarningFragment.showInstance(mapActivity.getSupportFragmentManager(), true);
			}
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return getActivity() == null ? null : ((MapActivity) getActivity());
	}

	@NonNull
	private OsmandApplication requireMyApplication() {
		return ((OsmandApplication) requireActivity().getApplication());
	}

	public static boolean wasNotShown() {
		return notShown;
	}

	public static boolean shouldShowDialog(@NonNull OsmandApplication app) {
		return app.getAppInitializer().checkAppVersionChanged() && notShown;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			notShown = false;
			WhatsNewDialogFragment fragment = new WhatsNewDialogFragment();
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}
}