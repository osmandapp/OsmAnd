package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.liveupdates.CountrySelectionFragment;
import net.osmand.plus.liveupdates.CountrySelectionFragment.OnFragmentInteractionListener;
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

	private static final String OSMAND_PURCHASES_URL = "https://docs.osmand.net/en/main@latest/osmand/purchases";

	private OsmandApplication app;
	private InAppPurchaseHelper purchaseHelper;

	private ViewGroup cardsContainer;
	private SubscriptionsCard subscriptionsCard;

	private boolean nightMode;
	private Boolean isPaidVersion;

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			PurchasesFragment fragment = new PurchasesFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
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
		isPaidVersion = Version.isPaidVersion(app);
		nightMode = !app.getSettings().isLightContent();
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);

		View mainView = themedInflater.inflate(R.layout.purchases_layout, container, false);
		AndroidUtils.addStatusBarPadding21v(getActivity(), mainView);
		createToolbar(mainView, nightMode);
		cardsContainer = mainView.findViewById(R.id.cards_container);

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateCards();
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		purchaseHelper = getInAppPurchaseHelper();
		cardsContainer.removeAllViews();
		if (mapActivity == null || purchaseHelper == null) {
			return;
		}

		boolean hasSubscriptions = !Algorithms.isEmpty(purchaseHelper.getEverMadeSubscriptions());
		if (hasSubscriptions) {
			subscriptionsCard = new SubscriptionsCard(mapActivity, this, purchaseHelper);
			cardsContainer.addView(subscriptionsCard.build(mapActivity));
		}

		cardsContainer.addView(new TroubleshootingOrPurchasingCard(mapActivity, purchaseHelper, isPaidVersion)
				.build(mapActivity));
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
			isPaidVersion = savedInstanceState.getBoolean(KEY_IS_SUBSCRIBER);
		}
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
				if (getContext() != null) {
					WikipediaDialogFragment.showFullArticle(getContext(), Uri.parse(OSMAND_PURCHASES_URL), nightMode);
				}
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

	@Override
	public void onError(InAppPurchaseHelper.InAppPurchaseTaskType taskType, String error) {
	}

	@Override
	public void onGetItems() {
		if (app != null) {
			isPaidVersion = Version.isPaidVersion(app);
		}
		updateCards();
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		isPaidVersion = true;
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

	@Override
	public void onSearchResult(CountrySelectionFragment.CountryItem name) {
		if (subscriptionsCard != null) {
			subscriptionsCard.onSupportRegionSelected(name);
		}
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}
}