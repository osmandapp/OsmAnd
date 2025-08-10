package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.DistanceByTapTextSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class DistanceByTapFragment extends BaseFullScreenFragment {

	public static final String TAG = DistanceByTapFragment.class.getSimpleName();

	private Toolbar toolbar;
	private View toolbarSwitchContainer;
	private ImageView navigationIcon;

	private ApplicationMode selectedAppMode;
	private LinearLayout buttonsCard;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		selectedAppMode = settings.getApplicationMode();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.distance_by_tap_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		toolbar = view.findViewById(R.id.toolbar);
		navigationIcon = toolbar.findViewById(R.id.close_button);
		toolbarSwitchContainer = toolbar.findViewById(R.id.toolbar_switch_container);
		buttonsCard = view.findViewById(R.id.items_container);

		ImageView imageView = view.findViewById(R.id.descriptionImage);
		imageView.setImageResource(nightMode ? R.drawable.img_distance_by_tap_night : R.drawable.img_distance_by_tap_day);

		setupToolbar();
		setupConfigButtons();

		return view;
	}

	private void setupToolbar() {
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(R.string.map_widget_distance_by_tap);

		updateToolbarNavigationIcon();
		updateToolbarActionButton();
		updateToolbarSwitch();

		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void updateToolbarNavigationIcon() {
		navigationIcon.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void updateToolbarActionButton() {
		toolbar.findViewById(R.id.action_button).setOnClickListener(view -> {
			Activity activity = getActivity();
			if (activity != null) {
				AndroidUtils.openUrl(activity, R.string.docs_widget_distance_by_tap, nightMode);
			}
		});
	}

	private void updateToolbarSwitch() {
		boolean checked = settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode);
		int color = checked ? selectedAppMode.getProfileColor(nightMode) : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		AndroidUtils.setBackground(toolbarSwitchContainer, new ColorDrawable(color));

		TextView title = toolbarSwitchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);

		SwitchCompat switchView = toolbarSwitchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, nightMode, TOOLBAR);

		toolbarSwitchContainer.setOnClickListener(view -> {
			settings.SHOW_DISTANCE_RULER.setModeValue(selectedAppMode, !checked);

			updateToolbarSwitch();
			setupConfigButtons();
		});
	}

	protected void showTextSizeDialog() {
		String[] items = new String[DistanceByTapTextSize.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = DistanceByTapTextSize.values()[i].toHumanString(app);
		}
		int selected = settings.DISTANCE_BY_TAP_TEXT_SIZE.get().ordinal();

		AlertDialogData dialogData = new AlertDialogData(requireContext(), nightMode)
				.setTitle(R.string.text_size)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			settings.DISTANCE_BY_TAP_TEXT_SIZE.set(DistanceByTapTextSize.values()[which]);
			setupConfigButtons();
		});
	}

	private View createButtonWithState(int iconId,
	                                   @NonNull String title,
	                                   boolean enabled,
	                                   boolean showShortDivider,
	                                   OnClickListener listener) {
		View view = inflate(R.layout.configure_screen_list_item);

		Drawable icon = getPaintedIcon(iconId, enabled
				? ColorUtilities.getDefaultIconColor(app, nightMode)
				: ColorUtilities.getSecondaryIconColor(app, nightMode));
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);
		tvTitle.setTextColor(enabled
				? ColorUtilities.getPrimaryTextColor(app, nightMode)
				: ColorUtilities.getSecondaryTextColor(app, nightMode));

		if (showShortDivider) {
			view.findViewById(R.id.short_divider).setVisibility(View.VISIBLE);
		}

		TextView stateContainer = view.findViewById(R.id.items_count_descr);
		stateContainer.setText(settings.DISTANCE_BY_TAP_TEXT_SIZE.get().toHumanString(app));
		stateContainer.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimensionPixelSize(R.dimen.default_sub_text_size));

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

		boolean enabled = settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode);
		buttonsCard.addView(createButtonWithState(
				R.drawable.ic_action_map_text_size,
				getString(R.string.text_size),
				enabled,
				false,
				v -> {
					if (AndroidUtils.isActivityNotDestroyed(getActivity())) {
						showTextSizeDialog();
					}
				}
		));
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
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

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DistanceByTapFragment fragment = new DistanceByTapFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
