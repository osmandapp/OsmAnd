package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.ui.BackupUiUtils.getLastBackupTimeDescription;
import static net.osmand.plus.backup.ui.status.BackupStatus.BACKUP_COMPLETE;
import static net.osmand.plus.backup.ui.status.BackupStatus.CONFLICTS;
import static net.osmand.plus.backup.ui.status.BackupStatus.MAKE_BACKUP;
import static net.osmand.plus.base.OsmandBaseExpandableListAdapter.adjustIndicator;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.SyncBackupTask;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.backup.ui.BackupCloudFragment;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class CloudSyncCard extends BaseCard implements OnBackupSyncListener, OnPrepareBackupListener {

	public static final int INVALID_ID = -1;
	public static final int LOCAL_CHANGES_BUTTON_INDEX = 0;
	public static final int CLOUD_CHANGES_BUTTON_INDEX = 1;
	public static final int CONFLICTS_BUTTON_INDEX = 2;
	public static final int SYNC_BUTTON_INDEX = 3;
	public static final int STOP_BUTTON_INDEX = 4;

	private final BackupHelper backupHelper;
	private final NetworkSettingsHelper settingsHelper;
	private final BackupCloudFragment fragment;

	private View header;
	private ProgressBar progressBar;
	private View changesContainer;
	private View localChangesButton;
	private View cloudChangesButton;
	private View conflictsButton;
	private View syncButton;

	public CloudSyncCard(@NonNull FragmentActivity activity, @NonNull BackupCloudFragment fragment) {
		super(activity, false);
		this.fragment = fragment;
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.cloud_sync_card;
	}

	@Override
	protected void updateContent() {
		header = view.findViewById(R.id.header_container);
		progressBar = view.findViewById(R.id.progress_bar);
		changesContainer = view.findViewById(R.id.changes_container);
		localChangesButton = view.findViewById(R.id.local_changes_button);
		cloudChangesButton = view.findViewById(R.id.cloud_changes_button);
		conflictsButton = view.findViewById(R.id.conflicts_button);
		syncButton = view.findViewById(R.id.sync_button);

		PrepareBackupResult backup = backupHelper.getBackup();

		setupHeader();
		setupLocalChangesButton(backup);
		setupCloudChangesButton(backup);
		setupConflictsButton(backup);
		setupSyncButton(backup);

		updateButtonsVisibility();
	}

	private void setupHeader() {
		TextView title = header.findViewById(R.id.title);
		ImageView icon = header.findViewById(R.id.icon);
		TextView description = header.findViewById(R.id.description);
		description.setText(getLastBackupTimeDescription(app, app.getString(R.string.shared_string_never)));

		SyncBackupTask syncTask = settingsHelper.getSyncTask(SYNC_ITEMS_KEY);
		if (syncTask != null) {
			icon.setImageDrawable(getIcon(R.drawable.ic_action_update_colored));

			int progress = syncTask.getGeneralProgress();
			int maxProgress = syncTask.getMaxProgress();
			int percentage = maxProgress != 0 ? ProgressHelper.normalizeProgressPercent(progress * 100 / maxProgress) : 0;

			title.setText(app.getString(R.string.cloud_sync_progress, percentage + "%"));

			progressBar.setMax(maxProgress);
			progressBar.setProgress(progress);
		} else {
			PrepareBackupResult backup = backupHelper.getBackup();
			BackupStatus status = BackupStatus.getBackupStatus(app, backup);
			title.setText(status.statusTitleRes);
			icon.setImageDrawable(getIcon(status.statusIconRes));
		}
		header.setOnClickListener(v -> {
			fragment.toggleActionsVisibility();
			updateButtonsVisibility();
		});
		setupSelectableBackground(header);
		AndroidUiHelper.updateVisibility(progressBar, syncTask != null);
		AndroidUiHelper.updateVisibility(description, syncTask == null);
		AndroidUiHelper.updateVisibility(header.findViewById(R.id.bottom_divider), false);
	}

	private void setupSyncButton(@NonNull PrepareBackupResult backup) {
		boolean syncing = settingsHelper.isBackupSyncing();
		boolean preparing = backupHelper.isBackupPreparing();
		BackupStatus status = BackupStatus.getBackupStatus(app, backup);

		if (preparing) {
			String title = app.getString(R.string.checking_progress);
			syncButton.setEnabled(true);
			setupButton(syncButton, title, null, null);
		} else if (syncing) {
			String title = app.getString(R.string.shared_string_control_stop);
			Drawable icon = getTwoStateIcon(R.drawable.ic_action_rec_stop);

			syncButton.setEnabled(true);
			setupButton(syncButton, title, icon, v -> notifyButtonPressed(STOP_BUTTON_INDEX));
		} else {
			String title = app.getString(R.string.sync_now);
			Drawable icon = getTwoStateIcon(R.drawable.ic_action_update);

			setupButton(syncButton, title, icon, v -> notifyButtonPressed(SYNC_BUTTON_INDEX));
			syncButton.setEnabled(status == MAKE_BACKUP || status == CONFLICTS || status == BACKUP_COMPLETE);
		}

		ImageView imageView = syncButton.findViewById(android.R.id.icon);
		ProgressBar progressBar = syncButton.findViewById(R.id.progress_bar_small);

		AndroidUiHelper.updateVisibility(imageView, !preparing);
		AndroidUiHelper.updateVisibility(progressBar, preparing);
	}

	private void setupLocalChangesButton(@NonNull PrepareBackupResult backup) {
		BackupInfo info = backup.getBackupInfo();
		int itemsSize = info != null ? info.filteredFilesToUpload.size() + info.filteredFilesToDelete.size() : -1;
		String count = itemsSize >= 0 ? String.valueOf(itemsSize) : null;
		String title = app.getString(R.string.local_changes);
		Drawable icon = itemsSize > 0 ? getActiveIcon(R.drawable.ic_action_phone_filled) : getContentIcon(R.drawable.ic_action_phone_filled);

		OnClickListener listener = v -> notifyButtonPressed(LOCAL_CHANGES_BUTTON_INDEX);

		setupButton(localChangesButton, title, icon, listener);
		TextView countView = localChangesButton.findViewById(R.id.count);
		countView.setText(count);
	}

	private void setupCloudChangesButton(@NonNull PrepareBackupResult backup) {
		BackupInfo info = backup.getBackupInfo();
		int itemsSize = info != null
				? BackupUtils.getItemsMapForRestore(info, backup.getSettingsItems()).size() + info.filteredLocalFilesToDelete.size()
				: -1;
		String count = itemsSize >= 0 ? String.valueOf(itemsSize) : null;
		String title = app.getString(R.string.cloud_changes);
		Drawable icon = itemsSize > 0 ? getActiveIcon(R.drawable.ic_action_cloud) : getContentIcon(R.drawable.ic_action_cloud);

		OnClickListener listener = v -> notifyButtonPressed(CLOUD_CHANGES_BUTTON_INDEX);

		setupButton(cloudChangesButton, title, icon, listener);
		TextView countView = cloudChangesButton.findViewById(R.id.count);
		countView.setText(count);
	}

	private void setupConflictsButton(@NonNull PrepareBackupResult backup) {
		BackupInfo info = backup.getBackupInfo();
		int itemsSize = info != null ? backup.getBackupInfo().filteredFilesToMerge.size() : -1;
		String count = itemsSize >= 0 ? String.valueOf(itemsSize) : null;
		String title = app.getString(R.string.backup_conflicts);
		Drawable icon = itemsSize > 0 ? getActiveIcon(R.drawable.ic_action_alert) : getContentIcon(R.drawable.ic_action_alert);

		OnClickListener listener = v -> notifyButtonPressed(CONFLICTS_BUTTON_INDEX);

		setupButton(conflictsButton, title, icon, listener);
		TextView countView = conflictsButton.findViewById(R.id.count);
		countView.setText(count);
	}

	private void setupButton(@NonNull View button, @Nullable String text, @Nullable Drawable icon, @Nullable OnClickListener listener) {
		TextView textView = button.findViewById(android.R.id.title);
		ImageView imageView = button.findViewById(android.R.id.icon);

		textView.setText(text);
		imageView.setImageDrawable(icon);
		button.setOnClickListener(listener);
		setupSelectableBackground(button);
	}

	private Drawable getTwoStateIcon(@DrawableRes int iconId) {
		Drawable enabled = UiUtilities.createTintedDrawable(app, iconId, ColorUtilities.getActiveColor(app, nightMode));
		Drawable disabled = UiUtilities.createTintedDrawable(app, iconId, ColorUtilities.getDefaultIconColor(app, nightMode));
		return AndroidUtils.createEnabledStateListDrawable(disabled, enabled);
	}

	private void updateButtonsVisibility() {
		boolean visible = fragment.isChangesVisible();
		adjustIndicator(app, header, visible, nightMode);
		AndroidUiHelper.updateVisibility(changesContainer, visible);
	}

	private void setupSelectableBackground(@NonNull View view) {
		Context ctx = view.getContext();
		int color = ColorUtilities.getActiveColor(ctx, nightMode);
		AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(ctx, color, 0.3f));
	}

	@Override
	public void onBackupPreparing() {
		update();
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		update();
	}

	@Override
	public void onBackupSyncStarted() {
		app.runInUIThread(this::update);
	}

	@Override
	public void onBackupProgressUpdate(int progress) {
		app.runInUIThread(this::setupHeader);
	}

	@Override
	public void onBackupSyncFinished(@Nullable String error) {
		app.runInUIThread(this::update);
	}
}