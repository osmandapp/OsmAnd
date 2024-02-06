package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
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
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.widgets.SpeedometerWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.SimpleWidgetState.WidgetSize;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;

public class SpeedometerSettingsFragment extends BaseOsmAndFragment {

	public static final String TAG = SpeedometerSettingsFragment.class.getSimpleName();

	private View view;
	private View toolbarSwitchContainer;
	private LinearLayout buttonsCard;
	private ApplicationMode selectedAppMode;

	private SpeedometerWidget speedometerWidget;
	private OsmandPreference<WidgetSize> sizePref;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		selectedAppMode = settings.getApplicationMode();
		sizePref = settings.SPEEDOMETER_SIZE;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = themedInflater.inflate(R.layout.speedometer_settings_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		toolbarSwitchContainer = view.findViewById(R.id.toolbar_switch_container);
		buttonsCard = view.findViewById(R.id.items_container);

		speedometerWidget = new SpeedometerWidget(view.findViewById(R.id.speedometer_widget), app, selectedAppMode);
		speedometerWidget.updatePreviewInfo(nightMode);

		setupToolbar();
		setupToggleButtons();
		setupConfigButtons();

		return view;
	}

	private void setupToolbar() {
		CollapsingToolbarLayout toolbarLayout = view.findViewById(R.id.toolbar_layout);
		ViewCompat.setElevation(toolbarLayout, 5);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app), ColorUtilities.getPrimaryIconColorId(nightMode)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		toolbarLayout.setTitle(getString(R.string.shared_string_speedometer));
		toolbarLayout.setExpandedTitleColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		toolbarLayout.setCollapsedTitleTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		toolbarLayout.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		updateToolbarSwitch();

		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void setupToggleButtons() {
		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		IconToggleButton radioGroup = new IconToggleButton(app, container, nightMode);
		IconRadioItem btnMedium = createToggleButton(radioGroup, WidgetSize.MEDIUM);
		IconRadioItem btnLarge = createToggleButton(radioGroup, WidgetSize.LARGE);
		radioGroup.setItems(btnMedium, btnLarge);
		radioGroup.setSelectedItem(isMediumHeight() ? btnMedium : btnLarge);
	}

	public int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}

	private IconRadioItem createToggleButton(@NonNull IconToggleButton group, WidgetSize value) {
		IconRadioItem item = new IconRadioItem(value.iconId);
		item.setOnClickListener((radioItem, v) -> {
			sizePref.setModeValue(selectedAppMode, value);
			View view = getView();
			if (view != null) {
				setupToggleButtons();
			}
			speedometerWidget.updatePreviewInfo(nightMode);
			return true;
		});
		return item;
	}

	private boolean isMediumHeight() {
		return sizePref.getModeValue(selectedAppMode) == WidgetSize.MEDIUM;
	}

	private void updateToolbarSwitch() {
		boolean checked = settings.SHOW_SPEEDOMETER.getModeValue(selectedAppMode);
		int color = checked ? selectedAppMode.getProfileColor(nightMode) : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		AndroidUtils.setBackground(toolbarSwitchContainer, new ColorDrawable(color));

		TextView title = toolbarSwitchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);

		SwitchCompat switchView = toolbarSwitchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, nightMode, TOOLBAR);

		toolbarSwitchContainer.setOnClickListener(view -> {
			settings.SHOW_SPEEDOMETER.setModeValue(selectedAppMode, !checked);

			updateToolbarSwitch();
			setupConfigButtons();
		});
	}

	protected void showTextSizeDialog() {
		String[] items = new String[SpeedometerWidget.SpeedLimitWarningState.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = SpeedometerWidget.SpeedLimitWarningState.values()[i].toHumanString(app);
		}
		int selected = settings.SHOW_SPEED_LIMIT_WARNING.get().ordinal();

		AlertDialogData dialogData = new AlertDialogData(requireContext(), nightMode)
				.setTitle(getString(R.string.speed_limit_warning))
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			settings.SHOW_SPEED_LIMIT_WARNING.set(SpeedometerWidget.SpeedLimitWarningState.values()[which]);
			setupConfigButtons();
			speedometerWidget.updatePreviewInfo(nightMode);
		});
	}

	@NonNull
	private View createButtonWithState(int iconId,
									   @NonNull String title,
									   boolean enabled,
									   boolean showShortDivider,
									   @Nullable View.OnClickListener listener) {
		View view = themedInflater.inflate(R.layout.configure_screen_list_item, null);
		ImageView ivIcon = view.findViewById(R.id.icon);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);
		tvTitle.setTextColor(enabled
				? ColorUtilities.getPrimaryTextColor(app, nightMode)
				: ColorUtilities.getSecondaryTextColor(app, nightMode));

		if (showShortDivider) {
			view.findViewById(R.id.short_divider).setVisibility(View.VISIBLE);
		}

		TextView stateContainer = view.findViewById(R.id.description);
		stateContainer.setText(settings.SHOW_SPEED_LIMIT_WARNING.get().toHumanString(app));
		stateContainer.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));

		AndroidUiHelper.updateVisibility(ivIcon, false);
		AndroidUiHelper.updateVisibility(stateContainer, true);

		View button = view.findViewById(R.id.button_container);
		button.setOnClickListener(listener);
		button.setEnabled(enabled);

		setupListItemBackground(view);
		return view;
	}

	private void setupListItemBackground(@NonNull View view) {
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(view.findViewById(R.id.button_container), background);
	}

	private void setupConfigButtons() {
		buttonsCard.removeAllViews();
		boolean enabled = settings.SHOW_SPEEDOMETER.getModeValue(selectedAppMode);
		if (enabled) {
			buttonsCard.addView(createButtonWithState(
					0,
					getString(R.string.speed_limit_warning),
					true,
					false,
					v -> {
						if (AndroidUtils.isActivityNotDestroyed(getActivity())) {
							showTextSizeDialog();
						}
					}
			));
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings), enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings_top_divider), enabled);
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onResume() {
		super.onResume();
		getMapActivity().disableDrawer();
	}

	@Override
	public void onPause() {
		super.onPause();
		getMapActivity().enableDrawer();
	}

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
