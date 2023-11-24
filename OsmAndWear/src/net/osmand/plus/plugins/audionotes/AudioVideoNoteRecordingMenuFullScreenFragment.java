package net.osmand.plus.plugins.audionotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
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
		requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	@Override
	public void onPause() {
		super.onPause();
		requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (!dismissing && menu != null) {
			menu.finishRecording();
		}
	}

	public static void showInstance(@NonNull AudioVideoNoteRecordingMenuFullScreen menu) {
		FragmentManager fragmentManager = menu.requireMapActivity().getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			AudioVideoNoteRecordingMenuFullScreenFragment fragment = new AudioVideoNoteRecordingMenuFullScreenFragment();
			fragment.menu = menu;
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			fragmentManager.executePendingTransactions();
		}
	}

	public void dismiss() {
		dismissing = true;
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
			}
		}
	}
}
