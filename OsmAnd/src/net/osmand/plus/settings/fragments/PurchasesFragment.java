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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.liveupdates.LiveUpdatesFragmentNew;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

public class PurchasesFragment extends BaseOsmAndFragment {

	public static final String TAG = PurchasesFragment.class.getName();
	public static final String KEY_IS_SUBSCRIBER = "action_is_new";
	private static final String PLAY_STORE_SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions";
	private static final String PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";
	private InAppPurchaseHelper purchaseHelper;
	private View mainView;
	private Context context;
	private OsmandApplication app;
	private String url;
	private Boolean isSubscriber;

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			PurchasesFragment fragment = new PurchasesFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
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
		purchaseHelper = getInAppPurchaseHelper();
		isSubscriber = Version.isPaidVersion(app);
		final MapActivity mapActivity = (MapActivity) getActivity();
		final boolean nightMode = !getMyApplication().getSettings().isLightContent();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);

		if (!isSubscriber) {
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
		mainView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
		LinearLayout purchasesRestore = mainView.findViewById(R.id.restore_purchases);
		purchasesRestore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (purchaseHelper != null && !purchaseHelper.hasInventory()) {
					purchaseHelper.requestInventory();
				}
			}
		});
		LinearLayout newDeviceAccountContainer = mainView.findViewById(R.id.new_device_account_container);
		newDeviceAccountContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WikipediaDialogFragment.showFullArticle(context, Uri.parse("https://docs.osmand.net/en/main@latest/osmand/purchases#new-device--new-account"), nightMode);
			}
		});

		setFormatStrings();
		return mainView;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_IS_SUBSCRIBER, isSubscriber);
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
				WikipediaDialogFragment.showFullArticle(context, Uri.parse("https://docs.osmand.net/en/main@latest/osmand/purchases"), nightMode);
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
		String urlSupport = "mailto:support@osmand.net";
		spannableStringSupport.setSpan(new URLSpan(urlSupport), 0, spannableStringSupport.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		String emailString = "support@osmand.net";
		String supportDescriptionString = getString(R.string.contact_support_description, emailString);
		SpannableString spannableStringMail = new SpannableString(supportDescriptionString);
		int startIndex = supportDescriptionString.indexOf(emailString);
		int endIndex = startIndex + emailString.length();
		StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
		spannableStringMail.setSpan(boldSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		contactSupportLink.setText(spannableStringSupport);
		supportDescription.setText(spannableStringMail);

		AndroidUtils.removeLinkUnderline(contactSupportLink);

		contactSupportLink.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private void getSkuAppId() {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		if (purchaseHelper != null) {
			String sku = purchaseHelper.getFullVersion().getSku();
			url = String.format(PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL,
					sku, context.getPackageName());
		} else {
			url = PLAY_STORE_SUBSCRIPTION_URL;
		}
	}
}
