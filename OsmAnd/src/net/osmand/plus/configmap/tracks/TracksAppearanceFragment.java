package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableInSubscription;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.PRIMARY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.card.base.headed.HeadedContentCard;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.width.WidthComponentController;
import net.osmand.plus.configmap.tracks.AppearanceConfirmationBottomSheet.OnAppearanceChangeConfirmedListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.myplaces.tracks.SearchMyPlacesTracksFragment;
import net.osmand.plus.myplaces.tracks.dialogs.TracksSelectionFragment;
import net.osmand.plus.myplaces.tracks.tasks.ChangeTracksAppearanceTask;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.cards.DirectionArrowsCard;
import net.osmand.plus.track.cards.ShowStartFinishCard;
import net.osmand.plus.track.cards.SplitIntervalCard;
import net.osmand.plus.track.fragments.SplitIntervalBottomSheet;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.track.fragments.controller.TrackColorController;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.track.fragments.controller.TrackWidthController;
import net.osmand.plus.track.fragments.controller.TrackWidthController.ITrackWidthSelectedListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TracksAppearanceFragment extends BaseOsmAndDialogFragment
		implements CardListener, IColorCardControllerListener, ITrackWidthSelectedListener,
		InAppPurchaseListener, SelectionHelperProvider<TrackItem>, OnAppearanceChangeConfirmedListener {

	private static final String TAG = TracksAppearanceFragment.class.getSimpleName();

	private ItemsSelectionHelper<TrackItem> selectionHelper;

	private TrackDrawInfo trackDrawInfo;
	private final List<BaseCard> cards = new ArrayList<>();

	private DialogButton applyButton;

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_main_dark : R.color.activity_background_color_light;
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		selectionHelper = getSelectionHelper();

		if (savedInstanceState != null) {
			trackDrawInfo = new TrackDrawInfo(savedInstanceState);
		} else {
			trackDrawInfo = new TrackDrawInfo(app, TrackDrawInfo.DEFAULT);
		}
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		Activity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(activity, themeId) {
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
			window.setStatusBarColor(ContextCompat.getColor(app, getStatusBarColorId()));
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.tracks_appearance_fragment, container, false);
		view.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light));

		setupToolbar(view);
		setupButtons(view);
		setupActions(view);
		setupCards(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_main_dark : R.color.card_and_list_background_light));

		Toolbar toolbar = appbar.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		String appearance = getString(R.string.change_appearance);
		String count = "(" + selectionHelper.getSelectedItemsSize() + ")";

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getString(R.string.ltr_or_rtl_combine_via_space, appearance, count));
		toolbarTitle.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));

		TextView toolbarSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
		toolbarSubtitle.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode));
		toolbarSubtitle.setText(settings.getApplicationMode().toHumanString());
		AndroidUiHelper.updateVisibility(toolbarSubtitle, true);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> dismiss());
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	private void setupCards(@NonNull View view) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ViewGroup container = view.findViewById(R.id.cards_container);
			container.removeAllViews();

			addCard(container, new SplitIntervalCard(activity, trackDrawInfo));
			addCard(container, new DirectionArrowsCard(activity, trackDrawInfo));
			addCard(container, new ShowStartFinishCard(activity, trackDrawInfo));

			LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
			inflater.inflate(R.layout.list_item_divider_basic, container, true);
			addCard(container, new MultiStateCard(activity, getColorCardController()));

			inflater.inflate(R.layout.list_item_divider_basic, container, true);
			addCard(container, new HeadedContentCard(activity, getWidthCardController()));
		}
	}

	private void addCard(@NonNull ViewGroup container, @NonNull BaseCard card) {
		cards.add(card);
		card.setListener(this);
		container.addView(card.build(container.getContext()));
	}

	private void setupActions(@NonNull View view) {
		setupResetButton(view);
	}

	private void setupResetButton(@NonNull View view) {
		View button = view.findViewById(R.id.button_reset);
		TextView title = button.findViewById(android.R.id.title);
		ImageView icon = button.findViewById(android.R.id.icon);
		title.setText(R.string.reset_to_default);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_reset, ColorUtilities.getActiveColorId(nightMode)));
		button.setOnClickListener(v -> resetToDefault());
		AndroidUtils.setBackground(button.findViewById(R.id.selectable_list_item), UiUtilities.getSelectableDrawable(app));
	}

	private void resetToDefault() {
		trackDrawInfo.resetParams(app, null);
		updateContent();
	}

	private void setupButtons(@NonNull View view) {
		View container = view.findViewById(R.id.buttons_container);
		container.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		applyButton = view.findViewById(R.id.right_bottom_button);
		applyButton.setOnClickListener(v -> {
			ItemsSelectionHelper<TrackItem> helper = getSelectionHelper();
			AppearanceConfirmationBottomSheet.showInstance(getChildFragmentManager(), helper.getSelectedItemsSize());
		});
		applyButton.setButtonType(PRIMARY);
		applyButton.setTitleId(R.string.shared_string_apply);

		DialogButton cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(v -> dismiss());
		cancelButton.setButtonType(SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);

		AndroidUiHelper.updateVisibility(applyButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		trackDrawInfo.saveToBundle(outState);
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (card instanceof SplitIntervalCard) {
				SplitIntervalBottomSheet.showInstance(activity.getSupportFragmentManager(), trackDrawInfo, this);
			}
		}
	}

	@Override
	public void onColorAddedToPalette(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
		if (oldColor != null && oldColor.isCustom()) {
			TrackColorController.saveCustomColorsToTracks(app, oldColor.getColor(), newColor.getColor());
		}
	}

	@Override
	public void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		if (coloringStyle != null) {
			trackDrawInfo.setColoringType(coloringStyle.getType());
			trackDrawInfo.setRouteInfoAttribute(coloringStyle.getRouteInfoAttribute());
			applyButton.setEnabled(isAvailableInSubscription(app, coloringStyle));
		}
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		trackDrawInfo.setColor(paletteColor.getColor());
	}

	@Override
	public void onTrackWidthSelected(@Nullable String width) {

	}

	@Nullable
	public TrackDrawInfo getTrackDrawInfo() {
		return trackDrawInfo;
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof SelectionHelperProvider) {
			return ((SelectionHelperProvider<TrackItem>) fragment).getSelectionHelper();
		}
		return new ItemsSelectionHelper<>();
	}

	@Override
	public void onAppearanceChangeConfirmed() {
		saveTracksAppearance();
	}

	public void saveTracksAppearance() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			getColorCardController().getColorsPaletteController().refreshLastUsedTime();
			CallbackWithObject<Void> callback = result -> {
				onAppearanceSaved();
				return true;
			};
			Set<TrackItem> items = selectionHelper.getSelectedItems();
			ChangeTracksAppearanceTask appearanceTask = new ChangeTracksAppearanceTask(activity, trackDrawInfo, items, callback);
			appearanceTask.execute();
		}
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
		dismiss();
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		updateContent();
	}

	public void updateContent() {
		TrackColorController colorController = getColorCardController();
		colorController.askSelectColoringStyle(trackDrawInfo.getColoringStyle());

		IColorsPaletteController paletteController = colorController.getColorsPaletteController();
		paletteController.selectColor(trackDrawInfo.getColor());

		TrackWidthController widthController = getWidthCardController();
		WidthComponentController widthComponentController = widthController.getWidthComponentController();
		widthComponentController.askSelectWidthMode(trackDrawInfo.getWidth());

		for (BaseCard card : cards) {
			card.update();
		}
	}

	private TrackColorController getColorCardController() {
		return TrackColorController.getInstance(app, null, trackDrawInfo, this);
	}

	private TrackWidthController getWidthCardController() {
		OnNeedScrollListener onNeedScrollListener = y -> {
			View view = getView();
			if (view != null) {
				int bottomVisibleY = view.findViewById(R.id.buttons_container).getTop();
				if (y > bottomVisibleY) {
					ScrollView scrollView = view.findViewById(R.id.scroll_view);
					int diff = y - bottomVisibleY;
					int scrollY = scrollView.getScrollY();
					scrollView.smoothScrollTo(0, scrollY + diff);
				}
			}
		};
		return TrackWidthController.getInstance(app, trackDrawInfo, onNeedScrollListener, this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FragmentActivity activity = getActivity();
		getColorCardController().onDestroy(activity);
		getWidthCardController().onDestroy(activity);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksAppearanceFragment fragment = new TracksAppearanceFragment();
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
