package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickActionListFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class AndroidAutoMapButtonFragment extends BaseMapButtonsFragment {

	public static final String TAG = AndroidAutoMapButtonFragment.class.getSimpleName();

	private MapButtonsHelper mapButtonsHelper;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapButtonsHelper = app.getMapButtonsHelper();
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);
		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.aauto_cbutton);

		// Disable add button for now
		View actionButton = toolbar.findViewById(R.id.action_button);
		AndroidUiHelper.updateVisibility(actionButton, false);
	}

	@NonNull
	@Override
	protected List<MapButtonState> getAdapterItems() {
		List<MapButtonState> items = new ArrayList<>();
		// Get Android Auto specific button state
		QuickActionButtonState state = mapButtonsHelper.getAndroidAutoButtonState();
		if (state != null) {
			items.add(state);
		}
		return items;
	}

	@Override
	public void onItemClick(@NonNull MapButtonState buttonState) {
		FragmentActivity activity = getActivity();
		if (activity != null && buttonState instanceof QuickActionButtonState) {
			QuickActionListFragment.showInstance(activity, (QuickActionButtonState) buttonState);
		}
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		// Android Auto settings are typically global or handled differently
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AndroidAutoMapButtonFragment fragment = new AndroidAutoMapButtonFragment();
			fragment.setTargetFragment(target, 0);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}