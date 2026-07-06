package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.mediastorage.MediaSource;
import net.osmand.shared.media.LinkMediaFactory;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class AttachedMediaSettingsItem extends FileSettingsItem {

	private final MediaSource source;
	private final Set<String> hrefKeys = new LinkedHashSet<>();
	private final String rewrittenHref;

	public AttachedMediaSettingsItem(@NonNull OsmandApplication app, @NonNull MediaSource source, @NonNull String targetFileName) throws IllegalArgumentException {
		super(app, new File(app.getAppPath(IndexConstants.AV_INDEX_DIR), targetFileName));
		this.source = source;
		this.subtype = FileSubtype.ATTACHED_MEDIA;
		this.rewrittenHref = LinkMediaFactory.createInternalMediaUri(targetFileName);

		for (String key : source.getHrefKeys()) {
			addHrefKey(key);
		}
	}

	@NonNull
	public String getRewrittenHref() {
		return rewrittenHref;
	}

	@NonNull
	public Set<String> getHrefKeys() {
		return Collections.unmodifiableSet(hrefKeys);
	}

	public void addHrefKey(@Nullable String href) {
		if (!Algorithms.isEmpty(href)) {
			hrefKeys.add(href.trim());
		}
	}

	@Override
	public long getSize() {
		long length = source.getLength();
		return length > 0 ? length : super.getSize();
	}

	@Override
	public long getLocalModifiedTime() {
		long lastModified = source.getLastModified();
		return lastModified > 0 ? lastModified : super.getLocalModifiedTime();
	}

	@Override
	public boolean exists() {
		// The export target is synthetic; source readability is checked when the stream is opened.
		return true;
	}

	@DrawableRes
	public int getIconId() {
		switch (source.getDirType()) {
			case AUDIO:
				return R.drawable.ic_action_micro_dark;
			case VIDEO:
				return R.drawable.ic_action_video_dark;
			case PHOTO:
			default:
				return R.drawable.ic_action_photo_dark;
		}
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return source.getFileName();
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return new SettingsItemWriter<>(this) {
			@Override
			public void writeToStream(@NonNull OutputStream outputStream, @Nullable IProgress progress) throws IOException {
				AttachedMediaSettingsItem item = getItem();
				int bytesDivisor = 1024;
				if (progress != null) {
					progress.startWork((int) (item.getSize() / bytesDivisor));
				}
				if (PluginsHelper.isDevelopment()) {
					SettingsHelper.LOG.debug("Attached media export: source=" + item.source.getId() + ", target=" + item.getFileName() + ", name=" + item.source.getFileName() + ", size=" + item.getSize());
				}
				try (InputStream inputStream = item.source.openInputStream()) {
					Algorithms.streamCopy(inputStream, outputStream, progress, bytesDivisor);
				} catch (Exception e) {
					item.warnings.add(item.app.getString(R.string.settings_item_read_error, item.source.getFileName()));
					SettingsHelper.LOG.error("Failed to stream attached media: " + item.source.getId(), e);
					throw e instanceof IOException ? (IOException) e : new IOException(e);
				} finally {
					if (progress != null) {
						progress.finishTask();
					}
				}
			}
		};
	}
}
