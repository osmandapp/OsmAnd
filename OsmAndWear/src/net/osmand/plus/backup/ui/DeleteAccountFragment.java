package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.ui.DeleteAccountFragment.DeletionStatus.FINISHED;
import static net.osmand.plus.backup.ui.DeleteAccountFragment.DeletionStatus.NOT_STARTED;
import static net.osmand.plus.backup.ui.DeleteAccountFragment.DeletionStatus.RUNNING;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.TERTIARY_HARMFUL;

import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteAccountListener;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.purchase.PurchasesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class DeleteAccountFragment extends BaseOsmAndFragment implements OnDeleteAccountListener {

	public static final String TAG = DeleteAccountFragment.class.getSimpleName();

	private static final Log log = PlatformUtil.getLog(DeleteAccountFragment.class);

	private static final String DELETE_TOKEN_KEY = "delete_token_key";

	private BackupHelper backupHelper;

	private CollapsingToolbarLayout toolbarLayout;
	private TextView progressDescription;
	private ProgressBar progressBar;
	private View progressContainer;
	private View warningsContainer;
	private View closeButton;
	private View deleteButton;
	private View deleteButtonWarning;

	private String token;
	@Nullable
	private BackupError backupError;
	private DeletionStatus deletionStatus = NOT_STARTED;

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupHelper = app.getBackupHelper();

		if (savedInstanceState != null) {
			token = savedInstanceState.getString(DELETE_TOKEN_KEY);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_delete_backup_account, container, false);
		AndroidUtils.addStatusBarPadding21v(requireActivity(), view);

		setupToolbar(view);
		setupProgress(view);
		setupWarnings(view);
		setupButtons(view);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateContent();
		backupHelper.getBackupListeners().addDeleteAccountListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		backupHelper.getBackupListeners().removeDeleteAccountListener(this);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(DELETE_TOKEN_KEY, token);
		super.onSaveInstanceState(outState);
	}

	private void setupToolbar(@NonNull View view) {
		AppBarLayout appBarLayout = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appBarLayout, 5.0f);
		toolbarLayout = view.findViewById(R.id.toolbar_layout);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(R.drawable.ic_action_close, ColorUtilities.getPrimaryIconColorId(nightMode)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void setupProgress(@NonNull View view) {
		progressContainer = view.findViewById(R.id.progress_container);
		progressBar = progressContainer.findViewById(R.id.progress_bar);
		progressDescription = progressContainer.findViewById(R.id.progress_description);
		updateProgress();
	}

	private void updateContent() {
		if (isAdded()) {
			boolean hasError = backupError != null;
			toolbarLayout.setTitle(getString(hasError ? R.string.deletion_error : deletionStatus.titleId));

			if (hasError) {
				progressDescription.setText(backupError.getLocalizedError(app));
			} else if (deletionStatus.descriptionId != -1) {
				progressDescription.setText(deletionStatus.descriptionId);
			}
			updateProgress();

			boolean deletionStarted = deletionStatus != NOT_STARTED;
			AndroidUiHelper.updateVisibility(progressContainer, deletionStarted);
			AndroidUiHelper.updateVisibility(warningsContainer, !deletionStarted);

			AndroidUiHelper.updateVisibility(closeButton, deletionStarted);
			AndroidUiHelper.updateVisibility(deleteButton, !deletionStarted);
			AndroidUiHelper.updateVisibility(deleteButtonWarning, !deletionStarted);
		}
	}

	private void updateProgress() {
		if (isAdded() && progressBar != null) {
			progressBar.setIndeterminate(deletionStatus != FINISHED);
		}
	}

	private void setupWarnings(@NonNull View view) {
		warningsContainer = view.findViewById(R.id.warnings_container);

		setupDescription(warningsContainer);
		setupDataDeletedWarning(warningsContainer);
		setupAccountDeletedWarning(warningsContainer);
		setupSecondaryDevicesWarning(warningsContainer);
		setupManageSubscriptions(warningsContainer);
	}

	private void setupDescription(@NonNull View view) {
		String cloud = getString(R.string.osmand_cloud);
		String warning = getString(R.string.delete_account_warning, cloud);
		TextView title = view.findViewById(R.id.warning);
		title.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), warning, cloud));
	}

	private void setupDataDeletedWarning(@NonNull View view) {
		String deleted = getString(R.string.shared_string_deleted).toLowerCase();
		String warning = getString(R.string.osmand_cloud_deletion_all_data_warning, deleted);
		CharSequence text = UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), warning, deleted);

		View container = view.findViewById(R.id.data_deleted);
		setupWarning(container, text, R.drawable.ic_action_file_delete);
	}

	private void setupAccountDeletedWarning(@NonNull View view) {
		String deleted = getString(R.string.shared_string_deleted).toLowerCase();
		String warning = getString(R.string.osmand_cloud_deletion_account_warning, deleted);
		CharSequence text = UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), warning, deleted);

		View container = view.findViewById(R.id.account_deleted);
		setupWarning(container, text, R.drawable.ic_action_user_account_delete);
	}

	private void setupSecondaryDevicesWarning(@NonNull View view) {
		View container = view.findViewById(R.id.secondary_devices);
		String text = getString(R.string.osmand_cloud_deletion_secondary_devices_warning);
		setupWarning(container, text, R.drawable.ic_action_secondary_devices_disabled);
	}

	private void setupWarning(@NonNull View view, @NonNull CharSequence text, int iconId) {
		TextView title = view.findViewById(R.id.title);
		ImageView icon = view.findViewById(R.id.icon);

		title.setText(text);
		icon.setImageDrawable(getIcon(iconId, R.color.deletion_color_warning));
	}

	private void setupManageSubscriptions(@NonNull View view) {
		String subscriptions = getString(R.string.manage_subscriptions);
		String warning = getString(R.string.osmand_cloud_deletion_subscriptions_warning);
		String text = getString(R.string.ltr_or_rtl_combine_via_space, warning, subscriptions);

		SpannableString spannable = UiUtilities.createClickableSpannable(text, subscriptions, unused -> {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				PurchasesFragment.showInstance(manager);
			}
			return true;
		});
		TextView textView = view.findViewById(R.id.manage_subscriptions);
		UiUtilities.setupClickableText(textView, spannable, nightMode);
	}

	private void setupButtons(@NonNull View view) {
		deleteButtonWarning = view.findViewById(R.id.delete_button_warning);

		deleteButton = view.findViewById(R.id.delete_button);
		deleteButton.setOnClickListener(this::showConfirmationDialog);
		UiUtilities.setupDialogButton(nightMode, deleteButton, TERTIARY_HARMFUL, getString(R.string.delete_account));

		closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> {
			MapActivity activity = (MapActivity) getActivity();
			if (activity != null) {
				if (backupError == null) {
					activity.getFragmentsHelper().dismissFragment(BackupCloudFragment.TAG);
					BackupAuthorizationFragment.showInstance(activity.getSupportFragmentManager());
				} else {
					activity.onBackPressed();
				}
			}
		});
		UiUtilities.setupDialogButton(nightMode, closeButton, SECONDARY, getString(R.string.shared_string_close));
	}

	private void showConfirmationDialog(@NonNull View view) {
		AlertDialogData data = new AlertDialogData(view.getContext(), nightMode)
				.setTitle(R.string.osmand_cloud_delete_account_confirmation)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
					deletionStatus = RUNNING;
					deleteAccount();
				})
				.setPositiveButtonTextColor(ColorUtilities.getColor(app, R.color.deletion_color_warning));
		CustomAlert.showSimpleMessage(data, R.string.osmand_cloud_delete_account_confirmation_descr);
	}

	private void deleteAccount() {
		try {
			updateContent();
			backupHelper.deleteAccount(settings.BACKUP_USER_EMAIL.get(), token);
		} catch (UserNotRegisteredException e) {
			updateContent();
			log.error(e);
		}
	}

	@Override
	public void onDeleteAccount(int status, @Nullable String message, @Nullable BackupError error) {
		backupError = error;
		deletionStatus = FINISHED;
		updateContent();

		if (error == null) {
			backupHelper.logout();
		}
		String text = error != null ? error.getLocalizedError(app) : message;
		if (!Algorithms.isEmpty(text)) {
			app.showToastMessage(text);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String token) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DeleteAccountFragment fragment = new DeleteAccountFragment();
			fragment.token = token;
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	protected enum DeletionStatus {
		NOT_STARTED(R.string.delete_account, -1),
		RUNNING(R.string.shared_string_deleting, R.string.osmand_cloud_deleting_account_descr),
		FINISHED(R.string.deleting_complete, R.string.osmand_cloud_deleted_account_descr);

		@StringRes
		private final int titleId;
		@StringRes
		private final int descriptionId;

		DeletionStatus(int titleId, int descriptionId) {
			this.titleId = titleId;
			this.descriptionId = descriptionId;
		}
	}
}