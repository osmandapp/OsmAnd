package net.osmand.plus.dashboard.tools;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;

public class NumberPickerDialogFragment extends DialogFragment {
	public static final String TAG = "NumberPickerDialogFragment";
	private static final org.apache.commons.logging.Log LOG =
			PlatformUtil.getLog(NumberPickerDialogFragment.class);

	private static final String NUMBER_TAG = "number_tag";
	private static final String HEADER_TEXT = "header_text";
	private static final String SUBHEADER_TEXT = "subheader_text";
	private static final String NUMBER_OF_ITEMS = "number_of_items";
	private static final String CURRENT_NUMBER = "current_number";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (!(getParentFragment() instanceof CanAcceptNumber)) {
			throw new RuntimeException("Parent fragment must implement CanAcceptNumber");
		}
		Bundle args = getArguments();
		String numberTag = args.getString(NUMBER_TAG);
		String headerText = args.getString(HEADER_TEXT);
		String subHeaderText = args.getString(SUBHEADER_TEXT);
		int numberOfItems = args.getInt(NUMBER_OF_ITEMS);
		int currentNumber = args.getInt(CURRENT_NUMBER);

		String[] items = new String[numberOfItems];
		for (int i = 0; i < numberOfItems; i++) {
			items[i] = String.valueOf(i + 1);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setSingleChoiceItems(items, Math.max(0, currentNumber - 1), null)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int userChoice =
								((AlertDialog) dialog).getListView().getCheckedItemPosition() + 1;
						((CanAcceptNumber) getParentFragment()).acceptNumber(numberTag, userChoice);
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		if (subHeaderText != null) {
			View titleView = LayoutInflater.from(getActivity())
					.inflate(R.layout.number_picker_dialog_title, null);
			TextView titleTextView = titleView.findViewById(R.id.titleTextView);
			titleTextView.setText(headerText);
			TextView subtitleTextView = titleView.findViewById(R.id.subtitleTextView);
			subtitleTextView.setText(subHeaderText);
			builder.setCustomTitle(titleView);
		} else {
			builder.setTitle(headerText);
		}
		return builder.create();
	}

	public static NumberPickerDialogFragment createInstance(String header, String subheader,
															String tag, int currentRow, int maxNumber) {
		Bundle args = new Bundle();
		args.putString(HEADER_TEXT, header);
		args.putString(SUBHEADER_TEXT, subheader);
		args.putString(NUMBER_TAG, tag);
		args.putInt(CURRENT_NUMBER, currentRow);
		args.putInt(NUMBER_OF_ITEMS, maxNumber);
		NumberPickerDialogFragment fragment = new NumberPickerDialogFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public interface CanAcceptNumber {
		void acceptNumber(String tag, int number);
	}
}
