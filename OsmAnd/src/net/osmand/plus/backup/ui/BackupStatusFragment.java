package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ServerError;
import net.osmand.plus.backup.ui.cards.BackupUploadCard;
import net.osmand.plus.backup.ui.cards.LocalBackupCard;
import net.osmand.plus.backup.ui.cards.RestoreBackupCard;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.List;
import java.util.Map;

public class BackupStatusFragment extends BaseOsmAndFragment implements BackupExportListener,
		OnDeleteFilesListener, OnPrepareBackupListener, ImportListener {

	private OsmandApplication app;
	private BackupHelper backupHelper;
	private NetworkSettingsHelper settingsHelper;

	private BackupUploadCard uploadCard;

	private ProgressBar progressBar;
	private ViewGroup cardsContainer;

	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
		nightMode = !app.getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_backup_status, container, false);

		progressBar = view.findViewById(R.id.progress_bar);
		cardsContainer = view.findViewById(R.id.cards_container);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateCards();
		settingsHelper.updateExportListener(this);
		backupHelper.addPrepareBackupListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		settingsHelper.updateExportListener(null);
		backupHelper.removePrepareBackupListener(this);
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			cardsContainer.removeAllViews();

			uploadCard = new BackupUploadCard(mapActivity, backupHelper.getBackup(), this, this);
			cardsContainer.addView(uploadCard.build(mapActivity), 0);

			cardsContainer.addView(new RestoreBackupCard(mapActivity).build(mapActivity));
			cardsContainer.addView(new LocalBackupCard(mapActivity).build(mapActivity));
		}
	}

	@Override
	public void onBackupPreparing() {
		AndroidUiHelper.setVisibility(View.VISIBLE, progressBar);
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		AndroidUiHelper.setVisibility(View.INVISIBLE, progressBar);
		updateCards();
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	@Override
	public void onBackupExportStarted(int itemsCount) {
		if (uploadCard != null) {
			uploadCard.update();
		}
	}

	@Override
	public void onBackupExportProgressUpdate(int value) {
		if (uploadCard != null) {
			uploadCard.updateProgress(value);
		}
	}

	@Override
	public void onBackupExportFinished(@Nullable String error) {
		if (error != null) {
			String err = new ServerError(error).getLocalizedError(app);
			app.showShortToastMessage(err);
		}
		backupHelper.prepareBackup();
	}

	@Override
	public void onBackupExportItemStarted(@NonNull String type, @NonNull String fileName, int work) {
		if (uploadCard != null) {
			uploadCard.onBackupExportItemStarted(type, fileName, work);
		}
	}

	@Override
	public void onBackupExportItemFinished(@NonNull String type, @NonNull String fileName) {
		if (uploadCard != null) {
			uploadCard.onBackupExportItemFinished(type, fileName);
		}
	}

	@Override
	public void onBackupExportItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		if (uploadCard != null) {
			uploadCard.onBackupExportItemProgress(type, fileName, value);
		}
	}

	@Override
	public void onFileDeleteProgress(@NonNull RemoteFile file) {

	}

	@Override
	public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {

	}

	@Override
	public void onFilesDeleteError(int status, @NonNull String message) {

	}

	@Override
	public void onImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
		backupHelper.prepareBackup();
	}
}