package net.osmand.plus.backup.ui.status;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.AuthorizeFragment.LoginDialogType;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BackupStatusAdapter extends RecyclerView.Adapter<ViewHolder> implements BackupExportListener, OnDeleteFilesListener, ImportListener {

	public static final int HEADER_TYPE = 0;
	public static final int WARNING_TYPE = 1;
	public static final int UPLOAD_TYPE = 2;
	public static final int CONFLICTS_TYPE = 3;
	public static final int ACTION_BUTTON_TYPE = 4;
	public static final int RESTORE_TYPE = 5;
	public static final int LOCAL_BACKUP_TYPE = 6;
	public static final int INTRODUCTION_TYPE = 7;

	private final OsmandApplication app;
	private final MapActivity mapActivity;

	private PrepareBackupResult backup;
	private BackupInfo info;
	private BackupStatus status;
	private String error;

	private final List<Object> items = new ArrayList<>();

	private final BackupStatusFragment fragment;
	private final LoginDialogType dialogType;

	private boolean uploadItemsVisible;
	private final boolean nightMode;

	public BackupStatusAdapter(@NonNull MapActivity mapActivity, @NonNull BackupStatusFragment fragment, boolean nightMode) {
		this.mapActivity = mapActivity;
		this.app = (OsmandApplication) mapActivity.getApplication();
		this.fragment = fragment;
		this.nightMode = nightMode;
		this.dialogType = fragment.getDialogType();
	}

	public void setBackup(@NonNull PrepareBackupResult backup) {
		this.backup = backup;
		error = backup.getError();
		info = backup.getBackupInfo();
		status = BackupStatus.getBackupStatus(app, backup);
		updateItems();
	}

	public boolean isUploadItemsVisible() {
		return uploadItemsVisible;
	}

	public void toggleUploadItemsVisibility() {
		uploadItemsVisible = !uploadItemsVisible;
		updateItems();
	}

	public void updateItems() {
		items.clear();

		boolean backupSaved = !Algorithms.isEmpty(backup.getRemoteFiles());
		boolean showIntroductionItem = info != null && dialogType == LoginDialogType.SIGN_UP && !backupSaved
				|| (dialogType == LoginDialogType.SIGN_IN && (backupSaved || !Algorithms.isEmpty(backup.getLocalFiles())));

		if (showIntroductionItem) {
			items.add(INTRODUCTION_TYPE);
		} else {
			items.add(HEADER_TYPE);

			if (status.warningTitleRes != -1 || !Algorithms.isEmpty(error)) {
				items.add(WARNING_TYPE);
			}
			if (info != null && uploadItemsVisible) {
				items.addAll(info.itemsToUpload);
				items.addAll(info.itemsToDelete);
				items.addAll(info.filteredFilesToMerge);
			}

			boolean actionButtonHidden = status == BackupStatus.BACKUP_COMPLETE || status == BackupStatus.CONFLICTS
					&& (info == null || (info.filteredFilesToUpload.isEmpty() && info.filteredFilesToDelete.isEmpty()));
			if (!actionButtonHidden) {
				items.add(ACTION_BUTTON_TYPE);
			}
		}

		items.add(RESTORE_TYPE);
		items.add(LOCAL_BACKUP_TYPE);
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(viewGroup.getContext(), nightMode);
		switch (viewType) {
			case RESTORE_TYPE:
				View itemView = inflater.inflate(R.layout.restore_backup_card, viewGroup, false);
				return new RestoreBackupViewHolder(itemView);
			case LOCAL_BACKUP_TYPE:
				itemView = inflater.inflate(R.layout.local_backup_card, viewGroup, false);
				return new LocalBackupViewHolder(itemView);
			case HEADER_TYPE:
				itemView = inflater.inflate(R.layout.backup_status_header, viewGroup, false);
				return new HeaderStatusViewHolder(itemView);
			case WARNING_TYPE:
				itemView = inflater.inflate(R.layout.backup_warning, viewGroup, false);
				return new WarningViewHolder(itemView);
			case UPLOAD_TYPE:
				itemView = inflater.inflate(R.layout.backup_upload_item, viewGroup, false);
				return new ItemViewHolder(itemView);
			case CONFLICTS_TYPE:
				itemView = inflater.inflate(R.layout.backup_upload_item, viewGroup, false);
				return new ConflictViewHolder(itemView);
			case ACTION_BUTTON_TYPE:
				itemView = inflater.inflate(R.layout.backup_action_button, viewGroup, false);
				return new ActionButtonViewHolder(itemView);
			case INTRODUCTION_TYPE:
				itemView = inflater.inflate(R.layout.backup_introduction_card, viewGroup, false);
				return new IntroductionViewHolder(itemView);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof HeaderStatusViewHolder) {
			HeaderStatusViewHolder viewHolder = (HeaderStatusViewHolder) holder;
			viewHolder.bindView(this, status, nightMode);
		} else if (holder instanceof WarningViewHolder) {
			boolean hideBottomPadding = false;
			if (items.size() > position + 1) {
				Object item = items.get(position + 1);
				hideBottomPadding = Algorithms.objectEquals(item, ACTION_BUTTON_TYPE) || item instanceof SettingsItem || item instanceof Pair;
			}
			WarningViewHolder viewHolder = (WarningViewHolder) holder;
			viewHolder.bindView(status, error, hideBottomPadding);
		} else if (holder instanceof ConflictViewHolder) {
			Pair<LocalFile, RemoteFile> pair = (Pair<LocalFile, RemoteFile>) items.get(position);
			ConflictViewHolder viewHolder = (ConflictViewHolder) holder;
			viewHolder.bindView(pair, fragment, fragment, nightMode);
		} else if (holder instanceof ItemViewHolder) {
			boolean lastBackupItem = false;
			if (items.size() > position + 1) {
				Object item = items.get(position + 1);
				lastBackupItem = !(item instanceof SettingsItem) && !(item instanceof Pair);
			}
			SettingsItem item = (SettingsItem) items.get(position);
			ItemViewHolder viewHolder = (ItemViewHolder) holder;
			viewHolder.bindView(item, lastBackupItem, info.itemsToDelete.contains(item));
		} else if (holder instanceof ActionButtonViewHolder) {
			ActionButtonViewHolder viewHolder = (ActionButtonViewHolder) holder;
			viewHolder.bindView(mapActivity, backup, fragment, uploadItemsVisible, nightMode);
		} else if (holder instanceof LocalBackupViewHolder) {
			LocalBackupViewHolder viewHolder = (LocalBackupViewHolder) holder;
			viewHolder.bindView(mapActivity, nightMode);
		} else if (holder instanceof RestoreBackupViewHolder) {
			RestoreBackupViewHolder viewHolder = (RestoreBackupViewHolder) holder;
			viewHolder.bindView(mapActivity, nightMode);
		} else if (holder instanceof IntroductionViewHolder) {
			IntroductionViewHolder viewHolder = (IntroductionViewHolder) holder;
			viewHolder.bindView(mapActivity, fragment, backup, dialogType, nightMode);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object obj = items.get(position);
		if (Algorithms.objectEquals(obj, RESTORE_TYPE)) {
			return RESTORE_TYPE;
		} else if (Algorithms.objectEquals(obj, LOCAL_BACKUP_TYPE)) {
			return LOCAL_BACKUP_TYPE;
		} else if (Algorithms.objectEquals(obj, HEADER_TYPE)) {
			return HEADER_TYPE;
		} else if (Algorithms.objectEquals(obj, WARNING_TYPE)) {
			return WARNING_TYPE;
		} else if (Algorithms.objectEquals(obj, ACTION_BUTTON_TYPE)) {
			return ACTION_BUTTON_TYPE;
		} else if (Algorithms.objectEquals(obj, INTRODUCTION_TYPE)) {
			return INTRODUCTION_TYPE;
		} else if (obj instanceof SettingsItem) {
			return UPLOAD_TYPE;
		} else if (obj instanceof Pair) {
			return CONFLICTS_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public void onBackupExportStarted() {
		notifyItemChanged(items.indexOf(HEADER_TYPE));
		notifyItemChanged(items.indexOf(ACTION_BUTTON_TYPE));
	}

	@Override
	public void onBackupExportProgressUpdate(int value) {
		notifyItemChanged(items.indexOf(HEADER_TYPE));
	}

	@Override
	public void onBackupExportFinished(@Nullable String error) {
		notifyItemChanged(items.indexOf(HEADER_TYPE));
	}

	@Override
	public void onBackupExportItemStarted(@NonNull String type, @NonNull String fileName, int max) {
		SettingsItem item = getSettingsItem(type, fileName);
		if (item != null) {
			notifyItemChanged(items.indexOf(item));
		}
	}

	@Override
	public void onBackupExportItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		SettingsItem item = getSettingsItem(type, fileName);
		if (item != null) {
			notifyItemChanged(items.indexOf(item));
		}
	}

	@Override
	public void onBackupExportItemFinished(@NonNull String type, @NonNull String fileName) {
		SettingsItem item = getSettingsItem(type, fileName);
		if (item != null) {
			notifyItemChanged(items.indexOf(item));
		}
	}

	@Nullable
	private SettingsItem getSettingsItem(@NonNull String type, @NonNull String fileName) {
		if (info != null) {
			for (LocalFile file : info.filteredFilesToUpload) {
				if (file.item != null && BackupHelper.applyItem(file.item, type, fileName)) {
					return file.item;
				}
			}
			for (RemoteFile file : info.filteredFilesToDelete) {
				if (file.item != null && BackupHelper.applyItem(file.item, type, fileName)) {
					return file.item;
				}
			}
			for (Pair<LocalFile, RemoteFile> pair : info.filteredFilesToMerge) {
				SettingsItem item = pair.first.item;
				if (item != null && BackupHelper.applyItem(item, type, fileName)) {
					return item;
				}
			}
		}
		return null;
	}

	@Override
	public void onFilesDeleteStarted(@NonNull List<RemoteFile> files) {

	}

	@Override
	public void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
		SettingsItem item = getSettingsItem(file.getType(), file.getName());
		if (item != null) {
			notifyItemChanged(items.indexOf(item));
		}
	}

	@Override
	public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {

	}

	@Override
	public void onFilesDeleteError(int status, @NonNull String message) {

	}

	@Override
	public void onImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {

	}
}