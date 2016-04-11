package net.osmand.plus.audionotes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.animation.AnimatorCompatHelper;
import android.support.v4.animation.AnimatorUpdateListenerCompat;
import android.support.v4.animation.ValueAnimatorCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.lang.ref.WeakReference;

public class AudioVideoNoteRecordingMenuFullScreen extends AudioVideoNoteRecordingMenu {

	protected ImageView imageview;
	protected ProgressBar progressBar;
	protected ValueAnimatorCompat animatorCompat;

	public AudioVideoNoteRecordingMenuFullScreen(AudioVideoNotesPlugin plugin, double lat, double lon) {
		super(plugin, lat, lon);
		imageview = (ImageView) view.findViewById(R.id.imageview);
		progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
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
		leftButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				plugin.shootAgain();
			}
		});

		View centerButtonView = view.findViewById(R.id.centerButtonView);
		centerButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopProgress();
				finishRecording();
			}
		});

		View rightButtonView = view.findViewById(R.id.rightButtonView);
		rightButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				plugin.finishPhotoRecording(true);
				recExternal(plugin.getMapActivity());
			}
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

	public void showFinalPhoto(final byte[] data, long duration) {
		setImage(data);
		imageview.setVisibility(View.VISIBLE);
		viewfinder.setVisibility(View.GONE);

		startProgress(duration);
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
		ViewCompat.setAlpha(progressBar, 1f);
		progressBar.setVisibility(View.VISIBLE);

		animatorCompat = AnimatorCompatHelper.emptyValueAnimator();
		final Interpolator interpolator = new LinearInterpolator();
		animatorCompat.setDuration(duration);
		animatorCompat.setTarget(progressBar);
		animatorCompat.addUpdateListener(new AnimatorUpdateListenerCompat() {
			@Override
			public void onAnimationUpdate(ValueAnimatorCompat animation) {
				float fraction = interpolator.getInterpolation(animation.getAnimatedFraction());
				progressBar.setProgress((int)(500 * fraction));
			}
		});
		animatorCompat.start();
	}

	private void stopProgress() {
		if (animatorCompat != null)
			animatorCompat.cancel();
	}

	private void setImage(final byte[] data) {
		Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
		DisplayMetrics dm = new DisplayMetrics();
		getMapActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

		//imageview.setMinimumHeight(dm.heightPixels);
		//imageview.setMinimumWidth(dm.widthPixels);
		imageview.setImageBitmap(bm);
	}
}
