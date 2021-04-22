package net.osmand.plus.backup;

import net.osmand.util.Algorithms;

import java.io.File;

public class GpxFileInfo {
	public File file;
	public String subfolder;
	public long uploadTime = 0;

	private String name = null;
	private int sz = -1;
	private String fileName = null;

	public String getName() {
		if (name == null) {
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

	public String getFileName(boolean includeSubfolder) {
		String result;
		if (fileName != null) {
			result = fileName;
		} else {
			if (file == null) {
				result = "";
			} else {
				result = fileName = file.getName();
			}
		}
		if (includeSubfolder && !Algorithms.isEmpty(subfolder)) {
			result = subfolder + "/" + result;
		}
		return result;
	}
}
