package net.osmand.plus.plugins.audionotes;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.AVActionType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.util.Algorithms;

import java.util.Timer;
import java.util.TimerTask;

public class AudioVideoNoteRecordingMenu {

	protected View view;
	protected LinearLayout viewfinder;

	protected AudioVideoNotesPlugin plugin;
	protected long startTime;
	protected Handler handler;
	protected boolean portraitMode;
	protected Timer recTimer;

	protected double lat;
	protected double lon;

	private final int screenHeight;
	private final int buttonsHeight;
	private final int statusBarHeight;

	public static boolean showViewfinder = true;

	public AudioVideoNoteRecordingMenu(AudioVideoNotesPlugin plugin, double lat, double lon) {
		this.plugin = plugin;
		this.lat = lat;
		this.lon = lon;
		handler = new Handler();

		MapActivity mapActivity = requireMapActivity();
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);

		initView(mapActivity);
		viewfinder = view.findViewById(R.id.viewfinder);
		showViewfinder = true;

		screenHeight = AndroidUtils.getScreenHeight(mapActivity);
		statusBarHeight = AndroidUtils.getStatusBarHeight(mapActivity);
		buttonsHeight = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_route_buttons_height);

		update();
	}

	@Nullable
	public MapActivity getMapActivity() {
		return plugin.getMapActivity();
	}

	@NonNull
	public MapActivity requireMapActivity() {
		return plugin.requireMapActivity();
	}

	protected void initView(MapActivity mapActivity) {
		view = mapActivity.findViewById(R.id.recording_note_layout);
	}

	public SurfaceView prepareSurfaceView() {
		return prepareSurfaceView(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	}

	public SurfaceView prepareSurfaceView(int width, int height) {
		int w = width;
		int h = height;
		if (w != ViewGroup.LayoutParams.MATCH_PARENT && h != ViewGroup.LayoutParams.MATCH_PARENT) {
			int vfw = getViewfinderWidth();
			int vfh = getViewfinderHeight();
			float vfRatio = vfw / (float) vfh;
			float sourceRatio;
			if (vfRatio > 1) {
				sourceRatio = width / (float) height;
			} else {
				sourceRatio = height / (float) width;
			}
			if (sourceRatio > vfRatio) {
				w = vfw;
				h = (int) (w / sourceRatio);
			} else {
				h = vfh;
				w = (int) (h * sourceRatio);
			}
		}
		viewfinder.removeAllViews();
		SurfaceView surfaceView = new SurfaceView(viewfinder.getContext());
		surfaceView.setLayoutParams(new LinearLayout.LayoutParams(w, h));

		surfaceView.setZOrderMediaOverlay(true);
		viewfinder.addView(surfaceView);
		return surfaceView;
	}

	public boolean isLandscapeLayout() {
		return !portraitMode;
	}

	public void show() {
		requireMapActivity().getContextMenu().hide();
		view.setVisibility(View.VISIBLE);
		if (plugin.getCurrentRecording().getType() != AVActionType.REC_PHOTO) {
			startCounter();
		}
	}

	public void hide() {
		stopCounter();
		view.setVisibility(View.GONE);
		plugin.stopCamera();
		viewfinder.removeAllViews();
	}

	public void update() {
		CurrentRecording recording = plugin.getCurrentRecording();
		UiUtilities iconsCache = requireMapActivity().getApp().getUIUtilities();

		ImageView leftButtonIcon = view.findViewById(R.id.leftButtonIcon);
		View leftButtonView = view.findViewById(R.id.leftButtonView);
		if (recording.getType() != AVActionType.REC_AUDIO) {
			leftButtonIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_minimize));
			TextView showHideText = view.findViewById(R.id.leftButtonText);
			showHideText.setText(showViewfinder ?
					view.getResources().getString(R.string.shared_string_hide) : view.getResources().getString(R.string.shared_string_show));
			leftButtonView.setVisibility(View.VISIBLE);
			viewfinder.setVisibility(showViewfinder ? View.VISIBLE : View.GONE);
		} else {
			leftButtonView.setVisibility(View.INVISIBLE);
			viewfinder.setVisibility(View.GONE);
		}
		leftButtonView.setOnClickListener(v -> showHideViewfinder());

		View centerButtonView = view.findViewById(R.id.centerButtonView);
		ImageView recIcon = view.findViewById(R.id.centerButtonIcon);
		TextView recText = view.findViewById(R.id.centerButtonText);
		View timeView = view.findViewById(R.id.timeView);
		switch (recording.getType()) {
			case REC_AUDIO:
			case REC_VIDEO:
				recIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_rec_stop));
				recText.setText(view.getResources().getString(R.string.shared_string_control_stop));
				recText.setVisibility(View.VISIBLE);
				updateDuration();
				timeView.setVisibility(View.VISIBLE);
				break;
			case REC_PHOTO:
				recIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_photo_dark));
				recText.setVisibility(View.GONE);
				timeView.setVisibility(View.INVISIBLE);
				break;
		}
		centerButtonView.setOnClickListener(v -> rec(requireMapActivity(), false));
		applyViewfinderVisibility();
	}

	public boolean restartRecordingIfNeeded() {
		boolean restart = false;
		CurrentRecording recording = plugin.getCurrentRecording();
		if (recording != null
				&& recording.getType() == AVActionType.REC_VIDEO
				&& plugin.AV_RECORDER_SPLIT.get()) {
			int clipLength = plugin.AV_RS_CLIP_LENGTH.get() * 60;
			int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);
			restart = duration >= clipLength;
			if (restart) {
				rec(requireMapActivity(), true);
			}
		}
		return restart;
	}

	public void updateDuration() {
		if (plugin.getCurrentRecording() != null) {
			TextView timeText = view.findViewById(R.id.timeText);
			int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);
			timeText.setText(Algorithms.formatDuration(duration, requireMapActivity().getApp().accessibilityEnabled()));
		}
	}

	protected void applyViewfinderVisibility() {
		MapActivity mapActivity = plugin.getMapActivity();
		CurrentRecording recording = plugin.getCurrentRecording();
		boolean show = showViewfinder && recording != null && recording.getType() != AVActionType.REC_AUDIO;
		if (isLandscapeLayout() && mapActivity != null) {
			int buttonsHeight = (int) view.getResources().getDimension(R.dimen.map_route_buttons_height);
			int tileBoxHeight = mapActivity.getMapView().getCurrentRotatedTileBox().getPixHeight();
			int h = show ? tileBoxHeight : buttonsHeight;
			view.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtils.dpToPx(mapActivity, 320f), h));
			view.requestLayout();
		}
		viewfinder.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public void showHideViewfinder() {
		showViewfinder = !showViewfinder;
		TextView showHideText = view.findViewById(R.id.leftButtonText);
		showHideText.setText(showViewfinder ? view.getResources().getString(R.string.shared_string_hide) : view.getResources().getString(R.string.shared_string_show));
		applyViewfinderVisibility();
	}

	public int getViewfinderWidth() {
		int res;
		CurrentRecording recording = plugin.getCurrentRecording();
		MapActivity mapActivity = requireMapActivity();
		if (recording.getType() == AVActionType.REC_PHOTO) {
			DisplayMetrics dm = new DisplayMetrics();
			mapActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
			res = dm.widthPixels;
		} else {
			if (isLandscapeLayout()) {
				res = AndroidUtils.dpToPx(mapActivity, 320 - 16f);
			} else {
				res = AndroidUtils.dpToPx(mapActivity, 240f);
			}
		}
		return res;
	}

	public int getViewfinderHeight() {
		int res;
		CurrentRecording recording = plugin.getCurrentRecording();
		MapActivity mapActivity = requireMapActivity();
		if (recording.getType() == AVActionType.REC_PHOTO) {
			DisplayMetrics dm = new DisplayMetrics();
			mapActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
			res = dm.heightPixels;
		} else {
			if (isLandscapeLayout()) {
				res = screenHeight - statusBarHeight - buttonsHeight;
			} else {
				res = AndroidUtils.dpToPx(mapActivity, 240f);
			}
		}
		return res;
	}

	public void rec(@NonNull MapActivity mapActivity, boolean restart) {
		stopCounter();
		CurrentRecording recording = plugin.getCurrentRecording();
		int delay;
		if (recording != null && recording.getType() == AVActionType.REC_PHOTO) {
			delay = 200;
		} else {
			delay = 1;
		}
		handler.postDelayed(() -> {
			if (recording != null) {
				if (recording.getType() == AVActionType.REC_PHOTO) {
					plugin.shoot();
				} else {
					plugin.stopRecording(mapActivity, restart);
					if (restart) {
						startCounter();
					}
				}
			}
		}, delay);
	}

	public void recExternal(@NonNull MapActivity mapActivity) {
		stopCounter();
		handler.postDelayed(() -> {
			CurrentRecording recording = plugin.getCurrentRecording();
			if (recording != null) {
				if (recording.getType() == AVActionType.REC_PHOTO) {
					plugin.takePhotoExternal(lat, lon, mapActivity);
				}
			}
		}, 20);
	}

	private void startCounter() {
		startTime = System.currentTimeMillis();
		if (recTimer != null) {
			recTimer.cancel();
		}
		recTimer = new Timer();
		recTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				handler.post(() -> {
					if (!restartRecordingIfNeeded()) {
						updateDuration();
					}
				});
			}

		}, 0, 1000);
	}

	private void stopCounter() {
		if (recTimer != null) {
			recTimer.cancel();
			recTimer = null;
		}
	}

	public void showFinalPhoto(byte[] jpeg, long duration) {
	}

	public void hideFinalPhoto() {
	}

}
