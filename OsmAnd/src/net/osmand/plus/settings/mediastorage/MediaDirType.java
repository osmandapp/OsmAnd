package net.osmand.plus.settings.mediastorage;

import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.DIRECTORY_MUSIC;
import static android.os.Environment.DIRECTORY_PICTURES;
import static net.osmand.shared.media.MediaFileNameFormat.IMG_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.MPEG4_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.THREEGP_EXTENSION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.media.domain.MediaType;

public enum MediaDirType {

	PHOTO("Photo", DIRECTORY_PICTURES, IMG_EXTENSION),
	AUDIO("Audio", DIRECTORY_MUSIC, THREEGP_EXTENSION),
	VIDEO("Video", DIRECTORY_MOVIES, MPEG4_EXTENSION);

	private final String dirName;
	private final String extension;
	private final String standardDir;

	MediaDirType(@NonNull String dirName, @NonNull String standardDir, @NonNull String extension) {
		this.dirName = dirName;
		this.extension = extension;
		this.standardDir = standardDir;
	}

	@NonNull
	public String getExtension() {
		return extension;
	}

	@NonNull
	public String getDirName() {
		return dirName;
	}

	@NonNull
	public String getStandardDir() {
		return standardDir;
	}

	@NonNull
	public static MediaDirType fromExtension(@NonNull String extension) {
		MediaDirType dirType = getDirTypeByExtension(extension);
		return dirType != null ? dirType : PHOTO;
	}

	@NonNull
	public static MediaDirType fromMimeTypeOrExtension(@Nullable String mimeType,
			@Nullable String extension) {
		MediaDirType dirType = getDirTypeByMimeType(mimeType);
		if (dirType == null) {
			dirType = getDirTypeByExtension(extension);
		}
		return dirType != null ? dirType : PHOTO;
	}

	public static boolean isSupportedExtension(@Nullable String extension) {
		return getDirTypeByExtension(extension) != null;
	}

	@Nullable
	private static MediaDirType getDirTypeByMimeType(@Nullable String mimeType) {
		return fromMediaType(MediaType.fromMimeType(mimeType));
	}

	@Nullable
	private static MediaDirType getDirTypeByExtension(@Nullable String extension) {
		return fromMediaType(MediaType.fromExtension(extension));
	}

	@Nullable
	private static MediaDirType fromMediaType(@NonNull MediaType mediaType) {
		return switch (mediaType) {
			case PHOTO -> PHOTO;
			case AUDIO -> AUDIO;
			case VIDEO -> VIDEO;
			default -> null;
		};
	}
}