package net.osmand.plus.download.local;

import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.io.File;

public class LocalItem implements Comparable<LocalItem> {

	private final File file;
	private final LocalItemType type;
	private final String path;
	private final String fileName;
	private final long size;
	private final boolean backuped;

	private String name;
	private String description;
	@Nullable
	private Object attachedObject;
	private long lastModified;


	public LocalItem(@NonNull File file, @NonNull LocalItemType type) {
		this.file = file;
		this.type = type;
		this.fileName = file.getName();
		this.path = file.getAbsolutePath();
		this.size = file.length();
		this.backuped = path.contains(BACKUP_INDEX_DIR);
		this.name = Algorithms.getFileNameWithoutExtension(fileName).replace('_', ' ');
		this.lastModified = file.lastModified();
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@NonNull
	public LocalItemType getType() {
		return type;
	}

	@NonNull
	public String getPath() {
		return path;
	}

	@NonNull
	public String getFileName() {
		return fileName;
	}

	public long getSize() {
		return size;
	}

	public boolean isBackuped() {
		return backuped;
	}

	public String getName() {
		return name;
	}

	public void setName(@NonNull String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(@NonNull String description) {
		this.description = description;
	}

	@Nullable
	public Object getAttachedObject() {
		return attachedObject;
	}

	public void setAttachedObject(@Nullable Object object) {
		this.attachedObject = object;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public boolean isLoaded(@NonNull OsmandApplication app) {
		return !isBackuped() && app.getResourceManager().getIndexFileNames().containsKey(fileName);
	}

	public boolean isCorrupted() {
		if (type == TILES_DATA) {
			return file.isDirectory() && !TileSourceManager.isTileSourceMetaInfoExist(file);
		}
		return false;
	}

	@Override
	public int compareTo(LocalItem item) {
		return fileName.compareTo(item.fileName);
	}
}
