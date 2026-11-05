package net.osmand.plus.search.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.search.listitems.QuickSearchListItem;

import java.util.List;

public class DeleteDialogFragment extends DialogFragment {

	private List<QuickSearchListItem> selectedItems;

	public void setSelectedItems(@NonNull List<QuickSearchListItem> selectedItems) {
		this.selectedItems = selectedItems;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.confirmation_to_delete_history_items).setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			Fragment parentFragment = getParentFragment();
			if (parentFragment instanceof QuickSearchDialogFragment) {
				QuickSearchDialogFragment fragment = (QuickSearchDialogFragment) parentFragment;

				OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
				SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
				for (QuickSearchListItem searchListItem : selectedItems) {
					helper.remove(searchListItem.getSearchResult().object);
				}
				fragment.reloadHistory();
				fragment.enableSelectionMode(false, -1);
			}
		}).setNegativeButton(R.string.shared_string_no, null);
		return builder.create();
	}
}
