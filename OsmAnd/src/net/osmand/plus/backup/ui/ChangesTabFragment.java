package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DELETE;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_CONFLICTS;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_LOCAL;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.List;

public abstract class ChangesTabFragment extends BaseOsmAndFragment {

	protected OsmandApplication app;
	protected BackupHelper backupHelper;
	protected NetworkSettingsHelper settingsHelper;

	protected ChangesAdapter adapter;
	protected RecentChangesType tabType = getChangesTabType();

	protected boolean nightMode;

	@NonNull
	public abstract RecentChangesType getChangesTabType();

	@NonNull
	public abstract List<CloudChangeItem> generateData();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
		nightMode = isNightMode(false);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_changes_tab, container, false);

		adapter = new ChangesAdapter(this, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(activity));
		recyclerView.setAdapter(adapter);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateAdapter();
	}

	private void updateAdapter() {
		if (adapter != null) {
			adapter.setCloudChangeItems(generateData());
		}
	}

	private String localizedSummaryForOperation(SyncOperationType operation, LocalFile localFile, RemoteFile remoteFile) {
		switch (operation) {
			case SYNC_OPERATION_DOWNLOAD:
				return app.getString(localFile != null ? R.string.shared_string_modified : R.string.shared_string_added);
			case SYNC_OPERATION_UPLOAD:
				return app.getString(remoteFile != null ? R.string.shared_string_modified : R.string.shared_string_added);
			case SYNC_OPERATION_DELETE:
				return app.getString(R.string.shared_string_deleted);
			default:
				return app.getString(R.string.shared_string_modified);
		}
	}

	public static String generateTimeString(OsmandApplication app, long time, String summary) {
		String never = app.getString(R.string.shared_string_never);
		if (time != -1) {
			String formattedTime = OsmAndFormatter.getFormattedPassedTime(app, time, never);
			return app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, formattedTime);
		} else {
			return app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, never);
		}
	}


	static class FileInfo {
		public LocalFile localFile;
		public RemoteFile remoteFile;
		public boolean deleted;
	}

	public static class CloudChangeItem {

		public String title;
		public String fileName;
		public String description;
		public int iconId;
		public LocalFile localFile;
		public RemoteFile remoteFile;
		public SettingsItem settingsItem;
		public SyncOperationType operation;
	}

	protected CloudChangeItem rowFromKey(String key,
	                                     SyncOperationType operation,
	                                     LocalFile localFile,
	                                     RemoteFile remoteFile) {
		SettingsItem settingsItem = getSettingsItem(localFile, remoteFile);
		long time = getTime(operation, localFile, remoteFile);
		String summary = localizedSummaryForOperation(operation, localFile, remoteFile);

		CloudChangeItem changeItem = new CloudChangeItem();
		changeItem.title = getName(settingsItem);
		changeItem.description = generateTimeString(app, time, summary);
		changeItem.iconId = getIcon(settingsItem);
		changeItem.settingsItem = settingsItem;
		changeItem.operation = operation;
		changeItem.localFile = localFile;
		changeItem.remoteFile = remoteFile;
		changeItem.fileName = Algorithms.getFileWithoutDirs(key);

		return changeItem;
	}

	private long getTime(SyncOperationType operation, LocalFile localFile, RemoteFile remoteFile) {
		long time = 0;
		if (tabType == RECENT_CHANGES_LOCAL && operation == SYNC_OPERATION_DELETE)
			time = remoteFile.getClienttimems();
		else if (tabType == RECENT_CHANGES_LOCAL || tabType == RECENT_CHANGES_CONFLICTS)
			time = localFile.localModifiedTime;
		else if (operation == SYNC_OPERATION_DELETE)
			time = localFile.uploadTime;
		else {
			time = remoteFile.getUpdatetimems();
		}
		return time;
	}

	private String getName(SettingsItem settingsItem) {
		String name = "";
		if (settingsItem instanceof ProfileSettingsItem) {
			name = ((ProfileSettingsItem) settingsItem).getAppMode().toHumanString();
		} else {
			name = settingsItem.getPublicName(app);
			if (settingsItem instanceof FileSettingsItem) {
				FileSettingsItem fileItem = (FileSettingsItem) settingsItem;
				if (fileItem.getSubtype() == FileSubtype.TTS_VOICE) {
					String suffix = app.getString(R.string.tts_title);
					name = app.getString(R.string.ltr_or_rtl_combine_via_space, name, suffix);
				} else if (fileItem.getSubtype() == FileSubtype.VOICE) {
					String suffix = app.getString(R.string.shared_string_record);
					name = app.getString(R.string.ltr_or_rtl_combine_via_space, name, suffix);
				}
			} else if (Algorithms.isEmpty(name)) {
				name = app.getString(R.string.res_unknown);
			}
		}
		return name;
	}

	private SettingsItem getSettingsItem(LocalFile localFile, RemoteFile remoteFile) {
		SettingsItem settingsItem;
		if (tabType == RECENT_CHANGES_LOCAL) {
			settingsItem = localFile.item;
			if (settingsItem == null) {
				settingsItem = remoteFile.item;
			}
		} else {
			settingsItem = remoteFile.item;
			if (settingsItem == null) {
				settingsItem = localFile.item;
			}
		}
		return settingsItem;
	}

	private int getIcon(@NonNull SettingsItem item) {
		if (item instanceof ProfileSettingsItem) {
			ProfileSettingsItem profileItem = (ProfileSettingsItem) item;
			ApplicationMode mode = profileItem.getAppMode();
			return mode.getIconRes();
		} else {
			ExportSettingsType type = ExportSettingsType.getExportSettingsTypeForItem(item);
			if (type != null) {
				return type.getIconRes();
			}
		}
		return -1;
	}
}