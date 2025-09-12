package net.osmand.plus.plugins.audionotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

// TODO: consider extending BaseFullScreenFragment instead of BaseOsmAndFragment.
// This fragment behaves like a full-screen UI and shares common logic.
// Migration currently postponed due to complexity (custom logic, transactions, etc.).
public class AudioVideoNoteRecordingMenuFullScreenFragment extends BaseOsmAndFragment {

	public static final String TAG = AudioVideoNoteRecordingMenuFullScreenFragment.class.getSimpleName();

	private AudioVideoNoteRecordingMenuFullScreen menu;
	private boolean dismissing;

	@Nullable
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.bottom_buttons_container);
		return ids;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		return inflate(R.layout.recording_note_fragment_fullscreen, container, false);
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

	public static void showInstance(@NonNull AudioVideoNoteRecordingMenuFullScreen menu) {
		FragmentManager manager = menu.requireMapActivity().getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AudioVideoNoteRecordingMenuFullScreenFragment fragment = new AudioVideoNoteRecordingMenuFullScreenFragment();
			fragment.menu = menu;
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			manager.executePendingTransactions();
		}
	}
}
