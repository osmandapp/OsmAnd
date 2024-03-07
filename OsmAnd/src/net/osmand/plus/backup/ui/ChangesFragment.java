package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DOWNLOAD;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_UPLOAD;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_CONFLICTS;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_LOCAL;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_REMOTE;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;

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
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ChangesFragment extends BaseOsmAndFragment implements OnPrepareBackupListener, OnBackupSyncListener {

	public static final String TAG = ChangesFragment.class.getSimpleName();

	private static final String SELECTED_TAB_TYPE_KEY = "SELECTED_TAB_TYPE_KEY";

	private BackupHelper backupHelper;
	private NetworkSettingsHelper settingsHelper;

	private View buttonsContainer;
	private View buttonsShadow;

	private RecentChangesType tabType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
		if (savedInstanceState != null) {
			tabType = RecentChangesType.valueOf(savedInstanceState.getString(SELECTED_TAB_TYPE_KEY, RECENT_CHANGES_LOCAL.name()));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_osmand_cloud_changes, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

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
		buttonsShadow = view.findViewById(R.id.buttons_shadow);

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

		setupSyncButton(syncing, preparing);
		setupCancelButton(syncing, preparing);
		AndroidUiHelper.updateVisibility(buttonsShadow, tabType != RECENT_CHANGES_CONFLICTS);
		AndroidUiHelper.updateVisibility(buttonsContainer, tabType != RECENT_CHANGES_CONFLICTS);
	}

	private void setupSyncButton(boolean syncing, boolean preparing) {
		DialogButton button = buttonsContainer.findViewById(R.id.action_button);
		if (tabType.buttonTextId != -1) {
			button.setOnClickListener(v -> {
				if (tabType == RECENT_CHANGES_REMOTE) {
					settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_DOWNLOAD);
				} else if (tabType == RECENT_CHANGES_LOCAL) {
					settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_UPLOAD);
				}
				setupBottomButtons();
			});
			boolean enabled = !syncing && !preparing && hasItems();
			button.setEnabled(enabled);
			button.setTitleId(tabType.buttonTextId);

			if (tabType.buttonIconId != -1) {
				int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
				int activeColor = ColorUtilities.getButtonSecondaryTextColor(app, nightMode);

				TextView textView = button.findViewById(R.id.button_text);
				Drawable icon = getPaintedContentIcon(tabType.buttonIconId, enabled ? activeColor : defaultColor);
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
				textView.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.content_padding_small));
			}
		}
		AndroidUiHelper.updateVisibility(button, tabType.buttonTextId != -1);
	}

	private void setupCancelButton(boolean syncing, boolean preparing) {
		DialogButton button = buttonsContainer.findViewById(R.id.cancel_button);
		button.setOnClickListener(v -> {
			settingsHelper.cancelSync();
			setupBottomButtons();
		});
		button.setEnabled(syncing && !preparing);
		button.setTitleId(R.string.shared_string_control_stop);
		AndroidUiHelper.updateVisibility(button, syncing);
	}

	private boolean hasItems() {
		PrepareBackupResult backup = backupHelper.getBackup();
		BackupInfo info = backup.getBackupInfo();
		if (info != null) {
			switch (tabType) {
				case RECENT_CHANGES_REMOTE:
					return BackupHelper.getItemsMapForRestore(info, backup.getSettingsItems()).size() > 0;
				case RECENT_CHANGES_LOCAL:
					return info.filteredFilesToDelete.size() + info.filteredFilesToUpload.size() > 0;
				default:
					return false;
			}
		}
		return false;
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
		outState.putString(SELECTED_TAB_TYPE_KEY, tabType.name());
	}

	@Override
	public void onBackupPreparing() {
		app.runInUIThread(() -> {
			if (isResumed()) {
				setupBottomButtons();
			}
		});
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		app.runInUIThread(() -> {
			if (isResumed()) {
				setupBottomButtons();
			}
		});
	}

	@Override
	public void onBackupSyncStarted() {
		app.runInUIThread(() -> {
			if (isResumed()) {
				setupBottomButtons();
			}
		});
	}

	public void onBackupSyncTasksUpdated() {
		app.runInUIThread(() -> {
			if (isResumed()) {
				setupBottomButtons();
			}
		});
	}

	@Override
	public void onBackupSyncFinished(@Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			app.showToastMessage(new BackupError(error).getLocalizedError(app));
		} else if (!settingsHelper.isBackupSyncing() && !backupHelper.isBackupPreparing()) {
			backupHelper.prepareBackup();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public enum RecentChangesType {

		RECENT_CHANGES_LOCAL(R.string.download_tab_local, R.string.upload_all, R.drawable.ic_action_cloud_upload_outline, LocalTabFragment.class),
		RECENT_CHANGES_REMOTE(R.string.shared_string_cloud, R.string.download_all, R.drawable.ic_action_cloud_download_outline, CloudTabFragment.class),
		RECENT_CHANGES_CONFLICTS(R.string.cloud_conflicts, -1, -1, ConflictsTabFragment.class);

		@StringRes
		private final int titleId;
		@StringRes
		private final int buttonTextId;
		@DrawableRes
		private final int buttonIconId;
		private final Class<?> fragment;

		RecentChangesType(@StringRes int titleId, @StringRes int buttonTextId, @StringRes int buttonIconId, @NonNull Class<?> fragment) {
			this.titleId = titleId;
			this.buttonTextId = buttonTextId;
			this.buttonIconId = buttonIconId;
			this.fragment = fragment;
		}
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
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