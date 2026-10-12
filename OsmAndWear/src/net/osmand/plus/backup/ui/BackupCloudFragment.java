package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_SYNC;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_CONFLICTS;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_LOCAL;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_REMOTE;
import static net.osmand.plus.backup.ui.status.BackupStorageCard.TRASH_BUTTON_INDEX;
import static net.osmand.plus.backup.ui.status.CloudSyncCard.CLOUD_CHANGES_BUTTON_INDEX;
import static net.osmand.plus.backup.ui.status.CloudSyncCard.CONFLICTS_BUTTON_INDEX;
import static net.osmand.plus.backup.ui.status.CloudSyncCard.LOCAL_CHANGES_BUTTON_INDEX;
import static net.osmand.plus.backup.ui.status.CloudSyncCard.STOP_BUTTON_INDEX;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.backup.ui.status.BackupStatus;
import net.osmand.plus.backup.ui.status.BackupStorageCard;
import net.osmand.plus.backup.ui.status.CloudSyncCard;
import net.osmand.plus.backup.ui.status.IntroductionCard;
import net.osmand.plus.backup.ui.status.WarningStatusCard;
import net.osmand.plus.backup.ui.trash.CloudTrashFragment;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

public class BackupCloudFragment extends BaseOsmAndFragment implements InAppPurchaseListener,
		OnPrepareBackupListener, CardListener, OnBackupSyncListener {

	public static final String TAG = BackupCloudFragment.class.getSimpleName();

	private static final String DIALOG_TYPE_KEY = "dialog_type_key";
	private static final String CHANGES_VISIBLE_KEY = "changes_visible_key";

	private BackupHelper backupHelper;
	private NetworkSettingsHelper settingsHelper;

	private CloudSyncCard syncCard;

	private LoginDialogType dialogType;
	private boolean changesVisible;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public boolean isChangesVisible() {
		return changesVisible;
	}

	public void toggleActionsVisibility() {
		changesVisible = !changesVisible;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();

		if (savedInstanceState != null) {
			changesVisible = savedInstanceState.getBoolean(CHANGES_VISIBLE_KEY);
			if (savedInstanceState.containsKey(DIALOG_TYPE_KEY)) {
				dialogType = LoginDialogType.valueOf(savedInstanceState.getString(DIALOG_TYPE_KEY));
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.osmand_cloud, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar(view);
		setupSwipeRefresh(view);
		setupCards(view);

		prepareBackup();

		return view;
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
		actionButton.setOnClickListener(v -> openSettings());
		AndroidUiHelper.updateVisibility(actionButton, true);
	}

	private void openSettings() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			BackupSettingsFragment.showInstance(activity.getSupportFragmentManager());
		}
		dialogType = null;
	}

	private void setupSwipeRefresh(@NonNull View view) {
		SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh);
		int swipeColor = ContextCompat.getColor(app, nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange);
		swipeRefresh.setColorSchemeColors(swipeColor);
		swipeRefresh.setOnRefreshListener(() -> {
			prepareBackup();
			swipeRefresh.setRefreshing(false);
		});
	}

	private void prepareBackup() {
		if (!settingsHelper.isBackupSyncing() && !backupHelper.isBackupPreparing()) {
			backupHelper.prepareBackup();
		}
	}

	private void setupCards(@Nullable View view) {
		if (view == null) {
			return;
		}
		FragmentActivity activity = requireActivity();
		ViewGroup container = view.findViewById(R.id.container);
		container.removeAllViews();

		PrepareBackupResult backup = backupHelper.getBackup();
		BackupInfo info = backup.getBackupInfo();
		BackupStatus status = BackupStatus.getBackupStatus(app, backup);

		boolean backupSaved = !Algorithms.isEmpty(backup.getRemoteFiles());
		boolean showIntroductionItem = info != null && dialogType == LoginDialogType.SIGN_UP && !backupSaved
				|| (dialogType == LoginDialogType.SIGN_IN && (backupSaved || !Algorithms.isEmpty(backup.getLocalFiles())));

		if (showIntroductionItem) {
			IntroductionCard introductionCard = new IntroductionCard(activity, dialogType);
			introductionCard.setListener(this);
			container.addView(introductionCard.build(view.getContext()));
		} else {
			if (status.warningTitleRes != -1 || !Algorithms.isEmpty(backup.getError())) {
				WarningStatusCard warningCard = new WarningStatusCard(activity);
				warningCard.setListener(this);
				container.addView(warningCard.build(view.getContext()));
			}
			syncCard = new CloudSyncCard(activity, this);
			syncCard.setListener(this);
			container.addView(syncCard.build(view.getContext()));

			BackupStorageCard storageCard = new BackupStorageCard(activity);
			storageCard.setListener(this);
			container.addView(storageCard.build(view.getContext()));
		}
	}

	private void refreshContent() {
		setupCards(getView());
	}

	@Override
	public void onResume() {
		super.onResume();
		backupHelper.addPrepareBackupListener(this);
		settingsHelper.addBackupSyncListener(this);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		backupHelper.removePrepareBackupListener(this);
		settingsHelper.removeBackupSyncListener(this);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (dialogType != null) {
			outState.putString(DIALOG_TYPE_KEY, dialogType.name());
		}
		outState.putBoolean(CHANGES_VISIBLE_KEY, changesVisible);
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		prepareBackup();
	}

	@Override
	public void onBackupPreparing() {
		app.runInUIThread(this::refreshContent);
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backup) {
		app.runInUIThread(this::refreshContent);
	}

	@Override
	public void onBackupSyncStarted() {
		app.runInUIThread(() -> {
			if (syncCard != null) {
				syncCard.onBackupSyncStarted();
			}
		});
	}

	@Override
	public void onBackupProgressUpdate(int progress) {
		app.runInUIThread(() -> {
			if (syncCard != null) {
				syncCard.onBackupProgressUpdate(progress);
			}
		});
	}

	@Override
	public void onBackupSyncFinished(@Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			refreshContent();
			app.showToastMessage(new BackupError(error).getLocalizedError(app));
		} else {
			prepareBackup();
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		FragmentManager manager = getFragmentManager();
		if (manager == null) {
			return;
		}
		if (card instanceof IntroductionCard) {
			if (IntroductionCard.SYNC_BUTTON_INDEX == buttonIndex) {
				startSync();
				dialogType = null;
				refreshContent();
			} else if (IntroductionCard.SETTINGS_BUTTON_INDEX == buttonIndex) {
				dialogType = null;
				BackupTypesFragment.showInstance(manager);
			}
		} else if (card instanceof CloudSyncCard) {
			if (SYNC_BUTTON_INDEX == buttonIndex) {
				startSync();
			} else if (STOP_BUTTON_INDEX == buttonIndex) {
				settingsHelper.cancelSync();
			} else if (LOCAL_CHANGES_BUTTON_INDEX == buttonIndex) {
				ChangesFragment.showInstance(manager, RECENT_CHANGES_LOCAL);
			} else if (CLOUD_CHANGES_BUTTON_INDEX == buttonIndex) {
				ChangesFragment.showInstance(manager, RECENT_CHANGES_REMOTE);
			} else if (CONFLICTS_BUTTON_INDEX == buttonIndex) {
				ChangesFragment.showInstance(manager, RECENT_CHANGES_CONFLICTS);
			}
		} else if (card instanceof BackupStorageCard) {
			if (TRASH_BUTTON_INDEX == buttonIndex) {
				if (InAppPurchaseUtils.isBackupAvailable(app)) {
					CloudTrashFragment.showInstance(manager);
				} else {
					OsmAndProPlanFragment.showInstance(requireActivity());
				}
			}
		}
	}

	private void startSync() {
		if (!backupHelper.isBackupPreparing() && backupHelper.getBackup().getBackupInfo() == null) {
			backupHelper.prepareBackup();
		}
		if (!settingsHelper.isBackupSyncing()) {
			settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_SYNC);
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