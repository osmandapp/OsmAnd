package net.osmand.plus.search.dialogs;

import static net.osmand.data.PointDescription.*;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.utils.AndroidUtils;

public class ChooseCoordsFormatDialogFragment extends BaseAlertDialogFragment {

	private static final String TAG = ChooseCoordsFormatDialogFragment.class.getSimpleName();

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		updateNightMode();
		int format = QuickSearchCoordinatesFragment.CURRENT_FORMAT;
		QuickSearchCoordinatesFragment parent = (QuickSearchCoordinatesFragment) getParentFragment();

		String[] entries = {
				PointDescription.formatToHumanString(app, FORMAT_DEGREES),
				PointDescription.formatToHumanString(app, FORMAT_MINUTES),
				PointDescription.formatToHumanString(app, FORMAT_SECONDS),
				PointDescription.formatToHumanString(app, UTM_FORMAT),
				PointDescription.formatToHumanString(app, OLC_FORMAT),
				PointDescription.formatToHumanString(app, MGRS_FORMAT),
				PointDescription.formatToHumanString(app, SWISS_GRID_FORMAT),
				PointDescription.formatToHumanString(app, SWISS_GRID_PLUS_FORMAT)
		};

		AlertDialog.Builder builder = createDialogBuilder();
		builder.setTitle(R.string.coords_format)
				.setSingleChoiceItems(entries, format, (dialog, which) -> {
					if (parent != null) {
						parent.applyFormat(which, false);
					}
					dialog.dismiss();
				});
		return builder.create();
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			new ChooseCoordsFormatDialogFragment().show(childFragmentManager, TAG);
		}
	}
}
