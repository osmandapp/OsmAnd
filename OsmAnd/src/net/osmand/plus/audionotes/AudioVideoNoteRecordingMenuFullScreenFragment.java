package net.osmand.plus.audionotes;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import net.osmand.plus.R;

public class AudioVideoNoteRecordingMenuFullScreenFragment extends Fragment {
	public static final String TAG = "AudioVideoNoteRecordingMenuFullScreenFragment";

	private AudioVideoNoteRecordingMenuFullScreen menu;
	private boolean dismissing;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.recording_note_fragment_fullscreen, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (!dismissing && menu != null) {
			menu.finishRecording();
		}
	}

	public static void showInstance(AudioVideoNoteRecordingMenuFullScreen menu) {

		AudioVideoNoteRecordingMenuFullScreenFragment fragment = new AudioVideoNoteRecordingMenuFullScreenFragment();
		fragment.menu = menu;
		FragmentManager fragmentManager = menu.getMapActivity().getSupportFragmentManager();
		fragmentManager.beginTransaction()
				//.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out, R.anim.abc_fade_in, R.anim.abc_fade_out)
				.add(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(TAG).commit();

		fragmentManager.executePendingTransactions();
	}

	public void dismiss() {
		dismissing = true;
		getActivity().getSupportFragmentManager().popBackStack();
	}
}
