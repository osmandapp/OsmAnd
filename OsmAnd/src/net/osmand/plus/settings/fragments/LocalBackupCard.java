package net.osmand.plus.settings.fragments;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;

import static net.osmand.plus.importfiles.ImportHelper.ImportType.SETTINGS;
import static net.osmand.plus.settings.fragments.BackupUploadCard.adjustIndicator;

public class LocalBackupCard extends BaseCard {

	private View backupToFile;
	private View restoreFromFile;
	private View localBackupDivider;

	private boolean buttonsVisible = true;

	public LocalBackupCard(@NonNull MapActivity mapActivity) {
		super(mapActivity, false);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.local_backup_card;
	}

	@Override
	protected void updateContent() {
		backupToFile = view.findViewById(R.id.backup_to_file);
		restoreFromFile = view.findViewById(R.id.restore_from_file);
		localBackupDivider = view.findViewById(R.id.local_backup_divider);

		View localBackup = view.findViewById(R.id.local_backup);
		TextView title = localBackup.findViewById(android.R.id.title);
		TextView description = localBackup.findViewById(android.R.id.summary);

		title.setText(R.string.local_backup);
		description.setText(R.string.local_backup_descr);
		setupSelectableBackground(localBackup);

		localBackup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonsVisible = !buttonsVisible;
				adjustIndicator(app, buttonsVisible, localBackup, nightMode);
				updateButtonsVisibility();
			}
		});
		adjustIndicator(app, buttonsVisible, localBackup, nightMode);
		setupBackupButton();
		setupRestoreButton();
		updateButtonsVisibility();
		AndroidUiHelper.updateVisibility(view.findViewById(android.R.id.icon), false);
	}

	private void setupBackupButton() {
		ImageView icon = backupToFile.findViewById(android.R.id.icon);
		TextView title = backupToFile.findViewById(android.R.id.title);

		title.setText(R.string.backup_into_file);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_read_from_file));
		setupSelectableBackground(backupToFile);

		backupToFile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					ApplicationMode mode = app.getSettings().getApplicationMode();
					FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
					ExportSettingsFragment.showInstance(fragmentManager, mode, true);
				}
			}
		});
	}

	private void setupRestoreButton() {
		ImageView icon = restoreFromFile.findViewById(android.R.id.icon);
		TextView title = restoreFromFile.findViewById(android.R.id.title);

		title.setText(R.string.restore_from_file);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_save_to_file));
		setupSelectableBackground(restoreFromFile);

		restoreFromFile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.getImportHelper().chooseFileToImport(SETTINGS, null);
				}
			}
		});
	}

	private void setupSelectableBackground(View view) {
		View selectableView = view.findViewById(R.id.selectable_list_item);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f);
		AndroidUtils.setBackground(selectableView, drawable);
	}

	private void updateButtonsVisibility() {
		AndroidUiHelper.updateVisibility(backupToFile, buttonsVisible);
		AndroidUiHelper.updateVisibility(restoreFromFile, buttonsVisible);
		AndroidUiHelper.updateVisibility(localBackupDivider, buttonsVisible);
	}
}