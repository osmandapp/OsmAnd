package net.osmand.plus.plugins.audionotes;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;

public class AudioRecordingBottomSheet extends BottomSheetDialogFragment {

	private static final String TAG = AudioRecordingBottomSheet.class.getSimpleName();

	@Nullable
	private AudioVideoNoteRecordingMenu recordingMenu;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		return inflate(R.layout.recording_note_fragment, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (recordingMenu != null) {
			recordingMenu.setView(view);
		}
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createBottomContainer(R.id.recording_note_layout));
		collection.removeType(InsetTarget.Type.ROOT_INSET);
		return collection;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (recordingMenu == null) {
			dismiss();
			return;
		}

		View view = requireView();
		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		layoutParams.height = getDimensionPixelSize(R.dimen.map_route_buttons_height) + view.getPaddingTop();
		view.setLayoutParams(layoutParams);
		ViewCompat.requestApplyInsets(view);

		if (!AndroidUiHelper.isOrientationPortrait(requireActivity())) {
			Window window = requireDialog().getWindow();
			if (window != null) {
				WindowManager.LayoutParams params = window.getAttributes();
				params.width = getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
				window.setAttributes(params);
			}
		}
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		AudioVideoNoteRecordingMenu dismissedMenu = recordingMenu;
		recordingMenu = null;
		if (dismissedMenu != null) {
			dismissedMenu.onRecordingDialogDismissed();
		}
	}

	public void close() {
		recordingMenu = null;
		dismissAllowingStateLoss();
	}

	@Nullable
	public static AudioRecordingBottomSheet showInstance(@NonNull MapActivity activity, @NonNull AudioVideoNoteRecordingMenu menu) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AudioRecordingBottomSheet fragment = new AudioRecordingBottomSheet();
			fragment.recordingMenu = menu;
			fragment.show(manager, TAG);
			manager.executePendingTransactions();
			return fragment;
		}
		return null;
	}
}