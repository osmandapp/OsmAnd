package net.osmand.plus.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
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
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import static net.osmand.plus.OsmandApplication.SHOW_PLUS_VERSION_INAPP_PARAM;

public class ChoosePlanDialogFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener {
	public static final String TAG = ChoosePlanDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ChoosePlanDialogFragment.class);

	private static final String PLAN_TYPE_KEY = "plan_type";

	private OsmandApplication app;
	private InAppPurchaseHelper purchaseHelper;

	private boolean nightMode;
	private PlanType planType;

	private View osmLiveCardButton;
	private View planTypeCardButton;

	public enum PlanType {
		FREE_VERSION_BANNER(
				new OsmAndFeature[]{
						OsmAndFeature.DAILY_MAP_UPDATES,
						OsmAndFeature.UNLIMITED_DOWNLOADS,
						OsmAndFeature.WIKIPEDIA_OFFLINE,
						OsmAndFeature.WIKIVOYAGE_OFFLINE,
						OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
						OsmAndFeature.SEA_DEPTH_MAPS,
						OsmAndFeature.UNLOCK_ALL_FEATURES,
						OsmAndFeature.DONATION_TO_OSM,
				},
				new OsmAndFeature[]{
						OsmAndFeature.DAILY_MAP_UPDATES,
						OsmAndFeature.UNLIMITED_DOWNLOADS,
				},
				new OsmAndFeature[]{
						OsmAndFeature.WIKIPEDIA_OFFLINE,
						OsmAndFeature.WIKIVOYAGE_OFFLINE,
						OsmAndFeature.UNLIMITED_DOWNLOADS,
						OsmAndFeature.MONTHLY_MAP_UPDATES,
				},
				new OsmAndFeature[]{});

		private final OsmAndFeature[] osmLiveFeatures;
		private final OsmAndFeature[] planTypeFeatures;
		private final OsmAndFeature[] selectedOsmLiveFeatures;
		private final OsmAndFeature[] selectedPlanTypeFeatures;

		PlanType(OsmAndFeature[] osmLiveFeatures, OsmAndFeature[] selectedOsmLiveFeatures,
				 OsmAndFeature[] planTypeFeatures, OsmAndFeature[] selectedPlanTypeFeatures) {
			this.osmLiveFeatures = osmLiveFeatures;
			this.planTypeFeatures = planTypeFeatures;
			this.selectedOsmLiveFeatures = selectedOsmLiveFeatures;
			this.selectedPlanTypeFeatures = selectedPlanTypeFeatures;
		}

		public OsmAndFeature[] getOsmLiveFeatures() {
			return osmLiveFeatures;
		}

		public OsmAndFeature[] getPlanTypeFeatures() {
			return planTypeFeatures;
		}

		public OsmAndFeature[] getSelectedOsmLiveFeatures() {
			return selectedOsmLiveFeatures;
		}

		public OsmAndFeature[] getSelectedPlanTypeFeatures() {
			return selectedPlanTypeFeatures;
		}

		public boolean hasSelectedOsmLiveFeature(OsmAndFeature feature) {
			if (selectedOsmLiveFeatures != null) {
				for (OsmAndFeature f : selectedOsmLiveFeatures) {
					if (feature == f) {
						return true;
					}
				}
			}
			return false;
		}

		public boolean hasSelectedPlanTypeFeature(OsmAndFeature feature) {
			if (selectedPlanTypeFeatures != null) {
				for (OsmAndFeature f : selectedPlanTypeFeatures) {
					if (feature == f) {
						return true;
					}
				}
			}
			return false;
		}
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
				case WIKIVOYAGE_OFFLINE:
					return ctx.getSettings().TRAVEL_ARTICLES_PURCHASED.get();
				case CONTOUR_LINES_HILLSHADE_MAPS:
					boolean srtmEnabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null;
					return srtmEnabled && ctx.getSettings().DEPTH_CONTOURS_PURCHASED.get();
			}
			return false;
		}

		public static OsmAndFeature[] possibleValues() {
			return new OsmAndFeature[]{WIKIVOYAGE_OFFLINE, DAILY_MAP_UPDATES, MONTHLY_MAP_UPDATES, UNLIMITED_DOWNLOADS,
					WIKIPEDIA_OFFLINE, CONTOUR_LINES_HILLSHADE_MAPS, UNLOCK_ALL_FEATURES, DONATION_TO_OSM};
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = getMyApplication();
		purchaseHelper = app.getInAppPurchaseHelper();

		Bundle args = getArguments();
		if (args == null) {
			args = savedInstanceState;
		}
		if (args != null) {
			String planTypeStr = args.getString(PLAN_TYPE_KEY);
			if (!TextUtils.isEmpty(planTypeStr)) {
				planType = PlanType.valueOf(planTypeStr);
			}
		}

		nightMode = isNightMode(getMapActivity() != null);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(getContext(), themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			if (Build.VERSION.SDK_INT >= 21) {
				window.setStatusBarColor(ContextCompat.getColor(getContext(), getStatusBarColor()));
			}
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Context ctx = getContext();
		if (planType == null || ctx == null) {
			return null;
		}
		View view = inflate(R.layout.purchase_dialog_fragment, container);

		view.findViewById(R.id.button_back).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		view.findViewById(R.id.button_later).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		ViewGroup infoContainer = (ViewGroup) view.findViewById(R.id.info_container);
		TextViewEx infoDescription = (TextViewEx) view.findViewById(R.id.info_description);
		ViewGroup cardsContainer = (ViewGroup) view.findViewById(R.id.cards_container);

		switch (planType) {
			case FREE_VERSION_BANNER: {
				infoDescription.setText(getString(R.string.free_version_message,
						DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS));
				break;
			}
		}
		cardsContainer.addView(buildOsmLiveCard(ctx, cardsContainer));
		cardsContainer.addView(buildPlanTypeCard(ctx, cardsContainer));
		return view;
	}

	@ColorRes
	protected int getStatusBarColor() {
		return nightMode ? R.color.status_bar_wikivoyage_dark : R.color.status_bar_wikivoyage_light;
	}

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
		for (OsmAndFeature feature : planType.getOsmLiveFeatures()) {
			String featureName = feature.toHumanString(ctx);
			View featureRow = inflate(planType.hasSelectedOsmLiveFeature(feature)
					? R.layout.purchase_dialog_card_selected_row : R.layout.purchase_dialog_card_row, cardView);
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
				osmLiveCardButton.setOnClickListener(new View.OnClickListener() {
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
		Activity ctx = getActivity();
		if (ctx != null) {
			app.logEvent(ctx, "click_subscribe_live_osm");
			Intent intent = new Intent(ctx, OsmLiveActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.putExtra(OsmLiveActivity.OPEN_SUBSCRIPTION_INTENT_PARAM, true);
			ctx.startActivity(intent);
		}
	}

	private ViewGroup buildPlanTypeCard(@NonNull Context ctx, ViewGroup container) {
		ViewGroup cardView = (ViewGroup) inflate(R.layout.purchase_dialog_card, container);
		TextView headerTitle = (TextView) cardView.findViewById(R.id.header_title);
		TextView headerDescr = (TextView) cardView.findViewById(R.id.header_descr);
		headerTitle.setText(R.string.osmand_unlimited);
		headerDescr.setText(R.string.in_app_purchase);
		ViewGroup rowsContainer = (ViewGroup) cardView.findViewById(R.id.rows_container);
		View featureRow = null;
		for (OsmAndFeature feature : planType.getPlanTypeFeatures()) {
			String featureName = feature.toHumanString(ctx);
			featureRow = inflate(planType.hasSelectedPlanTypeFeature(feature)
					? R.layout.purchase_dialog_card_selected_row : R.layout.purchase_dialog_card_row, cardView);
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
			if (!purchaseHelper.hasPrices()) {
				buttonTitle.setText(getString(R.string.purchase_unlim_title, getString(R.string.full_version_price)));
			} else {
				buttonTitle.setText(getString(R.string.purchase_unlim_title, purchaseHelper.getFullVersionPrice()));
			}
			buttonSubtitle.setText(R.string.in_app_purchase_desc);
			if (progress) {
				buttonTitle.setVisibility(View.GONE);
				buttonSubtitle.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				planTypeCardButton.setOnClickListener(null);
			} else {
				buttonTitle.setVisibility(View.VISIBLE);
				buttonSubtitle.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				planTypeCardButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						purchaseFullVersion();
						dismiss();
					}
				});
			}
		}
	}

	public void purchaseFullVersion() {
		if (app.getRemoteBoolean(SHOW_PLUS_VERSION_INAPP_PARAM, true)) {
			app.logEvent(getActivity(), "in_app_purchase_redirect_from_banner");
		} else {
			app.logEvent(getActivity(), "paid_version_redirect_from_banner");
		}
		if (Version.isFreeVersion(app)) {
			if (app.getRemoteBoolean(SHOW_PLUS_VERSION_INAPP_PARAM, true)) {
				InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
				if (purchaseHelper != null) {
					app.logEvent(getActivity(), "in_app_purchase_redirect");
					purchaseHelper.purchaseFullVersion(getActivity());
				}
			} else {
				app.logEvent(getActivity(), "paid_version_redirect");
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(Version.getUrlWithUtmRef(app, "net.osmand.plus")));
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException e) {
					LOG.error("ActivityNotFoundException", e);
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PLAN_TYPE_KEY, planType.name());
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
		PlanType planType = PlanType.FREE_VERSION_BANNER;
		showInstance(fm, planType);
	}

	private static void showInstance(@NonNull FragmentManager fm, PlanType planType) {
		try {
			Bundle args = new Bundle();
			args.putString(PLAN_TYPE_KEY, planType.name());

			ChoosePlanDialogFragment fragment = new ChoosePlanDialogFragment();
			fragment.setArguments(args);
			fragment.show(fm, TAG);

		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
