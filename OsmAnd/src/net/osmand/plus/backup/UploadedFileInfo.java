package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public class UploadedFileInfo {

	private final String type;
	private final String name;
	private long uploadTime;
	private String md5Digest = "";

	public UploadedFileInfo(@NonNull String type, @NonNull String name) {
		this(type, name, 0, "");
	}

	public UploadedFileInfo(@NonNull String type, @NonNull String name, long uploadTime) {
		this(type, name, uploadTime, "");
	}

	public UploadedFileInfo(@NonNull String type, @NonNull String name, long uploadTime, @NonNull String md5Digest) {
		this.type = type;
		this.name = name;
		this.uploadTime = uploadTime;
		this.md5Digest = md5Digest;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public long getUploadTime() {
		return uploadTime;
	}

	public void setUploadTime(long uploadTime) {
		this.uploadTime = uploadTime;
	}

	@Nullable
	public String getMd5Digest() {
		return md5Digest;
	}

	public void setMd5Digest(@NonNull String md5Digest) {
		this.md5Digest = md5Digest;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UploadedFileInfo that = (UploadedFileInfo) o;
		return type.equals(that.type) &&
				name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(type, name);
	}

	@NonNull
	@Override
	public String toString() {
		return "UploadedFileInfo{type=" + type + ", name=" + name + ", uploadTime=" + uploadTime + '}';
	}
}
