package net.osmand.plus.settings.mediastorage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class MediaSource {

	private final String href;
	private final String fileName;
	@Nullable
	private final String mimeType;
	private final MediaDirType dirType;
	private final Set<String> hrefKeys = new LinkedHashSet<>();
	private final long length;

	MediaSource(@NonNull String href, @NonNull String fileName, long length,
	            @Nullable String mimeType, @NonNull MediaDirType dirType) {
		this.href = href;
		this.fileName = fileName;
		this.length = Math.max(length, 0);
		this.mimeType = mimeType;
		this.dirType = dirType;
		addHrefKey(href);
	}

	@NonNull
	public String getHref() {
		return href;
	}

	@NonNull
	public String getFileName() {
		return fileName;
	}

	public long getLength() {
		return length;
	}

	@Nullable
	public String getMimeType() {
		return mimeType;
	}

	@NonNull
	public MediaDirType getDirType() {
		return dirType;
	}

	@NonNull
	public abstract InputStream openInputStream() throws IOException;

	public abstract void delete() throws IOException;

	@NonNull
	public List<String> getHrefKeys() {
		return List.copyOf(hrefKeys);
	}

	@NonNull
	public String getId() {
		return href;
	}

	protected void addHrefKey(@Nullable String href) {
		if (!Algorithms.isEmpty(href)) {
			hrefKeys.add(href);
		}
	}
}