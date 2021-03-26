package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.liveupdates.CountrySelectionFragment;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
import net.osmand.plus.liveupdates.CountrySelectionFragment.OnFragmentInteractionListener;
import net.osmand.plus.liveupdates.LiveUpdatesFragmentNew;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class PurchasesFragment extends BaseOsmAndFragment implements InAppPurchaseListener, OnFragmentInteractionListener {

	private static final Log log = PlatformUtil.getLog(PurchasesFragment.class);
	public static final String TAG = PurchasesFragment.class.getName();

	public static final String KEY_IS_SUBSCRIBER = "action_is_new";

	private static final String PLAY_STORE_SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions";
	private static final String PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";
	private static final String EMAIL_DEEPLINK_URI = "mailto:support@osmand.net";
	private static final String OSMAND_EMAIL = "support@osmand.net";
	private static final String OSMAND_NEW_DEVICE_URL = "https://docs.osmand.net/en/main@latest/osmand/purchases#new-device--new-account";
	private static final String OSMAND_PURCHASES_URL = "https://docs.osmand.net/en/main@latest/osmand/purchases";

	private OsmandApplication app;
	private Context context;
	private InAppPurchaseHelper purchaseHelper;

	private View mainView;
	private SubscriptionsCard subscriptionsCard;

	private CountrySelectionFragment countrySelectionFragment = new CountrySelectionFragment();

	private String url;
	private Boolean isPaidVersion;

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			PurchasesFragment fragment = new PurchasesFragment();
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			log.error(e);
			return false;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		app = getMyApplication();
		context = requireContext();
		isPaidVersion = Version.isPaidVersion(app);
		final MapActivity mapActivity = getMapActivity();
		final boolean nightMode = !app.getSettings().isLightContent();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);

		if (isPaidVersion) {
			mainView = themedInflater.inflate(R.layout.purchases_layout, container, false);
			setSubscriptionClick(mapActivity);
		} else {
			mainView = themedInflater.inflate(R.layout.empty_purchases_layout, container, false);
			LinearLayout osmandLive = mainView.findViewById(R.id.osmand_live);
			osmandLive.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getMyApplication() != null && getMyActivity() != null) {
						ChoosePlanDialogFragment.showDialogInstance(getMyApplication(), getMyActivity().getSupportFragmentManager(), ChoosePlanDialogFragment.ChoosePlanDialogType.OSM_LIVE);
					}
				}
			});
			TextView infoDescription = mainView.findViewById(R.id.info_description);

			String restorePurchases = getString(R.string.restore_purchases);
			String infoPurchases = String.format(getString(R.string.empty_purchases_description), restorePurchases);
			infoDescription.setText(infoPurchases);
		}
		AndroidUtils.addStatusBarPadding21v(getActivity(), mainView);
		createToolbar(mainView, nightMode);
		LinearLayout purchasesRestore = mainView.findViewById(R.id.restore_purchases);
		purchasesRestore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (purchaseHelper != null) {
					purchaseHelper.requestInventory();
				}
			}
		});
		LinearLayout newDeviceAccountContainer = mainView.findViewById(R.id.new_device_account_container);
		newDeviceAccountContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WikipediaDialogFragment.showFullArticle(context, Uri.parse(OSMAND_NEW_DEVICE_URL), nightMode);
			}
		});

		setFormatStrings();
		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		purchaseHelper = getInAppPurchaseHelper();
		if (isPaidVersion) {
			MapActivity mapActivity = getMapActivity();
			if (getMapActivity() != null && purchaseHelper != null) {
				ViewGroup subscriptionsCardContainer = mainView.findViewById(R.id.subscriptions_card_container);
				subscriptionsCardContainer.removeAllViews();
				subscriptionsCard = new SubscriptionsCard(mapActivity, purchaseHelper);
				subscriptionsCardContainer.addView(subscriptionsCard.build(mapActivity));
			}
			setupSupportRegion();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_IS_SUBSCRIBER, isPaidVersion);
	}

	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState != null) {
			savedInstanceState.getBoolean(KEY_IS_SUBSCRIBER);
		}
	}

	private void setSubscriptionClick(final MapActivity mapActivity) {
		LinearLayout reportContainer = mainView.findViewById(R.id.report_container);
		reportContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mapActivity, OsmLiveActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				if (mapActivity != null) {
					mapActivity.startActivity(intent);
				}
			}
		});
		LinearLayout liveUpdatesContainer = mainView.findViewById(R.id.live_updates_container);
		liveUpdatesContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					LiveUpdatesFragmentNew.showInstance(activity.getSupportFragmentManager(), PurchasesFragment.this);
				}
			}
		});
		getSkuAppId();
		LinearLayout manageSubscriptionContainer = mainView.findViewById(R.id.manage_subscription_container);
		manageSubscriptionContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				if (AndroidUtils.isIntentSafe(context, intent)) {
					startActivity(intent);
				}
			}
		});

	}

	private void setupSupportRegion() {
		String region = getSupportRegionName(app, purchaseHelper);
		String header = getSupportRegionHeader(app, region);
		TextView supportRegionHeader = mainView.findViewById(R.id.support_region_header);
		TextView supportRegion = mainView.findViewById(R.id.support_region);
		supportRegionHeader.setText(header);
		supportRegion.setText(region);

		View supportRegionContainer = mainView.findViewById(R.id.support_region_container);
		supportRegionContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CountrySelectionFragment countryCountrySelectionFragment = countrySelectionFragment;
				countryCountrySelectionFragment.show(getChildFragmentManager(), CountrySelectionFragment.TAG);
			}
		});

		countrySelectionFragment.initCountries(app);
	}

	public static String getSupportRegionName(OsmandApplication app, InAppPurchaseHelper purchaseHelper) {
		OsmandSettings settings = app.getSettings();
		String countryName = settings.BILLING_USER_COUNTRY.get();
		if (purchaseHelper != null) {
			InAppSubscription monthlyPurchased = purchaseHelper.getPurchasedMonthlyLiveUpdates();
			if (monthlyPurchased != null && monthlyPurchased.isDonationSupported()) {
				if (Algorithms.isEmpty(countryName)) {
					if (OsmandSettings.BILLING_USER_DONATION_NONE_PARAMETER.equals(settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get())) {
						countryName = app.getString(R.string.osmand_team);
					} else {
						countryName = app.getString(R.string.shared_string_world);
					}
				}
			} else {
				countryName = app.getString(R.string.osmand_team);
			}
		} else {
			countryName = app.getString(R.string.osmand_team);
		}
		return countryName;
	}

	public static String getSupportRegionHeader(OsmandApplication app, String supportRegion) {
		return supportRegion.equals(app.getString(R.string.osmand_team)) ?
				app.getString(R.string.default_buttons_support) :
				app.getString(R.string.osm_live_support_region);
	}

	@Nullable
	public InAppPurchaseHelper getInAppPurchaseHelper() {
		Activity activity = getActivity();
		if (activity instanceof OsmandInAppPurchaseActivity) {
			return ((OsmandInAppPurchaseActivity) activity).getPurchaseHelper();
		} else {
			return null;
		}
	}

	private void createToolbar(View mainView, final boolean nightMode) {
		AppBarLayout appbar = mainView.findViewById(R.id.appbar);
		View toolbar = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.profile_preference_toolbar_with_icon, appbar, false);

		View iconToolbarContainer = toolbar.findViewById(R.id.icon_toolbar);
		ImageView icon = iconToolbarContainer.findViewById(R.id.profile_icon);
		icon.setImageResource(R.drawable.ic_action_help_online);
		icon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WikipediaDialogFragment.showFullArticle(context, Uri.parse(OSMAND_PURCHASES_URL), nightMode);
			}
		});
		ImageButton backButton = toolbar.findViewById(R.id.close_button);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity fragmentActivity = getActivity();
				if (fragmentActivity != null) {
					fragmentActivity.onBackPressed();
				}
			}
		});

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getString(R.string.purchases));
		appbar.addView(toolbar);
	}

	public void setFormatStrings() {
		TextView contactSupportLink = mainView.findViewById(R.id.contact_support_title);
		TextView supportDescription = mainView.findViewById(R.id.support_link_title);

		SpannableString spannableStringSupport = new SpannableString(getString(R.string.contact_support));
		spannableStringSupport.setSpan(new URLSpan(EMAIL_DEEPLINK_URI), 0, spannableStringSupport.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		String supportDescriptionString = getString(R.string.contact_support_description, OSMAND_EMAIL);
		SpannableString spannableStringMail = new SpannableString(supportDescriptionString);
		int startIndex = supportDescriptionString.indexOf(OSMAND_EMAIL);
		int endIndex = startIndex + OSMAND_EMAIL.length();
		StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
		spannableStringMail.setSpan(boldSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		contactSupportLink.setText(spannableStringSupport);
		supportDescription.setText(spannableStringMail);

		AndroidUtils.removeLinkUnderline(contactSupportLink);

		contactSupportLink.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private void getSkuAppId() {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		if (purchaseHelper != null && purchaseHelper.getFullVersion() != null) {
			String sku = purchaseHelper.getFullVersion().getSku();
			url = String.format(PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL,
					sku, context.getPackageName());
		} else {
			url = PLAY_STORE_SUBSCRIPTION_URL;
		}
	}

	@Override
	public void onSearchResult(CountryItem selectedCountryItem) {
		String countryName = selectedCountryItem != null ? selectedCountryItem.getLocalName() : "";
		String countryDownloadName = selectedCountryItem != null ?
				selectedCountryItem.getDownloadName() : OsmandSettings.BILLING_USER_DONATION_WORLD_PARAMETER;

		OsmandApplication app = getMyApplication();
		if (app != null) {
			TextView supportRegionHeader = mainView.findViewById(R.id.support_region_header);
			TextView supportRegion = mainView.findViewById(R.id.support_region);
			supportRegionHeader.setText(getSupportRegionHeader(app, countryName));
			supportRegion.setText(countryName);
			app.getSettings().BILLING_USER_COUNTRY.set(countryName);
			app.getSettings().BILLING_USER_COUNTRY_DOWNLOAD_NAME.set(countryDownloadName);
		}
	}

	@Override
	public void onError(InAppPurchaseHelper.InAppPurchaseTaskType taskType, String error) {
	}

	@Override
	public void onGetItems() {
		if (subscriptionsCard != null) {
			subscriptionsCard.update();
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		if (purchaseHelper != null) {
			purchaseHelper.requestInventory();
		}
	}

	@Override
	public void showProgress(InAppPurchaseHelper.InAppPurchaseTaskType taskType) {
	}

	@Override
	public void dismissProgress(InAppPurchaseHelper.InAppPurchaseTaskType taskType) {
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}
}
