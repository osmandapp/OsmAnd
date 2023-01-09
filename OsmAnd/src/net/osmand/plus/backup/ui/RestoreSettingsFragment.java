package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.RESTORE_ITEMS_KEY;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.ImportBackupTask;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupCollectListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.fragments.ImportCompleteFragment;
import net.osmand.plus.settings.fragments.ImportSettingsFragment;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RestoreSettingsFragment extends ImportSettingsFragment implements OnPrepareBackupListener {

	public static final String TAG = RestoreSettingsFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(RestoreSettingsFragment.class.getSimpleName());

	private NetworkSettingsHelper settingsHelper;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		exportMode = false;
		settingsHelper = app.getNetworkSettingsHelper();

		ImportBackupTask importTask = settingsHelper.getImportTask(RESTORE_ITEMS_KEY);
		if (importTask != null) {
			if (settingsItems == null) {
				settingsItems = importTask.getItems();
			}
			List<Object> duplicates = importTask.getDuplicates();
			List<SettingsItem> selectedItems = importTask.getSelectedItems();
			if (duplicates == null) {
				importTask.setDuplicatesListener(getDuplicatesListener());
			} else if (duplicates.isEmpty() && selectedItems != null) {
				try {
					settingsHelper.importSettings(RESTORE_ITEMS_KEY, selectedItems, false, getImportListener());
				} catch (IllegalStateException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			Toolbar toolbar = view.findViewById(R.id.toolbar);
			toolbar.setTitle(R.string.restore_from_osmand_cloud);
			description.setText(R.string.choose_what_to_restore);
		}
		collectItems();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getBackupHelper().addPrepareBackupListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getBackupHelper().removePrepareBackupListener(this);
	}

	@Override
	protected void onContinueButtonClickAction() {
		importItems();
	}

	@Override
	protected void importFinished(boolean succeed, boolean needRestart, List<SettingsItem> items) {
		if (succeed) {
			FragmentManager fm = getFragmentManager();
			if (fm != null) {
				ImportCompleteFragment.showInstance(fm, items, getString(R.string.osmand_cloud), needRestart);
			}
		}
	}

	@Override
	protected void processDuplicates(List<Object> duplicates, List<SettingsItem> items) {
		FragmentManager fragmentManager = getFragmentManager();
		if (duplicates.isEmpty()) {
			if (isAdded()) {
				updateUi(R.string.shared_string_restore, R.string.receiving_data_from_server);
			}
			try {
				settingsHelper.importSettings(RESTORE_ITEMS_KEY, items, false, getImportListener());
			} catch (IllegalStateException e) {
				LOG.error(e.getMessage(), e);
			}
		} else if (fragmentManager != null) {
			RestoreDuplicatesFragment.showInstance(fragmentManager, duplicates, items, this);
		}
	}

	private void updateUi(int toolbarTitleRes, int descriptionRes) {
		toolbarLayout.setTitle(getString(toolbarTitleRes));
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(descriptionRes), getString(R.string.osmand_cloud)),
				Typeface.BOLD, getString(R.string.osmand_cloud)
		));
		buttonsContainer.setVisibility(View.GONE);
		progressBar.setVisibility(View.VISIBLE);
		adapter.clearSettingsList();
	}

	private void importItems() {
		if (settingsItems != null) {
			try {
				duplicateStartTime = System.currentTimeMillis();
				List<SettingsItem> selectedItems = settingsHelper.prepareSettingsItems(adapter.getData(), settingsItems, false);
				settingsHelper.checkDuplicates(RESTORE_ITEMS_KEY, settingsItems, selectedItems, getDuplicatesListener());
			} catch (IllegalStateException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		updateUi(R.string.shared_string_preparing, R.string.checking_for_duplicate_description);
	}

	@Override
	public void onBackupPreparing() {
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		collectAndReadSettings();
	}

	private void collectItems() {
		updateUi(R.string.shared_string_preparing, R.string.shared_string_preparing);
		if (!app.getBackupHelper().isBackupPreparing()) {
			collectAndReadSettings();
		}
	}

	private void collectAndReadSettings() {
		BackupCollectListener collectListener = new BackupCollectListener() {

			@Override
			public void onBackupCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items, @NonNull List<RemoteFile> remoteFiles) {
				FragmentActivity activity = getActivity();
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					toolbarLayout.setTitle(getString(R.string.restore_from_osmand_cloud));
					description.setText(R.string.choose_what_to_restore);
					buttonsContainer.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
					if (succeed) {
						PrepareBackupResult backup = app.getBackupHelper().getBackup();
						BackupInfo info = backup.getBackupInfo();
						Set<SettingsItem> itemsForRestore = new HashSet<>();
						if (info != null) {
							Map<RemoteFile, SettingsItem> restoreItems = BackupHelper.getRemoteFilesSettingsItems(items, info.filesToDownload, false);
							itemsForRestore.addAll(restoreItems.values());
						}
						setSettingsItems(new ArrayList<>(itemsForRestore));
						dataList = SettingsHelper.getSettingsToOperateByCategory(settingsItems, false, false);
						adapter.updateSettingsItems(dataList, selectedItemsMap);
					}
				}
			}
		};
		try {
			settingsHelper.collectSettings(RESTORE_ITEMS_KEY, true, collectListener);
		} catch (IllegalStateException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			RestoreSettingsFragment fragment = new RestoreSettingsFragment();
			manager.beginTransaction().
					replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(SETTINGS_LIST_TAG)
					.commitAllowingStateLoss();
		}
	}
}