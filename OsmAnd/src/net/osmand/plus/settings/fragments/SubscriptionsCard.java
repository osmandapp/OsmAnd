package net.osmand.plus.settings.fragments;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.util.Algorithms;

public class SubscriptionsCard extends BaseCard {

	private static final String PLAY_STORE_SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions";
	private static final String PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";

	private Fragment target;
	private SubscriptionsListCard subscriptionsListCard;

	private InAppPurchaseHelper purchaseHelper;

	@Override
	public int getCardLayoutId() {
		return R.layout.subscriptions_card;
	}

	public SubscriptionsCard(@NonNull MapActivity mapActivity, @NonNull Fragment target, @NonNull InAppPurchaseHelper purchaseHelper) {
		super(mapActivity, false);
		this.target = target;
		this.purchaseHelper = purchaseHelper;
	}

	@Override
	protected void updateContent() {
		if (purchaseHelper == null || Algorithms.isEmpty(purchaseHelper.getEverMadeSubscriptions())) {
			AndroidUiHelper.updateVisibility(view, false);
			return;
		} else {
			AndroidUiHelper.updateVisibility(view, true);
		}

		updateSubscriptionsListCard();

		LinearLayout reportContainer = view.findViewById(R.id.report_container);
		reportContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mapActivity, OsmLiveActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				mapActivity.startActivity(intent);
			}
		});

		LinearLayout liveUpdatesContainer = view.findViewById(R.id.live_updates_container);
		liveUpdatesContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LiveUpdatesFragment.showInstance(mapActivity.getSupportFragmentManager(), target);
			}
		});

		final String subscriptionUrl = getSubscriptionUrl();
		LinearLayout manageSubscriptionContainer = view.findViewById(R.id.manage_subscription_container);
		manageSubscriptionContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(subscriptionUrl));
				if (AndroidUtils.isIntentSafe(mapActivity, intent)) {
					target.startActivity(intent);
				}
			}
		});
	}

	public void updateSubscriptionsListCard() {
		if (subscriptionsListCard == null) {
			ViewGroup subscriptionsListContainer = view.findViewById(R.id.subscriptions_list_container);
			subscriptionsListContainer.removeAllViews();
			subscriptionsListCard = new SubscriptionsListCard(mapActivity, purchaseHelper);
			subscriptionsListContainer.addView(subscriptionsListCard.build(mapActivity));
		} else {
			subscriptionsListCard.update();
		}
	}

	private String getSubscriptionUrl() {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		if (purchaseHelper != null && purchaseHelper.getFullVersion() != null) {
			String sku = purchaseHelper.getFullVersion().getSku();
			return String.format(PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL,
					sku, mapActivity.getPackageName());
		} else {
			return PLAY_STORE_SUBSCRIPTION_URL;
		}
	}
}