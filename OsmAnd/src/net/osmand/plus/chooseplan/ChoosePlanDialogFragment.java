package net.osmand.plus.chooseplan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionIntroductoryInfo;
import net.osmand.plus.liveupdates.SubscriptionFragment;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.List;

public abstract class ChoosePlanDialogFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener {
	public static final String TAG = ChoosePlanDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ChoosePlanDialogFragment.class);

	private OsmandApplication app;
	private InAppPurchaseHelper purchaseHelper;

	private boolean nightMode;

	private ViewGroup osmLiveCardButtonsContainer;
	private ProgressBar osmLiveCardProgress;
	private View planTypeCardButton;
	private View planTypeCardButtonDisabled;

	public interface ChoosePlanDialogListener {
		void onChoosePlanDialogDismiss();
	}

	public enum OsmAndFeature {
		WIKIVOYAGE_OFFLINE(R.string.wikivoyage_offline, R.drawable.ic_action_explore),
		DAILY_MAP_UPDATES(R.string.daily_map_updates, R.drawable.ic_action_time_span),
		MONTHLY_MAP_UPDATES(R.string.monthly_map_updates, R.drawable.ic_action_sand_clock),
		UNLIMITED_DOWNLOADS(R.string.unlimited_downloads, R.drawable.ic_action_unlimited_download),
		WIKIPEDIA_OFFLINE(R.string.wikipedia_offline, R.drawable.ic_plugin_wikipedia),
		CONTOUR_LINES_HILLSHADE_MAPS(R.string.contour_lines_hillshade_maps, R.drawable.ic_plugin_srtm),
		SEA_DEPTH_MAPS(R.string.index_item_depth_contours_osmand_ext, R.drawable.ic_action_nautical_depth),
		UNLOCK_ALL_FEATURES(R.string.unlock_all_features, R.drawable.ic_action_osmand_logo),
		DONATION_TO_OSM(R.string.donation_to_osm, 0);

		private final int key;
		private final int iconId;

		OsmAndFeature(int key, int iconId) {
			this.key = key;
			this.iconId = iconId;
		}

		public String toHumanString(Context ctx) {
			return ctx.getString(key);
		}

		public int getIconId() {
			return iconId;
		}

		public boolean isFeaturePurchased(OsmandApplication ctx) {
			if (InAppPurchaseHelper.isSubscribedToLiveUpdates(ctx)) {
				return true;
			}
			switch (this) {
				case DAILY_MAP_UPDATES:
				case DONATION_TO_OSM:
				case UNLOCK_ALL_FEATURES:
					return false;
				case MONTHLY_MAP_UPDATES:
				case UNLIMITED_DOWNLOADS:
				case WIKIPEDIA_OFFLINE:
				case WIKIVOYAGE_OFFLINE:
					return !Version.isFreeVersion(ctx) || InAppPurchaseHelper.isFullVersionPurchased(ctx);
				case SEA_DEPTH_MAPS:
					return InAppPurchaseHelper.isDepthContoursPurchased(ctx);
				case CONTOUR_LINES_HILLSHADE_MAPS:
					return OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null;
			}
			return false;
		}

		public static OsmAndFeature[] possibleValues() {
			return new OsmAndFeature[]{WIKIVOYAGE_OFFLINE, DAILY_MAP_UPDATES, MONTHLY_MAP_UPDATES, UNLIMITED_DOWNLOADS,
					WIKIPEDIA_OFFLINE, CONTOUR_LINES_HILLSHADE_MAPS, UNLOCK_ALL_FEATURES, DONATION_TO_OSM};
		}
	}

	public boolean hasSelectedOsmLiveFeature(OsmAndFeature feature) {
		if (getSelectedOsmLiveFeatures() != null) {
			for (OsmAndFeature f : getSelectedOsmLiveFeatures()) {
				if (feature == f) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasSelectedPlanTypeFeature(OsmAndFeature feature) {
		if (getSelectedPlanTypeFeatures() != null) {
			for (OsmAndFeature f : getSelectedPlanTypeFeatures()) {
				if (feature == f) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		purchaseHelper = app.getInAppPurchaseHelper();
		nightMode = isNightMode(getMapActivity() != null);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity ctx = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			if (Build.VERSION.SDK_INT >= 21) {
				window.setStatusBarColor(ContextCompat.getColor(ctx, getStatusBarColor()));
			}
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return null;
		}
		View view = inflate(R.layout.purchase_dialog_fragment, container);

		ImageView buttonBack = view.findViewById(R.id.button_back);
		buttonBack.setImageResource(AndroidUtils.getNavigationIconResId(ctx));
		buttonBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		view.findViewById(R.id.button_later).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		view.findViewById(R.id.button_restore).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				purchaseHelper.requestInventory();
			}
		});

		ViewGroup infoContainer = (ViewGroup) view.findViewById(R.id.info_container);
		TextViewEx infoDescription = (TextViewEx) view.findViewById(R.id.info_description);
		ViewGroup cardsContainer = (ViewGroup) view.findViewById(R.id.cards_container);
		if (!TextUtils.isEmpty(getInfoDescription())) {
			infoDescription.setText(getInfoDescription());
		}
		TextViewEx planInfoDescription = (TextViewEx) view.findViewById(R.id.plan_info_description);
		planInfoDescription.setText(Version.isHuawei(app)
				? R.string.osm_live_payment_subscription_management_hw : R.string.osm_live_payment_subscription_management);
		ViewGroup osmLiveCard = buildOsmLiveCard(ctx, cardsContainer);
		if (osmLiveCard != null) {
			cardsContainer.addView(osmLiveCard);
		}
		ViewGroup planTypeCard = buildPlanTypeCard(ctx, cardsContainer);
		if (planTypeCard != null) {
			cardsContainer.addView(planTypeCard);
		}
		return view;
	}

	@ColorRes
	protected int getStatusBarColor() {
		return nightMode ? R.color.status_bar_wikivoyage_dark : R.color.status_bar_wikivoyage_light;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		Activity activity = getActivity();
		if (activity instanceof ChoosePlanDialogListener) {
			((ChoosePlanDialogListener) activity).onChoosePlanDialogDismiss();
		}
	}

	public String getInfoDescription() {
		if (!Version.isPaidVersion(getOsmandApplication())) {
			String description = getString(R.string.free_version_message,
					DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "") + "\n";
			return description + getString(R.string.get_osmand_live);
		} else {
			return getString(R.string.get_osmand_live);
		}
	}

	public OsmandApplication getOsmandApplication() {
		return app;
	}

	public abstract OsmAndFeature[] getOsmLiveFeatures();

	public abstract OsmAndFeature[] getPlanTypeFeatures();

	public abstract OsmAndFeature[] getSelectedOsmLiveFeatures();

	public abstract OsmAndFeature[] getSelectedPlanTypeFeatures();

	@DrawableRes
	public abstract int getPlanTypeHeaderImageId();

	public abstract String getPlanTypeHeaderTitle();

	public abstract String getPlanTypeHeaderDescription();

	public String getPlanTypeButtonTitle() {
		InAppPurchase purchase = getPlanTypePurchase();
		if (purchase != null) {
			if (purchase.isPurchased()) {
				return purchase.getPrice(getContext());
			} else {
				return getString(R.string.purchase_unlim_title, purchase.getPrice(getContext()));
			}
		}
		return "";
	}

	public abstract String getPlanTypeButtonDescription();

	public abstract void setPlanTypeButtonClickListener(View button);

	@Nullable
	public abstract InAppPurchase getPlanTypePurchase();

	private View inflate(@LayoutRes int layoutId, @Nullable ViewGroup container) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		return LayoutInflater.from(new ContextThemeWrapper(getContext(), themeRes))
				.inflate(layoutId, container, false);
	}

	private ViewGroup buildOsmLiveCard(@NonNull Context ctx, ViewGroup container) {
		ViewGroup cardView = (ViewGroup) inflate(R.layout.purchase_dialog_osm_live_card, container);
		TextView headerTitle = (TextView) cardView.findViewById(R.id.header_title);
		TextView headerDescr = (TextView) cardView.findViewById(R.id.header_descr);
		headerTitle.setText(R.string.osm_live);
		headerDescr.setText(R.string.osm_live_subscription);
		ViewGroup rowsContainer = (ViewGroup) cardView.findViewById(R.id.rows_container);
		View featureRowDiv = null;
		for (OsmAndFeature feature : getOsmLiveFeatures()) {
			String featureName = feature.toHumanString(ctx);
			View featureRow = inflate(hasSelectedOsmLiveFeature(feature)
					? R.layout.purchase_dialog_card_selected_row : R.layout.purchase_dialog_card_row, cardView);
			AppCompatImageView imgView = (AppCompatImageView) featureRow.findViewById(R.id.img);
			AppCompatImageView imgPurchasedView = (AppCompatImageView) featureRow.findViewById(R.id.img_purchased);
			if (feature.isFeaturePurchased(app)) {
				imgView.setVisibility(View.GONE);
				imgPurchasedView.setVisibility(View.VISIBLE);
			} else {
				imgView.setImageResource(feature.getIconId());
				imgView.setVisibility(View.VISIBLE);
				imgPurchasedView.setVisibility(View.GONE);
			}
			TextViewEx titleView = (TextViewEx) featureRow.findViewById(R.id.title);
			titleView.setText(featureName);
			featureRowDiv = featureRow.findViewById(R.id.div);
			LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) featureRowDiv.getLayoutParams();
			p.rightMargin = AndroidUtils.dpToPx(ctx, 1f);
			featureRowDiv.setLayoutParams(p);
			rowsContainer.addView(featureRow);
		}
		if (featureRowDiv != null) {
			featureRowDiv.setVisibility(View.GONE);
		}

		osmLiveCardButtonsContainer = (ViewGroup) cardView.findViewById(R.id.card_buttons_container);
		osmLiveCardProgress = (ProgressBar) cardView.findViewById(R.id.card_progress);
		if (osmLiveCardProgress != null) {
			int color =  ContextCompat.getColor(ctx, nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light);
			osmLiveCardProgress.getIndeterminateDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
		}
		return cardView;
	}

	@SuppressLint("CutPasteId")
	private void setupOsmLiveCardButtons(boolean progress) {
		final Context ctx = getContext();
		View view = getView();
		if (ctx == null || view == null) {
			return;
		}
		view.findViewById(R.id.button_restore).setEnabled(!progress);
		if (progress) {
			if (osmLiveCardButtonsContainer != null) {
				osmLiveCardButtonsContainer.setVisibility(View.GONE);
			}
			if (osmLiveCardProgress != null) {
				osmLiveCardProgress.setVisibility(View.VISIBLE);
			}
		} else if (osmLiveCardButtonsContainer != null) {
			osmLiveCardButtonsContainer.removeAllViews();
			View lastBtn = null;
			List<InAppSubscription> visibleSubscriptions = purchaseHelper.getLiveUpdates().getVisibleSubscriptions();
			InAppSubscription maxDiscountSubscription = null;
			double maxDiscount = 0;
			boolean anyPurchased = false;
			for (final InAppSubscription s : visibleSubscriptions) {
				if (s.isPurchased()) {
					anyPurchased = true;
				}
				double discount = s.getDiscountPercent(purchaseHelper.getMonthlyLiveUpdates());
				if (discount > maxDiscount) {
					maxDiscountSubscription = s;
					maxDiscount = discount;
				}
			}
			boolean maxDiscountAction = maxDiscountSubscription != null && maxDiscountSubscription.hasDiscountOffer();
			for (final InAppSubscription s : visibleSubscriptions) {
				InAppSubscriptionIntroductoryInfo introductoryInfo = s.getIntroductoryInfo();
				boolean hasIntroductoryInfo = introductoryInfo != null;
				CharSequence descriptionText = s.getDescription(ctx);
				if (s.isPurchased()) {
					View buttonPurchased = inflate(R.layout.purchase_dialog_card_button_active_ex, osmLiveCardButtonsContainer);
					TextViewEx title = (TextViewEx) buttonPurchased.findViewById(R.id.title);
					TextViewEx description = (TextViewEx) buttonPurchased.findViewById(R.id.description);
					TextViewEx descriptionContribute = (TextViewEx) buttonPurchased.findViewById(R.id.description_contribute);
					descriptionContribute.setVisibility(s.isDonationSupported() ? View.VISIBLE : View.GONE);
					TextViewEx buttonTitle = (TextViewEx) buttonPurchased.findViewById(R.id.button_title);
					View buttonView = buttonPurchased.findViewById(R.id.button_view);
					View buttonCancelView = buttonPurchased.findViewById(R.id.button_cancel_view);
					View div = buttonPurchased.findViewById(R.id.div);
					AppCompatImageView rightImage = (AppCompatImageView) buttonPurchased.findViewById(R.id.right_image);

					CharSequence priceTitle = hasIntroductoryInfo ?
							introductoryInfo.getFormattedDescription(ctx, buttonTitle.getCurrentTextColor()) : s.getPriceWithPeriod(ctx);
					title.setText(s.getTitle(ctx));
					if (Algorithms.isEmpty(descriptionText.toString())) {
						description.setVisibility(View.GONE);
					} else {
						description.setText(descriptionText);
					}
					buttonTitle.setText(priceTitle);
					buttonView.setVisibility(View.VISIBLE);
					buttonCancelView.setVisibility(View.GONE);
					buttonPurchased.setOnClickListener(null);
					div.setVisibility(View.GONE);
					rightImage.setVisibility(View.GONE);
					if (s.isDonationSupported()) {
						buttonPurchased.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								showDonationSettings();
								dismiss();
							}
						});
					} else {
						buttonPurchased.setOnClickListener(null);
					}
					osmLiveCardButtonsContainer.addView(buttonPurchased);

					View buttonCancel = inflate(R.layout.purchase_dialog_card_button_active_ex, osmLiveCardButtonsContainer);
					title = (TextViewEx) buttonCancel.findViewById(R.id.title);
					description = (TextViewEx) buttonCancel.findViewById(R.id.description);
					buttonView = buttonCancel.findViewById(R.id.button_view);
					buttonCancelView = buttonCancel.findViewById(R.id.button_cancel_view);
					div = buttonCancel.findViewById(R.id.div);
					rightImage = (AppCompatImageView) buttonCancel.findViewById(R.id.right_image);

					title.setText(getString(R.string.osm_live_payment_current_subscription));
					description.setText(s.getRenewDescription(ctx));
					buttonView.setVisibility(View.GONE);
					buttonCancelView.setVisibility(View.VISIBLE);
					buttonCancelView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							purchaseHelper.manageSubscription(ctx, s.getSku());
						}
					});
					div.setVisibility(View.VISIBLE);
					rightImage.setVisibility(View.VISIBLE);
					osmLiveCardButtonsContainer.addView(buttonCancel);
					lastBtn = buttonCancel;
				} else {
					View button = inflate(R.layout.purchase_dialog_card_button_ex, osmLiveCardButtonsContainer);
					TextViewEx title = (TextViewEx) button.findViewById(R.id.title);
					TextViewEx description = (TextViewEx) button.findViewById(R.id.description);
					TextViewEx descriptionContribute = (TextViewEx) button.findViewById(R.id.description_contribute);
					descriptionContribute.setVisibility(s.isDonationSupported() ? View.VISIBLE : View.GONE);

					View buttonView = button.findViewById(R.id.button_view);
					View buttonExView = button.findViewById(R.id.button_ex_view);
					TextViewEx buttonTitle = (TextViewEx) button.findViewById(R.id.button_title);
					TextViewEx buttonExTitle = (TextViewEx) button.findViewById(R.id.button_ex_title);

					boolean showSolidButton = !anyPurchased
							&& (!maxDiscountAction || hasIntroductoryInfo || maxDiscountSubscription.isUpgrade());
					int descriptionColor = showSolidButton ? buttonExTitle.getCurrentTextColor() : buttonTitle.getCurrentTextColor();
					buttonView.setVisibility(!showSolidButton ? View.VISIBLE : View.GONE);
					buttonExView.setVisibility(showSolidButton ? View.VISIBLE : View.GONE);
					View div = button.findViewById(R.id.div);

					CharSequence priceTitle = hasIntroductoryInfo ?
							introductoryInfo.getFormattedDescription(ctx, descriptionColor) : s.getPriceWithPeriod(ctx);
					title.setText(s.getTitle(ctx));
					if (Algorithms.isEmpty(descriptionText.toString())) {
						description.setVisibility(View.GONE);
					} else {
						description.setText(descriptionText);
					}
					buttonTitle.setText(priceTitle);
					buttonExTitle.setText(priceTitle);

					TextViewEx buttonDiscountTitle = (TextViewEx) button.findViewById(R.id.button_discount_title);
					View buttonDiscountView = button.findViewById(R.id.button_discount_view);
					String discountTitle = s.getDiscountTitle(ctx, purchaseHelper.getMonthlyLiveUpdates());
					if (!Algorithms.isEmpty(discountTitle)) {
						buttonDiscountTitle.setText(discountTitle);
						buttonDiscountView.setVisibility(View.VISIBLE);
						if (s.equals(maxDiscountSubscription)) {
							int saveTextColor = R.color.color_osm_edit_delete;
							if (hasIntroductoryInfo || maxDiscountSubscription.isUpgrade()) {
								saveTextColor = R.color.active_buttons_and_links_text_light;
								AndroidUtils.setBackground(buttonDiscountView, UiUtilities.tintDrawable(buttonDiscountView.getBackground(),
										ContextCompat.getColor(ctx, R.color.color_osm_edit_delete)));
							}
							buttonDiscountTitle.setTextColor(ContextCompat.getColor(ctx, saveTextColor));
						}
					}
					if (!showSolidButton) {
						buttonView.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								subscribe(s.getSku());
							}
						});
					} else {
						buttonExView.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								subscribe(s.getSku());
							}
						});
					}
					div.setVisibility(View.VISIBLE);
					osmLiveCardButtonsContainer.addView(button);
					lastBtn = button;
				}
			}
			if (lastBtn != null) {
				View div = lastBtn.findViewById(R.id.div);
				if (div != null) {
					div.setVisibility(View.GONE);
				}
			}
			if (osmLiveCardProgress != null) {
				osmLiveCardProgress.setVisibility(View.GONE);
			}
			osmLiveCardButtonsContainer.setVisibility(View.VISIBLE);
		}
	}

	private void showDonationSettings() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SubscriptionFragment subscriptionFragment = new SubscriptionFragment();
			subscriptionFragment.show(activity.getSupportFragmentManager(), SubscriptionFragment.TAG);
		}
	}

	private void subscribe(String sku) {
		if (!app.getSettings().isInternetConnectionAvailable(true)) {
			Toast.makeText(app, R.string.internet_not_available, Toast.LENGTH_LONG).show();
		} else {
			FragmentActivity ctx = getActivity();
			if (ctx != null && purchaseHelper != null) {
				OsmandSettings settings = app.getSettings();
				purchaseHelper.purchaseLiveUpdates(ctx, sku,
						settings.BILLING_USER_EMAIL.get(),
						settings.BILLING_USER_NAME.get(),
						settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get(),
						settings.BILLING_HIDE_USER_NAME.get());
			}
		}
	}

	private ViewGroup buildPlanTypeCard(@NonNull Context ctx, ViewGroup container) {
		if (getPlanTypeFeatures().length == 0) {
			return null;
		}
		ViewGroup cardView = (ViewGroup) inflate(R.layout.purchase_dialog_card, container);
		AppCompatImageView headerImageView = (AppCompatImageView) cardView.findViewById(R.id.header_img);
		TextView headerTitleView = (TextView) cardView.findViewById(R.id.header_title);
		TextView headerDescrView = (TextView) cardView.findViewById(R.id.header_descr);
		int headerImageId = getPlanTypeHeaderImageId();
		if (headerImageId != 0) {
			headerImageView.setImageDrawable(getIcon(headerImageId, 0));
		}
		String headerTitle = getPlanTypeHeaderTitle();
		if (!TextUtils.isEmpty(headerTitle)) {
			headerTitleView.setText(headerTitle);
		}
		String headerDescr = getPlanTypeHeaderDescription();
		if (!TextUtils.isEmpty(headerDescr)) {
			headerDescrView.setText(headerDescr);
		}

		ViewGroup rowsContainer = (ViewGroup) cardView.findViewById(R.id.rows_container);
		View featureRow = null;
		for (OsmAndFeature feature : getPlanTypeFeatures()) {
			String featureName = feature.toHumanString(ctx);
			featureRow = inflate(hasSelectedPlanTypeFeature(feature)
					? R.layout.purchase_dialog_card_selected_row : R.layout.purchase_dialog_card_row, cardView);
			AppCompatImageView imgView = (AppCompatImageView) featureRow.findViewById(R.id.img);
			AppCompatImageView imgPurchasedView = (AppCompatImageView) featureRow.findViewById(R.id.img_purchased);
			if (feature.isFeaturePurchased(app)) {
				imgView.setVisibility(View.GONE);
				imgPurchasedView.setVisibility(View.VISIBLE);
			} else {
				imgView.setImageResource(feature.getIconId());
				imgView.setVisibility(View.VISIBLE);
				imgPurchasedView.setVisibility(View.GONE);
			}
			TextViewEx titleView = (TextViewEx) featureRow.findViewById(R.id.title);
			titleView.setText(featureName);
			rowsContainer.addView(featureRow);
		}
		if (featureRow != null) {
			featureRow.findViewById(R.id.div).setVisibility(View.GONE);
		}
		TextViewEx cardDescription = (TextViewEx) cardView.findViewById(R.id.card_descr);
		cardDescription.setText(R.string.in_app_purchase_desc_ex);

		planTypeCardButton = cardView.findViewById(R.id.card_button);
		planTypeCardButtonDisabled = cardView.findViewById(R.id.card_button_disabled);

		return cardView;
	}

	private void setupPlanTypeCardButtons(boolean progress) {
		if (planTypeCardButton != null && planTypeCardButtonDisabled != null) {
			InAppPurchase purchase = getPlanTypePurchase();
			boolean purchased = purchase != null && purchase.isPurchased();

			ProgressBar progressBar = (ProgressBar) planTypeCardButton.findViewById(R.id.card_button_progress);
			TextViewEx buttonTitle = (TextViewEx) planTypeCardButton.findViewById(R.id.card_button_title);
			TextViewEx buttonSubtitle = (TextViewEx) planTypeCardButton.findViewById(R.id.card_button_subtitle);
			buttonTitle.setText(getPlanTypeButtonTitle());
			buttonSubtitle.setText(getPlanTypeButtonDescription());
			if (progress) {
				planTypeCardButton.setVisibility(View.VISIBLE);
				planTypeCardButtonDisabled.setVisibility(View.GONE);
				buttonTitle.setVisibility(View.GONE);
				buttonSubtitle.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				planTypeCardButton.setOnClickListener(null);
			} else {
				if (!purchased) {
					planTypeCardButton.setVisibility(View.VISIBLE);
					planTypeCardButtonDisabled.setVisibility(View.GONE);
					buttonTitle.setVisibility(View.VISIBLE);
					buttonSubtitle.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
					setPlanTypeButtonClickListener(planTypeCardButton);
				} else {
					planTypeCardButton.setVisibility(View.GONE);
					planTypeCardButtonDisabled.setVisibility(View.VISIBLE);
					buttonTitle = (TextViewEx) planTypeCardButtonDisabled.findViewById(R.id.card_button_title);
					buttonSubtitle = (TextViewEx) planTypeCardButtonDisabled.findViewById(R.id.card_button_subtitle);
					buttonTitle.setText(getPlanTypeButtonTitle());
					buttonSubtitle.setText(getPlanTypeButtonDescription());
				}
			}
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}

		boolean requestingInventory = purchaseHelper != null && purchaseHelper.getActiveTask() == InAppPurchaseTaskType.REQUEST_INVENTORY;
		if (osmLiveCardButtonsContainer != null) {
			setupOsmLiveCardButtons(requestingInventory);
		}
		if (planTypeCardButton != null) {
			setupPlanTypeCardButtons(requestingInventory);
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

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			setupOsmLiveCardButtons(false);
			setupPlanTypeCardButtons(false);
		}
	}

	@Override
	public void onGetItems() {
		OsmandApplication app = getMyApplication();
		if (app != null && InAppPurchaseHelper.isSubscribedToLiveUpdates(app)) {
			dismissAllowingStateLoss();
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		if (purchaseHelper != null) {
			InAppSubscription s = purchaseHelper.getLiveUpdates().getSubscriptionBySku(sku);
			if (s != null && s.isDonationSupported()) {
				showDonationSettings();
			}
		}
		dismissAllowingStateLoss();
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			setupOsmLiveCardButtons(true);
			setupPlanTypeCardButtons(true);
		}
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			setupOsmLiveCardButtons(false);
			setupPlanTypeCardButtons(false);
		}
	}

	public static void showFreeVersionInstance(@NonNull FragmentManager fm) {
		try {
			ChoosePlanFreeBannerDialogFragment fragment = new ChoosePlanFreeBannerDialogFragment();
			fragment.show(fm, ChoosePlanFreeBannerDialogFragment.TAG);
		} catch (RuntimeException e) {
			LOG.error("showFreeVersionInstance", e);
		}
	}

	public static void showWikipediaInstance(@NonNull FragmentManager fm) {
		try {
			ChoosePlanWikipediaDialogFragment fragment = new ChoosePlanWikipediaDialogFragment();
			fragment.show(fm, ChoosePlanWikipediaDialogFragment.TAG);
		} catch (RuntimeException e) {
			LOG.error("showWikipediaInstance", e);
		}
	}

	public static void showWikivoyageInstance(@NonNull FragmentManager fm) {
		try {
			ChoosePlanWikivoyageDialogFragment fragment = new ChoosePlanWikivoyageDialogFragment();
			fragment.show(fm, ChoosePlanWikivoyageDialogFragment.TAG);
		} catch (RuntimeException e) {
			LOG.error("showWikivoyageInstance", e);
		}
	}

	public static void showSeaDepthMapsInstance(@NonNull FragmentManager fm) {
		try {
			ChoosePlanSeaDepthMapsDialogFragment fragment = new ChoosePlanSeaDepthMapsDialogFragment();
			fragment.show(fm, ChoosePlanSeaDepthMapsDialogFragment.TAG);
		} catch (RuntimeException e) {
			LOG.error("showSeaDepthMapsInstance", e);
		}
	}

	public static void showHillshadeSrtmPluginInstance(@NonNull FragmentManager fm) {
		try {
			ChoosePlanHillshadeSrtmDialogFragment fragment = new ChoosePlanHillshadeSrtmDialogFragment();
			fragment.show(fm, ChoosePlanHillshadeSrtmDialogFragment.TAG);
		} catch (RuntimeException e) {
			LOG.error("showHillshadeSrtmPluginInstance", e);
		}
	}

	public static void showOsmLiveInstance(@NonNull FragmentManager fm) {
		try {
			ChoosePlanOsmLiveBannerDialogFragment fragment = new ChoosePlanOsmLiveBannerDialogFragment();
			fragment.show(fm, ChoosePlanOsmLiveBannerDialogFragment.TAG);
		} catch (RuntimeException e) {
			LOG.error("showOsmLiveInstance", e);
		}
	}
}
