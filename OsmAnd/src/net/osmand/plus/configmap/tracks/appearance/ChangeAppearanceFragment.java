package net.osmand.plus.configmap.tracks.appearance;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class ChangeAppearanceFragment extends BaseOsmAndDialogFragment implements IAskDismissDialog {

	private static final String TAG = ChangeAppearanceFragment.class.getSimpleName();

	private ChangeAppearanceController controller;

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		Activity activity = requireActivity();
		Dialog dialog = new Dialog(activity, getThemeId()) {
			@Override
			public void onBackPressed() {
				dismiss();
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(ColorUtilities.getColor(app, getStatusBarColorId()));
		}
		return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = ChangeAppearanceController.getInstance(app);
		app.getDialogManager().register(controller.getProcessId(), this);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_tracks_change_appearance, container);
		view.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));

		setupToolbar(view);
		setupCards(view);
		setupApplyButton(view);

		return view;
	}

	protected void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ColorUtilities.getColor(app, nightMode ? R.color.app_bar_main_dark : R.color.card_and_list_background_light));

		Toolbar toolbar = appbar.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		String appearance = getString(R.string.change_appearance);
		String count = "(" + controller.getEditedItemsCount() + ")";

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getString(R.string.ltr_or_rtl_combine_via_space, appearance, count));
		toolbarTitle.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> dismiss());
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	protected void setupCards(@NonNull View view) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ViewGroup cardsContainer = view.findViewById(R.id.cards_container);
			int cardsBackgroundColor = ColorUtilities.getListBgColor(app, nightMode);

			MultiStateCard directionArrowsCard = new MultiStateCard(activity, controller.getDirectionArrowsCardController());
			cardsContainer.addView(directionArrowsCard.build(activity));
			directionArrowsCard.setBackgroundColor(cardsBackgroundColor);
			inflate(R.layout.list_item_divider_with_padding_basic, cardsContainer, true);

			MultiStateCard showStartFinishIconsCard = new MultiStateCard(activity, controller.getShowStartAndFinishIconsCardController());
			cardsContainer.addView(showStartFinishIconsCard.build(activity));
			showStartFinishIconsCard.setBackgroundColor(cardsBackgroundColor);
			inflate(R.layout.list_item_divider, cardsContainer, true);

			MultiStateCard colorsCard = new MultiStateCard(activity, controller.getColorCardController());
			cardsContainer.addView(colorsCard.build(activity));
			colorsCard.setBackgroundColor(cardsBackgroundColor);
			inflate(R.layout.list_item_divider, cardsContainer, true);

			MultiStateCard widthCard = new MultiStateCard(activity, controller.getWidthCardController());
			cardsContainer.addView(widthCard.build(activity));
			widthCard.setBackgroundColor(cardsBackgroundColor);
		}
	}

	protected void setupApplyButton(@NonNull View view) {
		View btnApply = view.findViewById(R.id.apply_button);
		btnApply.setOnClickListener(v -> controller.onApplyButtonClicked());
		updateApplyButtonEnabling(view);
	}

	protected void updateApplyButtonEnabling(@NonNull View view) {
		DialogButton dialogButton = view.findViewById(R.id.apply_button);
		dialogButton.setEnabled(controller.hasAnyChangesToCommit());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			app.getDialogManager().unregister(controller.getProcessId());
		}
	}

	@Override
	public void onAskDismissDialog(@NonNull String processId) {
		dismiss();
	}

	public int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_main_dark : R.color.activity_background_color_light;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ChangeAppearanceFragment fragment = new ChangeAppearanceFragment();
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
