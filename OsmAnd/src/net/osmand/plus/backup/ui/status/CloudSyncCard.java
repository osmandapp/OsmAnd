package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.backup.NetworkSettingsHelper.BACKUP_ITEMS_KEY;
import static net.osmand.plus.base.OsmandBaseExpandableListAdapter.adjustIndicator;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.ExportBackupTask;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.fragments.MainSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class CloudSyncCard extends BaseCard {

	public static final int LOCAL_CHANGES_BUTTON_INDEX = 0;
	public static final int CLOUD_CHANGES_BUTTON_INDEX = 1;
	public static final int CONFLICTS_BUTTON_INDEX = 2;
	public static final int SYNC_BUTTON_INDEX = 3;

	private final BackupHelper backupHelper;
	private final NetworkSettingsHelper settingsHelper;

	private boolean actionsVisible;

	public CloudSyncCard(@NonNull FragmentActivity activity) {
		super(activity, false);
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.cloud_sync_card;
	}

	@Override
	protected void updateContent() {
		PrepareBackupResult backup = backupHelper.getBackup();
		setupHeader(backup);
		setupButtons(backup);
		updateButtonsVisibility();
	}

	private void setupHeader(@NonNull PrepareBackupResult backup) {
		BackupStatus status = BackupStatus.getBackupStatus(app, backup);

		View headerContainer = view.findViewById(R.id.header_container);
		TextView title = headerContainer.findViewById(R.id.title);
		ImageView icon = headerContainer.findViewById(R.id.icon);
		TextView description = headerContainer.findViewById(R.id.description);
		ProgressBar progressBar = headerContainer.findViewById(R.id.progress_bar);

		ExportBackupTask exportTask = settingsHelper.getExportTask(BACKUP_ITEMS_KEY);
		if (exportTask != null) {
			title.setText(R.string.uploading);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_cloud_upload));

			int progress = exportTask.getGeneralProgress();
			int maxProgress = (int) exportTask.getMaxProgress();
			int percentage = maxProgress != 0 ? ProgressHelper.normalizeProgressPercent(progress * 100 / maxProgress) : 0;

			String uploading = app.getString(R.string.local_openstreetmap_uploading);
			title.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, uploading, percentage + "%"));

			progressBar.setMax(maxProgress);
			progressBar.setProgress(progress);

			AndroidUiHelper.updateVisibility(description, false);
			AndroidUiHelper.updateVisibility(progressBar, true);
		} else {
			description.setText(status.statusTitleRes);
			icon.setImageDrawable(getContentIcon(status.statusIconRes));

			String backupTime = MainSettingsFragment.getLastBackupTimeDescription(app);
			if (Algorithms.isEmpty(backupTime)) {
				title.setText(R.string.shared_string_never);
			} else {
				title.setText(backupTime);
			}
			AndroidUiHelper.updateVisibility(description, true);
			AndroidUiHelper.updateVisibility(progressBar, false);
		}
		headerContainer.setOnClickListener(v -> {
			actionsVisible = !actionsVisible;
			updateButtonsVisibility();
			adjustIndicator(app, headerContainer, actionsVisible, nightMode);
		});
		setupSelectableBackground(headerContainer);
	}

	private void setupButtons(@NonNull PrepareBackupResult backup) {
		setupLocalChangesButton(backup);
		setupCloudChangesButton(backup);
		setupConflictsButton(backup);
		setupSyncButton();
	}

	private void setupSyncButton() {
		String title = app.getString(R.string.sync_now);
		Drawable icon = getActiveIcon(R.drawable.ic_action_update);
		setupButton(view.findViewById(R.id.sync_button), title, icon, null, SYNC_BUTTON_INDEX);
	}

	private void setupLocalChangesButton(@NonNull PrepareBackupResult backup) {
		View buttonView = view.findViewById(R.id.local_changes_button);
		BackupInfo info = backup.getBackupInfo();
		if (info != null) {
			String title = app.getString(R.string.local_changes);
			Drawable icon = getActiveIcon(R.drawable.ic_action_phone_filled);
			int count = info.filteredFilesToUpload.size() + info.filteredFilesToDelete.size();
			setupButton(buttonView, title, icon, String.valueOf(count), LOCAL_CHANGES_BUTTON_INDEX);
		}
		AndroidUiHelper.updateVisibility(buttonView, info != null);
	}

	private void setupCloudChangesButton(@NonNull PrepareBackupResult backup) {
		View buttonView = view.findViewById(R.id.cloud_changes_button);
		BackupInfo info = backup.getBackupInfo();
		if (info != null) {
			String title = app.getString(R.string.cloud_changes);
			Drawable icon = getActiveIcon(R.drawable.ic_action_cloud);
			int count = BackupHelper.getItemsMapForRestore(info, backup.getSettingsItems()).size();
			setupButton(buttonView, title, icon, String.valueOf(count), CLOUD_CHANGES_BUTTON_INDEX);
		}
		AndroidUiHelper.updateVisibility(buttonView, info != null);
	}

	private void setupConflictsButton(@NonNull PrepareBackupResult backup) {
		View buttonView = view.findViewById(R.id.conflicts_button);
		BackupInfo info = backup.getBackupInfo();
		if (info != null) {
			String title = app.getString(R.string.backup_conflicts);
			Drawable icon = getActiveIcon(R.drawable.ic_small_warning);
			int count = backup.getBackupInfo().filteredFilesToMerge.size();
			setupButton(buttonView, title, icon, String.valueOf(count), CONFLICTS_BUTTON_INDEX);
		}
		AndroidUiHelper.updateVisibility(buttonView, info != null);
	}

	private void setupButton(@NonNull View button, @Nullable String text, @Nullable Drawable icon, @Nullable String count, int index) {
		TextView textView = button.findViewById(android.R.id.title);
		ImageView imageView = button.findViewById(android.R.id.icon);

		textView.setText(text);
		imageView.setImageDrawable(icon);
		button.setOnClickListener(v -> notifyButtonPressed(index));
		setupSelectableBackground(button);

		TextView countView = button.findViewById(R.id.count);
		if (countView != null) {
			countView.setText(count);
		}
	}

	private void updateButtonsVisibility() {
		adjustIndicator(app, view.findViewById(R.id.header_container), actionsVisible, nightMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.local_changes_button), actionsVisible);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.cloud_changes_button), actionsVisible);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.conflicts_button), actionsVisible);
	}

	private void setupSelectableBackground(@NonNull View view) {
		Context ctx = view.getContext();
		int color = ColorUtilities.getActiveColor(ctx, nightMode);
		AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(ctx, color, 0.3f));
	}
}

