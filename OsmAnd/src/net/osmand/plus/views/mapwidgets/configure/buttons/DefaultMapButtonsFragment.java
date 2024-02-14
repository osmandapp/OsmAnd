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
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.settings.enums.CompassVisibility;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.configure.dialogs.CompassVisibilityBottomSheet;
import net.osmand.plus.views.mapwidgets.configure.dialogs.CompassVisibilityBottomSheet.CompassVisibilityUpdateListener;
import net.osmand.plus.views.mapwidgets.configure.dialogs.Map3DModeBottomSheet;
import net.osmand.plus.views.mapwidgets.configure.dialogs.Map3DModeBottomSheet.Map3DModeUpdateListener;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

public class DefaultMapButtonsFragment extends BaseMapButtonsFragment implements Map3DModeUpdateListener,
		CompassVisibilityUpdateListener, ConfirmationDialogListener {

	public static final String TAG = DefaultMapButtonsFragment.class.getSimpleName();

	private Map3DButtonState map3DButtonState;
	private CompassButtonState compassButtonState;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MapButtonsHelper mapButtonsHelper = app.getMapButtonsHelper();
		map3DButtonState = mapButtonsHelper.getMap3DButtonState();
		compassButtonState = mapButtonsHelper.getCompassButtonState();
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.default_buttons);

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		AndroidUiHelper.updateVisibility(actionButton, false);
	}

	@NonNull
	@Override
	protected List<MapButtonState> getAdapterItems() {
		List<MapButtonState> items = new ArrayList<>();

		items.add(map3DButtonState);
		items.add(compassButtonState);

		return items;
	}

	@Override
	public void onItemClick(@NonNull MapButtonState buttonState) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ApplicationMode mode = settings.getApplicationMode();
			FragmentManager manager = activity.getSupportFragmentManager();

			if (buttonState instanceof Map3DButtonState) {
				Map3DModeBottomSheet.showInstance(manager, this, mode);
			} else if (buttonState instanceof CompassButtonState) {
				CompassVisibilityBottomSheet.showInstance(manager, this, mode);
			}
		}
	}

	@Override
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
	public void onMap3DModeUpdated(@NonNull Map3DModeVisibility visibility) {
		updateAdapter();
	}

	@Override
	public void onCompassVisibilityUpdated(@NonNull CompassVisibility visibility) {
		updateAdapter();
	}

	@Override
	public void onActionConfirmed(int actionId) {
		ApplicationMode appMode = settings.getApplicationMode();
		map3DButtonState.getVisibilityPref().resetModeToDefault(appMode);
		compassButtonState.getVisibilityPref().resetModeToDefault(appMode);

		updateAdapter();
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode fromAppMode) {
		ApplicationMode appMode = settings.getApplicationMode();
		map3DButtonState.getVisibilityPref().setModeValue(appMode, map3DButtonState.getVisibility(fromAppMode));
		compassButtonState.getVisibilityPref().setModeValue(appMode, compassButtonState.getModeVisibility(fromAppMode));

		updateAdapter();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DefaultMapButtonsFragment fragment = new DefaultMapButtonsFragment();
			fragment.setTargetFragment(target, 0);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}