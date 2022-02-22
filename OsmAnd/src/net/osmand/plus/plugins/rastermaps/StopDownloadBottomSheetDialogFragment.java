package net.osmand.plus.plugins.rastermaps;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class StopDownloadBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = StopDownloadBottomSheetDialogFragment.class.getSimpleName();

	private boolean night;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		night = requiredMyApplication().getDaynightHelper().isNightModeForMapControls();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Context context = requireContext();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, night);
		View view = themedInflater.inflate(R.layout.stop_download_bottom_sheet_dialog_fragment, container, false);
		setupExitButton(view);
		setupBackButton(view);
		return view;
	}

	private void setupExitButton(@NonNull View view) {
		View exitButton = view.findViewById(R.id.exit_button);
		UiUtilities.setupDialogButton(night, exitButton, DialogButtonType.SECONDARY, R.string.stop_and_exit);
		exitButton.setOnClickListener(v -> {
			Fragment target = getTargetFragment();
			if (target instanceof TilesDownloadProgressFragment) {
				((TilesDownloadProgressFragment) target).dismiss(false);
			}
			dismiss();
		});
	}

	private void setupBackButton(@NonNull View view) {
		View backButton = view.findViewById(R.id.back_button);
		UiUtilities.setupDialogButton(night, backButton, DialogButtonType.SECONDARY, R.string.shared_string_back);
		backButton.setOnClickListener(v -> dismiss());
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			StopDownloadBottomSheetDialogFragment fragment = new StopDownloadBottomSheetDialogFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}