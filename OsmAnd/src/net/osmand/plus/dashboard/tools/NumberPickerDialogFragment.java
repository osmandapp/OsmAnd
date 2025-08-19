package net.osmand.plus.dashboard.tools;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.utils.AndroidUtils;

public class NumberPickerDialogFragment extends BaseAlertDialogFragment {

	private static final String TAG = NumberPickerDialogFragment.class.getSimpleName();
	private static final org.apache.commons.logging.Log LOG =
			PlatformUtil.getLog(NumberPickerDialogFragment.class);

	private static final String NUMBER_TAG = "number_tag";
	private static final String HEADER_TEXT = "header_text";
	private static final String SUBHEADER_TEXT = "subheader_text";
	private static final String NUMBER_OF_ITEMS = "number_of_items";
	private static final String CURRENT_NUMBER = "current_number";

	private boolean usedOnMap;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (!(getParentFragment() instanceof CanAcceptNumber)) {
			throw new RuntimeException("Parent fragment must implement CanAcceptNumber");
		}
		updateNightMode();
		Bundle args = requireArguments();
		String numberTag = args.getString(NUMBER_TAG);
		String headerText = args.getString(HEADER_TEXT);
		String subHeaderText = args.getString(SUBHEADER_TEXT);
		int numberOfItems = args.getInt(NUMBER_OF_ITEMS);
		int currentNumber = args.getInt(CURRENT_NUMBER);

		String[] items = new String[numberOfItems];
		for (int i = 0; i < numberOfItems; i++) {
			items[i] = String.valueOf(i + 1);
		}
		AlertDialog.Builder builder = createDialogBuilder();
		builder.setSingleChoiceItems(items, Math.max(0, currentNumber - 1), null)
				.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					int userChoice = ((AlertDialog) dialog).getListView().getCheckedItemPosition() + 1;
					((CanAcceptNumber) getParentFragment()).acceptNumber(numberTag, userChoice);
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		if (subHeaderText != null) {
			View titleView = inflate(R.layout.number_picker_dialog_title);
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

	@Override
	protected boolean isUsedOnMap() {
		return usedOnMap;
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull String header, @NonNull String subheader,
	                                @NonNull String tag, int currentRow, int maxNumber,
	                                boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(HEADER_TEXT, header);
			args.putString(SUBHEADER_TEXT, subheader);
			args.putString(NUMBER_TAG, tag);
			args.putInt(CURRENT_NUMBER, currentRow);
			args.putInt(NUMBER_OF_ITEMS, maxNumber);
			NumberPickerDialogFragment fragment = new NumberPickerDialogFragment();
			fragment.setArguments(args);
			fragment.usedOnMap = usedOnMap;
			fragment.show(childFragmentManager, TAG);
		}
	}

	public interface CanAcceptNumber {
		void acceptNumber(String tag, int number);
	}
}
