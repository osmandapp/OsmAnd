package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.backup.NetworkSettingsHelper.BACKUP_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.RESTORE_ITEMS_KEY;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.ExportBackupTask;
import net.osmand.plus.backup.ExportBackupTask.ItemProgressInfo;
import net.osmand.plus.backup.ImportBackupTask;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.ui.ChangeItemActionsBottomSheet;
import net.osmand.plus.backup.ui.ChangesTabFragment;
import net.osmand.plus.backup.ui.ChangesTabFragment.CloudChangeItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import org.apache.commons.logging.Log;

public class ItemViewHolder extends RecyclerView.ViewHolder {

	private static final Log log = PlatformUtil.getLog(ItemViewHolder.class);

	private final OsmandApplication app;
	private final NetworkSettingsHelper settingsHelper;

	private final TextView title;
	private final TextView description;
	private final ProgressBar progressBar;
	private final AppCompatImageView icon;
	private final AppCompatImageView secondIcon;
	private final View divider;
	private final View bottomShadow;
	private final View selectableView;
	private final boolean nightMode;

	public ItemViewHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		settingsHelper = app.getNetworkSettingsHelper();
		this.nightMode = nightMode;

		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		progressBar = itemView.findViewById(R.id.progressBar);
		secondIcon = itemView.findViewById(R.id.second_icon);
		description = itemView.findViewById(R.id.description);
		divider = itemView.findViewById(R.id.bottom_divider);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);
		selectableView = itemView.findViewById(R.id.selectable_list_item);
	}

	public void bindView(@NonNull CloudChangeItem item, @Nullable ChangesTabFragment fragment, boolean lastItem) {
		title.setText(item.title);
		description.setText(item.description);

		if (item.iconId != -1) {
			icon.setImageDrawable(getContentIcon(item.iconId));
		}
		secondIcon.setImageDrawable(getContentIcon(item.synced ? R.drawable.ic_action_cloud_done : R.drawable.ic_overflow_menu_white, item.synced));
		setupProgress(item);
		OnClickListener listener = fragment != null ? view -> {
			FragmentManager manager = fragment.getFragmentManager();
			if (manager != null) {
				ChangeItemActionsBottomSheet.showInstance(manager, item, fragment);
			}
		} : null;
		itemView.setOnClickListener(listener);
		boolean enabled = listener != null && !settingsHelper.isSyncing(item.fileName);
		itemView.setEnabled(enabled);
		if (enabled) {
			setupSelectableBackground();
		}
		AndroidUiHelper.updateVisibility(divider, !lastItem);
		AndroidUiHelper.updateVisibility(bottomShadow, lastItem);
	}

	private void setupProgress(@NonNull CloudChangeItem item) {
		ItemProgressInfo progressInfo = getItemProgressInfo(item);
		if (progressInfo != null) {
			progressBar.setMax(progressInfo.getWork());
			progressBar.setProgress(progressInfo.getValue());
		}
		boolean syncing = settingsHelper.isSyncing(item.fileName);
		AndroidUiHelper.updateVisibility(secondIcon, item.synced || !syncing);
		AndroidUiHelper.updateVisibility(progressBar, !item.synced && syncing);
	}

	private ItemProgressInfo getItemProgressInfo(@NonNull CloudChangeItem item) {
		ImportBackupTask importTask = settingsHelper.getImportTask(item.fileName);
		if (importTask == null) {
			importTask = settingsHelper.getImportTask(RESTORE_ITEMS_KEY);
		}
		if (importTask != null) {
			return importTask.getItemProgressInfo(item.settingsItem.getType().name(), item.fileName);
		}
		ExportBackupTask exportTask = settingsHelper.getExportTask(item.fileName);
		if (exportTask == null) {
			exportTask = settingsHelper.getExportTask(BACKUP_ITEMS_KEY);
		}
		if (exportTask != null) {
			return exportTask.getItemProgressInfo(item.settingsItem.getType().name(), item.fileName);
		}
		return null;
	}

	private void setupSelectableBackground() {
		int color = ColorUtilities.getActiveColor(app, nightMode);
		AndroidUtils.setBackground(selectableView, UiUtilities.getColoredSelectableDrawable(app, color, 0.3f));
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int icon) {
		return getContentIcon(icon, false);
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int icon, boolean active) {
		return app.getUIUtilities().getIcon(icon, active ? ColorUtilities.getActiveIconColorId(nightMode) : ColorUtilities.getDefaultIconColorId(nightMode));
	}
}