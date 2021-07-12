package net.osmand.plus.backup.ui.status;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ExportSettingsFragment;

import static net.osmand.plus.activities.OsmandBaseExpandableListAdapter.adjustIndicator;
import static net.osmand.plus.importfiles.ImportHelper.ImportType.SETTINGS;

public class LocalBackupViewHolder extends RecyclerView.ViewHolder {

	private final View localBackup;
	private final View backupToFile;
	private final View restoreFromFile;

	private boolean buttonsVisible = true;

	public LocalBackupViewHolder(@NonNull View itemView) {
		super(itemView);
		localBackup = itemView.findViewById(R.id.local_backup);
		backupToFile = itemView.findViewById(R.id.backup_to_file);
		restoreFromFile = itemView.findViewById(R.id.restore_from_file);
	}

	public void bindView(@NonNull MapActivity mapActivity, boolean nightMode) {
		localBackup.setOnClickListener(v -> {
			buttonsVisible = !buttonsVisible;
			updateButtonsVisibility(nightMode);
		});

		updateButtonsVisibility(nightMode);
		setupSelectableBackground(localBackup);
		setupBackupButton(mapActivity, nightMode);
		setupRestoreButton(mapActivity, nightMode);
	}

	private void updateButtonsVisibility(boolean nightMode) {
		adjustIndicator(getApplication(), localBackup, buttonsVisible, nightMode);
		AndroidUiHelper.updateVisibility(backupToFile, buttonsVisible);
		AndroidUiHelper.updateVisibility(restoreFromFile, buttonsVisible);
	}

	private void setupBackupButton(@NonNull MapActivity mapActivity, boolean nightMode) {
		ImageView icon = backupToFile.findViewById(android.R.id.icon);
		TextView title = backupToFile.findViewById(android.R.id.title);

		title.setText(R.string.backup_into_file);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_read_from_file, nightMode));

		backupToFile.setOnClickListener(v -> {
			if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
				ApplicationMode mode = getApplication().getSettings().getApplicationMode();
				ExportSettingsFragment.showInstance(mapActivity.getSupportFragmentManager(), mode, true);
			}
		});
		setupSelectableBackground(backupToFile);
		AndroidUiHelper.updateVisibility(backupToFile.findViewById(R.id.divider), true);
	}

	private void setupRestoreButton(@NonNull MapActivity mapActivity, boolean nightMode) {
		ImageView icon = restoreFromFile.findViewById(android.R.id.icon);
		TextView title = restoreFromFile.findViewById(android.R.id.title);

		title.setText(R.string.restore_from_file);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_save_to_file, nightMode));

		restoreFromFile.setOnClickListener(v -> {
			if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
				mapActivity.getImportHelper().chooseFileToImport(SETTINGS, null);
			}
		});
		setupSelectableBackground(restoreFromFile);
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.active_color_basic);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(view.getContext(), color, 0.3f);
		AndroidUtils.setBackground(view.findViewById(R.id.selectable_list_item), drawable);
	}

	@NonNull
	private OsmandApplication getApplication() {
		return (OsmandApplication) itemView.getContext().getApplicationContext();
	}

	@ColorRes
	private int getActiveColorId(boolean nightMode) {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@Nullable
	private Drawable getActiveIcon(@DrawableRes int icon, boolean nightMode) {
		OsmandApplication app = getApplication();
		return app.getUIUtilities().getIcon(icon, getActiveColorId(nightMode));
	}
}