package net.osmand.plus.download.local.dialogs.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.MultipleLocalItem;
import net.osmand.plus.download.local.dialogs.LocalItemFragment;
import net.osmand.plus.download.local.dialogs.LocalItemsFragment;
import net.osmand.plus.download.local.dialogs.MemoryInfo;
import net.osmand.plus.download.local.dialogs.MemoryInfo.MemoryItem;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalItemsController extends BaseDialogController {

	public static final String PROCESS_ID = "local_items_dialog_controller";

	private final LocalItemsCollector collector;
	private final ItemsSelectionHelper<BaseLocalItem> selectionHelper;

	private boolean selectionMode;
	private String currentFolderId;
	private LocalItemsCollection lastCollection;

	public LocalItemsController(@NonNull OsmandApplication app) {
		super(app);
		this.collector = new LocalItemsCollector(app);
		this.selectionHelper = new ItemsSelectionHelper<>();
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void openFolder(@NonNull MultipleLocalItem folder) {
		this.currentFolderId = folder.getId();
		updateDialogContent();
	}

	public boolean handleBackPress() {
		if (selectionMode) {
			setSelectionMode(false);
			return true;
		} else if (currentFolderId != null) {
			currentFolderId = null;
			updateDialogContent();
			return true;
		}
		return false;
	}

	public boolean isRootFolder() {
		return lastCollection != null && lastCollection.isRootFolder();
	}

	@Nullable
	public CharSequence getToolbarTitle(@NonNull LocalGroup group) {
		if (selectionMode) {
			return getString(R.string.shared_string_select);
		} else if (lastCollection != null && lastCollection.currentFolder != null) {
			return lastCollection.currentFolder.getName();
		} else {
			return group.getName(app);
		}
	}

	public boolean isSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		selectionHelper.clearSelectedItems();
		updateDialogContent();
	}

	@NonNull
	public ItemsSelectionHelper<BaseLocalItem> getSelectionHelper() {
		return selectionHelper;
	}

	public boolean isItemSelected(@NonNull BaseLocalItem item) {
		return selectionHelper.isItemSelected(item);
	}

	public void onItemClick(@NonNull BaseLocalItem item, @NonNull FragmentActivity activity) {
		if (selectionMode) {
			boolean selected = !isItemSelected(item);
			selectionHelper.onItemsSelected(Collections.singleton(item), selected);
			updateDialogContent();
		} else if (item instanceof MultipleLocalItem folder) {
			openFolder(folder);
		} else {
			LocalItemFragment.showInstance(activity.getSupportFragmentManager(), item, null);
		}
	}

	public void updateDisplayItems(@Nullable LocalGroup group) {
		if (group == null) return;
		lastCollection = collector.collect(group, currentFolderId);
		if (lastCollection.isRootFolder()) {
			currentFolderId = null;
		}
	}

	@NonNull
	public List<Object> getDisplayItems(@Nullable LocalGroup group, @Nullable LocalCategory category) {
		updateDisplayItems(group);
		List<Object> displayList = new ArrayList<>(lastCollection.items);
		if (!selectionMode && isRootFolder()) {
			addMemoryInfo(displayList, group, category);
		}
		return displayList;
	}

	@Nullable
	public List<BaseLocalItem> getCurrentFolderItems() {
		if (lastCollection != null && lastCollection.currentFolder != null) {
			return lastCollection.currentFolder.getItems();
		}
		return null;
	}

	private void addMemoryInfo(@NonNull List<Object> items, @Nullable LocalGroup group,
	                           @Nullable LocalCategory category) {
		List<MemoryItem> memoryItems = calculateMemoryItems(group, category);
		if (memoryItems != null) {
			MemoryInfo memoryInfo = new MemoryInfo();
			memoryInfo.setItems(memoryItems);
			items.add(0, memoryInfo);
		}
	}

	@Nullable
	private List<MemoryItem> calculateMemoryItems(@Nullable LocalGroup group,
	                                              @Nullable LocalCategory category) {
		if (group == null || category == null) return null;

		List<MemoryItem> memoryItems = new ArrayList<>();
		boolean nightMode = isNightMode();

		if (!Algorithms.isEmpty(group.getItems())) {
			long size = group.getSize();
			String title = group.getName(app);
			String sizeDescription = group.getSizeDescription(app);
			String text = getString(R.string.ltr_or_rtl_combine_via_dash, title, sizeDescription);
			memoryItems.add(new MemoryItem(text, size, ColorUtilities.getActiveColor(app, nightMode)));
		}

		String title = category.getName(app);
		int color = ColorUtilities.getColor(app, category.getType().getColorId());
		String text = getString(R.string.ltr_or_rtl_combine_via_dash, title, AndroidUtils.formatSize(app, category.getSize()));

		long remainingSize = category.getSize() - group.getSize();
		memoryItems.add(new MemoryItem(text, remainingSize, color));

		return memoryItems;
	}

	private void updateDialogContent() {
		dialogManager.askRefreshDialogCompletely(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull LocalItemType type,
	                              @Nullable Fragment target) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();

		LocalItemsController controller = new LocalItemsController(app);
		dialogManager.register(PROCESS_ID, controller);

		LocalItemsFragment.showInstance(activity.getSupportFragmentManager(), type, target);
	}

	@Nullable
	public static LocalItemsController getExistedInstance(@NonNull OsmandApplication app) {
		return (LocalItemsController) app.getDialogManager().findController(PROCESS_ID);
	}
}