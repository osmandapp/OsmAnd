package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.SpeedLimitWarningState;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.widgets.SpeedometerWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;

public class SpeedometerSettingsFragment extends BaseOsmAndFragment {

	public static final String TAG = SpeedometerSettingsFragment.class.getSimpleName();

	private ApplicationMode appMode;
	private SpeedometerWidget widget;

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appMode = settings.getApplicationMode();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.speedometer_settings_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		View speedometerView = view.findViewById(R.id.speedometer_widget);
		widget = new SpeedometerWidget(app, speedometerView);
		widget.updatePreviewInfo(nightMode);

		setupToolbar(view);
		setupToggleButtons(view);
		setupSettingsCard(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app), ColorUtilities.getPrimaryIconColorId(nightMode)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		CollapsingToolbarLayout collapsingToolbar = view.findViewById(R.id.toolbar_layout);
		collapsingToolbar.setExpandedTitleColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		collapsingToolbar.setCollapsedTitleTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		collapsingToolbar.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		ViewCompat.setElevation(collapsingToolbar, 5);

		updateToolbarSwitch(view);

		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void updateToolbarSwitch(@NonNull View view) {
		View container = view.findViewById(R.id.toolbar_switch_container);

		boolean checked = settings.SHOW_SPEEDOMETER.getModeValue(appMode);
		int color = checked ? appMode.getProfileColor(nightMode) : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		AndroidUtils.setBackground(container, new ColorDrawable(color));

		TextView title = container.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);

		CompoundButton compoundButton = container.findViewById(R.id.switchWidget);
		compoundButton.setChecked(checked);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, TOOLBAR);

		container.setOnClickListener(v -> {
			settings.SHOW_SPEEDOMETER.setModeValue(appMode, !checked);

			updateToolbarSwitch(view);
			setupSettingsCard(view);
		});
	}

	private void setupToggleButtons(@NonNull View view) {
		IconRadioItem large = createToggleButton(view, WidgetSize.LARGE);
		IconRadioItem medium = createToggleButton(view, WidgetSize.MEDIUM);

		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		IconToggleButton toggleButton = new IconToggleButton(app, container, nightMode);
		toggleButton.setItems(medium, large);
		toggleButton.setSelectedItem(isMediumHeight() ? medium : large);
	}

	@NonNull
	private IconRadioItem createToggleButton(@NonNull View view, @NonNull WidgetSize widgetSize) {
		IconRadioItem item = new IconRadioItem(widgetSize.iconId);
		item.setOnClickListener((radioItem, v) -> {
			settings.SPEEDOMETER_SIZE.setModeValue(appMode, widgetSize);

			setupToggleButtons(view);
			widget.updatePreviewInfo(nightMode);
			return true;
		});
		return item;
	}

	private boolean isMediumHeight() {
		return settings.SPEEDOMETER_SIZE.getModeValue(appMode) == WidgetSize.MEDIUM;
	}

	private void setupSettingsCard(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.settings);
		setupAlertsTypePref(container);
		AndroidUiHelper.updateVisibility(container, settings.SHOW_SPEEDOMETER.getModeValue(appMode));
	}

	private void setupAlertsTypePref(@NonNull View view) {
		View itemView = view.findViewById(R.id.alerts_type);

		TextView title = itemView.findViewById(R.id.title);
		title.setText(R.string.speed_limit_warning);

		TextView description = itemView.findViewById(R.id.description);
		description.setText(settings.SHOW_SPEED_LIMIT_WARNING.get().toHumanString(app));
		description.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));

		View button = itemView.findViewById(R.id.button_container);
		button.setOnClickListener(v -> showWidgetSizeDialog(view));

		int color = appMode.getProfileColor(nightMode);
		UiUtilities.setupListItemBackground(itemView.getContext(), button, color);

		AndroidUiHelper.updateVisibility(description, true);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.icon), false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.short_divider), false);
	}

	private void showWidgetSizeDialog(@NonNull View view) {
		String[] items = new String[SpeedLimitWarningState.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = SpeedLimitWarningState.values()[i].toHumanString(app);
		}

		AlertDialogData dialogData = new AlertDialogData(requireContext(), nightMode)
				.setTitle(getString(R.string.speed_limit_warning))
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode));

		int selected = settings.SHOW_SPEED_LIMIT_WARNING.get().ordinal();
		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			settings.SHOW_SPEED_LIMIT_WARNING.set(SpeedLimitWarningState.values()[which]);
			setupSettingsCard(view);
			widget.updatePreviewInfo(nightMode);
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SpeedometerSettingsFragment fragment = new SpeedometerSettingsFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}