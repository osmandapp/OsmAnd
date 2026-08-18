package net.osmand.plus.backup.ui.trash;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.BackupUiUtils;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class TrashItem {

	public RemoteFile oldFile;
	@Nullable
	public RemoteFile deletedFile;
	public boolean synced;

	public TrashItem(@NonNull RemoteFile oldFile, @Nullable RemoteFile deletedFile) {
		this.oldFile = oldFile;
		this.deletedFile = deletedFile;
	}

	public long getTime() {
		return deletedFile != null ? deletedFile.getUpdatetimems() : oldFile.getUpdatetimems();
	}

	@NonNull
	public String getName(@NonNull Context context) {
		SettingsItem item = getSettingsItem();
		return item != null ? BackupUiUtils.getItemName(context, item) : oldFile.getName();
	}

	@NonNull
	public String getDescription(@NonNull OsmandApplication app) {
		String deleted = app.getString(R.string.shared_string_deleted);
		String formattedTime = BackupUiUtils.formatPassedTime(app, getTime(), "MMM d, HH:mm", "HH:mm", "");
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, deleted, formattedTime);
	}

	@DrawableRes
	public int getIconId() {
		SettingsItem item = getSettingsItem();
		return item != null ? BackupUiUtils.getIconId(item) : -1;
	}

	@Nullable
	public SettingsItem getSettingsItem() {
		SettingsItem item = deletedFile != null ? deletedFile.item : null;
		if (item == null) {
			item = oldFile != null ? oldFile.item : null;
		}
		return item;
	}

	public boolean isLocalDeletion() {
		return deletedFile == null;
	}

	@NonNull
	public List<RemoteFile> getRemoteFiles() {
		List<RemoteFile> files = new ArrayList<>();
		files.add(oldFile);
		if (deletedFile != null) {
			files.add(deletedFile);
		}
		return files;
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(oldFile, deletedFile);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TrashItem)) return false;

		TrashItem item = (TrashItem) o;
		return Algorithms.objectEquals(oldFile, item.oldFile) && Algorithms.objectEquals(deletedFile, item.deletedFile);
	}

	@NonNull
	@Override
	public String toString() {
		return oldFile.getName();
	}
}
