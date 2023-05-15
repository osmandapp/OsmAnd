package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.utils.UiUtilities.DialogButtonType.PRIMARY;
import static net.osmand.plus.utils.UiUtilities.DialogButtonType.SECONDARY;

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
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.cards.ColoringTypeCard;
import net.osmand.plus.track.cards.ColorsCard;
import net.osmand.plus.track.cards.DirectionArrowsCard;
import net.osmand.plus.track.cards.ShowStartFinishCard;
import net.osmand.plus.track.cards.SplitIntervalCard;
import net.osmand.plus.track.cards.TrackColoringCard;
import net.osmand.plus.track.cards.TrackWidthCard;
import net.osmand.plus.track.fragments.CustomColorBottomSheet;
import net.osmand.plus.track.fragments.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.plus.track.fragments.SplitIntervalBottomSheet;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRulesStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TracksAppearanceFragment extends BaseOsmAndDialogFragment implements CardListener, ColorPickerListener, InAppPurchaseListener {

	private static final String TAG = TracksAppearanceFragment.class.getSimpleName();

	private GpxDbHelper gpxDbHelper;
	private GpxSelectionHelper gpxSelectionHelper;
	private SelectedTracksHelper selectedTracksHelper;

	private TrackDrawInfo trackDrawInfo;
	private final List<BaseCard> cards = new ArrayList<>();

	private TrackWidthCard trackWidthCard;
	private TrackColoringCard trackColoringCard;
	private ColorsCard colorsCard;
	private ColoringTypeCard coloringTypeCard;
	private PromoBannerCard promoCard;
	private View applyButton;

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_color_dark : R.color.activity_background_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gpxDbHelper = app.getGpxDbHelper();
		gpxSelectionHelper = app.getSelectedGpxHelper();
		selectedTracksHelper = getSelectedTracksHelper();

		if (savedInstanceState != null) {
			trackDrawInfo = new TrackDrawInfo(savedInstanceState);
		} else {
			trackDrawInfo = new TrackDrawInfo(app, TrackDrawInfo.DEFAULT);
		}
	}

	@Override
	protected boolean useMapNightMode() {
		return true;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
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
		inflater = UiUtilities.getInflater(requireActivity(), nightMode);
		View view = inflater.inflate(R.layout.tracks_appearance_fragment, container, false);
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
		appbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_color_dark : R.color.card_and_list_background_light));

		Toolbar toolbar = appbar.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		String appearance = getString(R.string.change_appearance);
		ItemsSelectionHelper<TrackItem> selectionHelper =  selectedTracksHelper.getItemsSelectionHelper();
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup container = view.findViewById(R.id.cards_container);
			container.removeAllViews();

			addCard(container, new SplitIntervalCard(mapActivity, trackDrawInfo));
			addCard(container, new DirectionArrowsCard(mapActivity, trackDrawInfo));
			addCard(container, new ShowStartFinishCard(mapActivity, trackDrawInfo));

			trackColoringCard = new TrackColoringCard(mapActivity, null, trackDrawInfo);
			addCard(container, trackColoringCard);

			setupColorsCard(container);

			ColoringType coloringType = trackDrawInfo.getColoringType();
			coloringTypeCard = new ColoringTypeCard(mapActivity, null, coloringType);
			addCard(container, coloringTypeCard);

			promoCard = new PromoBannerCard(mapActivity, true);
			addCard(container, promoCard);

			trackWidthCard = new TrackWidthCard(mapActivity, trackDrawInfo, y -> {
				View cardView = trackWidthCard.getView();
				if (cardView != null) {
					ScrollView scrollView = view.findViewById(R.id.scroll_view);
					int height = scrollView.getHeight();
					int bottom = scrollView.getChildAt(0).getBottom();
					int maxScrollY = Math.max(0, bottom - height);
					scrollView.smoothScrollTo(0, maxScrollY);
				}
			});
			addCard(container, trackWidthCard);

			updatePromoCardVisibility();
		}
	}

	private void setupColorsCard(@NonNull ViewGroup container) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			List<Integer> colors = GpxAppearanceAdapter.getTrackColors(app);
			colorsCard = new ColorsCard(mapActivity, null, this,
					trackDrawInfo.getColor(), colors, settings.CUSTOM_TRACK_COLORS, true);
			addCard(container, colorsCard);
			int dp12 = getResources().getDimensionPixelSize(R.dimen.card_padding);
			AndroidUtils.setPadding(colorsCard.getView(), 0, dp12, 0, dp12);
			boolean shouldShowColorsCard = trackDrawInfo.getColoringType().isTrackSolid();
			AndroidUiHelper.updateVisibility(colorsCard.getView(), shouldShowColorsCard);
		}
	}

	private void updatePromoCardVisibility() {
		boolean available = isAvailableColoringType();
		if (!available) {
			promoCard.updateVisibility(true);
			coloringTypeCard.updateVisibility(false);
			colorsCard.updateVisibility(false);
		} else {
			promoCard.updateVisibility(false);
		}
		applyButton.setEnabled(available);
	}

	private boolean isAvailableColoringType() {
		if (trackColoringCard != null) {
			ColoringType currentColoringType = trackColoringCard.getSelectedColoringType();
			String routeInfoAttribute = trackColoringCard.getRouteInfoAttribute();
			return currentColoringType.isAvailableInSubscription(app, routeInfoAttribute, false);
		}
		return false;
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
		applyButton.setOnClickListener(v -> AppearanceConfirmationBottomSheet.showInstance(getChildFragmentManager()));

		View cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(v -> dismiss());

		UiUtilities.setupDialogButton(nightMode, applyButton, PRIMARY, R.string.shared_string_apply);
		UiUtilities.setupDialogButton(nightMode, cancelButton, SECONDARY, R.string.shared_string_cancel);

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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (card instanceof SplitIntervalCard) {
				SplitIntervalBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), trackDrawInfo, this);
			} else if (card instanceof TrackColoringCard) {
				TrackColoringCard trackColoringCard = ((TrackColoringCard) card);
				ColoringType currentColoringType = trackColoringCard.getSelectedColoringType();
				String routeInfoAttribute = trackColoringCard.getRouteInfoAttribute();
				trackDrawInfo.setColoringType(currentColoringType);
				trackDrawInfo.setRouteInfoAttribute(routeInfoAttribute);

				if (coloringTypeCard != null) {
					coloringTypeCard.setColoringType(currentColoringType);

					View cardView = coloringTypeCard.getView();
					if (cardView != null) {
						AndroidUiHelper.updateVisibility(cardView.findViewById(R.id.grey_color_description), false);
					}
				}
				if (colorsCard != null) {
					AndroidUiHelper.updateVisibility(colorsCard.getView(), currentColoringType.isTrackSolid());
				}
				updatePromoCardVisibility();
			} else if (card instanceof ColorsCard) {
				int color = ((ColorsCard) card).getSelectedColor();
				trackDrawInfo.setColor(color);
				updateColorItems();
			}
		}
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		if (prevColor != null) {
			List<Integer> customColors = ColorsCard.getCustomColors(settings.CUSTOM_TRACK_COLORS);
			int index = customColors.indexOf(prevColor);
			if (index != ColorsCard.INVALID_VALUE) {
				CustomColorBottomSheet.saveCustomColorsToTracks(app, prevColor, newColor);
			}
		}
		trackDrawInfo.setColor(newColor);
		colorsCard.onColorSelected(prevColor, newColor);
		updateColorItems();
	}

	private void updateColorItems() {
		if (trackWidthCard != null) {
			trackWidthCard.updateItems();
		}
		if (trackColoringCard != null) {
			trackColoringCard.updateColor();
		}
	}

	@Nullable
	public TrackDrawInfo getTrackDrawInfo() {
		return trackDrawInfo;
	}

	@Nullable
	public SelectedTracksHelper getSelectedTracksHelper() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof TracksFragment) {
			return ((TracksFragment) fragment).getSelectedTracksHelper();
		}
		return null;
	}

	public void saveTracksAppearance() {
		saveDefaultAppearance();
		updateSelectedTracksAppearance();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
		}
		Fragment fragment = getTargetFragment();
		if (fragment instanceof TracksFragment) {
			((TracksFragment) fragment).updateTabsContent();
		}
		dismiss();
	}

	private void updateSelectedTracksAppearance() {
		GpxDataItemCallback callback = new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return !isAdded();
			}

			@Override
			public void onGpxDataItemReady(@NonNull GpxDataItem item) {
				updateTrackAppearance(item);
			}
		};
		ItemsSelectionHelper<TrackItem> selectionHelper =  selectedTracksHelper.getItemsSelectionHelper();
		for (TrackItem trackItem : selectionHelper.getSelectedItems()) {
			File file = trackItem.getFile();
			if (file != null) {
				GpxDataItem item = gpxDbHelper.getItem(file, callback);
				if (item != null) {
					updateTrackAppearance(item);
				}
			}
		}
	}

	private void saveDefaultAppearance() {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		String colorName = GpxAppearanceAdapter.parseTrackColorName(renderer, trackDrawInfo.getColor());

		settings.getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR).set(colorName);
		settings.getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).set(trackDrawInfo.getWidth());
	}

	private void updateTrackAppearance(@NonNull GpxDataItem item) {
		gpxDbHelper.updateColor(item, trackDrawInfo.getColor());
		gpxDbHelper.updateWidth(item, trackDrawInfo.getWidth());
		gpxDbHelper.updateShowArrows(item, trackDrawInfo.isShowArrows());
		gpxDbHelper.updateShowStartFinish(item, trackDrawInfo.isShowStartFinish());

		GpxSplitType splitType = GpxSplitType.getSplitTypeByTypeId(trackDrawInfo.getSplitType());
		gpxDbHelper.updateSplit(item, splitType, trackDrawInfo.getSplitInterval());
		ColoringType coloringType = trackDrawInfo.getColoringType();
		String routeInfoAttribute = trackDrawInfo.getRouteInfoAttribute();
		gpxDbHelper.updateColoringType(item, coloringType.getName(routeInfoAttribute));

		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(item.getFile().getAbsolutePath());
		if (selectedGpxFile != null) {
			selectedGpxFile.resetSplitProcessed();
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		updateContent();
		updatePromoCardVisibility();
	}

	public void updateContent() {
		if (colorsCard != null) {
			colorsCard.setSelectedColor(trackDrawInfo.getColor());
		}
		if (trackColoringCard != null) {
			trackColoringCard.resetSelectedAppearanceItem();
		}
		for (BaseCard card : cards) {
			card.update();
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		return (MapActivity) getActivity();
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
