package net.osmand.plus.dialogs;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_WHATS_NEW_ID;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.datastorage.SharedStorageWarningFragment;
import net.osmand.plus.utils.AndroidUtils;

import java.lang.reflect.Field;


public class WhatsNewDialogFragment extends DialogFragment {

	public static final String TAG = WhatsNewDialogFragment.class.getSimpleName();

	private static boolean notShown = true;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		OsmandApplication app = requireMyApplication();
		Class<? extends R.string> cl = R.string.class;
		String ver = Version.getAppVersion(app);
		String message = "Release " + Version.getAppVersion(app);
		if(ver.length() > 0 ) {
			try {
				Field f = R.string.class.getField("release_" + ver.charAt(0) + "_" + ver.charAt(2));
				if (f != null) {
					Integer in = (Integer) f.get(null);
					if (in != null) {
						message = getString(in);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String appVersion = Version.getAppVersion(app);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setTitle(getString(R.string.whats_new) + " " + appVersion)
				.setMessage(message)
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
			Uri uri = Uri.parse(app.getString(R.string.docs_latest_version));
			boolean nightMode = !app.getSettings().isLightContent();
			AndroidUtils.openUrl(mapActivity, uri, nightMode);
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
			if (mapActivity.getFragmentsHelper().getFragment(SharedStorageWarningFragment.TAG) == null
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
		if (app.getAppCustomization().isFeatureEnabled(FRAGMENT_WHATS_NEW_ID)) {
			return app.getAppInitializer().checkAppVersionChanged() && notShown;
		}
		return false;
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
