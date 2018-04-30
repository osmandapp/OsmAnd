package net.osmand.plus.chooseplan;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.liveupdates.SubscriptionFragment;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

public abstract class ChoosePlanDialogFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener {
	public static final String TAG = ChoosePlanDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ChoosePlanDialogFragment.class);

	private OsmandApplication app;
	private InAppPurchaseHelper purchaseHelper;

	private boolean nightMode;

	private View osmLiveCardButton;
	private View planTypeCardButton;

	public interface ChoosePlanDialogListener {
		void onChoosePlanDialogDismiss();
	}

	public enum OsmAndFeature {
		WIKIVOYAGE_OFFLINE(R.string.wikivoyage_offline),
		DAILY_MAP_UPDATES(R.string.daily_map_updates),
		MONTHLY_MAP_UPDATES(R.string.monthly_map_updates),
		UNLIMITED_DOWNLOADS(R.string.unlimited_downloads),
		WIKIPEDIA_OFFLINE(R.string.wikipedia_offline),
		CONTOUR_LINES_HILLSHADE_MAPS(R.string.contour_lines_hillshade_maps),
		SEA_DEPTH_MAPS(R.string.index_item_depth_contours_osmand_ext),
		UNLOCK_ALL_FEATURES(R.string.unlock_all_features),
		DONATION_TO_OSM(R.string.donation_to_osm);

		private final int key;

		OsmAndFeature(int key) {
			this.key = key;
		}

		public String toHumanString(Context ctx) {
			return ctx.getString(key);
		}

		public boolean isFeaturePurchased(OsmandApplication ctx) {
			switch (this) {
				case DAILY_MAP_UPDATES:
				case MONTHLY_MAP_UPDATES:
				case UNLIMITED_DOWNLOADS:
				case WIKIPEDIA_OFFLINE:
				case UNLOCK_ALL_FEATURES:
				case DONATION_TO_OSM:
					return false;
				case SEA_DEPTH_MAPS:
					return ctx.getSettings().DEPTH_CONTOURS_PURCHASED.get();
				case WIKIVOYAGE_OFFLINE:
					return ctx.getSettings().TRAVEL_ARTICLES_PURCHASED.get();
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

		view.findViewById(R.id.button_back).setOnClickListener(new OnClickListener() {
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

		ViewGroup infoContainer = (ViewGroup) view.findViewById(R.id.info_container);
		TextViewEx infoDescription = (TextViewEx) view.findViewById(R.id.info_description);
		ViewGroup cardsContainer = (ViewGroup) view.findViewById(R.id.cards_container);
		if (!TextUtils.isEmpty(getInfoDescription())) {
			infoDescription.setText(getInfoDescription());
		}
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
		if (activity != null && activity instanceof ChoosePlanDialogListener) {
			((ChoosePlanDialogListener) activity).onChoosePlanDialogDismiss();
		}
	}

	public OsmandApplication getOsmandApplication() {
		return app;
	}

	public abstract OsmAndFeature[] getOsmLiveFeatures();

	public abstract OsmAndFeature[] getPlanTypeFeatures();

	public abstract OsmAndFeature[] getSelectedOsmLiveFeatures();

	public abstract OsmAndFeature[] getSelectedPlanTypeFeatures();

	public abstract String getInfoDescription();

	@DrawableRes
	public abstract int getPlanTypeHeaderImageId();

	public abstract String getPlanTypeHeaderTitle();

	public abstract String getPlanTypeHeaderDescription();

	public abstract String getPlanTypeButtonTitle();

	public abstract String getPlanTypeButtonDescription();

	public abstract void setPlanTypeButtonClickListener(View button);

	private View inflate(@LayoutRes int layoutId, @Nullable ViewGroup container) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		return LayoutInflater.from(new ContextThemeWrapper(getContext(), themeRes))
				.inflate(layoutId, container, false);
	}

	private ViewGroup buildOsmLiveCard(@NonNull Context ctx, ViewGroup container) {
		ViewGroup cardView = (ViewGroup) inflate(R.layout.purchase_dialog_active_card, container);
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
		TextViewEx cardDescription = (TextViewEx) cardView.findViewById(R.id.card_descr);
		cardDescription.setText(R.string.osm_live_payment_desc);

		osmLiveCardButton = cardView.findViewById(R.id.card_button);
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		boolean requestingInventory = purchaseHelper != null && purchaseHelper.getActiveTask() == InAppPurchaseTaskType.REQUEST_INVENTORY;
		setupOsmLiveCardButton(requestingInventory);

		return cardView;
	}

	private void setupOsmLiveCardButton(boolean progress) {
		if (osmLiveCardButton != null) {
			ProgressBar progressBar = (ProgressBar) osmLiveCardButton.findViewById(R.id.card_button_progress);
			TextViewEx buttonTitle = (TextViewEx) osmLiveCardButton.findViewById(R.id.card_button_title);
			TextViewEx buttonSubtitle = (TextViewEx) osmLiveCardButton.findViewById(R.id.card_button_subtitle);
			if (!purchaseHelper.hasPrices()) {
				buttonTitle.setText(getString(R.string.purchase_subscription_title, getString(R.string.osm_live_default_price)));
			} else {
				buttonTitle.setText(getString(R.string.purchase_subscription_title, purchaseHelper.getLiveUpdatesPrice()));
			}
			buttonSubtitle.setText(R.string.osm_live_month_cost_desc);
			if (progress) {
				buttonTitle.setVisibility(View.GONE);
				buttonSubtitle.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				osmLiveCardButton.setOnClickListener(null);
			} else {
				buttonTitle.setVisibility(View.VISIBLE);
				buttonSubtitle.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				osmLiveCardButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						subscript();
						dismiss();
					}
				});
			}
		}
	}

	private void subscript() {
		FragmentActivity ctx = getActivity();
		if (ctx != null) {
			if (ctx instanceof OsmLiveActivity) {
				SubscriptionFragment subscriptionFragment = new SubscriptionFragment();
				subscriptionFragment.show(ctx.getSupportFragmentManager(), SubscriptionFragment.TAG);
			} else {
				Intent intent = new Intent(ctx, OsmLiveActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				intent.putExtra(OsmLiveActivity.OPEN_SUBSCRIPTION_INTENT_PARAM, true);
				ctx.startActivity(intent);
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
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		boolean requestingInventory = purchaseHelper != null && purchaseHelper.getActiveTask() == InAppPurchaseTaskType.REQUEST_INVENTORY;
		setupPlanTypeCardButton(requestingInventory);

		return cardView;
	}

	private void setupPlanTypeCardButton(boolean progress) {
		if (planTypeCardButton != null) {
			ProgressBar progressBar = (ProgressBar) planTypeCardButton.findViewById(R.id.card_button_progress);
			TextViewEx buttonTitle = (TextViewEx) planTypeCardButton.findViewById(R.id.card_button_title);
			TextViewEx buttonSubtitle = (TextViewEx) planTypeCardButton.findViewById(R.id.card_button_subtitle);
			buttonTitle.setText(getPlanTypeButtonTitle());
			buttonSubtitle.setText(getPlanTypeButtonDescription());
			if (progress) {
				buttonTitle.setVisibility(View.GONE);
				buttonSubtitle.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				planTypeCardButton.setOnClickListener(null);
			} else {
				buttonTitle.setVisibility(View.VISIBLE);
				buttonSubtitle.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				setPlanTypeButtonClickListener(planTypeCardButton);
			}
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
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
			setupOsmLiveCardButton(false);
			setupPlanTypeCardButton(false);
		}
	}

	@Override
	public void onGetItems() {
	}

	@Override
	public void onItemPurchased(String sku) {
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			setupOsmLiveCardButton(true);
			setupPlanTypeCardButton(true);
		}
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			setupOsmLiveCardButton(false);
			setupPlanTypeCardButton(false);
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
