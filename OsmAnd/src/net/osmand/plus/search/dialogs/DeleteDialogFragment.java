package net.osmand.plus.search.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.search.listitems.QuickSearchListItem;

import java.util.List;

public class DeleteDialogFragment extends BaseAlertDialogFragment {

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
}
