package net.osmand.plus.backup.ui.status;


import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.ui.ChangeItemActionsBottomSheet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.OsmAndFormatter;

public class ChangesItemViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final TextView description;
	private final AppCompatImageView icon;
	private final AppCompatImageView secondIcon;
	private final View selectableItem;

	public ChangesItemViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		secondIcon = itemView.findViewById(R.id.second_icon);
		description = itemView.findViewById(R.id.description);
		selectableItem = itemView.findViewById(R.id.selectable_list_item);
	}

	public void bindView(SettingsItem item, FragmentManager supportFragmentManager, boolean showBottomSheetOnClick, boolean showOverflowButton) {
		setupItemView(item, supportFragmentManager, showBottomSheetOnClick);
		AndroidUiHelper.updateVisibility(secondIcon, showOverflowButton);
	}

	public void bindView(SettingsItem item, FragmentManager supportFragmentManager, boolean showBottomSheetOnClick) {
		setupItemView(item, supportFragmentManager, showBottomSheetOnClick);
	}

	protected void setupItemView(@NonNull SettingsItem item, FragmentManager supportFragmentManager, boolean showBottomSheetOnClick) {
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

		String fileName = BackupHelper.getItemFileName(item);
		String summary = "";
		UploadedFileInfo info = app.getBackupHelper().getDbHelper().getUploadedFileInfo(item.getType().name(), fileName);
		if (info != null) {
			String time = OsmAndFormatter.getFormattedPassedTime(app, info.getUploadTime(), app.getString(R.string.shared_string_never));
			description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, time));
		} else {
			description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, app.getString(R.string.shared_string_never)));
		}
		icon.setImageDrawable(getItemIcon(item));
		secondIcon.setImageDrawable(getContentIcon(R.drawable.ic_overflow_menu_white));

		if (showBottomSheetOnClick) {
			selectableItem.setOnClickListener(view -> {
				FragmentManager fragmentManager = supportFragmentManager;
				ChangeItemActionsBottomSheet.showInstance(fragmentManager, item);
			});
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
		return app.getUIUtilities().getIcon(icon, R.color.icon_color_secondary_light);
	}

	@NonNull
	protected OsmandApplication getApplication() {
		return (OsmandApplication) itemView.getContext().getApplicationContext();
	}
}