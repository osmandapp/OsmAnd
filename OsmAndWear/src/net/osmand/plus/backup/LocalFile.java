package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.io.File;

public class LocalFile {

	@Nullable
	public File file;
	public String fileName;
	public long uploadTime;
	public long localModifiedTime;

	private String name;
	private int sz = -1;

	public SettingsItem item;

	public String getName() {
		if (name == null && file != null) {
			name = formatName(file.getName());
		}
		return name;
	}

	private String formatName(String name) {
		int ext = name.lastIndexOf('.');
		if (ext != -1) {
			name = name.substring(0, ext);
		}
		return name.replace('_', ' ');
	}

	// Usage: AndroidUtils.formatSize(v.getContext(), getSize() * 1024l);
	public int getSize() {
		if (sz == -1) {
			if (file == null) {
				return -1;
			}
			sz = (int) ((file.length() + 512) >> 10);
		}
		return sz;
	}

	public long getFileDate() {
		if (file == null) {
			return 0;
		}
		return file.lastModified();
	}

	public String getFileName() {
		String result;
		if (fileName != null) {
			result = fileName;
		} else if (file == null) {
			result = "";
		} else {
			result = fileName = file.getName();
		}
		return result;
	}

	public String getTypeFileName() {
		String type = item != null ? item.getType().name() : "";
		String fileName = getFileName();
		if (!Algorithms.isEmpty(fileName)) {
			return type + (fileName.charAt(0) == '/' ? fileName : "/" + fileName);
		} else {
			return type;
		}
	}

	@NonNull
	@Override
	public String toString() {
		return getFileName() + " (" + (file != null ? file.length() : -1) + ") localTime=" + localModifiedTime + " uploadTime=" + uploadTime;
	}
}
