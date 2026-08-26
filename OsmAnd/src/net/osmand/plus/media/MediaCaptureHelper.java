package net.osmand.plus.media;

import static net.osmand.shared.media.MediaFileNameFormat.IMG_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.MPEG4_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.THREEGP_EXTENSION;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.media.MediaFileNameFormat;

import java.io.File;

public class MediaCaptureHelper {

	private final AudioRecorder audioRecorder;
	private final CameraRecorder cameraRecorder;

	private final File targetDir;

	public MediaCaptureHelper(@NonNull OsmandApplication app, @NonNull File targetDir) {
		this.targetDir = targetDir;
		this.audioRecorder = new AudioRecorder(app);
		this.cameraRecorder = new CameraRecorder(app);
	}

	@NonNull
	public File getTargetDir() {
		return targetDir;
	}

	@NonNull
	public AudioRecorder getAudioRecorder() {
		return audioRecorder;
	}

	@NonNull
	public CameraRecorder getCameraRecorder() {
		return cameraRecorder;
	}

	@NonNull
	public File createAudioFile(double lat, double lon) {
		return getBaseFileName(lat, lon, targetDir, THREEGP_EXTENSION);
	}

	@NonNull
	public File createPhotoFile(double lat, double lon) {
		return getBaseFileName(lat, lon, targetDir, IMG_EXTENSION);
	}

	@NonNull
	public File createVideoFile(double lat, double lon) {
		return getBaseFileName(lat, lon, targetDir, MPEG4_EXTENSION);
	}

	@NonNull
	public static File getBaseFileName(double lat, double lon, @NonNull File dir, @NonNull String ext) {
		dir.mkdirs();
		String fileName = MediaFileNameFormat.createUniqueMediaFileName(lat, lon, ext, name -> new File(dir, name).exists());
		return new File(dir, fileName);
	}

	@NonNull
	public Intent createVideoCaptureIntent(@NonNull Context context, @NonNull File file) {
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		Uri fileUri = AndroidUtils.getUriForFile(context, file);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the output file name
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
		return intent;
	}

	@NonNull
	public Intent createPhotoCaptureIntent(@NonNull Context context, @NonNull File file) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		Uri uri = AndroidUtils.getUriForFile(context, file);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		return intent;
	}
}