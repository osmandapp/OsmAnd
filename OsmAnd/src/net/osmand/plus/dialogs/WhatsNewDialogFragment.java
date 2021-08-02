package net.osmand.plus.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

import static net.osmand.plus.AppInitializer.LATEST_CHANGES_URL;

public class WhatsNewDialogFragment extends DialogFragment {

	public static boolean SHOW = true;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final OsmandApplication app = (OsmandApplication) getActivity().getApplication();

		String appVersion = Version.getAppVersion(app);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.whats_new) + " " + appVersion)
				.setMessage(getString(R.string.release_4_0))
				.setNegativeButton(R.string.shared_string_close, null);
		builder.setPositiveButton(R.string.read_more, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					boolean nightMode = !app.getSettings().isLightContent();
					WikipediaDialogFragment.showFullArticle(activity, Uri.parse(LATEST_CHANGES_URL), nightMode);
				}
				dismiss();
			}
		});
		return builder.create();
	}
}
