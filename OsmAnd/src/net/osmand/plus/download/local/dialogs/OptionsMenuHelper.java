package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.ItemType.MAP_SOURCES;
import static net.osmand.plus.download.local.ItemType.NAUTICAL_MAPS;
import static net.osmand.plus.download.local.ItemType.PROFILES;
import static net.osmand.plus.download.local.ItemType.REGULAR_MAPS;
import static net.osmand.plus.download.local.ItemType.RENDERING_STYLES;
import static net.osmand.plus.download.local.ItemType.TERRAIN_MAPS;
import static net.osmand.plus.download.local.ItemType.WIKI_AND_TRAVEL_MAPS;
import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.CLEAR_TILES_OPERATION;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.ItemType;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.ui.SearchDialogFragment;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptionsMenuHelper {

	public static final int SEARCH_ID = 0;
	public static final int SORT_ID = 1;
	public static final int ACTIONS_ID = 2;

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final DownloadActivity activity;
	private final Map<String, IndexItem> itemsToUpdate = new HashMap<>();

	private boolean nightMode;

	public OptionsMenuHelper(@NonNull DownloadActivity activity) {
		this.activity = activity;
		app = activity.getMyApplication();
		uiUtilities = app.getUIUtilities();
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
	}

	public void reloadItemsToUpdate() {
		itemsToUpdate.clear();
		for (IndexItem item : app.getDownloadThread().getIndexes().getItemsToUpdate()) {
			itemsToUpdate.put(item.getTargetFileName(), item);
		}
	}

	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull ItemType type, @NonNull LocalItemsFragment fragment) {
		menu.clear();

		boolean selectionMode = fragment.isSelectionMode();
		int colorResId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

		if (!selectionMode) {
			MenuItem searchItem = menu.add(0, SEARCH_ID, 0, R.string.shared_string_search);
			searchItem.setIcon(getIcon(R.drawable.ic_action_search_dark, colorResId));
			searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			searchItem.setOnMenuItemClickListener(item -> {
				activity.showDialog(activity, SearchDialogFragment.createInstance(""));
				return true;
			});
		}

		if (type == REGULAR_MAPS) {
			MenuItem sortItem = menu.add(0, SORT_ID, 0, R.string.shared_string_sort);
			sortItem.setIcon(getIcon(R.drawable.ic_action_sort_by_name_ascending, colorResId));
			sortItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			sortItem.setOnMenuItemClickListener(item -> {
				FragmentManager manager = activity.getSupportFragmentManager();
				SortMapsBottomSheet.showInstance(manager, fragment);
				return true;
			});
		}

		MenuItem actionsItem = menu.add(0, ACTIONS_ID, 0, R.string.shared_string_sort);
		actionsItem.setIcon(getIcon(R.drawable.ic_overflow_menu_white, colorResId));
		actionsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		actionsItem.setOnMenuItemClickListener(item -> {
			showAdditionalActions(activity.findViewById(ACTIONS_ID), type, fragment);
			return true;
		});
	}

	private void showAdditionalActions(@NonNull View view, @NonNull ItemType type, @NonNull LocalItemsFragment fragment) {
		List<PopUpMenuItem> items = new ArrayList<>();

		boolean selectionMode = fragment.isSelectionMode();
		if (selectionMode) {
			addOperationItem(items, DELETE_OPERATION, fragment);

			if (Algorithms.equalsToAny(type, REGULAR_MAPS, NAUTICAL_MAPS, TERRAIN_MAPS, WIKI_AND_TRAVEL_MAPS)) {
				addOperationItem(items, BACKUP_OPERATION, fragment);
				addOperationItem(items, RESTORE_OPERATION, fragment);
			}
		} else {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_select)
					.setIcon(getContentIcon(R.drawable.ic_action_select_all))
					.setOnClickListener(v -> fragment.setSelectionMode(true))
					.create());

			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_import)
					.setIcon(getContentIcon(R.drawable.ic_action_import))
					.setOnClickListener(v -> {
						Intent intent = ImportHelper.getImportFileIntent();
						AndroidUtils.startActivityForResultIfSafe(fragment, intent, IMPORT_FILE_REQUEST);
					})
					.showTopDivider(true)
					.create());
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void addOperationItem(@NonNull List<PopUpMenuItem> items, @NonNull OperationType type, @NonNull LocalItemsFragment fragment) {
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(type.getTitleId())
				.setIcon(getContentIcon(type.getIconId()))
				.setOnClickListener(v -> showConfirmation(type, fragment))
				.create());
	}

	private void showConfirmation(@NonNull OperationType type, @NonNull LocalItemsFragment fragment) {
		String action = app.getString(type.getTitleId());
		ItemsSelectionHelper<LocalItem> helper = fragment.getSelectionHelper();

		Set<LocalItem> selectedItems = helper.getSelectedItems();
		if (!Algorithms.isEmpty(selectedItems)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
			builder.setMessage(app.getString(R.string.local_index_action_do, action.toLowerCase(), String.valueOf(helper.getSelectedItemsSize())));
			builder.setPositiveButton(action, (dialog, which) -> fragment.performOperation(type, selectedItems.toArray(new LocalItem[0])));
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			builder.show();
		} else {
			showNoItemsForActionsToast(action);
		}
	}

	private void showNoItemsForActionsToast(@NonNull String action) {
		String message = app.getString(R.string.local_index_no_items_to_do, action.toLowerCase());
		app.showShortToastMessage(Algorithms.capitalizeFirstLetter(message));
	}

	public void onItemOptionsSelected(@NonNull LocalItem localItem, @NonNull View view, @NonNull LocalItemsFragment fragment) {
		ItemType type = localItem.getType();
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.info_button)
				.setIcon(getContentIcon(R.drawable.ic_action_info_outlined))
				.setOnClickListener(v -> fragment.onItemSelected(localItem))
				.create());

		if (type == REGULAR_MAPS) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_update)
					.setIcon(getContentIcon(R.drawable.ic_action_update))
					.setOnClickListener(v -> updateItem(localItem))
					.create());

//			items.add(new PopUpMenuItem.Builder(app)
//					.setTitleId(R.string.shared_string_show_on_map)
//					.setIcon(getContentIcon(R.drawable.ic_show_on_map))
//					.setOnClickListener(v -> showOnMap(localItem))
//					.create());
		} else if (type == RENDERING_STYLES) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_export)
					.setIcon(getContentIcon(R.drawable.ic_action_upload))
					.setOnClickListener(v -> exportItem(localItem))
					.create());
		} else if (type == MAP_SOURCES) {
			Object object = localItem.getAttachedObject();
			if ((object instanceof TileSourceTemplate) || ((object instanceof SQLiteTileSource)
					&& ((SQLiteTileSource) object).couldBeDownloadedFromInternet())) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.shared_string_edit)
						.setIcon(getContentIcon(R.drawable.ic_action_edit_outlined))
						.setOnClickListener(v -> OsmandRasterMapsPlugin.defineNewEditLayer(activity, fragment, localItem.getFile().getName()))
						.create());
			}
			if ((object instanceof ITileSource) && ((ITileSource) object).couldBeDownloadedFromInternet()) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.clear_tile_data)
						.setIcon(getContentIcon(R.drawable.ic_action_clear_all))
						.setOnClickListener(v -> {
							clearTiles(localItem, fragment);
						})
						.create());
			}
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_export)
					.setIcon(getContentIcon(R.drawable.ic_action_upload))
					.setOnClickListener(v -> exportItem(localItem))
					.create());
		}

		if (type != PROFILES) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_remove)
					.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
					.setOnClickListener(v -> {
						FragmentManager manager = activity.getSupportFragmentManager();
						DeleteConfirmationBottomSheet.showInstance(manager, fragment, localItem);
					})
					.showTopDivider(true)
					.create());
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void showOnMap(@NonNull LocalItem localItem) {

	}

	private void exportItem(@NonNull LocalItem localItem) {

	}

	private void clearTiles(@NonNull LocalItem localItem, @NonNull LocalItemsFragment fragment) {
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			LocalOperationTask task = new LocalOperationTask(app, CLEAR_TILES_OPERATION, fragment);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, localItem);
		});
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setMessage(app.getString(R.string.clear_confirmation_msg, localItem.getName()));
		builder.show();
	}

	private void updateItem(@NonNull LocalItem localItem) {
		File file = localItem.getFile();
		IndexItem indexItem = itemsToUpdate.get(file.getName());
		if (indexItem != null) {
			activity.startDownload(indexItem);
		} else {
			String text = app.getString(R.string.map_is_up_to_date, localItem.getName());
			Snackbar snackbar = Snackbar.make(activity.getLayout(), text, Snackbar.LENGTH_LONG);
			UiUtilities.setupSnackbar(snackbar, nightMode, 5);
			snackbar.show();
		}
	}

	@Nullable
	private Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return uiUtilities.getIcon(id, colorId);
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int id) {
		return uiUtilities.getThemedIcon(id);
	}
}
