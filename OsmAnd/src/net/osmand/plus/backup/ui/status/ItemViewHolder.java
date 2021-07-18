package net.osmand.plus.backup.ui.status;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.ExportBackupTask.ItemProgressInfo;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

public class ItemViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final ImageView secondIcon;
	private final ProgressBar progressBar;
	private final View divider;

	public ItemViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		secondIcon = itemView.findViewById(R.id.second_icon);
		description = itemView.findViewById(R.id.description);
		progressBar = itemView.findViewById(R.id.progressBar);
		divider = itemView.findViewById(R.id.bottom_divider);
	}

	public void bindView(SettingsItem item, boolean lastBackupItem, boolean deleteItem) {
		setupItemView(item, deleteItem);
		AndroidUiHelper.updateVisibility(divider, !lastBackupItem);
	}

	protected void setupItemView(@NonNull SettingsItem item, boolean deleteItem) {
		OsmandApplication app = getApplication();
		String publicName = item.getPublicName(app);
		if (item instanceof FileSettingsItem) {
			FileSettingsItem settingsItem = (FileSettingsItem) item;
			if (settingsItem.getSubtype() == FileSubtype.VOICE) {
				publicName += " (" + app.getString(R.string.shared_string_recorded) + ")";
			} else if (settingsItem.getSubtype() == FileSubtype.TTS_VOICE) {
				publicName += " (" + app.getString(R.string.tts_title) + ")";
			}
		}
		title.setText(publicName);

		String filename = BackupHelper.getItemFileName(item);
		String summary = app.getString(R.string.last_backup);
		UploadedFileInfo info = app.getBackupHelper().getDbHelper().getUploadedFileInfo(item.getType().name(), filename);
		if (info != null) {
			String time = OsmAndFormatter.getFormattedPassedTime(app, info.getUploadTime(), app.getString(R.string.shared_string_never));
			description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, time));
		} else {
			description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, app.getString(R.string.shared_string_never)));
		}
		icon.setImageDrawable(getItemIcon(item));

		NetworkSettingsHelper settingsHelper = app.getNetworkSettingsHelper();
		if (settingsHelper.getExportTask() != null) {
			String fileName = BackupHelper.getItemFileName(item);
			ItemProgressInfo progressInfo = settingsHelper.getExportTask().getItemProgressInfo(item.getType().name(), fileName);
			if (progressInfo != null) {
				if (progressInfo.isFinished()) {
					secondIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_cloud_done));
					AndroidUiHelper.updateVisibility(secondIcon, true);
					AndroidUiHelper.updateVisibility(progressBar, false);
					AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.server_button), false);
					AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.local_version_button), false);
				} else {
					progressBar.setMax(progressInfo.getWork());
					progressBar.setProgress(progressInfo.getValue());

					AndroidUiHelper.updateVisibility(progressBar, true);
					AndroidUiHelper.updateVisibility(secondIcon, false);
				}
			} else {
				AndroidUiHelper.updateVisibility(progressBar, false);
				AndroidUiHelper.updateVisibility(secondIcon, false);
			}
		} else {
			AndroidUiHelper.updateVisibility(secondIcon, deleteItem);
			AndroidUiHelper.updateVisibility(progressBar, false);
			secondIcon.setImageDrawable(getContentIcon(deleteItem ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_cloud_done));
		}
		itemView.setTag(item);
	}

	@Nullable
	protected Drawable getItemIcon(@NonNull SettingsItem item) {
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

	@Nullable
	protected Drawable getContentIcon(@DrawableRes int icon) {
		OsmandApplication app = getApplication();
		return app.getUIUtilities().getIcon(icon, R.color.description_font_and_bottom_sheet_icons);
	}

	@NonNull
	protected OsmandApplication getApplication() {
		return (OsmandApplication) itemView.getContext().getApplicationContext();
	}
}