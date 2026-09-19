package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class ValueExceedLimitDialogFragment extends DialogFragment {

	private static final String TAG = ValueExceedLimitDialogFragment.class.getSimpleName();

	private static final String KEY_TAG_NAME = "key_tag_name";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode());
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		String tagName = getTagName();

		String title = Algorithms.isEmpty(tagName)
				? getString(R.string.save_poi_value_exceed_length_title)
				: getString(R.string.save_poi_value_exceed_length_title, tagName);

		String message = Algorithms.isEmpty(tagName)
				? getString(R.string.save_poi_value_exceed_length)
				: getString(R.string.save_poi_value_exceed_length, tagName);

		builder.setTitle(title)
				.setMessage(message)
				.setNegativeButton(R.string.shared_string_ok, null);
		return builder.create();
	}

	private boolean isNightMode() {
		OsmandApplication app = ((OsmandApplication) requireActivity().getApplication());
		return !app.getSettings().isLightContent();
	}

	private String getTagName() {
		Bundle args = getArguments();
		if (args != null) {
			return args.getString(KEY_TAG_NAME);
		}
		return "";
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager, String tagWithExceedingLength) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			ValueExceedLimitDialogFragment fragment = new ValueExceedLimitDialogFragment();
			Bundle args = new Bundle();
			args.putString(KEY_TAG_NAME, tagWithExceedingLength);
			fragment.setArguments(args);
			fragment.show(childFragmentManager, TAG);
		}
	}
}