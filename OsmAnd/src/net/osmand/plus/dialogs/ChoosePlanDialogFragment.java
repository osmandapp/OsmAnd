package net.osmand.plus.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.inapp.InAppHelper;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import static net.osmand.plus.OsmandApplication.SHOW_PLUS_VERSION_INAPP_PARAM;

public class ChoosePlanDialogFragment extends BaseOsmAndDialogFragment {
	public static final String TAG = ChoosePlanDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ChoosePlanDialogFragment.class);

	private static final String PLAN_TYPE_KEY = "plan_type";

	private boolean nightMode;
	private PlanType planType;

	private OsmAndFeature[] osmandLiveFeatures = {
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.CONTOUR_LINES_SEA_DEPTH,
			OsmAndFeature.DONATION_TO_OSM,
//			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};

	private OsmAndFeature[] osmandUnlimitedFeatures = {
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.MONTHLY_MAP_UPDATES,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
	};

	public enum PlanType {
		FREE_VERSION_BANNER(OsmAndFeature.UNLIMITED_DOWNLOADS, OsmAndFeature.UNLIMITED_DOWNLOADS);

		private final OsmAndFeature osmandLiveFeature;
		private final OsmAndFeature osmandUnlimitedFeature;

		PlanType(OsmAndFeature osmandLiveFeature, OsmAndFeature osmandUnlimitedFeature) {
			this.osmandLiveFeature = osmandLiveFeature;
			this.osmandUnlimitedFeature = osmandUnlimitedFeature;
		}

		public OsmAndFeature getOsmandLiveFeature() {
			return osmandLiveFeature;
		}

		public OsmAndFeature getOsmandUnlimitedFeature() {
			return osmandUnlimitedFeature;
		}
	}

	public enum OsmAndFeature {
		WIKIVOYAGE_OFFLINE(R.string.wikivoyage_offline),
		DAILY_MAP_UPDATES(R.string.daily_map_updates),
		MONTHLY_MAP_UPDATES(R.string.monthly_map_updates),
		UNLIMITED_DOWNLOADS(R.string.unlimited_downloads),
		WIKIPEDIA_OFFLINE(R.string.wikipedia_offline),
		CONTOUR_LINES_SEA_DEPTH(R.string.contour_lines_sea_depth),
		UNLOCK_ALL_FEATURES(R.string.unlock_all_features),
		DONATION_TO_OSM(R.string.donation_to_osm);

		private final int key;

		OsmAndFeature(int key) {
			this.key = key;
		}

		public String toHumanString(Context ctx) {
			return ctx.getString(key);
		}

		public static OsmAndFeature[] possibleValues() {
			return new OsmAndFeature[]{WIKIVOYAGE_OFFLINE, DAILY_MAP_UPDATES, MONTHLY_MAP_UPDATES, UNLIMITED_DOWNLOADS,
					WIKIPEDIA_OFFLINE, CONTOUR_LINES_SEA_DEPTH, UNLOCK_ALL_FEATURES, DONATION_TO_OSM};
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		Dialog dialog = new Dialog(getContext(), themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
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
				infoDescription.setText(ctx.getString(R.string.free_version_message,
						DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS));
				//infoDescription.setVisibility(View.GONE);
				//View freeVersionInfoView = inflate(R.layout.purchase_dialog_info_free_version, infoContainer);
				//initFreeVersionInfoView(ctx, freeVersionInfoView);
				//infoContainer.addView(freeVersionInfoView);

				break;
			}
		}
		cardsContainer.addView(buildOsmAndLiveCard(ctx, cardsContainer));
		cardsContainer.addView(buildOsmAndUnlimitedCard(ctx, cardsContainer));
		return view;
	}

	/*
	private void initFreeVersionInfoView(Context ctx, View freeVersionInfoView) {
		TextView downloadsLeftTextView = (TextView) freeVersionInfoView.findViewById(R.id.downloadsLeftTextView);
		ProgressBar downloadsLeftProgressBar = (ProgressBar) freeVersionInfoView.findViewById(R.id.downloadsLeftProgressBar);
		TextView freeVersionDescriptionTextView = (TextView) freeVersionInfoView
				.findViewById(R.id.freeVersionDescriptionTextView);

		OsmandSettings settings = getMyApplication().getSettings();
		final Integer mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get();
		downloadsLeftProgressBar.setProgress(mapsDownloaded);
		int downloadsLeft = DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - mapsDownloaded;
		downloadsLeft = Math.max(downloadsLeft, 0);
		downloadsLeftTextView.setText(ctx.getString(R.string.downloads_left_template, downloadsLeft));
		downloadsLeftProgressBar.setMax(DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS);
		freeVersionDescriptionTextView.setText(ctx.getString(R.string.free_version_message,
				DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS));

		LinearLayout marksLinearLayout = (LinearLayout) freeVersionInfoView.findViewById(R.id.marksLinearLayout);
		Space spaceView = new Space(ctx);
		LinearLayout.LayoutParams layoutParams =
				new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		spaceView.setLayoutParams(layoutParams);
		marksLinearLayout.addView(spaceView);
		int markWidth = (int) (1 * ctx.getResources().getDisplayMetrics().density);
		int colorBlack = ctx.getResources().getColor(nightMode ? R.color.wikivoyage_bg_dark : R.color.wikivoyage_bg_light);
		for (int i = 1; i < DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS; i++) {
			View markView = new View(ctx);
			layoutParams = new LinearLayout.LayoutParams(markWidth, ViewGroup.LayoutParams.MATCH_PARENT);
			markView.setLayoutParams(layoutParams);
			markView.setBackgroundColor(colorBlack);
			marksLinearLayout.addView(markView);
			spaceView = new Space(ctx);
			layoutParams = new LinearLayout.LayoutParams(0,
					ViewGroup.LayoutParams.MATCH_PARENT, 1);
			spaceView.setLayoutParams(layoutParams);
			marksLinearLayout.addView(spaceView);
		}
	}
	*/

	private View inflate(@LayoutRes int layoutId, @Nullable ViewGroup container) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		return LayoutInflater.from(new ContextThemeWrapper(getContext(), themeRes))
				.inflate(layoutId, container, false);
	}

	private ViewGroup buildOsmAndLiveCard(@NonNull Context ctx, ViewGroup container) {
		ViewGroup cardView = (ViewGroup) inflate(R.layout.purchase_dialog_active_card, container);
		TextView headerTitle = (TextView) cardView.findViewById(R.id.header_title);
		TextView headerDescr = (TextView) cardView.findViewById(R.id.header_descr);
		headerTitle.setText(R.string.osm_live);
		headerDescr.setText(R.string.osm_live_subscription);
		ViewGroup rowsContainer = (ViewGroup) cardView.findViewById(R.id.rows_container);
		View featureRowDiv = null;
		for (OsmAndFeature feature : osmandLiveFeatures) {
			String featureName = feature.toHumanString(ctx);
//			View featureRow = inflate(planType.osmandLiveFeature == feature
//					? R.layout.purchase_dialog_card_selected_row : R.layout.purchase_dialog_card_row, cardView);
			View featureRow = inflate(R.layout.purchase_dialog_card_row, cardView);
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

		View cardButton = cardView.findViewById(R.id.card_button);
		TextViewEx buttonTitle = (TextViewEx) cardButton.findViewById(R.id.card_button_title);
		TextViewEx buttonSubtitle = (TextViewEx) cardButton.findViewById(R.id.card_button_subtitle);
		if (!InAppHelper.hasPrices(getMyApplication())) {
			buttonTitle.setText(ctx.getString(R.string.purchase_subscription_title, ctx.getString(R.string.osm_live_default_price)));
		} else {
			buttonTitle.setText(ctx.getString(R.string.purchase_subscription_title, InAppHelper.getLiveUpdatesPrice()));
		}
		buttonSubtitle.setText(R.string.osm_live_month_cost_desc);
		cardButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				subscript();
				dismiss();
			}
		});
		return cardView;
	}

	private void subscript() {
		Activity ctx = getActivity();
		if (ctx != null) {
			getMyApplication().logEvent(ctx, "click_subscribe_live_osm");
			Intent intent = new Intent(ctx, OsmLiveActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.putExtra(OsmLiveActivity.OPEN_SUBSCRIPTION_INTENT_PARAM, true);
			ctx.startActivity(intent);
		}
	}

	private ViewGroup buildOsmAndUnlimitedCard(@NonNull Context ctx, ViewGroup container) {
		ViewGroup cardView = (ViewGroup) inflate(R.layout.purchase_dialog_card, container);
		TextView headerTitle = (TextView) cardView.findViewById(R.id.header_title);
		TextView headerDescr = (TextView) cardView.findViewById(R.id.header_descr);
		headerTitle.setText(R.string.osmand_unlimited);
		headerDescr.setText(R.string.in_app_purchase);
		ViewGroup rowsContainer = (ViewGroup) cardView.findViewById(R.id.rows_container);
		View featureRow = null;
		for (OsmAndFeature feature : osmandUnlimitedFeatures) {
			String featureName = feature.toHumanString(ctx);
			featureRow = inflate(planType.osmandUnlimitedFeature == feature
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

		View cardButton = cardView.findViewById(R.id.card_button);
		TextViewEx buttonTitle = (TextViewEx) cardButton.findViewById(R.id.card_button_title);
		TextViewEx buttonSubtitle = (TextViewEx) cardButton.findViewById(R.id.card_button_subtitle);
		if (!InAppHelper.hasPrices(getMyApplication())) {
			buttonTitle.setText(ctx.getString(R.string.purchase_unlim_title, ctx.getString(R.string.full_version_price)));
		} else {
			buttonTitle.setText(ctx.getString(R.string.purchase_unlim_title, InAppHelper.getFullVersionPrice()));
		}
		buttonSubtitle.setText(R.string.in_app_purchase_desc);
		cardButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				purchaseFullVersion();
				dismiss();
			}
		});
		return cardView;
	}

	public void purchaseFullVersion() {
		/*
		OsmandApplication app = getMyApplication();
		if (app.getRemoteBoolean(SHOW_PLUS_VERSION_INAPP_PARAM, true)) {
			app.logEvent(getActivity(), "in_app_purchase_redirect_from_banner");
		} else {
			app.logEvent(getActivity(), "paid_version_redirect_from_banner");
		}
		if (Version.isFreeVersion(app)) {
			if (app.getRemoteBoolean(SHOW_PLUS_VERSION_INAPP_PARAM, true)) {
				if (inAppHelper != null) {
					app.logEvent(this, "in_app_purchase_redirect");
					inAppHelper.purchaseFullVersion(this);
				}
			} else {
				app.logEvent(this, "paid_version_redirect");
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(Version.getUrlWithUtmRef(app, "net.osmand.plus")));
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException e) {
					LOG.error("ActivityNotFoundException", e);
				}
			}
		}
		*/
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
