package net.osmand.plus.backup.ui.cards;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.fragments.MainSettingsFragment;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

import static net.osmand.plus.backup.ui.cards.LocalBackupCard.adjustIndicator;

public class BackupUploadCard extends MapBaseCard {

	private final BackupStatus status;
	private final PrepareBackupResult backup;
	private final BackupExportListener exportListener;
	private final ImportListener importListener;

	private TextView headerTitle;
	private ProgressBar progressBar;
	private ViewGroup itemsContainer;

	private boolean buttonsVisible = false;

	public BackupUploadCard(@NonNull MapActivity mapActivity, @NonNull PrepareBackupResult backup,
							@Nullable BackupExportListener exportListener,
							@Nullable ImportListener importListener) {
		super(mapActivity, false);
		this.backup = backup;
		this.status = getBackupStatus();
		this.exportListener = exportListener;
		this.importListener = importListener;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.backup_upload_card;
	}

	@Override
	protected void updateContent() {
		itemsContainer = view.findViewById(R.id.items_container);
		itemsContainer.removeAllViews();

		setupHeader();
		setupWarningContainer();
		BackupInfo info = backup.getBackupInfo();
		if (info != null) {
			setupProgress(info);
			setupUploadItems(info);
			setupItemsToDelete(info);
			setupConflictingItems(info);
		}
		setupActionButton();
		AndroidUiHelper.updateVisibility(itemsContainer, buttonsVisible);
	}

	private void setupHeader() {
		View container = view.findViewById(R.id.header_container);
		headerTitle = container.findViewById(R.id.title);
		progressBar = container.findViewById(R.id.progress_bar);

		ImageView icon = view.findViewById(R.id.icon);
		TextView description = view.findViewById(R.id.description);

		if (app.getNetworkSettingsHelper().isBackupExporting()) {
			headerTitle.setText(R.string.uploading);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_cloud_upload));
			AndroidUiHelper.updateVisibility(description, false);
		} else {
			description.setText(status.statusTitleRes);
			icon.setImageDrawable(getContentIcon(status.statusIconRes));

			String backupTime = MainSettingsFragment.getLastBackupTimeDescription(app);
			if (Algorithms.isEmpty(backupTime)) {
				headerTitle.setText(R.string.shared_string_never);
			} else {
				headerTitle.setText(backupTime);
			}
			AndroidUiHelper.updateVisibility(description, true);
		}
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonsVisible = !buttonsVisible;
				adjustIndicator(app, buttonsVisible, view, nightMode);
				AndroidUiHelper.updateVisibility(itemsContainer, buttonsVisible);
			}
		});
		adjustIndicator(app, buttonsVisible, view, nightMode);
		setupSelectableBackground(container);
		AndroidUiHelper.updateVisibility(progressBar, app.getNetworkSettingsHelper().isBackupExporting());
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.explicit_indicator), status != BackupStatus.BACKUP_COMPLETE);
	}

	private void setupWarningContainer() {
		ViewGroup warningView = view.findViewById(R.id.warning_card);
		String error = backup.getError();
		if (status.warningTitleRes != -1 || !Algorithms.isEmpty(error)) {
			AndroidUiHelper.updateVisibility(warningView, true);

			TextView title = warningView.findViewById(R.id.title);
			TextView description = warningView.findViewById(R.id.description);

			if (status.warningTitleRes != -1) {
				title.setText(status.warningTitleRes);
				description.setText(status.warningDescriptionRes);
			} else {
				title.setText(R.string.subscribe_email_error);
				description.setText(error);
			}
			ImageView icon = warningView.findViewById(R.id.icon);
			icon.setImageDrawable(getContentIcon(status.warningIconRes));
			setupWarningRoundedBg(warningView.findViewById(R.id.warning_container));
		} else {
			AndroidUiHelper.updateVisibility(warningView, false);
		}
	}

	public void setupWarningRoundedBg(View selectableView) {
		int color = AndroidUtils.getColorFromAttr(selectableView.getContext(), R.attr.activity_background_color);
		int selectedColor = UiUtilities.getColorWithAlpha(getActiveColor(), 0.3f);

		Drawable bgDrawable = getPaintedIcon(R.drawable.rectangle_rounded, color);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			Drawable selectable = getPaintedIcon(R.drawable.ripple_rectangle_rounded, selectedColor);
			Drawable[] layers = {bgDrawable, selectable};
			AndroidUtils.setBackground(selectableView, new LayerDrawable(layers));
		} else {
			AndroidUtils.setBackground(selectableView, bgDrawable);
		}
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) selectableView.getLayoutParams();
		params.setMargins(params.leftMargin, AndroidUtils.dpToPx(app, 6), params.rightMargin, params.bottomMargin);
	}

	private void setupUploadItems(BackupInfo info) {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		for (SettingsItem item : info.getItemsToUpload(app)) {
			View itemView = themedInflater.inflate(R.layout.backup_upload_item, itemsContainer, false);
			setupItemView(item, itemView);

			itemsContainer.addView(itemView);
			AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.second_icon), false);
		}
	}

	private void setupItemsToDelete(BackupInfo info) {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		for (SettingsItem item : info.getItemsToDelete(app)) {
			View itemView = themedInflater.inflate(R.layout.backup_upload_item, itemsContainer, false);
			setupItemView(item, itemView);

			ImageView icon = itemView.findViewById(R.id.second_icon);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_delete_dark));
			AndroidUiHelper.updateVisibility(icon, true);

			itemsContainer.addView(itemView);
		}
	}

	private void setupConflictingItems(BackupInfo info) {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		for (Pair<LocalFile, RemoteFile> pair : info.getFilteredFilesToMerge(app)) {
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
					app.getNetworkSettingsHelper().importSettings(Collections.singletonList(settingsItem), "", 1, true, importListener);
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
		View actionButton = view.findViewById(R.id.action_button);
		if (app.getNetworkSettingsHelper().isBackupExporting()) {
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
			actionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getNetworkSettingsHelper().cancelExport();
				}
			});
			AndroidUiHelper.updateVisibility(actionButton, true);
		} else if (status == BackupStatus.MAKE_BACKUP || status == BackupStatus.CONFLICTS) {
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.backup_now);
			actionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					BackupInfo info = backup.getBackupInfo();
					List<SettingsItem> items = info.getItemsToUpload(app);
					if (!items.isEmpty() || !Algorithms.isEmpty(info.getFilteredFilesToDelete(app))) {
						app.getNetworkSettingsHelper().exportSettings(items, info.getFilteredFilesToDelete(app), exportListener);
					}
				}
			});
			BackupInfo info = backup.getBackupInfo();
			AndroidUiHelper.updateVisibility(actionButton, info != null
					&& (!info.getFilteredFilesToUpload(app).isEmpty() || !info.getFilteredFilesToDelete(app).isEmpty()));
		} else if (status == BackupStatus.NO_INTERNET_CONNECTION || status == BackupStatus.ERROR) {
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.retry);
			actionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getBackupHelper().prepareBackup();
				}
			});
			AndroidUiHelper.updateVisibility(actionButton, true);
		} else if (status == BackupStatus.BACKUP_COMPLETE) {
			AndroidUiHelper.updateVisibility(actionButton, false);
		}
		AndroidUtils.setBackground(app, actionButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
	}

	public void setupProgress(@NonNull BackupInfo info) {
		int itemsCount = info.getFilteredFilesToUpload(app).size() + info.getFilteredFilesToDelete(app).size();
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
				AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.server_button), false);
				AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.local_version_button), false);
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
				AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.second_icon), false);
			}
		}
	}

	private SettingsItem getSettingsItem(@NonNull String type, @NonNull String fileName) {
		BackupInfo info = backup.getBackupInfo();
		if (info != null) {
			for (LocalFile file : info.getFilteredFilesToUpload(app)) {
				if (file.item != null && BackupHelper.applyItem(file.item, type, fileName)) {
					return file.item;
				}
			}
			for (RemoteFile file : info.getFilteredFilesToDelete(app)) {
				if (file.item != null && BackupHelper.applyItem(file.item, type, fileName)) {
					return file.item;
				}
			}
			for (Pair<LocalFile, RemoteFile> pair : info.getFilteredFilesToMerge(app)) {
				SettingsItem item = pair.first.item;
				if (item != null && BackupHelper.applyItem(item, type, fileName)) {
					return item;
				}
			}
		}
		return null;
	}

	public void updateProgress(int value) {
		String uploading = app.getString(R.string.local_openstreetmap_uploading);
		headerTitle.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, uploading, String.valueOf(value)));

		progressBar.setProgress(value);
		AndroidUiHelper.updateVisibility(progressBar, app.getNetworkSettingsHelper().isBackupExporting());
	}

	private BackupStatus getBackupStatus() {
		BackupInfo info = backup.getBackupInfo();
		if (info != null) {
			if (!Algorithms.isEmpty(info.getFilteredFilesToMerge(app))) {
				return BackupStatus.CONFLICTS;
			} else if (!Algorithms.isEmpty(info.getFilteredFilesToUpload(app))
					|| !Algorithms.isEmpty(info.getFilteredFilesToDelete(app))) {
				return BackupStatus.MAKE_BACKUP;
			}
		} else if (!app.getSettings().isInternetConnectionAvailable()) {
			return BackupStatus.NO_INTERNET_CONNECTION;
		} else if (backup.getError() != null) {
			return BackupStatus.ERROR;
		}
		return BackupStatus.BACKUP_COMPLETE;
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