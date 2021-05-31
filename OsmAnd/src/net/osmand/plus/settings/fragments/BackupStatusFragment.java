package net.osmand.plus.settings.fragments;

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
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.List;

public class BackupStatusFragment extends BaseOsmAndFragment implements CardListener, BackupExportListener {

	private OsmandApplication app;
	private OsmandSettings settings;
	private BackupHelper backupHelper;

	private BackupInfo backupInfo;
	private String error;

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
		settings = app.getSettings();
		backupHelper = app.getBackupHelper();
		nightMode = !settings.isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_backup_status, container, false);

		progressBar = view.findViewById(R.id.progress_bar);
		cardsContainer = view.findViewById(R.id.cards_container);

		updateCards();
		prepareBackup();

		return view;
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			cardsContainer.removeAllViews();

			if (backupInfo != null && app.getNetworkSettingsHelper().isBackupExporting()) {
				showUploadCard();
			} else if (backupInfo != null || error != null) {
				statusCard = new BackupStatusCard(mapActivity, backupInfo, error);
				statusCard.setListener(this);
				cardsContainer.addView(statusCard.build(mapActivity));
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
		if (mapActivity != null) {
			uploadCard = new BackupUploadCard(mapActivity, backupInfo, this);
			uploadCard.setListener(this);
			cardsContainer.addView(uploadCard.build(mapActivity), 0);
		}
	}

	private void prepareBackup() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AndroidUiHelper.setVisibility(View.VISIBLE, progressBar);
			app.getBackupHelper().prepareBackupInfo(new OnPrepareBackupListener() {

				@Override
				public void onBackupPrepared(@Nullable BackupInfo backupInfo, @Nullable List<SettingsItem> userFiles, String error) {
					AndroidUiHelper.setVisibility(View.INVISIBLE, progressBar);
					BackupStatusFragment.this.error = error;
					BackupStatusFragment.this.backupInfo = backupInfo;
					updateCards();
				}
			});
		}
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
				prepareBackup();
			} else if (buttonIndex == BackupStatusCard.BACKUP_BUTTON_INDEX) {
				hideStatusCard();
				showUploadCard();
			}
		}
	}

	@Override
	public void onBackupExportStarted() {
		if (uploadCard != null) {
			uploadCard.updateContent();
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
		prepareBackup();
	}
}