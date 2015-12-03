package net.osmand.plus.audionotes;

import android.content.res.Resources;
import android.os.Handler;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.AVActionType;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.CurrentRecording;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.util.Algorithms;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class AudioVideoNoteRecordingMenu {

	private View view;
	private LinearLayout viewfinder;
	private AudioVideoNotesPlugin plugin;
	private long startTime;
	private Handler handler;
	private boolean counting;
	private boolean portraitMode;
	private boolean largeDevice;

	public static boolean showViewfinder = true;

	public AudioVideoNoteRecordingMenu(AudioVideoNotesPlugin plugin) {
		this.plugin = plugin;
		handler = new Handler();

		MapActivity mapActivity = plugin.getMapActivity();
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
		largeDevice = AndroidUiHelper.isXLargeDevice(mapActivity);

		view = mapActivity.findViewById(R.id.recording_note_layout);
		viewfinder = (LinearLayout) view.findViewById(R.id.viewfinder);
		showViewfinder = true;

		update();
	}

	public SurfaceView prepareSurfaceView() {
		viewfinder.removeAllViews();
		SurfaceView surfaceView = new SurfaceView(viewfinder.getContext());
		surfaceView.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));

		surfaceView.setZOrderMediaOverlay(true);
		viewfinder.addView(surfaceView);
		return surfaceView;
	}

	public boolean isLandscapeLayout() {
		return !portraitMode && !largeDevice;
	}

	public void show() {
		plugin.getMapActivity().getContextMenu().hide();
		view.setVisibility(View.VISIBLE);
		if (plugin.getCurrentRecording().getType() != AVActionType.REC_PHOTO) {
			startCounter();
		}
	}

	public void hide() {
		view.setVisibility(View.GONE);
		plugin.stopCamera();
		viewfinder.removeAllViews();
	}

	public void update() {
		CurrentRecording recording = plugin.getCurrentRecording();
		IconsCache iconsCache = plugin.getMapActivity().getMyApplication().getIconsCache();

		ImageView showHideIcon = (ImageView) view.findViewById(R.id.showHideIcon);
		View showHideView = view.findViewById(R.id.showHideView);
		if (recording.getType() != AVActionType.REC_AUDIO) {
			showHideIcon.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_minimize));
			TextView showHideText = (TextView) view.findViewById(R.id.showHideText);
			showHideText.setText(showViewfinder ?
					view.getResources().getString(R.string.shared_string_hide) : view.getResources().getString(R.string.shared_string_show));
			showHideView.setVisibility(View.VISIBLE);
			viewfinder.setVisibility(showViewfinder ? View.VISIBLE : View.GONE);
		} else {
			showHideView.setVisibility(View.INVISIBLE);
			viewfinder.setVisibility(View.GONE);
		}
		showHideView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showHideViewfinder();
			}
		});

		View recView = view.findViewById(R.id.recView);
		ImageView recIcon = (ImageView) view.findViewById(R.id.recIcon);
		TextView recText = (TextView) view.findViewById(R.id.recText);
		View timeView = view.findViewById(R.id.timeView);
		switch (recording.getType()) {
			case REC_AUDIO:
			case REC_VIDEO:
				recIcon.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_rec_stop));
				recText.setText(view.getResources().getString(R.string.shared_string_control_stop));
				recText.setVisibility(View.VISIBLE);
				updateDuration();
				timeView.setVisibility(View.VISIBLE);
				break;
			case REC_PHOTO:
				recIcon.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_photo_dark));
				recText.setVisibility(View.GONE);
				timeView.setVisibility(View.INVISIBLE);
				break;
		}
		recView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				rec(plugin.getMapActivity());
			}
		});
		applyViewfinderVisibility();
	}

	public void updateDuration() {
		TextView timeText = (TextView) view.findViewById(R.id.timeText);
		int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);
		timeText.setText(Algorithms.formatDuration(duration));
	}

	private void applyViewfinderVisibility() {
		MapActivity mapActivity = plugin.getMapActivity();
		CurrentRecording recording = plugin.getCurrentRecording();
		boolean show = showViewfinder && recording != null && recording.getType() != AVActionType.REC_AUDIO;
		if (isLandscapeLayout() && mapActivity != null) {
			int buttonsHeight = view.findViewById(R.id.buttonsContainer).getHeight();
			int tileBoxHeight = mapActivity.getMapView().getCurrentRotatedTileBox().getPixHeight();
			int h = show ? tileBoxHeight : buttonsHeight;
			view.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(320f, mapActivity), h));
			view.requestLayout();
		}
		viewfinder.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public void showHideViewfinder() {
		showViewfinder = !showViewfinder;
		TextView showHideText = (TextView) view.findViewById(R.id.showHideText);
		showHideText.setText(showViewfinder ? view.getResources().getString(R.string.shared_string_hide) : view.getResources().getString(R.string.shared_string_show));
		applyViewfinderVisibility();
	}

	public int getViewfinderWidth() {
		return viewfinder.getWidth();
	}

	public int getViewfinderHeight() {
		return viewfinder.getHeight();
	}

	public void rec(final MapActivity mapActivity) {
		stopCounter();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				CurrentRecording recording = plugin.getCurrentRecording();
				if (recording != null) {
					if (recording.getType() == AVActionType.REC_PHOTO) {
						plugin.shoot();
					} else {
						plugin.stopRecording(mapActivity);
					}
				}
			}
		}, 200);
	}

	private void startCounter() {
		startTime = System.currentTimeMillis();
		counting = true;
		updateCounter();
	}

	private void stopCounter() {
		counting = false;
	}

	private void updateCounter() {
		updateDuration();
		if (counting) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					updateCounter();
				}
			}, 1000);
		}
	}

	private int dpToPx(float dp, MapActivity mapActivity) {
		Resources r = mapActivity.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}
}
