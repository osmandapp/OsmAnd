package net.osmand.plus.plugins.audionotes;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.lang.ref.WeakReference;

public class AudioVideoNoteRecordingMenuFullScreen extends AudioVideoNoteRecordingMenu {

	protected ImageView imageview;
	protected ProgressBar progressBar;
	protected ValueAnimator animatorCompat;

	public AudioVideoNoteRecordingMenuFullScreen(AudioVideoNotesPlugin plugin, double lat, double lon) {
		super(plugin, lat, lon);
		imageview = view.findViewById(R.id.imageview);
		progressBar = view.findViewById(R.id.progressBar);
	}

	protected void initView(MapActivity mapActivity) {
		mapActivity.getContextMenu().hide();
		AudioVideoNoteRecordingMenuFullScreenFragment.showInstance(this);
		WeakReference<AudioVideoNoteRecordingMenuFullScreenFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			view = fragmentRef.get().getView();
		}
		if (view == null) {
			super.initView(mapActivity);
		}
	}

	@Override
	protected void applyViewfinderVisibility() {
		imageview.setVisibility(View.GONE);
		progressBar.setVisibility(View.INVISIBLE);
	}

	public void show() {
	}

	public void hide() {
		stopProgress();
		plugin.stopCamera();
		WeakReference<AudioVideoNoteRecordingMenuFullScreenFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		}
	}

	public void update() {
		View leftButtonView = view.findViewById(R.id.leftButtonView);
		leftButtonView.setOnClickListener(v -> plugin.shootAgain());

		View centerButtonView = view.findViewById(R.id.centerButtonView);
		centerButtonView.setOnClickListener(v -> {
			stopProgress();
			finishRecording();
		});

		View rightButtonView = view.findViewById(R.id.rightButtonView);
		rightButtonView.setOnClickListener(v -> {
			plugin.finishPhotoRecording(true);
			recExternal(requireMapActivity());
		});
	}

	public void finishRecording() {
		plugin.finishPhotoRecording(false);
	}

	public WeakReference<AudioVideoNoteRecordingMenuFullScreenFragment> findMenuFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(AudioVideoNoteRecordingMenuFullScreenFragment.TAG);
			if (fragment != null && !fragment.isDetached()) {
				return new WeakReference<>((AudioVideoNoteRecordingMenuFullScreenFragment) fragment);
			}
		}
		return null;
	}

	public void showFinalPhoto(byte[] jpeg, long duration) {
		if (getMapActivity() != null) {
			setImage(jpeg);
			imageview.setVisibility(View.VISIBLE);
			viewfinder.setVisibility(View.GONE);

			startProgress(duration);
		}
	}

	public void hideFinalPhoto() {
		stopProgress();

		AudioVideoNotesPlugin.CurrentRecording recording = plugin.getCurrentRecording();
		boolean show = showViewfinder && recording != null && recording.getType() != AudioVideoNotesPlugin.AVActionType.REC_AUDIO;
		imageview.setVisibility(View.GONE);
		progressBar.setVisibility(View.INVISIBLE);
		viewfinder.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	private void startProgress(long duration) {
		stopProgress();

		progressBar.setProgress(0);
		progressBar.setAlpha(1f);
		progressBar.setVisibility(View.VISIBLE);

		animatorCompat = ValueAnimator.ofInt(0);
		Interpolator interpolator = new LinearInterpolator();
		animatorCompat.setDuration(duration);
		animatorCompat.setTarget(progressBar);
		animatorCompat.addUpdateListener(valueAnimator -> {
			float fraction = interpolator.getInterpolation(valueAnimator.getAnimatedFraction());
			progressBar.setProgress((int)(500 * fraction));
		});
		animatorCompat.start();
	}

	private void stopProgress() {
		if (animatorCompat != null)
			animatorCompat.cancel();
	}

	private void setImage(byte[] jpeg) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
			DisplayMetrics dm = new DisplayMetrics();
			Display display = mapActivity.getWindowManager().getDefaultDisplay();
			display.getMetrics(dm);

			int imageOrientation = getOrientation(jpeg);

			imageview.setMinimumHeight(dm.heightPixels);
			imageview.setMinimumWidth(dm.widthPixels);
			bmp = rotateBitmap(bmp, imageOrientation, dm.widthPixels, dm.heightPixels);
			imageview.setImageBitmap(bmp);
		}
	}

	private static Bitmap rotateBitmap(Bitmap src, int angle, int screenWidth, int screenHeight) {
		float srcWidth = (float) src.getWidth();
		float srcHeight = (float) src.getHeight();
		float[] srcRes = {srcWidth, srcHeight};

		Matrix mat = new Matrix();
		mat.setRotate(angle);
		mat.mapPoints(srcRes);
		srcWidth = Math.abs(srcRes[0]);
		srcHeight = Math.abs(srcRes[1]);
		float k = Math.min(screenWidth / srcWidth, screenHeight / srcHeight);
		mat.reset();
		mat.preScale(k, k);
		mat.postRotate(angle);

		return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), mat, true);
	}

	// Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
	public static int getOrientation(byte[] jpeg) {
		if (jpeg == null) {
			return 0;
		}

		int offset = 0;
		int length = 0;

		// ISO/IEC 10918-1:1993(E)
		while (offset + 3 < jpeg.length && (jpeg[offset++] & 0xFF) == 0xFF) {
			int marker = jpeg[offset] & 0xFF;

			// Check if the marker is a padding.
			if (marker == 0xFF) {
				continue;
			}
			offset++;

			// Check if the marker is SOI or TEM.
			if (marker == 0xD8 || marker == 0x01) {
				continue;
			}
			// Check if the marker is EOI or SOS.
			if (marker == 0xD9 || marker == 0xDA) {
				break;
			}

			// Get the length and check if it is reasonable.
			length = pack(jpeg, offset, 2, false);
			if (length < 2 || offset + length > jpeg.length) {
				return 0;
			}

			// Break if the marker is EXIF in APP1.
			if (marker == 0xE1 && length >= 8 &&
					pack(jpeg, offset + 2, 4, false) == 0x45786966 &&
					pack(jpeg, offset + 6, 2, false) == 0) {
				offset += 8;
				length -= 8;
				break;
			}

			// Skip other markers.
			offset += length;
			length = 0;
		}

		// JEITA CP-3451 Exif Version 2.2
		if (length > 8) {
			// Identify the byte order.
			int tag = pack(jpeg, offset, 4, false);
			if (tag != 0x49492A00 && tag != 0x4D4D002A) {
				return 0;
			}
			boolean littleEndian = (tag == 0x49492A00);

			// Get the offset and check if it is reasonable.
			int count = pack(jpeg, offset + 4, 4, littleEndian) + 2;
			if (count < 10 || count > length) {
				return 0;
			}
			offset += count;
			length -= count;

			// Get the count and go through all the elements.
			count = pack(jpeg, offset - 2, 2, littleEndian);
			while (count-- > 0 && length >= 12) {
				// Get the tag and check if it is orientation.
				tag = pack(jpeg, offset, 2, littleEndian);
				if (tag == 0x0112) {
					// We do not really care about type and count, do we?
					int orientation = pack(jpeg, offset + 8, 2, littleEndian);
					switch (orientation) {
						case 1:
							return 0;
						case 3:
							return 180;
						case 6:
							return 90;
						case 8:
							return 270;
					}
					return 0;
				}
				offset += 12;
				length -= 12;
			}
		}

		return 0;
	}

	private static int pack(byte[] bytes, int offset, int length,
							boolean littleEndian) {
		int step = 1;
		if (littleEndian) {
			offset += length - 1;
			step = -1;
		}

		int value = 0;
		while (length-- > 0) {
			value = (value << 8) | (bytes[offset] & 0xFF);
			offset += step;
		}
		return value;
	}
}
