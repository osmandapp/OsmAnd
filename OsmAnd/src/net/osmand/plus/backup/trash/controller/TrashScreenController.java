package net.osmand.plus.backup.trash.controller;

import static net.osmand.plus.backup.trash.ScreenItemType.*;
import static net.osmand.plus.settings.bottomsheets.SimpleConfirmationBottomSheet.showConfirmDeleteDialog;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.trash.ScreenItem;
import net.osmand.plus.backup.trash.TrashUtils;
import net.osmand.plus.backup.trash.data.TrashGroup;
import net.osmand.plus.backup.trash.data.TrashItem;
import net.osmand.plus.backup.ui.CloudTrashFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class TrashScreenController {

	public static final int CONFIRM_EMPTY_TRASH_ID = 1;

	private final OsmandApplication app;
	private final TrashUtils trashUtils;
	private final CloudTrashFragment fragment;

	public TrashScreenController(@NonNull OsmandApplication app,
	                             @NonNull CloudTrashFragment fragment) {
		this.app = app;
		this.fragment = fragment;
		this.trashUtils = new TrashUtils(app);
		trashUtils.setListener(fragment);
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		List<ScreenItem> screenItems = new ArrayList<>();

		List<TrashGroup> trashData = trashUtils.collectSortedTrashData();
		if (!Algorithms.isEmpty(trashData)) {
			screenItems.add(new ScreenItem(ALERT_CARD));
			screenItems.add(new ScreenItem(CARD_DIVIDER));
			for (TrashGroup trashGroup : trashData) {
				screenItems.add(new ScreenItem(HEADER, trashGroup));
				for (TrashItem trashItem : trashGroup.getTrashItems()) {
					screenItems.add(new ScreenItem(TRASH_ITEM, trashItem));
				}
				screenItems.add(new ScreenItem(DIVIDER));
			}
			// Replace last divider with card bottom shadow and extra space
			screenItems.remove(screenItems.size() - 1);
			screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
			screenItems.add(new ScreenItem(SPACE));
		} else {
			screenItems.add(new ScreenItem(EMPTY_TRASH_BANNER));
			screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
		}
		return screenItems;
	}

	public void askEmptyTrash() {
		FragmentActivity activity = fragment.getActivity();
		if (activity != null) {
			String dialogTitle = getString(R.string.delete_all_items);
			String dialogDescription = getString(R.string.are_you_sure_empty_trash_q);
			FragmentManager fm = activity.getSupportFragmentManager();
			showConfirmDeleteDialog(fm, fragment, dialogTitle, dialogDescription, CONFIRM_EMPTY_TRASH_ID);
		}
	}

	public void onEmptyTrashConfirmed() {
		trashUtils.emptyTrash();
	}

	public void onTrashItemClicked(@NonNull TrashItem trashItem) {
		app.showShortToastMessage("Show dialog for '" + trashItem.getName() + "'");
	}

	@NonNull
	private String getString(@StringRes int stringResId) {
		return app.getString(stringResId);
	}
}
