package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DOWNLOAD;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_UPLOAD;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_LOCAL;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_REMOTE;
import static net.osmand.plus.utils.UiUtilities.DialogButtonType.SECONDARY;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity.OsmandFragmentPagerAdapter;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ChangesFragment extends BaseOsmAndFragment implements OnPrepareBackupListener, OnBackupSyncListener {

	public static final String TAG = ChangesFragment.class.getSimpleName();

	private static final String SELECTED_TAB_TYPE_KEY = "SELECTED_TAB_TYPE_KEY";

	private OsmandApplication app;
	private BackupHelper backupHelper;
	private NetworkSettingsHelper settingsHelper;

	private View buttonsContainer;

	private RecentChangesType tabType;
	private boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
		nightMode = isNightMode(false);
		if (savedInstanceState != null) {
			tabType = RecentChangesType.valueOf(savedInstanceState.getString(SELECTED_TAB_TYPE_KEY, RECENT_CHANGES_LOCAL.name()));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = themedInflater.inflate(R.layout.fragment_osmand_cloud_changes, container, false);
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);

		buttonsContainer = view.findViewById(R.id.buttons_container);

		setupTabs(view);
		setupToolbar(view);
		setupBottomButtons();

		if (!settingsHelper.isBackupSyncing() && !backupHelper.isBackupPreparing()
				&& backupHelper.getBackup().getBackupInfo() == null) {
			backupHelper.prepareBackup();
		}

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.cloud_recent_changes);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void setupTabs(@NonNull View view) {
		List<TabItem> tabItems = new ArrayList<>();
		for (RecentChangesType tabType : RecentChangesType.values()) {
			tabItems.add(new TabItem(tabType.titleId, getString(tabType.titleId), tabType.fragment));
		}
		ViewPager viewPager = view.findViewById(R.id.pager);
		viewPager.setAdapter(new OsmandFragmentPagerAdapter(getChildFragmentManager(), tabItems));
		viewPager.setCurrentItem(tabType.ordinal());
		viewPager.addOnPageChangeListener(new SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				tabType = RecentChangesType.values()[position];
				setupBottomButtons();
			}
		});

		PagerSlidingTabStrip pagerSlidingTabStrip = view.findViewById(R.id.sliding_tabs);
		pagerSlidingTabStrip.setShouldExpand(true);
		pagerSlidingTabStrip.setViewPager(viewPager);
	}

	private void setupBottomButtons() {
		boolean syncing = settingsHelper.isBackupSyncing();
		boolean preparing = backupHelper.isBackupPreparing();

		View cancelButton = buttonsContainer.findViewById(R.id.cancel_button);
		UiUtilities.setupDialogButton(nightMode, cancelButton, SECONDARY, R.string.shared_string_cancel);
		cancelButton.setEnabled(syncing && !preparing);
		AndroidUiHelper.updateVisibility(cancelButton, !syncing);
		cancelButton.setOnClickListener(v -> {
			settingsHelper.cancelSync();
			setupBottomButtons();
		});
		View actionButton = buttonsContainer.findViewById(R.id.action_button);
		if (tabType.buttonTextId != -1) {
			UiUtilities.setupDialogButton(nightMode, actionButton, SECONDARY, tabType.buttonTextId);
			actionButton.setOnClickListener(v -> {
				if (tabType == RECENT_CHANGES_REMOTE) {
					settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_DOWNLOAD, ChangesFragment.this);
				} else if (tabType == RECENT_CHANGES_LOCAL) {
					settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_UPLOAD, ChangesFragment.this);
				}
				setupBottomButtons();
			});
			actionButton.setEnabled(!syncing && !preparing && hasItems());
		}
		AndroidUiHelper.updateVisibility(actionButton, tabType.buttonTextId != -1);
	}

	private boolean hasItems() {
		PrepareBackupResult backup = backupHelper.getBackup();
		BackupInfo info = backup.getBackupInfo();

		if (info == null) {
			return false;
		}

		switch (tabType) {
			case RECENT_CHANGES_REMOTE:
				return BackupHelper.getItemsMapForRestore(info, backup.getSettingsItems()).size() > 0;
			case RECENT_CHANGES_LOCAL:
				return info.filteredFilesToDelete.size() + info.filteredFilesToUpload.size() > 0;
			default:
				return false;
		}
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_TAB_TYPE_KEY, tabType.name());
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public void onBackupPreparing() {
		app.runInUIThread(this::setupBottomButtons);

		if (isAdded()) {
			FragmentManager manager = getChildFragmentManager();
			for (Fragment fragment : manager.getFragments()) {
				if (fragment instanceof OnPrepareBackupListener) {
					((OnPrepareBackupListener) fragment).onBackupPreparing();
				}
			}
		}
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		app.runInUIThread(this::setupBottomButtons);

		if (isAdded()) {
			FragmentManager manager = getChildFragmentManager();
			for (Fragment fragment : manager.getFragments()) {
				if (fragment instanceof OnPrepareBackupListener) {
					((OnPrepareBackupListener) fragment).onBackupPrepared(backupResult);
				}
			}
		}
	}

	@Override
	public void onBackupSyncStarted() {
		if (isAdded()) {
			FragmentManager manager = getChildFragmentManager();
			for (Fragment fragment : manager.getFragments()) {
				if (fragment instanceof OnBackupSyncListener) {
					((OnBackupSyncListener) fragment).onBackupSyncStarted();
				}
			}
		}
	}

	@Override
	public void onBackupProgressUpdate(int progress) {
		if (isAdded()) {
			FragmentManager manager = getChildFragmentManager();
			for (Fragment fragment : manager.getFragments()) {
				if (fragment instanceof OnBackupSyncListener) {
					((OnBackupSyncListener) fragment).onBackupProgressUpdate(progress);
				}
			}
		}
	}

	@Override
	public void onBackupSyncFinished(@Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			app.showToastMessage(new BackupError(error).getLocalizedError(app));
		} else if (!settingsHelper.isBackupSyncing() && !backupHelper.isBackupPreparing()) {
			backupHelper.prepareBackup();
		}
		if (isAdded()) {
			FragmentManager manager = getChildFragmentManager();
			for (Fragment fragment : manager.getFragments()) {
				if (fragment instanceof OnBackupSyncListener) {
					((OnBackupSyncListener) fragment).onBackupSyncFinished(error);
				}
			}
		}
	}

	public enum RecentChangesType {

		RECENT_CHANGES_LOCAL(R.string.download_tab_local, R.string.upload_all, LocalTabFragment.class),
		RECENT_CHANGES_REMOTE(R.string.shared_string_cloud, R.string.download_all, CloudTabFragment.class),
		RECENT_CHANGES_CONFLICTS(R.string.cloud_conflicts, -1, ConflictsTabFragment.class);

		@StringRes
		private final int titleId;
		@StringRes
		private final int buttonTextId;
		private final Class<?> fragment;

		RecentChangesType(@StringRes int titleId, @StringRes int buttonTextId, @NonNull Class<?> fragment) {
			this.titleId = titleId;
			this.buttonTextId = buttonTextId;
			this.fragment = fragment;
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull RecentChangesType tabType) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ChangesFragment fragment = new ChangesFragment();
			fragment.tabType = tabType;
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}