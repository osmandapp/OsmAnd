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
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.BackupHelper.OnDeleteFilesListener;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.cards.BackupStatusCard;
import net.osmand.plus.backup.ui.cards.BackupUploadCard;
import net.osmand.plus.backup.ui.cards.LocalBackupCard;
import net.osmand.plus.backup.ui.cards.RestoreBackupCard;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.util.Algorithms;

import java.util.Map;

public class BackupStatusFragment extends BaseOsmAndFragment implements CardListener, BackupExportListener,
		OnDeleteFilesListener, OnPrepareBackupListener {

	private OsmandApplication app;

	private BackupStatusCard statusCard;
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
		app.getBackupHelper().addPrepareBackupListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getBackupHelper().removePrepareBackupListener(this);
	}

	@Nullable
	private BackupInfo getBackupInfo() {
		PrepareBackupResult backup = app.getBackupHelper().getBackup();
		return backup != null ? backup.getBackupInfo() : null;
	}

	@Nullable
	private String getBackupError() {
		PrepareBackupResult backup = app.getBackupHelper().getBackup();
		return backup != null ? backup.getError() : null;
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			cardsContainer.removeAllViews();
			BackupInfo backupInfo = getBackupInfo();
			String error = getBackupError();
			if (backupInfo != null && app.getNetworkSettingsHelper().isBackupExporting()) {
				showUploadCard();
			} else if (backupInfo != null || error != null) {
				statusCard = new BackupStatusCard(mapActivity, backupInfo, error);
				statusCard.setListener(this);
				cardsContainer.addView(statusCard.build(mapActivity));
			}
			if (backupInfo != null && (!Algorithms.isEmpty(backupInfo.filesToDownload) || !Algorithms.isEmpty(backupInfo.filesToMerge))) {
				cardsContainer.addView(new RestoreBackupCard(mapActivity, backupInfo).build(mapActivity));
			}
			cardsContainer.addView(new LocalBackupCard(mapActivity).build(mapActivity));
		}
	}

	private void hideStatusCard() {
		if (statusCard != null && statusCard.getView() != null) {
			cardsContainer.removeView(statusCard.getView());
		}
	}

	private void showUploadCard() {
		MapActivity mapActivity = getMapActivity();
		BackupInfo backupInfo = getBackupInfo();
		if (mapActivity != null && backupInfo != null) {
			uploadCard = new BackupUploadCard(mapActivity, backupInfo, this, this);
			uploadCard.setListener(this);
			cardsContainer.addView(uploadCard.build(mapActivity), 0);
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
	public void onCardLayoutNeeded(@NonNull BaseCard card) {

	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {

	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		if (card instanceof BackupStatusCard) {
			if (buttonIndex == BackupStatusCard.RETRY_BUTTON_INDEX) {
				app.getBackupHelper().prepareBackup();
			} else if (buttonIndex == BackupStatusCard.BACKUP_BUTTON_INDEX) {
				hideStatusCard();
				showUploadCard();
			}
		}
	}

	@Override
	public void onBackupExportStarted(int itemsCount) {
		if (uploadCard != null) {
			uploadCard.update();
			uploadCard.setupProgress(itemsCount);
		}
	}

	@Override
	public void onBackupExportProgressUpdate(int value) {
		if (uploadCard != null) {
			uploadCard.updateProgress(value);
		}
	}

	@Override
	public void onBackupExportFinished(boolean succeed) {
		app.getBackupHelper().prepareBackup();
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
}