package net.osmand.plus.media;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.AudioAttributes;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

public class CameraRecorder {

	private static final Log log = PlatformUtil.getLog(CameraRecorder.class);

	// video output format
	public static final int VIDEO_OUTPUT_MP4 = 0;
	public static final int VIDEO_OUTPUT_3GP = 1;
	public static final int VIDEO_QUALITY_DEFAULT = CamcorderProfile.QUALITY_HIGH; // High (highest res)

	// camera picture size
	public static final int AV_PHOTO_SIZE_DEFAULT = -1;

	// camera focus type
	public static final int AV_CAMERA_FOCUS_AUTO = 0;
	public static final int AV_CAMERA_FOCUS_HIPERFOCAL = 1;
	public static final int AV_CAMERA_FOCUS_EDOF = 2;
	public static final int AV_CAMERA_FOCUS_INFINITY = 3;
	public static final int AV_CAMERA_FOCUS_MACRO = 4;
	public static final int AV_CAMERA_FOCUS_CONTINUOUS = 5;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public final CommonPreference<Integer> AV_VIDEO_FORMAT;
	public final CommonPreference<Integer> AV_VIDEO_QUALITY;
	public final CommonPreference<Integer> AV_CAMERA_PICTURE_SIZE;
	public final CommonPreference<Integer> AV_CAMERA_FOCUS_TYPE;
	public final CommonPreference<Boolean> AV_PHOTO_PLAY_SOUND;

	// runtime default picture-size index (set from the settings screen)
	private int cameraPictureSizeDefault;

	private Camera cam;
	private List<Camera.Size> supportedPreviewSizes;
	private boolean autofocus;

	// photo shutter sound
	private SoundPool soundPool;
	private int shotId;

	public CameraRecorder(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();

		AV_VIDEO_FORMAT = settings.registerIntPreference("av_video_format", VIDEO_OUTPUT_MP4);
		AV_VIDEO_QUALITY = settings.registerIntPreference("av_video_quality", VIDEO_QUALITY_DEFAULT);
		AV_CAMERA_PICTURE_SIZE = settings.registerIntPreference("av_camera_picture_size", AV_PHOTO_SIZE_DEFAULT);
		AV_CAMERA_FOCUS_TYPE = settings.registerIntPreference("av_camera_focus_type", AV_CAMERA_FOCUS_AUTO);
		AV_PHOTO_PLAY_SOUND = settings.registerBooleanPreference("av_photo_play_sound", true);
	}

	// region Camera lifecycle

	public void openCamera() {
		if (cam != null) {
			try {
				cam.release();
				cam = null;
			} catch (Exception e) {
				logErr(e);
			}
		}
		try {
			cam = Camera.open();
			if (supportedPreviewSizes == null) {
				supportedPreviewSizes = cam.getParameters().getSupportedPreviewSizes();
			}
		} catch (Exception e) {
			logErr(e);
		}
	}

	public void closeCamera() {
		if (cam != null) {
			try {
				cam.release();
			} catch (Exception e) {
				logErr(e);
			}
			cam = null;
		}
	}

	public void stopCamera() {
		try {
			if (cam != null) {
				cam.cancelAutoFocus();
				cam.stopPreview();
				cam.setPreviewDisplay(null);
			}
		} catch (Exception e) {
			logErr(e);
		} finally {
			closeCamera();
		}
	}

	public boolean hasCamera() {
		return cam != null;
	}

	@Nullable
	public Camera getCamera() {
		return cam;
	}

	public void lockCamera() {
		if (cam != null) {
			cam.lock();
		}
	}

	public void unlockCamera() {
		if (cam != null) {
			cam.unlock();
		}
	}

	public void startCamera(@Nullable Camera.Size previewSize, @Nullable SurfaceHolder holder,
			int rotation) throws IOException {
		Parameters parameters = cam.getParameters();

		// camera focus type
		List<String> sfm = parameters.getSupportedFocusModes();
		if (sfm.contains("continuous-video")) {
			parameters.setFocusMode("continuous-video");
		}

		int cameraOrientation = getPreviewOrientation(rotation);
		cam.setDisplayOrientation(cameraOrientation);
		parameters.set("rotation", cameraOrientation);
		if (previewSize != null) {
			parameters.setPreviewSize(previewSize.width, previewSize.height);
		}
		cam.setParameters(parameters);
		if (holder != null) {
			cam.setPreviewDisplay(holder);
		}
		cam.startPreview();
	}

	// region Video

	/**
	 * Configures an existing {@link MediaRecorder} for video capture. The caller
	 * is responsible for the preceding {@code cam.unlock()} / {@code mr.setCamera(cam)}
	 * sequence so the exact ordering is preserved.
	 */
	public void configureVideoRecorder(@NonNull MediaRecorder mr, @NonNull CamcorderProfile profile, @NonNull File file, int rotation) {
		mr.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		applyVideoOrientationHint(mr, rotation);
		mr.setProfile(profile);
		mr.setOutputFile(file.getAbsolutePath());
	}

	private static void applyVideoOrientationHint(@NonNull MediaRecorder mr, int rotation) {
		try {
			Method m = mr.getClass().getDeclaredMethod("setOrientationHint", Integer.TYPE);
			if (rotation == Surface.ROTATION_0) {
				m.invoke(mr, 90);
			} else if (rotation == Surface.ROTATION_270) {
				m.invoke(mr, 180);
			} else if (rotation == Surface.ROTATION_180) {
				m.invoke(mr, 270);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Nullable
	public Camera.Size getPreviewSize(boolean landscape) {
		CamcorderProfile p = CamcorderProfile.get(AV_VIDEO_QUALITY.get());
		Camera.Size previewSize;
		if (supportedPreviewSizes != null) {
			int width;
			int height;
			if (landscape) {
				width = p.videoFrameWidth;
				height = p.videoFrameHeight;
			} else {
				height = p.videoFrameWidth;
				width = p.videoFrameHeight;
			}
			previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
		} else {
			previewSize = null;
		}
		return previewSize;
	}

	// region Photo

	/**
	 * Resolves the configured picture size from the supported list, falling back
	 * to the runtime default index.
	 */
	@NonNull
	public Camera.Size resolvePictureSize(@NonNull Parameters parameters) {
		List<Camera.Size> psps = parameters.getSupportedPictureSizes();
		int camPicSizeIndex = AV_CAMERA_PICTURE_SIZE.get();
		if (camPicSizeIndex == AV_PHOTO_SIZE_DEFAULT) {
			camPicSizeIndex = cameraPictureSizeDefault;
		}
		return psps.get(camPicSizeIndex);
	}

	@Nullable
	public Camera.Size getOptimalPreviewSize(int width, int height) {
		return supportedPreviewSizes != null ? getOptimalPreviewSize(supportedPreviewSizes, width, height) : null;
	}

	/**
	 * Applies the photo capture parameters (picture size, focus mode, white
	 * balance, orientation, preview size) and starts the preview. Updates the
	 * autofocus flag exposed via {@link #isAutofocus()}.
	 */
	public void applyPhotoParameters(@NonNull Parameters parameters, @NonNull Camera.Size pictureSize,
			@Nullable Camera.Size previewSize, @Nullable SurfaceHolder holder, int rotation) throws IOException {
		parameters.setPictureSize(pictureSize.width, pictureSize.height);

		// camera focus type
		autofocus = true;
		parameters.removeGpsData();
		switch (AV_CAMERA_FOCUS_TYPE.get()) {
			case AV_CAMERA_FOCUS_HIPERFOCAL:
				parameters.setFocusMode(Parameters.FOCUS_MODE_FIXED);
				autofocus = false;
				break;
			case AV_CAMERA_FOCUS_EDOF:
				parameters.setFocusMode(Parameters.FOCUS_MODE_EDOF);
				autofocus = false;
				break;
			case AV_CAMERA_FOCUS_INFINITY:
				parameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
				autofocus = false;
				break;
			case AV_CAMERA_FOCUS_MACRO:
				parameters.setFocusMode(Parameters.FOCUS_MODE_MACRO);
				break;
			case AV_CAMERA_FOCUS_CONTINUOUS:
				parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				break;
			default:
				parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
				break;
		}

		if (parameters.getSupportedWhiteBalance() != null
				&& parameters.getSupportedWhiteBalance().contains(Parameters.WHITE_BALANCE_AUTO)) {
			parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
		}
		if (parameters.getSupportedFlashModes() != null
				&& parameters.getSupportedFlashModes().contains(Parameters.FLASH_MODE_AUTO)) {
			//parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
		}

		int cameraOrientation = getPreviewOrientation(rotation);
		cam.setDisplayOrientation(cameraOrientation);
		parameters.set("rotation", cameraOrientation);
		if (previewSize != null) {
			parameters.setPreviewSize(previewSize.width, previewSize.height);
		}
		cam.setParameters(parameters);
		cam.setPreviewDisplay(holder);
		cam.startPreview();
	}

	public boolean isAutofocus() {
		return autofocus;
	}

	public void takePicture(@NonNull Camera.PictureCallback callback) {
		if (cam != null) {
			cam.takePicture(null, null, callback);
		}
	}

	public void autoFocus(@NonNull Camera.AutoFocusCallback callback) {
		if (cam != null) {
			cam.autoFocus(callback);
		}
	}

	public void cancelAutoFocus() {
		if (cam != null) {
			cam.cancelAutoFocus();
		}
	}

	public void stopPreview() {
		if (cam != null) {
			cam.stopPreview();
		}
	}

	public void startPreview() {
		if (cam != null) {
			cam.startPreview();
		}
	}

	// region Orientation / sizing helpers

	/**
	 * Returns the current display rotation ({@link Surface} {@code ROTATION_*}) for
	 * the given activity. Provided here so capture callers can obtain the rotation
	 * value the camera/video helpers expect without duplicating the lookup.
	 */
	public static int getDisplayRotation(@NonNull Activity activity) {
		return activity.getWindowManager().getDefaultDisplay().getRotation();
	}

	private static int getPreviewOrientation(int rotation) {
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info);
		int degrees = switch (rotation) {
			case Surface.ROTATION_0 -> 0;
			case Surface.ROTATION_90 -> 90;
			case Surface.ROTATION_180 -> 180;
			case Surface.ROTATION_270 -> 270;
			default -> 0;
		};

		int result;
		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	@Nullable
	private static Camera.Size getOptimalPreviewSize(@Nullable List<Camera.Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio;
		if (w > h) {
			targetRatio = (double) w / h;
		} else {
			targetRatio = (double) h / w;
		}

		if (sizes == null) return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - h) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - h);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - h) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - h);
				}
			}
		}
		return optimalSize;
	}

	public int getCameraPictureSizeDefault() {
		return cameraPictureSizeDefault;
	}

	public void setCameraPictureSizeDefault(int cameraPictureSizeDefault) {
		this.cameraPictureSizeDefault = cameraPictureSizeDefault;
	}

	// region Shutter sound

	public void loadCameraSoundIfNeeded(boolean checkSoundPool) {
		if (isShutterSoundEnabled() && (!checkSoundPool || soundPool == null)) {
			loadCameraSound();
		}
	}

	private void loadCameraSound() {
		if (soundPool == null) {
			AudioAttributes attr = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build();
			soundPool = new SoundPool.Builder().setAudioAttributes(attr).setMaxStreams(5).build();
		}
		if (shotId == 0) {
			try {
				AssetFileDescriptor assetFileDescriptor = app.getAssets().openFd("sounds/camera_click.ogg");
				shotId = soundPool.load(assetFileDescriptor, 1);
				assetFileDescriptor.close();
			} catch (Exception e) {
				log.error("cannot get shotId for sounds/camera_click.ogg");
			}
		}
	}

	public boolean isShutterSoundEnabled() {
		return AV_PHOTO_PLAY_SOUND.get();
	}

	public static boolean canDisableShutterSound() {
		// Preserve the legacy behavior during extraction; changing this would make
		// the shutter-sound preference start affecting devices where it is hidden today.
		CameraInfo info = new CameraInfo();
		return info.canDisableShutterSound;
	}

	public void applyShutterSoundSetting() {
		if (cam != null && canDisableShutterSound()) {
			cam.enableShutterSound(isShutterSoundEnabled());
		}
	}

	public void playShutterSound() {
		if (isShutterSoundEnabled() && soundPool != null && shotId != 0) {
			soundPool.play(shotId, 0.7f, 0.7f, 0, 0, 1);
		}
	}

	public void releaseSound() {
		if (soundPool != null) {
			soundPool.release();
			soundPool = null;
			shotId = 0;
		}
	}

	private void logErr(@NonNull Exception e) {
		log.error("Error with camera ", e);
		app.showToastMessage(app.getString(R.string.recording_error) + " : " + e.getMessage());
	}
}