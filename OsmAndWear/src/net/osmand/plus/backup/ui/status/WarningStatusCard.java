package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.backup.ui.status.BackupStatus.NO_INTERNET_CONNECTION;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class WarningStatusCard extends BaseCard {

	private final BackupHelper backupHelper;

	public WarningStatusCard(@NonNull FragmentActivity activity) {
		super(activity, false);
		backupHelper = app.getBackupHelper();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.cloud_warning_card;
	}

	@Override
	protected void updateContent() {
		PrepareBackupResult backup = backupHelper.getBackup();
		BackupStatus status = BackupStatus.getBackupStatus(app, backup);

		boolean simpleWarning = status == NO_INTERNET_CONNECTION;
		if (simpleWarning) {
			setupSimpleWarning(status);
		} else {
			setupFullWarning(backup, status);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.simple_warning), simpleWarning);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.full_warning), !simpleWarning);
	}

	private void setupFullWarning(@NonNull PrepareBackupResult backup, @NonNull BackupStatus status) {
		View container = view.findViewById(R.id.full_warning);

		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);
		if (status.warningTitleRes != -1) {
			title.setText(status.warningTitleRes);
			description.setText(status.warningDescriptionRes);
		} else {
			title.setText(R.string.subscribe_email_error);
			description.setText(new BackupError(backup.getError()).getLocalizedError(app));
		}
		if (status.warningIconRes != -1) {
			ImageView icon = container.findViewById(R.id.icon);
			if (status != BackupStatus.SUBSCRIPTION_EXPIRED) {
				icon.setImageDrawable(getContentIcon(status.warningIconRes));
			} else {
				icon.setImageDrawable(getIcon(status.warningIconRes));
			}
		}
		setupButton(status);
	}

	private void setupSimpleWarning(@NonNull BackupStatus status) {
		View container = view.findViewById(R.id.simple_warning);
		TextView title = container.findViewById(R.id.title);
		ImageView icon = container.findViewById(R.id.icon);

		title.setText(status.warningTitleRes);
		icon.setImageDrawable(getIcon(status.warningIconRes));
		container.findViewById(R.id.retry_button).setOnClickListener(v -> onRetryPressed());
	}

	private void setupButton(@NonNull BackupStatus status) {
		DialogButton actionButton = view.findViewById(R.id.action_button);
		if (status == BackupStatus.ERROR) {
			actionButton.setOnClickListener(v -> onSupportPressed());
		} else if (status == BackupStatus.SUBSCRIPTION_EXPIRED) {
			actionButton.setOnClickListener(v -> onSubscriptionExpired());
		}
		actionButton.setTitleId(status.actionTitleRes);
	}

	private void onSupportPressed() {
		sendEmail();
	}

	private void sendEmail() {
		PrepareBackupResult backup = backupHelper.getBackup();
		String screenName = app.getString(R.string.backup_and_restore);
		app.getFeedbackHelper().sendSupportEmail(screenName, backup.getError());
	}

	private void onSubscriptionExpired() {
		if (Version.isInAppPurchaseSupported()) {
			OsmAndProPlanFragment.showInstance(activity);
		} else {
			PromoCodeBottomSheet.showInstance(activity.getSupportFragmentManager());
		}
	}

	private void onRetryPressed() {
		if (!backupHelper.isBackupPreparing()) {
			backupHelper.prepareBackup();
		}
	}
}