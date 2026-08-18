package net.osmand.plus.chooseplan;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.chooseplan.button.PriceButton;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class SelectedPlanFragment extends BasePurchaseDialogFragment {

	public static final String TAG = SelectedPlanFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SelectedPlanFragment.class);

	public static final String SELECTED_PRICE_BTN_ID = "selected_price_btn_id";

	protected List<OsmAndFeature> includedFeatures = new ArrayList<>();
	protected List<OsmAndFeature> noIncludedFeatures = new ArrayList<>();
	protected List<OsmAndFeature> previewFeatures = new ArrayList<>();
	protected List<PriceButton<?>> priceButtons = new ArrayList<>();
	private final Map<PriceButton<?>, View> buttonViews = new HashMap<>();
	private PriceButton<?> selectedPriceButton;

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_selected_plan;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		collectPriceButtons(priceButtons);

		if (!Algorithms.isEmpty(priceButtons)) {
			selectedPriceButton = getSelectedPriceButton(savedInstanceState);
			if (selectedPriceButton == null) {
				selectedPriceButton = getSelectedPriceButton(getArguments());
			}
			if (selectedPriceButton == null) {
				selectedPriceButton = priceButtons.get(0);
			}
		}
		collectFeatures();
	}

	private PriceButton<?> getSelectedPriceButton(@Nullable Bundle bundle) {
		if (bundle != null && bundle.containsKey(SELECTED_PRICE_BTN_ID)) {
			String key = bundle.getString(SELECTED_PRICE_BTN_ID);
			for (PriceButton<?> button : priceButtons) {
				if (key != null && button.getId().contains(key)) {
					return button;
				}
			}
		}
		return null;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (selectedPriceButton != null) {
			outState.putString(SELECTED_PRICE_BTN_ID, selectedPriceButton.getId());
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		setupToolbar();
		setupHeader();
		createFeaturesPreview();
		setupLearnMoreButton();
		setupPriceButtons();
		setupApplyButton();
		setupDescription();
		setupRestoreButton();
		createIncludesList();
		fullUpdate();

		return view;
	}

	private void fullUpdate() {
		updateToolbar();
	}

	private void setupToolbar() {
		ImageView backBtn = mainView.findViewById(R.id.button_back);
		backBtn.setImageResource(AndroidUtils.getNavigationIconResId(app));
		backBtn.setOnClickListener(v -> dismiss());

		ImageView helpBtn = mainView.findViewById(R.id.button_help);
		helpBtn.setOnClickListener(v -> {
			AndroidUtils.openUrl(requireActivity(), R.string.docs_purchases_android, nightMode);
		});
	}

	@Override
	protected void updateToolbar(int verticalOffset) {
		float absOffset = Math.abs(verticalOffset);
		float totalScrollRange = appBar.getTotalScrollRange();
		boolean collapsed = totalScrollRange > 0 && Math.abs(verticalOffset) == totalScrollRange;

		float alpha = ColorUtilities.getProportionalAlpha(0, totalScrollRange * 0.75f, absOffset);
		float inverseAlpha = 1.0f - ColorUtilities.getProportionalAlpha(0, totalScrollRange, absOffset);

		int defaultLinksColor = ContextCompat.getColor(app, getToolbarLinksColor());
		int activeLinksColor = ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode);
		int headerBgColor = ContextCompat.getColor(app, getHeaderBgColorId());
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);

		View header = mainView.findViewById(R.id.header);
		header.setAlpha(alpha);

		ImageView icBack = mainView.findViewById(R.id.button_back);
		ImageView icInfo = mainView.findViewById(R.id.button_help);
		int iconsColor = ColorUtilities.getProportionalColorMix(defaultLinksColor, activeLinksColor, 0, totalScrollRange, Math.abs(verticalOffset));
		icBack.setColorFilter(iconsColor);
		icInfo.setColorFilter(iconsColor);

		TextView tvTitle = mainView.findViewById(R.id.toolbar_title);
		tvTitle.setTextColor(activeLinksColor);
		tvTitle.setText(getHeader());
		tvTitle.setAlpha(inverseAlpha);

		int toolbarColor = ColorUtilities.getProportionalColorMix(headerBgColor, activeColor, 0, totalScrollRange, Math.abs(verticalOffset));
		appBar.setBackgroundColor(toolbarColor);
		Dialog dialog = getDialog();
		if (dialog != null && dialog.getWindow() != null) {
			Window window = dialog.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(toolbarColor);
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), mainView.getSystemUiVisibility(), !nightMode && !collapsed);
		}

		View shadow = mainView.findViewById(R.id.shadowView);
		shadow.setAlpha(inverseAlpha);
	}

	private void setupHeader() {
		View mainPart = mainView.findViewById(R.id.main_part);
		int bgColor = ContextCompat.getColor(app, getHeaderBgColorId());
		mainPart.setBackgroundColor(bgColor);

		TextView tvTitle = mainView.findViewById(R.id.header_title);
		tvTitle.setText(getHeader());

		TextView tvDescription = mainView.findViewById(R.id.header_descr);
		tvDescription.setText(getTagline());

		ImageView ivIcon = mainView.findViewById(R.id.header_icon);
		ivIcon.setImageResource(getHeaderIconId());
	}

	private void createFeaturesPreview() {
		LinearLayout container = mainView.findViewById(R.id.features_list);
		for (OsmAndFeature feature : previewFeatures) {
			View itemView = themedInflater.inflate(R.layout.purchase_dialog_preview_list_item, container, false);
			bindFeatureItem(itemView, feature);
			container.addView(itemView);
		}
	}

	private void setupLearnMoreButton() {
		View btn = mainView.findViewById(R.id.learn_more_button);
		btn.setOnClickListener(v -> {
			View includesContainer = mainView.findViewById(R.id.list_included);
			scrollView.smoothScrollTo(0, includesContainer.getTop());
			appBar.setExpanded(false);
		});
	}

	protected void bindFeatureItem(@NonNull View itemView, @NonNull OsmAndFeature feature) {
		bindFeatureItem(itemView, feature, false);
		ImageView ivCheckmark = itemView.findViewById(R.id.checkmark);
		boolean included = includedFeatures.contains(feature);
		if (included) {
			ivCheckmark.setImageDrawable(getCheckmark());
		}
		AndroidUiHelper.setVisibility(included ? View.VISIBLE : View.GONE, ivCheckmark);
		itemView.setContentDescription(getString(R.string.ltr_or_rtl_combine_via_space, getString(feature.getListTitleId()),
				getString(included ? R.string.shared_string_included : R.string.shared_string_not_included)));
	}

	private void setupPriceButtons() {
		LinearLayout container = mainView.findViewById(R.id.price_block);
		container.removeAllViews();

		for (PriceButton<?> button : priceButtons) {
			View itemView = themedInflater.inflate(R.layout.purchase_dialog_btn_payment, container, false);
			TextView tvTitle = itemView.findViewById(R.id.title);
			TextView tvPrice = itemView.findViewById(R.id.price);
			TextView tvDiscount = itemView.findViewById(R.id.discount);
			TextView tvDesc = itemView.findViewById(R.id.description);

			tvTitle.setText(button.getTitle());
			tvPrice.setText(button.getPrice());
			tvDesc.setText(button.getDescription());
			tvDiscount.setText(button.getDiscount());

			AndroidUiHelper.updateVisibility(tvDesc, !Algorithms.isEmpty(button.getDescription()));
			AndroidUiHelper.updateVisibility(tvDiscount, !Algorithms.isEmpty(button.getDiscount()));

			int iconId = button.isDiscountApplied() ? R.drawable.purchase_sc_discount_rectangle : R.drawable.purchase_save_discount_rectangle;
			AndroidUtils.setBackground(tvDiscount, getIcon(iconId));

			itemView.setOnClickListener(v -> {
				selectedPriceButton = button;
				updateButtons();
			});

			buttonViews.put(button, itemView);
			container.addView(itemView);
		}

		updateButtons();
	}

	private void updateButtons() {
		for (PriceButton<?> key : buttonViews.keySet()) {
			View itemView = buttonViews.get(key);
			if (itemView == null) {
				continue;
			}

			ImageView ivCheckmark = itemView.findViewById(R.id.icon);

			int colorNoAlpha = ColorUtilities.getActiveColor(app, nightMode);
			Drawable normal;
			boolean selected = key.equals(selectedPriceButton);
			if (selected) {
				ivCheckmark.setImageDrawable(getCheckmark());

				Drawable stroke = getActiveStrokeDrawable();
				int colorWithAlpha = ColorUtilities.getColorWithAlpha(colorNoAlpha, 0.1f);

				Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(R.drawable.rectangle_rounded, colorWithAlpha);
				Drawable[] layers = {bgDrawable, stroke};
				normal = new LayerDrawable(layers);
			} else {
				ivCheckmark.setImageDrawable(getEmptyCheckmark());
				normal = new ColorDrawable(Color.TRANSPARENT);
			}
			setupRoundedBackground(itemView, normal, colorNoAlpha, ButtonBackground.ROUNDED);
			itemView.setContentDescription(getButtonContentDescription(key, selected));
		}
		updateSelectedPriceButton();
	}

	private String getButtonContentDescription(PriceButton<?> button, boolean selected) {
		StringBuilder builder = new StringBuilder(button.getTitle());
		String discount = button.getDiscount();
		if (!Algorithms.isEmpty(discount)) {
			builder.append(" ").append(discount);
		}
		builder.append(" ").append(button.getPrice());
		String description = button.getDescription();
		if (!Algorithms.isEmpty(description)) {
			builder.append(" ").append(description);
		}
		builder.append(" ").append(getString(selected ? R.string.shared_string_selected : R.string.shared_string_not_selected));
		return builder.toString();
	}

	private void updateSelectedPriceButton() {
		if (selectedPriceButton != null) {
			View applyButton = mainView.findViewById(R.id.apply_button);
			TextView tvPrice = applyButton.findViewById(R.id.description);
			CharSequence price = selectedPriceButton.getPrice();
			if (price instanceof SpannableStringBuilder) {
				SpannableStringBuilder formattedPrice = (SpannableStringBuilder) price;
				ForegroundColorSpan[] textColorSpans =
						formattedPrice.getSpans(0, formattedPrice.length(), ForegroundColorSpan.class);
				int textColor = ((TextView) applyButton.findViewById(R.id.title)).getCurrentTextColor();
				if (textColorSpans.length > 0) {
					updateSpanColor(formattedPrice, textColorSpans[0], textColor);
				}
				if (textColorSpans.length > 1) {
					int semiTransparentTextColor = ColorUtilities.getColorWithAlpha(textColor, 0.5f);
					updateSpanColor(formattedPrice, textColorSpans[1], semiTransparentTextColor);
				}
			}
			tvPrice.setText(price);
		}
	}

	private void updateSpanColor(SpannableStringBuilder spannable, ForegroundColorSpan span, @ColorInt int color) {
		int start = spannable.getSpanStart(span);
		int end = spannable.getSpanEnd(span);
		spannable.removeSpan(span);
		spannable.setSpan(new ForegroundColorSpan(color), start, end, 0);
	}

	private void setupApplyButton() {
		View itemView = mainView.findViewById(R.id.apply_button);

		TextView tvTitle = itemView.findViewById(R.id.title);
		tvTitle.setText(getString(R.string.complete_purchase));

		itemView.setOnClickListener(v -> {
			if (selectedPriceButton != null && getActivity() != null) {
				selectedPriceButton.onApply(getActivity(), purchaseHelper);
			}
		});

		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		Drawable normal = createRoundedDrawable(activeColor, ButtonBackground.ROUNDED_SMALL);
		setupRoundedBackground(itemView, normal, activeColor, ButtonBackground.ROUNDED_SMALL);
	}

	private void setupDescription() {
		String cancelDesc;
		if (Version.isHuawei()) {
			cancelDesc = getString(R.string.cancel_anytime_in_huawei_appgallery);
		} else if (Version.isAmazon()) {
			cancelDesc = getString(R.string.cancel_anytime_in_amazon_app);
		} else {
			cancelDesc = getString(R.string.cancel_anytime_in_gplay);
		}

		String commonDesc;
		if (Version.isHuawei()) {
			commonDesc = getString(R.string.osm_live_payment_subscription_management_hw);
		} else if (Version.isAmazon()) {
			commonDesc = getString(R.string.osm_live_payment_subscription_management_amz);
		} else {
			commonDesc = getString(R.string.osm_live_payment_subscription_management);
		}

		((TextView) mainView.findViewById(R.id.cancel_description)).setText(cancelDesc);
		((TextView) mainView.findViewById(R.id.plan_info_description)).setText(commonDesc);
	}

	private void setupRestoreButton() {
		View button = mainView.findViewById(R.id.button_restore);
		button.setOnClickListener(v -> purchaseHelper.requestInventory(true));
		setupRoundedBackground(button, ButtonBackground.ROUNDED_SMALL);
	}

	private void createIncludesList() {
		LinearLayout container = mainView.findViewById(R.id.list_included);
		Map<String, List<OsmAndFeature>> chapters = new LinkedHashMap<>();
		chapters.put(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_includes), ""), includedFeatures);
		chapters.put(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_not_included), ""), noIncludedFeatures);

		for (String key : chapters.keySet()) {
			List<OsmAndFeature> features = chapters.get(key);
			if (features != null && features.size() > 0) {
				View v = themedInflater.inflate(R.layout.purchase_dialog_includes_block_header, container, false);
				TextView tvTitle = v.findViewById(R.id.title);
				tvTitle.setText(key);
				container.addView(v);
				for (OsmAndFeature feature : features) {
					View itemView = themedInflater.inflate(R.layout.purchase_dialog_includes_block_item, container, false);
					if (features.indexOf(feature) == 0) {
						itemView.findViewById(R.id.top_padding).setVisibility(View.GONE);
					}
					bindIncludesFeatureItem(itemView, feature);
					container.addView(itemView);
				}
			}
		}
	}

	private void bindIncludesFeatureItem(View itemView, OsmAndFeature feature) {
		bindFeatureItem(itemView, feature, true);

		TextView tvDescription = itemView.findViewById(R.id.description);
		tvDescription.setText(feature.getDescription(app));

		int iconBgColor = ContextCompat.getColor(app, feature.isAvailableInMapsPlus() ?
				R.color.maps_plus_item_bg :
				R.color.osmand_pro_item_bg);
		int color = ColorUtilities.getColorWithAlpha(iconBgColor, 0.2f);
		AndroidUtils.setBackground(itemView.findViewById(R.id.icon_background), createRoundedDrawable(color, ButtonBackground.ROUNDED));
	}

	@ColorRes
	protected int getToolbarLinksColor() {
		return nightMode ? R.color.purchase_sc_toolbar_active_dark : R.color.purchase_sc_toolbar_active_light;
	}

	protected int getHeaderBgColorId() {
		return nightMode ? R.color.purchase_sc_feature_list_bg_dark : R.color.purchase_sc_feature_list_bg_light;
	}

	protected abstract void collectPriceButtons(List<PriceButton<?>> priceButtons);

	protected abstract void collectFeatures();

	protected abstract String getHeader();

	protected abstract String getTagline();

	protected abstract int getHeaderIconId();

}
