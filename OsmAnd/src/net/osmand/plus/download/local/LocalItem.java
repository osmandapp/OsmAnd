package net.osmand.plus.download.local;

import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.FileUtils;

import java.io.File;

public class LocalItem extends BaseLocalItem implements Comparable<LocalItem> {

	private final File file;
	private final String path;
	private final String fileName;
	private final long size;

	@Nullable
	private Object attachedObject;
	private long lastModified;


	public LocalItem(@NonNull File file, @NonNull LocalItemType type) {
		super(type);
		this.file = file;
		this.fileName = file.getName();
		this.path = file.getAbsolutePath();
		this.size = file.length();
		this.lastModified = file.lastModified();
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@NonNull
	public String getPath() {
		return path;
	}

	@NonNull
	public String getFileName() {
		return fileName;
	}

	@Override
	public long getSize() {
		return size;
	}

	public boolean isBackuped(@NonNull OsmandApplication app) {
		File backupDir = FileUtils.getExistingDir(app, BACKUP_INDEX_DIR);
		File hiddenBackupDir = app.getAppInternalPath(IndexConstants.HIDDEN_BACKUP_DIR);
		return path.startsWith(backupDir.getAbsolutePath()) || path.startsWith(hiddenBackupDir.getAbsolutePath());
	}

	public boolean isHidden(@NonNull OsmandApplication app) {
		File hiddenDir = app.getAppInternalPath(IndexConstants.HIDDEN_DIR);
		return path.startsWith(hiddenDir.getAbsolutePath());
	}

	@Nullable
	public Object getAttachedObject() {
		return attachedObject;
	}

	public void setAttachedObject(@Nullable Object object) {
		this.attachedObject = object;
	}

	@Override
	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	@NonNull
	public CharSequence getName(@NonNull Context context) {
		return LocalItemUtils.getItemName(context, this);
	}

	@NonNull
	public String getDescription(@NonNull Context context) {
		return LocalItemUtils.getItemDescription(context, this);
	}

	@Override
	public int compareTo(LocalItem item) {
		return fileName.compareTo(item.fileName);
	}

	@NonNull
	@Override
	public String toString() {
		return fileName;
	}
}
