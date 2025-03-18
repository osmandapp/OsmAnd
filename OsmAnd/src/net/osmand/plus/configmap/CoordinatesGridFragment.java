package net.osmand.plus.configmap;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class CoordinatesGridFragment extends BaseOsmAndFragment implements ICoordinatesGridScreen {

	public static final String TAG = CoordinatesGridFragment.class.getSimpleName();

	private View view;
	private int profileColor;

	private CoordinatesGridController controller;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = CoordinatesGridController.getExistedInstance(app);
		if (controller != null) {
			controller.bindScreen(this);
		} else {
			dismiss();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = inflate(R.layout.fragment_coordinates_grid, container);
		if (controller != null) {
			profileColor = settings.getApplicationMode().getProfileColor(nightMode);
			showHideTopShadow();

			setupMainToggle();
			setupFormatButton();
			setupZoomLevelsButton();
		} else {
			dismiss();
		}
		return view;
	}

	private void showHideTopShadow() {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
	}

	private void setupMainToggle() {
		View button = view.findViewById(R.id.main_toggle);
		boolean enabled = controller.isEnabled();

		TextView tvTitle = button.findViewById(R.id.title_tv);
		tvTitle.setText(R.string.layer_coordinates_grid);
		updateMainToggle();

		CompoundButton cb = button.findViewById(R.id.switch_compat);
		cb.setChecked(enabled);
		cb.setVisibility(View.VISIBLE);
		UiUtilities.setupCompoundButton(nightMode, profileColor, cb);

		cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
			controller.setEnabled(isChecked);
			updateMainToggle();
			updateScreenMode(isChecked);
		});

		button.setOnClickListener(v -> {
			boolean newState = !cb.isChecked();
			cb.setChecked(newState);
		});

		updateScreenMode(enabled);
		setupSelectableBackground(button);
	}

	private void updateMainToggle() {
		boolean enabled = controller.isEnabled();
		View button = view.findViewById(R.id.main_toggle);
		int defIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);

		ImageView ivIcon = button.findViewById(R.id.icon_iv);
		ivIcon.setImageResource(CoordinatesGridController.getStateIcon(enabled));
		ivIcon.setColorFilter(enabled ? profileColor : defIconColor);

		TextView tvSummary = button.findViewById(R.id.state_tv);
		tvSummary.setText(enabled ? R.string.shared_string_on : R.string.shared_string_off);
	}

	private void setupFormatButton() {
		View button = view.findViewById(R.id.format_button);
		View selector = button.findViewById(R.id.format_selector);
		button.setOnClickListener(v -> controller.onFormatSelectorClicked(selector, profileColor, nightMode));
		setupSelectableBackground(button);
		updateFormatButton();
	}

	@Override
	public void updateFormatButton() {
		View button = view.findViewById(R.id.format_button);
		TextView tvFormatValue = button.findViewById(R.id.format_value);
		tvFormatValue.setText(controller.getSelectedFormatName());
	}

	private void setupZoomLevelsButton() {
		View button = view.findViewById(R.id.zoom_levels_button);
		updateZoomLevelsButton();
		button.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity instanceof MapActivity mapActivity) {
				mapActivity.getDashboard().hideDashboard();
				controller.onZoomLevelsClicked(mapActivity);
			}
		});
		setupSelectableBackground(button);
	}

	@Override
	public void updateZoomLevelsButton() {
		View button = view.findViewById(R.id.zoom_levels_button);
		TextView tvZoomValue = button.findViewById(R.id.zoom_value);
		tvZoomValue.setText(controller.getFormattedZoomLevels());
	}

	private void updateScreenMode(boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.content_container), enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.titleBottomDivider), !enabled);
	}

	private void setupSelectableBackground(@NonNull View view) {
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (controller != null) {
			controller.finishProcessIfNeeded(getActivity());
		}
	}

	private void dismiss() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getDashboard().onBackPressed();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity && !activity.isFinishing()) {
			return (MapActivity) activity;
		}
		return null;
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.content, new CoordinatesGridFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}