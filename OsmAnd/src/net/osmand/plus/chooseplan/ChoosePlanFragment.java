package net.osmand.plus.chooseplan;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.chooseplan.button.PriceButton;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.settings.purchase.PurchasesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ChoosePlanFragment extends BasePurchaseDialogFragment implements CardListener {

	public static final String TAG = ChoosePlanFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(ChoosePlanFragment.class);

	public static final String OPEN_CHOOSE_PLAN = "open_choose_plan";
	public static final String CHOOSE_PLAN_FEATURE = "choose_plan_feature";
	public static final String SELECTED_FEATURE = "selected_feature";

	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

	private LinearLayout listContainer;
	private OsmAndFeature selectedFeature;
	private final List<OsmAndFeature> allFeatures = new ArrayList<>();

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_choose_plan;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		allFeatures.addAll(Arrays.asList(OsmAndFeature.values()));
		allFeatures.remove(OsmAndFeature.COMBINED_WIKI);
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_FEATURE)) {
			selectedFeature = OsmAndFeature.valueOf(savedInstanceState.getString(SELECTED_FEATURE));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		listContainer = mainView.findViewById(R.id.list_container);

		setupToolbar();
		createFeaturesList();
		setupLaterButton();
		createTroubleshootingCard();

		return mainView;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	@Override
	protected void updateContent(boolean progress) {
		updateHeader();
		updateToolbar();
		updateListSelection();
		updateContinueButtons();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (selectedFeature != null) {
			outState.putString(SELECTED_FEATURE, selectedFeature.name());
		}
	}

	private void createFeaturesList() {
		listContainer.removeAllViews();
		for (OsmAndFeature feature : allFeatures) {
			View view = createFeatureItemView(feature);
			listContainer.addView(view);
		}
	}

	private View createFeatureItemView(@NonNull OsmAndFeature feature) {
		View view = inflate(R.layout.purchase_dialog_list_item, listContainer, false);
		view.setTag(feature);
		view.setOnClickListener(v -> selectFeature(feature));
		bindFeatureItem(view, feature, false);
		return view;
	}

	@Override
	protected void bindFeatureItem(@NonNull View view, @NonNull OsmAndFeature feature, boolean useHeaderTitle) {
		super.bindFeatureItem(view, feature, useHeaderTitle);

		int visibility = feature.isAvailableInMapsPlus() ? View.VISIBLE : View.INVISIBLE;
		AndroidUiHelper.setVisibility(visibility, view.findViewById(R.id.secondary_icon));

		String osmandPro = getString(R.string.osmand_pro);
		String osmandPlus = getString(R.string.osmand_plus);
		String plan = feature.isAvailableInMapsPlus() ? getString(R.string.ltr_or_rtl_combine_via_or, osmandPlus , osmandPro) : osmandPro;
		String contentDescription = getString(R.string.feature_available_as_part_of_plan, getString(feature.getListTitleId()), plan);
		view.setContentDescription(contentDescription);

		boolean isLastItem = feature == allFeatures.get(allFeatures.size() - 1);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !isLastItem);
	}

	private void setupToolbar() {
		ImageView backBtn = mainView.findViewById(R.id.button_back);
		backBtn.setImageResource(AndroidUtils.getNavigationIconResId(app));
		backBtn.setOnClickListener(v -> dismiss());

		ImageView restoreBtn = mainView.findViewById(R.id.button_reset);
		restoreBtn.setOnClickListener(v -> purchaseHelper.requestInventory(true));

		FrameLayout iconBg = mainView.findViewById(R.id.header_icon_background);
		int color = AndroidUtils.getColorFromAttr(mainView.getContext(), R.attr.purchase_sc_header_icon_bg);
		AndroidUtils.setBackground(iconBg, createRoundedDrawable(color, ButtonBackground.ROUNDED_LARGE));
	}

	@Override
	protected void updateToolbar(int verticalOffset) {
		float absOffset = Math.abs(verticalOffset);
		float totalScrollRange = appBar.getTotalScrollRange();

		float alpha = ColorUtilities.getProportionalAlpha(totalScrollRange * 0.25f, totalScrollRange * 0.9f, absOffset);
		float inverseAlpha = 1.0f - ColorUtilities.getProportionalAlpha(totalScrollRange * 0.5f, totalScrollRange, absOffset);

		TextView tvTitle = mainView.findViewById(R.id.toolbar_title);
		tvTitle.setText(getString(selectedFeature.getTitleId()));
		tvTitle.setAlpha(inverseAlpha);

		mainView.findViewById(R.id.header).setAlpha(alpha);
		mainView.findViewById(R.id.shadowView).setAlpha(inverseAlpha);
	}

	private void setupLaterButton() {
		View button = mainView.findViewById(R.id.button_later);
		button.setOnClickListener(v -> dismiss());
		setupRoundedBackground(button, ButtonBackground.ROUNDED_SMALL);
	}

	private void createTroubleshootingCard() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FrameLayout container = mainView.findViewById(R.id.troubleshooting_card);
			TroubleshootingCard card = new TroubleshootingCard(activity, purchaseHelper, isUsedOnMap());
			card.setListener(this);
			container.addView(card.build(activity));
		}
	}

	private void selectFeature(OsmAndFeature feature) {
		if (selectedFeature != feature) {
			selectedFeature = feature;
			updateContent(isRequestingInventory());
		}
	}

	private void updateHeader() {
		if (selectedFeature != null) {
			Drawable icon = getIcon(selectedFeature.getIconId(nightMode));
			((ImageView) mainView.findViewById(R.id.header_icon)).setImageDrawable(icon);

			String title = getString(selectedFeature.getTitleId());
			((TextView) mainView.findViewById(R.id.header_title)).setText(title);

			String desc = selectedFeature.getDescription(app);
			((TextView) mainView.findViewById(R.id.primary_description)).setText(desc);

			String mapsPlus = getString(R.string.maps_plus);
			String osmAndPro = getString(R.string.osmand_pro);
			String availablePlans = osmAndPro;
			if (selectedFeature.isAvailableInMapsPlus()) {
				availablePlans = String.format(getString(R.string.ltr_or_rtl_combine_via_or), mapsPlus, osmAndPro);
			}
			String pattern = getString(R.string.you_can_get_feature_as_part_of_pattern);
			String secondaryDesc = String.format(pattern, title, availablePlans);
			SpannableString message = UiUtilities.createSpannableString(secondaryDesc, Typeface.BOLD, mapsPlus, osmAndPro);
			((TextView) mainView.findViewById(R.id.secondary_description)).setText(message);
		}
	}

	private void updateListSelection() {
		for (View view : AndroidUtils.getChildrenViews(listContainer)) {
			OsmAndFeature feature = (OsmAndFeature) view.getTag();
			boolean selected = feature == selectedFeature;
			int activeColor = ColorUtilities.getActiveColor(app, nightMode);
			int colorWithAlpha = ColorUtilities.getColorWithAlpha(activeColor, 0.1f);
			int bgColor = selected ? colorWithAlpha : Color.TRANSPARENT;

			Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.5f);
			Drawable drawable = UiUtilities.getLayeredIcon(new ColorDrawable(bgColor), selectableBg);
			AndroidUtils.setBackground(view, drawable);
		}
	}

	private void updateContinueButtons() {
		List<PriceButton<?>> priceButtons = OsmAndProPlanFragment.collectPriceButtons(app, purchaseHelper, nightMode);
		int iconId = nightMode ? R.drawable.ic_action_osmand_pro_logo_colored_night : R.drawable.ic_action_osmand_pro_logo_colored;
		CharSequence price = priceButtons.size() == 0 ? null : Collections.min(priceButtons).getPrice();

		updateContinueButton(mainView.findViewById(R.id.button_continue_pro),
				iconId,
				getString(R.string.osmand_pro),
				price,
				v -> OsmAndProPlanFragment.showInstance(requireActivity()),
				Version.isInAppPurchaseSupported());

		priceButtons = MapsPlusPlanFragment.collectPriceButtons(app, purchaseHelper, nightMode);
		price = priceButtons.size() == 0 ? null : Collections.min(priceButtons).getPrice();

		boolean fullVersion = !Version.isFreeVersion(app);
		boolean subscribedToMaps = InAppPurchaseUtils.isMapsPlusAvailable(app, false);
		boolean fullVersionPurchased = InAppPurchaseUtils.isFullVersionAvailable(app, false);

		boolean isFullVersion = fullVersion || fullVersionPurchased;
		boolean mapsPlusPurchased = subscribedToMaps || isFullVersion;
		boolean available = !mapsPlusPurchased && selectedFeature.isAvailableInMapsPlus() && Version.isInAppPurchaseSupported();

		iconId = available ? R.drawable.ic_action_osmand_maps_plus : R.drawable.ic_action_osmand_maps_plus_desaturated;

		View mapsPlusView = mainView.findViewById(R.id.button_continue_maps_plus);
		updateContinueButton(mapsPlusView, iconId, getString(R.string.maps_plus), price, v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				if (mapsPlusPurchased) {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					Fragment purchasesFragment = fragmentManager.findFragmentByTag(PurchasesFragment.TAG);
					boolean returnToPurchasesFragment = purchasesFragment != null;
					if (returnToPurchasesFragment) {
						dismiss();
					} else {
						PurchasesFragment.showInstance(fragmentManager);
					}
				} else {
					MapsPlusPlanFragment.showInstance(activity);
				}
			}
		}, available);

		if (mapsPlusPurchased) {
			updatePurchasedButton(mapsPlusView, fullVersion);
		}
	}

	private void updateContinueButton(@NonNull View view, int iconId, String plan,
	                                  CharSequence price, OnClickListener listener, boolean available) {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int colorNoAlpha = available ? activeColor : defaultIconColor;

		int pattern = available ? R.string.continue_with : R.string.not_available_with;
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(String.format(getString(pattern), plan));
		tvTitle.setTextColor(colorNoAlpha);

		TextView tvDescription = view.findViewById(R.id.description);
		String description = price != null ? price.toString() : "";
		tvDescription.setText(description);
		tvDescription.setTextColor(ColorUtilities.getColorWithAlpha(colorNoAlpha, 0.75f));

		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageResource(iconId);

		setupRoundedBackground(view, colorNoAlpha, ButtonBackground.ROUNDED_SMALL);
		view.setOnClickListener(listener);
		view.setEnabled(available);
	}

	private void updatePurchasedButton(@NonNull View view, boolean isFullVersion) {
		TextView tvTitle = view.findViewById(R.id.title);
		TextView tvDescription = view.findViewById(R.id.description);

		String version = getString(isFullVersion ? R.string.osmand_plus : R.string.maps_plus);
		boolean featureIncluded = selectedFeature.isAvailableInMapsPlus();
		int pattern = featureIncluded ? R.string.included_in_your_current_plan : R.string.not_available_with;
		tvTitle.setText(String.format(getString(pattern), version));

		String description = null;
		InAppPurchase purchase = purchaseHelper.getFullVersion();
		long purchaseTime = purchase != null ? purchase.getPurchaseTime() : 0;
		if (purchaseTime > 0) {
			description = DATE_FORMAT.format(purchaseTime);
		}
		tvDescription.setText(description);
		AndroidUiHelper.updateVisibility(tvDescription, !Algorithms.isEmpty(description));
		view.setEnabled(true);
	}

	public static void showDefaultInstance(@NonNull FragmentActivity activity) {
		showInstance(activity, OsmAndFeature.UNLIMITED_MAP_DOWNLOADS);
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull OsmAndFeature selectedFeature) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ChoosePlanFragment fragment = new ChoosePlanFragment();
			fragment.selectedFeature = selectedFeature;
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {

	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof TroubleshootingCard) {
			dismiss();
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {

	}
}