package net.osmand.plus.backup.ui.status;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.ui.AuthorizeFragment.LoginDialogType;
import net.osmand.plus.backup.ui.BackupTypesFragment;
import net.osmand.plus.backup.ui.RestoreSettingsFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.util.Algorithms;

public class IntroductionViewHolder extends RecyclerView.ViewHolder {

	private final ImageView icon;
	private final TextView title;
	private final TextView description;
	private final View backupButton;
	private final View restoreButton;

	public IntroductionViewHolder(@NonNull View itemView) {
		super(itemView);
		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		backupButton = itemView.findViewById(R.id.backup_button);
		restoreButton = itemView.findViewById(R.id.restore_button);
	}

	public void bindView(@NonNull FragmentActivity activity, @NonNull BackupStatusFragment fragment,
						 @NonNull PrepareBackupResult backup, @NonNull LoginDialogType dialogType, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		UiUtilities iconsCache = app.getUIUtilities();

		if (dialogType == LoginDialogType.SIGN_IN) {
			title.setText(R.string.backup_welcome_back);
			description.setText(R.string.backup_welcome_back_descr);
			icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_cloud_smile_face_colored));

		} else if (dialogType == LoginDialogType.SIGN_UP) {
			title.setText(R.string.backup_do_not_have_any);
			description.setText(R.string.backup_dont_have_any_descr);
			icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_cloud_neutral_face));
		}

		setupBackupButton(activity, fragment, backup, nightMode);
		setupRestoreButton(activity, fragment, backup, nightMode);
	}

	private void setupRestoreButton(@NonNull FragmentActivity activity, @NonNull BackupStatusFragment fragment,
									@NonNull PrepareBackupResult backup, boolean nightMode) {
		if (!Algorithms.isEmpty(backup.getRemoteFiles())) {
			restoreButton.setOnClickListener(v -> {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					RestoreSettingsFragment.showInstance(activity.getSupportFragmentManager());
				}
				fragment.removeDialogType();
			});
			AndroidUiHelper.updateVisibility(restoreButton, true);
			UiUtilities.setupDialogButton(nightMode, restoreButton, DialogButtonType.PRIMARY, R.string.backup_restore_now);

			if (backupButton.getVisibility() == View.VISIBLE) {
				ViewGroup.LayoutParams params = restoreButton.getLayoutParams();
				if (params instanceof MarginLayoutParams) {
					int margin = activity.getResources().getDimensionPixelSize(R.dimen.content_padding);
					AndroidUtils.setMargins(((MarginLayoutParams) params), 0, 0, 0, margin);
				}
			}
		} else {
			AndroidUiHelper.updateVisibility(restoreButton, false);
		}
	}

	private void setupBackupButton(@NonNull FragmentActivity activity, @NonNull BackupStatusFragment fragment,
								   @NonNull PrepareBackupResult backup, boolean nightMode) {
		if (!Algorithms.isEmpty(backup.getLocalFiles())) {
			backupButton.setOnClickListener(v -> {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					BackupTypesFragment.showInstance(activity.getSupportFragmentManager());
				}
				fragment.removeDialogType();
			});
			AndroidUiHelper.updateVisibility(backupButton, true);
			UiUtilities.setupDialogButton(nightMode, backupButton, DialogButtonType.SECONDARY, R.string.backup_setup);
			AndroidUtils.setBackground(activity, backupButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
		} else {
			AndroidUiHelper.updateVisibility(backupButton, false);
		}
	}
}