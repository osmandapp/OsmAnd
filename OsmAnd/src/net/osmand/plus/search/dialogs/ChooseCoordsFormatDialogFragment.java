package net.osmand.plus.search.dialogs;

import static net.osmand.data.PointDescription.*;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;

public class ChooseCoordsFormatDialogFragment extends DialogFragment {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context context = requireContext();
		int format = QuickSearchCoordinatesFragment.CURRENT_FORMAT;
		QuickSearchCoordinatesFragment parent = (QuickSearchCoordinatesFragment) getParentFragment();

		String[] entries = {
				PointDescription.formatToHumanString(context, FORMAT_DEGREES),
				PointDescription.formatToHumanString(context, FORMAT_MINUTES),
				PointDescription.formatToHumanString(context, FORMAT_SECONDS),
				PointDescription.formatToHumanString(context, UTM_FORMAT),
				PointDescription.formatToHumanString(context, OLC_FORMAT),
				PointDescription.formatToHumanString(context, MGRS_FORMAT),
				PointDescription.formatToHumanString(context, SWISS_GRID_FORMAT),
				PointDescription.formatToHumanString(context, SWISS_GRID_PLUS_FORMAT)
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.coords_format)
				.setSingleChoiceItems(entries, format, (dialog, which) -> {
					if (parent != null) {
						parent.applyFormat(which, false);
					}
					dialog.dismiss();
				});
		return builder.create();
	}
}
