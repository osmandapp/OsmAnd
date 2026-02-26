package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.CONFIGURE_MAP;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;

import java.util.ArrayList;
import java.util.List;

public class MapModeFragment extends ConfigureMapOptionFragment implements IDialog {

	private MapModeController controller;
	private IconToggleButton toggleButton;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = MapModeController.getExistedInstance(app);
		if (controller != null) {
			controller.registerDialog(this);
		} else {
			dismiss();
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, CONFIGURE_MAP, false);
			}
		});
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.map_mode);
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.fragment_map_mode, container);
		setupToggleButton(view);
		updateContent(view);
		container.addView(view);
	}

	private void setupToggleButton(@NonNull View view) {
		List<IconRadioItem> items = new ArrayList<>();
		for (DayNightMode mode : DayNightMode.values()) {
			items.add(createRadioItem(mode));
		}
		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		toggleButton = new IconToggleButton(app, container, nightMode);
		toggleButton.setItems(items);
		toggleButton.setSelectedItemByTag(controller.getMapTheme());
	}

	@NonNull
	private IconRadioItem createRadioItem(@NonNull DayNightMode mode) {
		IconRadioItem radioItem = new IconRadioItem(mode.getDefaultIcon());
		radioItem.setSelectedIconId(mode.getSelectedIcon())
				.setTag(mode)
				.setContentDescription(mode.toHumanString(app))
				.setOnClickListener((item, v) -> {
					if (controller.askSelectMapTheme((DayNightMode) item.requireTag())) {
						updateAfterThemeSelected();
					}
					return false;
				});
		return radioItem;
	}

	private void updateAfterThemeSelected() {
		toggleButton.setSelectedItemByTag(controller.getMapTheme());
		askUpdateContent();
		updateApplyButton(true);
		refreshMap();
	}

	private void askUpdateContent() {
		View view = getView();
		if (view != null) {
			updateContent(view);
		}
	}

	private void updateContent(@NonNull View view) {
		updateHeaderSummary(view);
		updateDescription(view);
	}

	private void updateHeaderSummary(@NonNull View view) {
		TextView tvSummary = view.findViewById(R.id.summary);
		tvSummary.setText(controller.getHeaderSummary());
	}

	private void updateDescription(@NonNull View view) {
		TextView tvDescription = view.findViewById(R.id.description);
		tvDescription.setText(controller.getDescription());

		TextView tvSecondaryDescription = view.findViewById(R.id.secondary_description);
		String secondaryDescription = controller.getSecondaryDescription();
		boolean hasSecondaryDescription = secondaryDescription != null;
		if (hasSecondaryDescription) {
			tvSecondaryDescription.setText(secondaryDescription);
		}
		AndroidUiHelper.updateVisibility(tvSecondaryDescription, hasSecondaryDescription);
	}

	@Override
	protected void resetToDefault() {
		controller.onResetToDefault();
		updateAfterThemeSelected();
	}

	@Override
	protected void applyChanges() {
		controller.onApplyChanges();
	}

	@Override
	protected void refreshMap() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		controller.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		controller.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.finishProcessIfNeeded(getActivity());
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new MapModeFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
