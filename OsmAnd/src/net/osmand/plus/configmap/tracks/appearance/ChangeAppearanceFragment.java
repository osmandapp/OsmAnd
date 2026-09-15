package net.osmand.plus.configmap.tracks.appearance;

import static net.osmand.plus.configmap.tracks.appearance.ChangeAppearanceController.PROCESS_ID;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
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
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.configmap.tracks.TracksTabsFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.SearchMyPlacesTracksFragment;
import net.osmand.plus.myplaces.tracks.dialogs.TracksSelectionFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class ChangeAppearanceFragment extends BaseFullScreenDialogFragment implements IAskDismissDialog, IAskRefreshDialogCompletely {

	private static final String TAG = ChangeAppearanceFragment.class.getSimpleName();

	private DialogManager dialogManager;
	private ChangeAppearanceController controller;

	@NonNull
	@Override
	public Dialog createDialog(@Nullable Bundle savedInstanceState) {
		return new Dialog(requireActivity(), getThemeId()) {
			@Override
			public void onBackPressed() {
				dismiss();
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dialogManager = app.getDialogManager();
		controller = (ChangeAppearanceController) dialogManager.findController(PROCESS_ID);
		dialogManager.register(PROCESS_ID, this);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_tracks_change_appearance, container, false);
		view.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));

		setupToolbar(view);
		setupCards(view);
		setupApplyButton(view);
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(PROCESS_ID);
		}
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
		FragmentActivity activity = requireActivity();
		ViewGroup container = view.findViewById(R.id.cards_container);

		MultiStateCard arrowsCard = new MultiStateCard(activity, controller.getArrowsCardController());
		container.addView(arrowsCard.build());

		inflate(R.layout.list_item_divider_with_padding_basic, container, true);

		MultiStateCard iconsCard = new MultiStateCard(activity, controller.getStartAndFinishIconsCardController());
		container.addView(iconsCard.build());

		inflate(R.layout.list_item_divider, container, true);

		MultiStateCard colorsCard = new MultiStateCard(activity, controller.getColorCardController());
		container.addView(colorsCard.build());

		inflate(R.layout.list_item_divider, container, true);

		MultiStateCard widthCard = new MultiStateCard(activity, controller.getWidthCardController());
		container.addView(widthCard.build());

		inflate(R.layout.list_item_divider, container, true);

		MultiStateCard splitCard = new MultiStateCard(activity, controller.getSplitCardController());
		container.addView(splitCard.build());

		setupOnNeedScrollListener();
	}

	protected void setupApplyButton(@NonNull View view) {
		View btnApply = view.findViewById(R.id.apply_button);
		btnApply.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				controller.saveChanges(activity);
			}
		});
		updateApplyButtonEnabling(view);
	}

	protected void updateApplyButtonEnabling(@NonNull View view) {
		DialogButton dialogButton = view.findViewById(R.id.apply_button);
		dialogButton.setEnabled(controller.hasAnyChangesToCommit());
	}

	private void onAppearanceSaved() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).refreshMapComplete();
		}
		Fragment fragment = getTargetFragment();
		if (fragment instanceof TracksTabsFragment) {
			((TracksTabsFragment) fragment).updateTabsContent();
		} else if (fragment instanceof TracksSelectionFragment) {
			((TracksSelectionFragment) fragment).dismiss();
		} else if (fragment instanceof SearchMyPlacesTracksFragment) {
			SearchMyPlacesTracksFragment searchTracksFragment = (SearchMyPlacesTracksFragment) fragment;
			searchTracksFragment.updateTargetFragment();
			searchTracksFragment.dismiss();
		}
	}

	private void setupOnNeedScrollListener() {
		controller.getWidthCardController().setOnNeedScrollListener(y -> {
			View view = getView();
			if (view != null) {
				int bottomVisibleY = view.findViewById(R.id.bottom_buttons_container).getTop();
				if (y > bottomVisibleY) {
					ScrollView scrollView = view.findViewById(R.id.scroll_view);
					int diff = y - bottomVisibleY;
					int scrollY = scrollView.getScrollY();
					scrollView.smoothScrollTo(0, scrollY + diff);
				}
			}
		});
	}

	@Override
	public void onAskRefreshDialogCompletely(@NonNull String processId) {
		View view = getView();
		if (view != null) {
			updateApplyButtonEnabling(view);
		}
	}

	@Override
	public void onAskDismissDialog(@NonNull String processId) {
		if (controller.isAppearanceSaved()) {
			onAppearanceSaved();
		}
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

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ChangeAppearanceFragment fragment = new ChangeAppearanceFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
