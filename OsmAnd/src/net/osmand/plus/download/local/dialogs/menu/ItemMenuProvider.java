package net.osmand.plus.download.local.dialogs.menu;

import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.CLEAR_TILES_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;
import static net.osmand.plus.settings.fragments.ExportSettingsFragment.SELECTED_TYPES;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalSizeController;
import net.osmand.plus.download.local.dialogs.DeleteConfirmationDialogController;
import net.osmand.plus.download.local.dialogs.LocalBaseFragment;
import net.osmand.plus.download.local.dialogs.LocalItemFragment;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemMenuProvider extends AbstractBaseMenuProvider {

	private BaseLocalItem item;
	private boolean showInfoItem = true;

	public ItemMenuProvider(@NonNull DownloadActivity activity, @NonNull BaseOsmAndFragment fragment) {
		super(activity, fragment);
	}

	public void setItem(@NonNull BaseLocalItem item) {
		this.item = item;
	}

	public void setShowInfoItem(boolean showInfoItem) {
		this.showInfoItem = showInfoItem;
	}

	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		MenuItem menuItem;
		if (showInfoItem) {
			menuItem = menu.add(0, 0, Menu.NONE, R.string.info_button);
			menuItem.setIcon(getIcon(R.drawable.ic_action_info_outlined, iconColorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(i -> {
				FragmentManager manager = fragment.getFragmentManager();
				if (manager != null) {
					LocalItemFragment.showInstance(manager, item, fragment);
				}
				return true;
			});
		}
		LocalItemType type = item.getType();
		if (item instanceof LocalItem localItem) {
			if (type.isUpdateSupported()) {
				menuItem = menu.add(0, R.string.shared_string_update, Menu.NONE, R.string.shared_string_update);
				menuItem.setIcon(getIcon(R.drawable.ic_action_update, iconColorId));
				menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menuItem.setOnMenuItemClickListener(item -> {
					updateItem(localItem);
					return true;
				});
			}
			boolean backuped = localItem.isBackuped(app);
			if (type.isBackupSupported() || backuped) {
				addOperationItem(menu, backuped ? RESTORE_OPERATION : BACKUP_OPERATION, localItem);
			}
			if (type == TILES_DATA) {
				menuItem = menu.add(0, R.string.calculate_size, Menu.NONE, R.string.calculate_size);
				menuItem.setIcon(getIcon(R.drawable.ic_action_file_info, iconColorId));
				menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menuItem.setOnMenuItemClickListener(item -> {
					LocalSizeController.calculateFullSize(app, localItem);
					return true;
				});
				Object object = localItem.getAttachedObject();
				if ((object instanceof TileSourceTemplate) || ((object instanceof SQLiteTileSource)
						&& ((SQLiteTileSource) object).couldBeDownloadedFromInternet())) {
					menuItem = menu.add(0, R.string.shared_string_edit, Menu.NONE, R.string.shared_string_edit);
					menuItem.setIcon(getIcon(R.drawable.ic_action_edit_outlined, iconColorId));
					menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					menuItem.setOnMenuItemClickListener(item -> {
						OsmandRasterMapsPlugin.defineNewEditLayer(activity, fragment, localItem.getFile().getName());
						return true;
					});
				}
				if ((object instanceof ITileSource) && ((ITileSource) object).couldBeDownloadedFromInternet()) {
					menuItem = menu.add(0, R.string.clear_tile_data, Menu.NONE, R.string.clear_tile_data);
					menuItem.setIcon(getIcon(R.drawable.ic_action_clear_all, iconColorId));
					menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					menuItem.setOnMenuItemClickListener(item -> {
						clearTiles(localItem);
						return true;
					});
				}
			}
			if (type.isRenamingSupported()) {
				menuItem = menu.add(0, R.string.shared_string_rename, Menu.NONE, R.string.shared_string_rename);
				menuItem.setIcon(getIcon(R.drawable.ic_action_edit_dark, iconColorId));
				menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menuItem.setOnMenuItemClickListener(item -> {
					FileUtils.renameFile(activity, localItem.getFile(), fragment, false);
					return true;
				});
			}
			ExportType exportType = ExportType.findBy(type);
			if (!localItem.isHidden(app) && exportType != null) {
				menuItem = menu.add(0, R.string.shared_string_export, Menu.NONE, R.string.shared_string_export);
				menuItem.setIcon(getIcon(R.drawable.ic_action_upload, iconColorId));
				menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menuItem.setOnMenuItemClickListener(item -> {
					exportItem(localItem, exportType);
					return true;
				});
			}
		}
		if (type.isDeletionSupported()) {
			menuItem = menu.add(1, R.string.shared_string_remove, Menu.NONE, R.string.shared_string_remove);
			menuItem.setIcon(getIcon(R.drawable.ic_action_delete_outlined, iconColorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(i -> {
				FragmentManager manager = fragment.getFragmentManager();
				if (manager != null && fragment instanceof LocalBaseFragment localFragment) {
					DeleteConfirmationDialogController.showDialog(app, manager, item, localFragment);
				}
				return true;
			});
		}
	}

	private void exportItem(@NonNull LocalItem localItem, @NonNull ExportType settingsType) {
		List<File> selectedFiles = new ArrayList<>();
		selectedFiles.add(localItem.getFile());

		HashMap<ExportType, List<?>> selectedTypes = new HashMap<>();
		selectedTypes.put(settingsType, selectedFiles);

		Bundle bundle = new Bundle();
		bundle.putSerializable(SELECTED_TYPES, selectedTypes);
		MapActivity.launchMapActivityMoveToTop(activity, null, null, bundle);
	}

	private void clearTiles(@NonNull LocalItem localItem) {
		Context context = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			if (fragment instanceof LocalBaseFragment localFragment) {
				LocalOperationTask task = new LocalOperationTask(app, CLEAR_TILES_OPERATION, localFragment);
				OsmAndTaskManager.executeTask(task, localItem);
			}
		});
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setMessage(app.getString(R.string.clear_confirmation_msg, localItem.getName(context)));
		builder.show();
	}

	private void updateItem(@NonNull LocalItem localItem) {
		if (fragment instanceof LocalBaseFragment localFragment) {
			File file = localItem.getFile();
			IndexItem indexItem = localFragment.getItemsToUpdate().get(file.getName());
			if (indexItem != null) {
				activity.startDownload(indexItem);
			} else {
				Context context = UiUtilities.getThemedContext(activity, nightMode);
				String text = app.getString(R.string.map_is_up_to_date, localItem.getName(context));
				Snackbar snackbar = Snackbar.make(activity.getLayout(), text, Snackbar.LENGTH_LONG);
				UiUtilities.setupSnackbar(snackbar, nightMode, 5);
				snackbar.show();
			}
		}
	}

	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
		return false;
	}
}