package net.osmand.plus.audionotes;

import android.support.v4.app.Fragment;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.lang.ref.WeakReference;

public class AudioVideoNoteRecordingMenuFullScreen extends AudioVideoNoteRecordingMenu {

	public AudioVideoNoteRecordingMenuFullScreen(AudioVideoNotesPlugin plugin, double lat, double lon) {
		super(plugin, lat, lon);
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
	}

	public void show() {
	}

	public void hide() {
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
}
