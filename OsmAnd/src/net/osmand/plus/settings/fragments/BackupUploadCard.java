package net.osmand.plus.settings.fragments;

import android.graphics.drawable.Drawable;
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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

public class BackupUploadCard extends BaseCard {

	private final BackupInfo info;
	private final BackupExportListener listener;

	private View actionButton;
	private View progressContainer;
	private TextView progressTitle;
	private ProgressBar progressBar;
	private ViewGroup itemsContainer;

	private boolean buttonsVisible = true;

	public BackupUploadCard(@NonNull MapActivity mapActivity, @NonNull BackupInfo info, @Nullable BackupExportListener listener) {
		super(mapActivity, false);
		this.info = info;
		this.listener = listener;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.backup_upload_card;
	}

	@Override
	protected void updateContent() {
		setupHeader();
		setupUploadItems();
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
		for (SettingsItem item : info.itemsToUpload) {
			View itemView = themedInflater.inflate(R.layout.backup_upload_item, itemsContainer, false);
			TextView title = itemView.findViewById(R.id.title);
			title.setText(item.getName());

			if (item.getFileName() != null) {
				TextView description = itemView.findViewById(R.id.description);
				String summary = app.getString(R.string.last_backup);
				UploadedFileInfo info = app.getBackupHelper().getDbHelper().getUploadedFileInfo(item.getType().name(), item.getFileName());
				if (info != null) {
					String time = MainSettingsFragment.getBackupTime(app, info.getUploadTime());
					description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, time));
				} else {
					description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, app.getString(R.string.shared_string_never)));
				}
			}
			itemsContainer.addView(itemView);
			AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.warningIcon), false);
		}
	}

	private void setupActionButton() {
		actionButton = view.findViewById(R.id.action_button);
		if (app.getNetworkSettingsHelper().isBackupExporting()) {
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
			actionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getNetworkSettingsHelper().cancelExport();
				}
			});
		} else {
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.backup_now);
			actionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getNetworkSettingsHelper().exportSettings(listener, info.itemsToUpload);
				}
			});
		}
	}

	protected void updateProgress(int value) {
		String uploading = app.getString(R.string.local_openstreetmap_uploading);
		progressTitle.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, uploading, String.valueOf(value)));

		progressBar.setProgress(value);
		AndroidUiHelper.updateVisibility(progressBar, app.getNetworkSettingsHelper().isBackupExporting());
	}

	private void setupSelectableBackground(View view) {
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f);
		AndroidUtils.setBackground(view, drawable);
	}

	public static void adjustIndicator(OsmandApplication app, boolean isExpanded, View row, boolean nightMode) {
		ImageView indicator = row.findViewById(R.id.explicit_indicator);
		if (!isExpanded) {
			indicator.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_down, !nightMode));
			indicator.setContentDescription(row.getContext().getString(R.string.access_collapsed_list));
		} else {
			indicator.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_up, !nightMode));
			indicator.setContentDescription(row.getContext().getString(R.string.access_expanded_list));
		}
	}
}