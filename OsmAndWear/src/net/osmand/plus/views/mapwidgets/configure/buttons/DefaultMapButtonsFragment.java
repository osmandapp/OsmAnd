package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

public class DefaultMapButtonsFragment extends BaseMapButtonsFragment implements ConfirmationDialogListener {

	public static final String TAG = DefaultMapButtonsFragment.class.getSimpleName();

	private List<MapButtonState> buttonStates;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		buttonStates = app.getMapButtonsHelper().getDefaultButtonsStates();
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.default_buttons);

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		AndroidUiHelper.updateVisibility(actionButton, false);

		ImageView optionsButton = toolbar.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(this::showOptionsMenu);
		optionsButton.setImageDrawable(getContentIcon(R.drawable.ic_overflow_menu_white));
	}

	@NonNull
	@Override
	protected List<MapButtonState> getAdapterItems() {
		return new ArrayList<>(buttonStates);
	}

	@Override
	public void onItemClick(@NonNull MapButtonState buttonState) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			DefaultMapButtonFragment.showInstance(activity.getSupportFragmentManager(), buttonState);
		}
	}

	protected void showOptionsMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(view.getContext())
				.setTitle(getString(R.string.reset_to_default))
				.setIcon(getContentIcon(R.drawable.ic_action_reset))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						ConfirmationBottomSheet.showResetSettingsDialog(manager, this, R.string.default_buttons);
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(view.getContext())
				.setTitle(getString(R.string.copy_from_other_profile))
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						ApplicationMode appMode = settings.getApplicationMode();
						FragmentManager manager = activity.getSupportFragmentManager();
						SelectCopyAppModeBottomSheet.showInstance(manager, this, appMode);
					}
				}).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.simple_popup_menu_item;
		PopUpMenu.show(displayData);
	}

	@Override
	public void onActionConfirmed(int actionId) {
		ApplicationMode appMode = settings.getApplicationMode();
		for (MapButtonState buttonState : buttonStates) {
			buttonState.resetToDefault(appMode);
		}
		updateAdapter();
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode fromAppMode) {
		ApplicationMode appMode = settings.getApplicationMode();
		for (MapButtonState buttonState : buttonStates) {
			buttonState.copyForMode(fromAppMode, appMode);
		}
		updateAdapter();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DefaultMapButtonsFragment fragment = new DefaultMapButtonsFragment();
			fragment.setTargetFragment(target, 0);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}