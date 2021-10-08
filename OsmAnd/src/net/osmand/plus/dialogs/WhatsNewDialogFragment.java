package net.osmand.plus.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import static net.osmand.plus.AppInitializer.LATEST_CHANGES_URL;

public class WhatsNewDialogFragment extends DialogFragment {

	private static final String TAG = WhatsNewDialogFragment.class.getSimpleName();

	private static final int OPEN_WIKIPEDIA_ARTICLE_REQUEST = 2001;

	private static boolean show = true;
	private static boolean showedAndClosedArticle = false;
	private static boolean articleNotShown = true;
	private static boolean dismissed;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dismissed = false;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final OsmandApplication app = requireMyApplication();

		String appVersion = Version.getAppVersion(app);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setTitle(getString(R.string.whats_new) + " " + appVersion)
				.setMessage(getString(R.string.release_4_0))
				.setNegativeButton(R.string.shared_string_close, null);
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
			CustomTabsIntent customTabsIntent = WikipediaDialogFragment.createCustomTabsIntent(mapActivity, uri, nightMode);

			if (AndroidUtils.isIntentSafe(mapActivity, customTabsIntent.intent)) {
				ActivityResultListener listener = new ActivityResultListener(OPEN_WIKIPEDIA_ARTICLE_REQUEST,
						(resultCode, resultData) -> showedAndClosedArticle = true);
				mapActivity.registerActivityResultListener(listener);
				startActivityForResult(customTabsIntent.intent, OPEN_WIKIPEDIA_ARTICLE_REQUEST);
			} else {
				app.showToastMessage(R.string.no_activity_for_intent);
				articleNotShown = true;
			}
		}
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		dismissed = true;
		MapActivity mapActivity = getMapActivity();
		if (articleNotShown && mapActivity != null) {
			OsmandApplication app = requireMyApplication();
			if (SharedStorageWarningBottomSheet.shouldShowStorageWarning(app)) {
				SharedStorageWarningBottomSheet.showInstance(mapActivity);
			}
		}
		super.onDismiss(dialog);
	}

	@Nullable
	private MapActivity getMapActivity() {
		return getActivity() == null ? null : ((MapActivity) getActivity());
	}

	@NonNull
	private OsmandApplication requireMyApplication() {
		return ((OsmandApplication) requireActivity().getApplication());
	}

	public static boolean isGone() {
		return show || showedAndClosedArticle || articleNotShown && dismissed;
	}

	public static boolean shouldShowDialog(@NonNull OsmandApplication app) {
		return app.getAppInitializer().checkAppVersionChanged() && show;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			show = false;
			WhatsNewDialogFragment fragment = new WhatsNewDialogFragment();
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}
}