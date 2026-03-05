package net.osmand.plus.download.local.dialogs.menu;

import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.MultipleLocalItem;
import net.osmand.plus.download.local.dialogs.DeleteConfirmationDialogController;
import net.osmand.plus.download.local.dialogs.LocalBaseFragment;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderMenuProvider extends AbstractBaseMenuProvider {

	private MultipleLocalItem item;

	public FolderMenuProvider(@NonNull DownloadActivity activity, @NonNull BaseOsmAndFragment fragment) {
		super(activity, fragment);
	}

	public void setItem(@NonNull MultipleLocalItem item) {
		this.item = item;
	}

	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
		MenuItem menuItem;
		LocalItemType type = item.getType();
		if (type.isUpdateSupported()) {
			menuItem = menu.add(0, R.string.shared_string_update, Menu.NONE, R.string.shared_string_update);
			menuItem.setIcon(getIcon(R.drawable.ic_action_update, iconColorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(selectedItem -> {
				updateItems(item.getLocalItems());
				return true;
			});
		}

		boolean backuped = item.isBackuped(app);
		if (type.isBackupSupported() || backuped) {
			LocalItem[] localItems = item.getLocalItems().toArray(new LocalItem[0]);
			addOperationItem(menu, backuped ? RESTORE_OPERATION : BACKUP_OPERATION, localItems);
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

	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
		return false;
	}

	private void updateItems(@NonNull List<LocalItem> localItems) {
		if (fragment instanceof LocalBaseFragment localFragment) {
			List<IndexItem> indexItems = new ArrayList<>();
			for (LocalItem localItem : localItems) {
				File file = localItem.getFile();
				IndexItem indexItem = localFragment.getItemsToUpdate().get(file.getName());
				if (indexItem != null) {
					indexItems.add(indexItem);
				}
			}
			if (!indexItems.isEmpty()) {
				IndexItem[] indexes = indexItems.toArray(new IndexItem[0]);
				activity.startDownload(indexes);
			} else {
				String text = app.getString(R.string.everything_up_to_date);
				Snackbar snackbar = Snackbar.make(activity.getLayout(), text, Snackbar.LENGTH_LONG);
				UiUtilities.setupSnackbar(snackbar, nightMode, 5);
				snackbar.show();
			}
		}
	}
}
