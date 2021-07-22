package net.osmand.plus.settings.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class InAppPurchaseCard extends MapBaseCard {

	private static final String PLAY_STORE_SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions";
	private static final String PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

	private final InAppPurchase purchase;
	private final InAppPurchaseHelper purchaseHelper;
	private final InAppPurchases purchases;

	public InAppPurchaseCard(@NonNull MapActivity mapActivity, @NonNull InAppPurchaseHelper purchaseHelper, @NonNull InAppPurchase purchase) {
		super(mapActivity);
		this.purchase = purchase;
		this.purchaseHelper = purchaseHelper;
		purchases = purchaseHelper.getInAppPurchases();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.inapp_purchase_card;
	}

	@Override
	protected void updateContent() {
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		if (purchases.isOsmAndProSubscription(purchase)) {
			title.setText(R.string.osmand_pro);
			icon.setImageDrawable(getIcon(R.drawable.ic_action_osmand_pro_logo));
		} else if (purchases.isLiveUpdatesSubscription(purchase)) {
			title.setText(R.string.osm_live);
			icon.setImageDrawable(getIcon(R.drawable.ic_action_subscription_osmand_live));
		} else if (purchases.isMapsSubscription(purchase) || purchases.isFullVersion(purchase)) {
			title.setText(R.string.maps_plus);
			icon.setImageDrawable(getIcon(R.drawable.ic_action_osmand_maps_plus));
		}
		if (purchase instanceof InAppSubscription) {
			setupSubscriptionCard((InAppSubscription) purchase);
		} else {
			setupPurchaseCard(purchase);
		}
		setupManageButton();
		setupLiveButton();
	}

	private void setupManageButton() {
		View manageSubscription = view.findViewById(R.id.manage_subscription);
		manageSubscription.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(getSubscriptionUrl()));
				if (AndroidUtils.isIntentSafe(mapActivity, intent)) {
					mapActivity.startActivity(intent);
				}
			}
		});
		ImageView icon = manageSubscription.findViewById(android.R.id.icon);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_purchases));

		TextView title = manageSubscription.findViewById(android.R.id.title);
		title.setText(R.string.manage_subscription);
	}

	private void setupLiveButton() {
		View osmandLive = view.findViewById(R.id.osmand_live);
		osmandLive.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(getSubscriptionUrl()));
				if (AndroidUtils.isIntentSafe(mapActivity, intent)) {
					mapActivity.startActivity(intent);
				}
			}
		});
		ImageView icon = osmandLive.findViewById(android.R.id.icon);
		TextView title = osmandLive.findViewById(android.R.id.title);

		title.setText(R.string.live_updates);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_osm_live));
		AndroidUiHelper.updateVisibility(osmandLive, purchases.isLiveUpdatesSubscription(purchase));
	}

	private void setupPurchaseCard(@NonNull InAppPurchase purchase) {
		TextView purchaseType = view.findViewById(R.id.purchase_type);
		purchaseType.setText(R.string.in_app_purchase_desc);

		TextView purchaseDate = view.findViewById(R.id.next_billing_date);
		long purchaseTime = purchase.getPurchaseInfo().getPurchaseTime();
		if (purchaseTime > 0) {
			String dateStr = dateFormat.format(purchaseTime);
			String purchased = app.getString(R.string.shared_string_purchased);
			purchaseDate.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, purchased, dateStr));
			AndroidUiHelper.updateVisibility(purchaseDate, true);
		} else {
			AndroidUiHelper.updateVisibility(purchaseDate, false);
		}
		AndroidUiHelper.updateVisibility(purchaseType, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.status), false);
	}

	private void setupSubscriptionCard(@NonNull InAppSubscription subscription) {
		SubscriptionState state = subscription.getState();
		boolean autoRenewing = false;
		if (subscription.isPurchased() && subscription.getPurchaseInfo() != null) {
			autoRenewing = subscription.getPurchaseInfo().isAutoRenewing();
			state = SubscriptionState.ACTIVE;
		} else if (state != SubscriptionState.UNDEFINED) {
			autoRenewing = state == SubscriptionState.ACTIVE || state == SubscriptionState.IN_GRACE_PERIOD;
		}

		TextView subscriptionPeriod = view.findViewById(R.id.purchase_type);
		String period = app.getString(subscription.getPeriodTypeString());
		if (!Algorithms.isEmpty(period)) {
			subscriptionPeriod.setText(period);
			AndroidUiHelper.updateVisibility(subscriptionPeriod, true);
		}

		long expiredTime = subscription.getExpireTime();
		if (expiredTime == 0) {
			expiredTime = subscription.getCalculatedExpiredTime();
		}
		if (autoRenewing) {
			String expiredTimeStr = null;
			if (expiredTime > 0) {
				expiredTimeStr = dateFormat.format(expiredTime);
			}
			TextView nextBillingDate = view.findViewById(R.id.next_billing_date);
			if (!Algorithms.isEmpty(expiredTimeStr)) {
				nextBillingDate.setText(app.getString(R.string.next_billing_date, expiredTimeStr));
				AndroidUiHelper.updateVisibility(nextBillingDate, true);
			}
		} else if (state != SubscriptionState.ACTIVE && state != SubscriptionState.CANCELLED) {
			View renewContainer = view.findViewById(R.id.renewContainer);
			AndroidUiHelper.updateVisibility(renewContainer, true);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(mapActivity, renewContainer, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			} else {
				AndroidUtils.setBackground(mapActivity, renewContainer, nightMode, R.drawable.btn_unstroked_light, R.drawable.btn_unstroked_dark);
			}
			final String sku = subscription.getSku();
			renewContainer.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					InAppPurchaseHelper.subscribe(mapActivity, purchaseHelper, sku);
				}
			});

			View renew = view.findViewById(R.id.renew);
			AndroidUtils.setBackground(mapActivity, renew, nightMode,
					R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_dark);
		}
		setupStatus(view, state, expiredTime, expiredTime);
	}

	protected static void setupStatus(@NonNull View card, @NonNull SubscriptionState state, long startTime, long expireTime) {
		OsmandApplication app = (OsmandApplication) card.getContext().getApplicationContext();
		TextView status = card.findViewById(R.id.status);
		status.setText(getStatus(app, state, startTime, expireTime));
		AndroidUtils.setBackground(status, AppCompatResources.getDrawable(app, getBackgroundRes(state)));
	}

	private static String getStatus(@NonNull OsmandApplication app, @NonNull SubscriptionState state, long startTime, long expireTime) {
		switch (state) {
			case UNDEFINED:
				return app.getString(R.string.shared_string_undefined);
			case ACTIVE:
			case CANCELLED:
			case IN_GRACE_PERIOD:
				return app.getString(R.string.active_till, dateFormat.format(expireTime));
			case EXPIRED:
				String expired = app.getString(R.string.expired);
				return app.getString(R.string.ltr_or_rtl_combine_via_space, expired, dateFormat.format(expireTime));
			case ON_HOLD:
				return app.getString(R.string.on_hold_since, dateFormat.format(startTime));
			case PAUSED:
				String paused = app.getString(R.string.shared_string_paused);
				return app.getString(R.string.ltr_or_rtl_combine_via_space, paused, dateFormat.format(expireTime));
		}
		return "";
	}

	@DrawableRes
	private static int getBackgroundRes(@NonNull SubscriptionState state) {
		return state.isActive() ? R.drawable.bg_osmand_live_active : R.drawable.bg_osmand_live_cancelled;
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
