package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_SYNC;
import static net.osmand.plus.backup.ui.ChangesFragment.ChangesTabType.CLOUD_CHANGES;
import static net.osmand.plus.backup.ui.ChangesFragment.ChangesTabType.CONFLICTS;
import static net.osmand.plus.backup.ui.ChangesFragment.ChangesTabType.LOCAL_CHANGES;
import static net.osmand.plus.backup.ui.status.CloudSyncCard.CLOUD_CHANGES_BUTTON_INDEX;
import static net.osmand.plus.backup.ui.status.CloudSyncCard.CONFLICTS_BUTTON_INDEX;
import static net.osmand.plus.backup.ui.status.CloudSyncCard.LOCAL_CHANGES_BUTTON_INDEX;
import static net.osmand.plus.backup.ui.status.CloudSyncCard.SYNC_BUTTON_INDEX;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.backup.ui.AuthorizeFragment.LoginDialogType;
import net.osmand.plus.backup.ui.status.CloudSyncCard;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class BackupCloudFragment extends BaseOsmAndFragment implements InAppPurchaseListener,
		OnPrepareBackupListener, CardListener, OnBackupSyncListener {

	public static final String TAG = BackupCloudFragment.class.getSimpleName();

	private static final String DIALOG_TYPE_KEY = "dialog_type_key";

	private OsmandApplication app;
	private BackupHelper backupHelper;
	private NetworkSettingsHelper settingsHelper;

	private CloudSyncCard syncCard;
	private LoginDialogType dialogType;

	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Nullable
	public LoginDialogType getDialogType() {
		return dialogType;
	}

	public void removeDialogType() {
		dialogType = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
		nightMode = !requireMyApplication().getSettings().isLightContent();

		if (savedInstanceState != null && savedInstanceState.containsKey(DIALOG_TYPE_KEY)) {
			dialogType = LoginDialogType.valueOf(savedInstanceState.getString(DIALOG_TYPE_KEY));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = themedInflater.inflate(R.layout.osmand_cloud, container, false);
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);

		setupCards(view);
		setupToolbar(view);

		if (!settingsHelper.isBackupExporting()) {
			app.getBackupHelper().prepareBackup();
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (dialogType != null) {
			outState.putString(DIALOG_TYPE_KEY, dialogType.name());
		}
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.osmand_cloud);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		actionButton.setImageDrawable(getIcon(R.drawable.ic_action_settings));
		actionButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				BackupSettingsFragment.showInstance(activity.getSupportFragmentManager());
			}
		});
		AndroidUiHelper.updateVisibility(actionButton, true);
	}

	private void setupCards(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.container);
		container.removeAllViews();

		syncCard = new CloudSyncCard(requireActivity());
		syncCard.setListener(this);
		container.addView(syncCard.build(view.getContext()));
	}

	@Override
	public void onResume() {
		super.onResume();
		backupHelper.addPrepareBackupListener(this);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		backupHelper.removePrepareBackupListener(this);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public void onBackupPreparing() {
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
	}

	@Override
	public void onBackupSyncStarted() {

	}

	@Override
	public void onBackupProgressUpdate(float progress) {

	}

	@Override
	public void onBackupSyncFinished(@Nullable String error) {

	}

	@Override
	public void onBackupItemStarted(@NonNull String type, @NonNull String fileName, int work) {

	}

	@Override
	public void onBackupItemProgress(@NonNull String type, @NonNull String fileName, int value) {

	}

	@Override
	public void onBackupItemFinished(@NonNull String type, @NonNull String fileName) {

	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		FragmentManager manager = getFragmentManager();
		if (manager == null) {
			return;
		}
		if (card instanceof CloudSyncCard) {
			if (SYNC_BUTTON_INDEX == buttonIndex) {
				settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_SYNC, this);
			} else if (LOCAL_CHANGES_BUTTON_INDEX == buttonIndex) {
				ChangesFragment.showInstance(manager, LOCAL_CHANGES);
			} else if (CLOUD_CHANGES_BUTTON_INDEX == buttonIndex) {
				ChangesFragment.showInstance(manager, CLOUD_CHANGES);
			} else if (CONFLICTS_BUTTON_INDEX == buttonIndex) {
				ChangesFragment.showInstance(manager, CONFLICTS);
			}
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		showInstance(manager, null);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable LoginDialogType dialogType) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			BackupCloudFragment fragment = new BackupCloudFragment();
			fragment.dialogType = dialogType;
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}