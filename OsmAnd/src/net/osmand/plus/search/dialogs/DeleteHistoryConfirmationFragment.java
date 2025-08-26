package net.osmand.plus.search.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.search.history.SearchHistoryHelper;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.utils.AndroidUtils;

import java.util.List;

public class DeleteHistoryConfirmationFragment extends BaseAlertDialogFragment {

	private static final String TAG = DeleteHistoryConfirmationFragment.class.getSimpleName();

	private List<QuickSearchListItem> selectedItems;

	public void setSelectedItems(@NonNull List<QuickSearchListItem> selectedItems) {
		this.selectedItems = selectedItems;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		updateNightMode();
		AlertDialog.Builder builder = createDialogBuilder();
		builder.setTitle(R.string.confirmation_to_delete_history_items)
				.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
					if (getParentFragment() instanceof QuickSearchDialogFragment fragment) {
						SearchHistoryHelper helper = app.getSearchHistoryHelper();
						for (QuickSearchListItem searchListItem : selectedItems) {
							helper.remove(searchListItem.getSearchResult());
						}
						fragment.reloadHistory();
						fragment.enableSelectionMode(false, -1);
					}
				})
				.setNegativeButton(R.string.shared_string_no, null);
		return builder.create();
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull List<QuickSearchListItem> selectedItems) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			DeleteHistoryConfirmationFragment fragment = new DeleteHistoryConfirmationFragment();
			fragment.setSelectedItems(selectedItems);
			fragment.show(childFragmentManager, TAG);
		}
	}
}
