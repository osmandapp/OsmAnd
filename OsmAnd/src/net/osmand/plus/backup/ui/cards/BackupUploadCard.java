package net.osmand.plus.backup.ui.cards;

import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.BackupHelper.OnDeleteFilesListener;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.fragments.MainSettingsFragment;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

import static net.osmand.plus.backup.ui.cards.LocalBackupCard.adjustIndicator;

public class BackupUploadCard extends MapBaseCard {

	private final BackupInfo info;
	private final BackupExportListener exportListener;
	private final OnDeleteFilesListener deleteFilesListener;

	private View actionButton;
	private View progressContainer;
	private TextView progressTitle;
	private ProgressBar progressBar;
	private ViewGroup itemsContainer;

	private boolean buttonsVisible = true;

	public BackupUploadCard(@NonNull MapActivity mapActivity,
							@NonNull BackupInfo info,
							@Nullable BackupExportListener exportListener,
							@Nullable OnDeleteFilesListener deleteFilesListener) {
		super(mapActivity, false);
		this.info = info;
		this.exportListener = exportListener;
		this.deleteFilesListener = deleteFilesListener;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.backup_upload_card;
	}

	@Override
	protected void updateContent() {
		setupHeader();
		setupUploadItems();
		setupItemsToDelete();
		setupConflictingItems();
		setupActionButton();
		AndroidUiHelper.updateVisibility(actionButton, buttonsVisible);
		AndroidUiHelper.updateVisibility(itemsContainer, buttonsVisible);
	}

	private void setupHeader() {
		progressContainer = view.findViewById(R.id.upload_container);
		progressTitle = progressContainer.findViewById(R.id.title);
		progressBar = progressContainer.findViewById(R.id.progress_bar);

		progressTitle.setText(R.string.shared_string_items);
		ImageView icon = progressContainer.findViewById(R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_cloud_upload));

		progressContainer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonsVisible = !buttonsVisible;
				adjustIndicator(app, buttonsVisible, view, nightMode);
				AndroidUiHelper.updateVisibility(actionButton, buttonsVisible);
				AndroidUiHelper.updateVisibility(itemsContainer, buttonsVisible);
			}
		});
		adjustIndicator(app, buttonsVisible, view, nightMode);

		setupSelectableBackground(progressContainer);
		AndroidUiHelper.updateVisibility(progressBar, app.getNetworkSettingsHelper().isBackupExporting());
	}

	private void setupUploadItems() {
		itemsContainer = view.findViewById(R.id.items_container);
		itemsContainer.removeAllViews();

		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		for (SettingsItem item : info.getItemsToUpload()) {
			View itemView = themedInflater.inflate(R.layout.backup_upload_item, itemsContainer, false);
			setupItemView(item, itemView);

			itemsContainer.addView(itemView);
			AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.second_icon), false);
		}
	}

	private void setupItemsToDelete() {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		for (SettingsItem item : info.getItemsToDelete()) {
			View itemView = themedInflater.inflate(R.layout.backup_upload_item, itemsContainer, false);
			setupItemView(item, itemView);

			ImageView icon = itemView.findViewById(R.id.second_icon);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_delete_dark));
			AndroidUiHelper.updateVisibility(icon, true);

			itemsContainer.addView(itemView);
		}
	}

	private void setupConflictingItems() {
		itemsContainer = view.findViewById(R.id.items_container);

		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		for (Pair<LocalFile, RemoteFile> pair : info.filesToMerge) {
			SettingsItem item = pair.first.item;
			if (pair.first.item == null || pair.second.item == null) {
				continue;
			}
			View itemView = themedInflater.inflate(R.layout.backup_upload_item, itemsContainer, false);
			setupItemView(item, itemView);

			View localVersionButton = itemView.findViewById(R.id.local_version_button);
			localVersionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getNetworkSettingsHelper().exportSettings(exportListener, pair.first.item);
				}
			});
			View serverButton = itemView.findViewById(R.id.server_button);
			serverButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					SettingsItem settingsItem = pair.second.item;
					settingsItem.setShouldReplace(true);
					app.getNetworkSettingsHelper().importSettings(Collections.singletonList(settingsItem), "", 1, true, null);
				}
			});
			AndroidUiHelper.updateVisibility(serverButton, true);
			AndroidUiHelper.updateVisibility(localVersionButton, true);
			AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.second_icon), true);
			UiUtilities.setupDialogButton(nightMode, localVersionButton, DialogButtonType.SECONDARY, R.string.upload_local_version);
			UiUtilities.setupDialogButton(nightMode, serverButton, DialogButtonType.SECONDARY, R.string.download_server_version);
			AndroidUtils.setBackground(app, localVersionButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
			AndroidUtils.setBackground(app, serverButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);

			itemsContainer.addView(itemView);
		}
	}

	private void setupItemView(SettingsItem item, View itemView) {
		TextView title = itemView.findViewById(R.id.title);
		title.setText(item.getPublicName(app));

		String filename = BackupHelper.getItemFileName(item);
		TextView description = itemView.findViewById(R.id.description);
		String summary = app.getString(R.string.last_backup);
		UploadedFileInfo info = app.getBackupHelper().getDbHelper().getUploadedFileInfo(item.getType().name(), filename);
		if (info != null) {
			String time = MainSettingsFragment.getLastBackupTimeDescription(app, info.getUploadTime(), app.getString(R.string.shared_string_never));
			description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, time));
		} else {
			description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, app.getString(R.string.shared_string_never)));
		}
		ImageView icon = itemView.findViewById(R.id.icon);
		icon.setImageDrawable(getItemIcon(item));
		itemView.setTag(item);
	}

	private void setupActionButton() {
		actionButton = view.findViewById(R.id.action_button);
		if (app.getNetworkSettingsHelper().isBackupExporting()) {
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
			AndroidUtils.setBackground(app, actionButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
			actionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getNetworkSettingsHelper().cancelExport();
				}
			});
		} else {
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.backup_now);
			AndroidUtils.setBackground(app, actionButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
			actionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					List<SettingsItem> items = info.getItemsToUpload();
					if (!items.isEmpty() || !Algorithms.isEmpty(info.filesToDelete)) {
						app.getNetworkSettingsHelper().exportSettings(items, info.filesToDelete, exportListener);
					}
				}
			});
		}
	}

	public void setupProgress(int itemsCount) {
		progressBar.setMax(itemsCount);
	}

	public void onBackupExportItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		SettingsItem item = getSettingsItem(type, fileName);
		if (item != null) {
			View itemView = view.findViewWithTag(item);
			if (itemView != null) {
				ProgressBar progress = itemView.findViewById(R.id.progressBar);
				progress.setProgress(value);
				AndroidUiHelper.updateVisibility(progress, true);
			}
		}
	}

	public void onBackupExportItemFinished(@NonNull String type, @NonNull String fileName) {
		SettingsItem item = getSettingsItem(type, fileName);
		if (item != null) {
			View itemView = view.findViewWithTag(item);
			if (itemView != null) {
				ProgressBar progress = itemView.findViewById(R.id.progressBar);
				AndroidUiHelper.updateVisibility(progress, false);

				ImageView icon = itemView.findViewById(R.id.second_icon);
				icon.setImageDrawable(getContentIcon(R.drawable.ic_action_cloud_done));
				AndroidUiHelper.updateVisibility(icon, true);
			}
		}
	}

	public void onBackupExportItemStarted(@NonNull String type, @NonNull String fileName, int max) {
		SettingsItem item = getSettingsItem(type, fileName);
		if (item != null) {
			View itemView = view.findViewWithTag(item);
			if (itemView != null) {
				ProgressBar progress = itemView.findViewById(R.id.progressBar);
				progress.setMax(max);
				progress.setProgress(0);
				AndroidUiHelper.updateVisibility(progress, true);
			}
		}
	}

	private SettingsItem getSettingsItem(@NonNull String type, @NonNull String fileName) {
		for (LocalFile file : info.filesToUpload) {
			if (file.item != null && file.item.getType().name().equals(type) && BackupHelper.getItemFileName(file.item).equals(fileName)) {
				return file.item;
			}
		}
		for (RemoteFile file : info.filesToDelete) {
			if (file.item != null && file.item.getType().name().equals(type) && BackupHelper.getItemFileName(file.item).equals(fileName)) {
				return file.item;
			}
		}
		for (Pair<LocalFile, RemoteFile> pair : info.filesToMerge) {
			SettingsItem item = pair.first.item;
			if (item != null && item.getType().name().equals(type) && BackupHelper.getItemFileName(item).equals(fileName)) {
				return item;
			}
		}
		return null;
	}

	public void updateProgress(int value) {
		String uploading = app.getString(R.string.local_openstreetmap_uploading);
		progressTitle.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, uploading, String.valueOf(value)));

		progressBar.setProgress(value);
		AndroidUiHelper.updateVisibility(progressBar, app.getNetworkSettingsHelper().isBackupExporting());
	}

	private void setupSelectableBackground(View view) {
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f);
		AndroidUtils.setBackground(view, drawable);
	}

	private Drawable getItemIcon(SettingsItem item) {
		if (item instanceof ProfileSettingsItem) {
			ProfileSettingsItem profileItem = (ProfileSettingsItem) item;
			ApplicationMode mode = profileItem.getAppMode();
			return getContentIcon(mode.getIconRes());
		}
		ExportSettingsType type = ExportSettingsType.getExportSettingsTypeForItem(item);
		if (type != null) {
			return getContentIcon(type.getIconRes());
		}
		return null;
	}
}