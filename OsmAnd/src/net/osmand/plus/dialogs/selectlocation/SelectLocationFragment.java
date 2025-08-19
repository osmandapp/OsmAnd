package net.osmand.plus.dialogs.selectlocation;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class SelectLocationFragment extends ConfigureMapOptionFragment implements IAskRefreshDialogCompletely {

	private SelectLocationController<?> controller;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = SelectLocationController.getExistedInstance(app);
		if (controller != null) {
			controller.bindDialog(requireMapActivity(), this);
		} else {
			dismiss();
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
			}
		});
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateApplyButton(true);
	}

	@Override
	protected void setupToolBar(@NonNull View view) {
		super.setupToolBar(view);
		View appbar = view.findViewById(R.id.appbar);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getAppBarColor(app, nightMode));

		int contentColor = ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode);
		TextView title = appbar.findViewById(R.id.title);
		title.setTextColor(contentColor);

		ImageView backButton = appbar.findViewById(R.id.back_button);
		backButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_close, contentColor));

		ImageButton resetButton = appbar.findViewById(R.id.reset_button);
		resetButton.setVisibility(View.GONE);
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return controller.getDialogTitle();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.fragment_select_location, container, false);
		updateContent(view);
		container.addView(view);
	}

	@Override
	protected void setupApplyButton(@NonNull DialogButton applyButton) {
		super.setupApplyButton(applyButton);
		applyButton.setTitleId(R.string.shared_string_select);
	}

	@Override
	public void onAskRefreshDialogCompletely(@NonNull String processId) {
		View view = getView();
		if (view != null && isAdded()) {
			updateContent(view);
		}
	}

	private void updateContent(@NonNull View view) {
		updateCoordinatesView(view);
	}

	private void updateCoordinatesView(@NonNull View view) {
		TextView tvCoordinates = view.findViewById(R.id.coordinates);
		if (tvCoordinates != null) {
			tvCoordinates.setText(controller.getFormattedCoordinates());
		}
	}

	@Override
	protected void applyChanges() {
		controller.onConfirmSelection();
	}

	@Override
	public int getStatusBarColorId() {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireMapActivity());
		return portrait ? ColorUtilities.getAppBarColorId(nightMode) : R.color.status_bar_transparent_light;
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		controller.onResume();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getWidgetsVisibilityHelper().hideWidgets();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		controller.onPause();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getWidgetsVisibilityHelper().showWidgets();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.onDestroy(getActivity());
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new SelectLocationFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
