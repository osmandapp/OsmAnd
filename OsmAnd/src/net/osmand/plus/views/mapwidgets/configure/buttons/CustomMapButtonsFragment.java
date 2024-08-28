package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import net.osmand.plus.quickaction.MapButtonsHelper.QuickActionUpdatesListener;
import net.osmand.plus.quickaction.QuickActionListFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class CustomMapButtonsFragment extends BaseMapButtonsFragment implements QuickActionUpdatesListener {

	public static final String TAG = CustomMapButtonsFragment.class.getSimpleName();

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
		title.setText(R.string.custom_buttons);

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		actionButton.setOnClickListener(this::showAddButtonDialog);
		actionButton.setImageDrawable(getContentIcon(R.drawable.ic_action_add_no_bg));

		ImageView optionsButton = toolbar.findViewById(R.id.options_button);
		AndroidUiHelper.updateVisibility(optionsButton, false);
	}

	@NonNull
	@Override
	protected List<MapButtonState> getAdapterItems() {
		return new ArrayList<>(mapButtonsHelper.getButtonsStates());
	}

	@Override
	public void onItemClick(@NonNull MapButtonState buttonState) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (buttonState instanceof QuickActionButtonState) {
				QuickActionListFragment.showInstance(activity, (QuickActionButtonState) buttonState);
			}
		}
	}

	private void showAddButtonDialog(@NonNull View view) {
		AlertDialogData dialogData = new AlertDialogData(view.getContext(), nightMode);
		dialogData.setTitle(R.string.add_button);
		dialogData.setNegativeButton(R.string.shared_string_cancel, null);
		dialogData.setPositiveButton(R.string.shared_string_save, (dialog, which) -> {
			Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
			if (extra instanceof EditText) {
				String name = ((EditText) extra).getText().toString().trim();
				if (Algorithms.isBlank(name)) {
					app.showToastMessage(R.string.empty_name);
				} else if (!mapButtonsHelper.isActionButtonNameUnique(name)) {
					app.showToastMessage(R.string.custom_map_button_name_present);
				} else {
					QuickActionButtonState buttonState = mapButtonsHelper.createNewButtonState();
					buttonState.setName(name);
					mapButtonsHelper.addQuickActionButtonState(buttonState);
					updateAdapter();
				}
			}
		});
		String caption = getString(R.string.enter_new_name);
		CustomAlert.showInput(dialogData, requireActivity(), null, caption);
	}

	@Override
	public void onResume() {
		super.onResume();
		mapButtonsHelper.addUpdatesListener(this);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mapButtonsHelper.removeUpdatesListener(this);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public void onActionsUpdated() {
		updateAdapter();
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		ApplicationMode toAppMode = settings.getApplicationMode();
		mapButtonsHelper.copyQuickActionsFromMode(toAppMode, appMode);
		updateAdapter();
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			CustomMapButtonsFragment fragment = new CustomMapButtonsFragment();
			fragment.setTargetFragment(target, 0);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}