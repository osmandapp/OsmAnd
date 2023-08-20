package net.osmand.plus.backup.trash.controller;

import static net.osmand.plus.backup.trash.ScreenItemType.*;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.trash.ScreenItem;
import net.osmand.plus.backup.trash.TrashUtils;
import net.osmand.plus.backup.trash.data.TrashGroup;
import net.osmand.plus.backup.trash.data.TrashItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class TrashScreenController {

	public final OsmandApplication app;
	public final TrashUtils trashUtils;

	public TrashScreenController(@NonNull OsmandApplication app, @NonNull TrashUtils trashUtils) {
		this.app = app;
		this.trashUtils = trashUtils;
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
		app.showShortToastMessage("Show 'Empty trash' confirmation dialog");
	}

	public void onTrashItemClicked(@NonNull TrashItem trashItem) {
		app.showShortToastMessage("Show dialog for '" + trashItem.getName() + "'");
	}

}
