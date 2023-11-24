package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY_ACTIVE;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.ui.LoginDialogType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class IntroductionCard extends BaseCard {

	public static final int SYNC_BUTTON_INDEX = 0;
	public static final int SETTINGS_BUTTON_INDEX = 1;

	private final LoginDialogType dialogType;

	public IntroductionCard(@NonNull FragmentActivity activity, @NonNull LoginDialogType dialogType) {
		super(activity);
		this.dialogType = dialogType;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.cloud_introduction_card;
	}

	@Override
	protected void updateContent() {
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		if (dialogType == LoginDialogType.SIGN_IN) {
			title.setText(R.string.backup_welcome_back);
			description.setText(R.string.backup_welcome_back_descr);
			icon.setImageDrawable(getIcon(R.drawable.ic_action_cloud_smile_face_colored));

		} else if (dialogType == LoginDialogType.SIGN_UP) {
			title.setText(R.string.backup_do_not_have_any);
			description.setText(R.string.backup_dont_have_any_descr);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_cloud_neutral_face));
		}
		setupSyncButton();
		setupSettingsButton();
	}

	private void setupSyncButton() {
		DialogButton syncButton = view.findViewById(R.id.sync_button);
		syncButton.setOnClickListener(v -> notifyButtonPressed(SYNC_BUTTON_INDEX));
		AndroidUiHelper.updateVisibility(syncButton, shouldShowSyncButton());
	}

	private void setupSettingsButton() {
		boolean signIn = dialogType == LoginDialogType.SIGN_IN;
		int titleId = signIn ? R.string.choose_what_to_sync : R.string.set_up_backup;
		DialogButtonType buttonType = signIn ? SECONDARY : SECONDARY_ACTIVE;

		DialogButton settingsButton = view.findViewById(R.id.settings_button);
		settingsButton.setTitleId(titleId);
		settingsButton.setButtonType(buttonType);
		settingsButton.setOnClickListener(v -> notifyButtonPressed(SETTINGS_BUTTON_INDEX));
	}

	private boolean shouldShowSyncButton() {
		PrepareBackupResult backup = app.getBackupHelper().getBackup();
		BackupInfo info = backup.getBackupInfo();
		return info != null && (info.filteredFilesToDelete.size() > 0
				|| info.filteredFilesToDownload.size() > 0
				|| info.filteredFilesToUpload.size() > 0);
	}
}