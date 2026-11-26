package net.osmand.plus.backup.ui.trash;

import static net.osmand.plus.backup.ui.ChangesAdapter.BACKUP_STATUS_TYPE;
import static net.osmand.plus.backup.ui.trash.CloudTrashAdapter.EMPTY_BANNER_TYPE;
import static net.osmand.plus.backup.ui.trash.CloudTrashController.CONFIRM_EMPTY_TRASH_ID;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CloudTrashFragment extends BaseOsmAndFragment implements ConfirmationDialogListener,
		OnPrepareBackupListener, OnBackupSyncListener {

	public static final String TAG = CloudTrashFragment.class.getSimpleName();

	private BackupHelper backupHelper;
	private NetworkSettingsHelper settingsHelper;

	private CloudTrashAdapter adapter;
	private CloudTrashController controller;
	private ProgressBar progressBar;


	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
		controller = new CloudTrashController(app, this);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_osmand_cloud_trash, container, false);
		AndroidUtils.addStatusBarPadding21v(requireActivity(), view);
		progressBar = view.findViewById(R.id.progress_bar);

		setupToolbar(view);
		setupRecyclerView(view);

		if (shouldPrepareBackup()) {
			backupHelper.prepareBackup();
		}
		updateAdapter();

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.shared_string_trash);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(app)));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		actionButton.setOnClickListener(this::showOptionsMenu);
		actionButton.setImageDrawable(getIcon(R.drawable.ic_overflow_menu_white));
		actionButton.setContentDescription(getString(R.string.shared_string_more));
		AndroidUiHelper.updateVisibility(actionButton, true);
	}

	private void showOptionsMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(view.getContext())
				.setTitleId(R.string.shared_string_empty_trash)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> controller.showClearConfirmationDialog())
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.simple_popup_menu_item;
		PopUpMenu.show(displayData);
	}

	private void setupRecyclerView(@NonNull View view) {
		adapter = new CloudTrashAdapter(app, controller, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setAdapter(adapter);
	}

	private void updateAdapter() {
		List<Object> items = new ArrayList<>();

		boolean preparing = backupHelper.isBackupPreparing();
		if (preparing) {
			items.add(BACKUP_STATUS_TYPE);
		}
		Map<String, TrashGroup> groups = controller.collectTrashGroups();
		if (Algorithms.isEmpty(groups)) {
			if (!preparing) {
				items.add(EMPTY_BANNER_TYPE);
			}
		} else {
			for (TrashGroup group : groups.values()) {
				items.add(group);
				items.addAll(group.getItems());
			}
		}
		adapter.setItems(items);
	}

	@Override
	public void onResume() {
		super.onResume();

		settingsHelper.addBackupSyncListener(this);
		backupHelper.addPrepareBackupListener(this);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		settingsHelper.removeBackupSyncListener(this);
		backupHelper.removePrepareBackupListener(this);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	private boolean shouldPrepareBackup() {
		return !settingsHelper.isBackupSyncing() && !backupHelper.isBackupPreparing()
				&& backupHelper.getBackup().getBackupInfo() == null;
	}

	public void showSnackbar(@NonNull String text) {
		View view = getView();
		if (view != null) {
			Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG);
			UiUtilities.setupSnackbar(snackbar, isNightMode(), 5);
			snackbar.show();
		}
	}

	public void updateProgressVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
	}

	@Override
	public void onActionConfirmed(int actionId) {
		if (actionId == CONFIRM_EMPTY_TRASH_ID) {
			controller.clearTrash();
		}
	}

	@Override
	public void onBackupPreparing() {
		updateAdapter();
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult result) {
		updateAdapter();
	}

	@Override
	public void onBackupSyncStarted() {
		app.runInUIThread(() -> adapter.onBackupSyncStarted());
	}

	@Override
	public void onBackupProgressUpdate(int progress) {
		app.runInUIThread(() -> adapter.onBackupProgressUpdate(progress));
	}

	@Override
	public void onBackupSyncFinished(@Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			app.showToastMessage(new BackupError(error).getLocalizedError(app));
		} else if (!settingsHelper.isBackupSyncing() && !backupHelper.isBackupPreparing()) {
			backupHelper.prepareBackup();
		}
	}

	@Override
	public void onBackupItemStarted(@NonNull String type, @NonNull String fileName, int work) {
		app.runInUIThread(() -> adapter.onBackupItemStarted(type, fileName, work));
	}

	@Override
	public void onBackupItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		app.runInUIThread(() -> adapter.onBackupItemProgress(type, fileName, value));
	}

	@Override
	public void onBackupItemFinished(@NonNull String type, @NonNull String fileName) {
		app.runInUIThread(() -> adapter.onBackupItemFinished(type, fileName));
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			CloudTrashFragment fragment = new CloudTrashFragment();
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
