package net.osmand.plus.settings.fragments;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
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
import java.util.List;
import java.util.Locale;

public class SubscriptionsListCard extends MapBaseCard {

	private final InAppPurchaseHelper purchaseHelper;

	private final SimpleDateFormat dateFormat;

	@Override
	public int getCardLayoutId() {
		return R.layout.subscriptions_list_card;
	}

	public SubscriptionsListCard(@NonNull MapActivity mapActivity, @NonNull InAppPurchaseHelper purchaseHelper) {
		super(mapActivity, false);
		this.purchaseHelper = purchaseHelper;
		this.dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
	}

	@Override
	protected void updateContent() {
		if (purchaseHelper == null || Algorithms.isEmpty(purchaseHelper.getEverMadeMainPurchases())) {
			AndroidUiHelper.updateVisibility(view, false);
			return;
		} else {
			AndroidUiHelper.updateVisibility(view, true);
		}
		InAppPurchases purchases = purchaseHelper.getInAppPurchases();

		LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		((ViewGroup) view).removeAllViews();

		List<InAppPurchase> subscriptions = purchaseHelper.getEverMadeMainPurchases();
		for (int i = 0; i < subscriptions.size(); i++) {
			InAppPurchase purchase = subscriptions.get(i);

			View card = inflater.inflate(R.layout.subscription_layout, null, false);
			((ViewGroup) view).addView(card);

			ImageView icon = card.findViewById(R.id.icon);
			TextView title = card.findViewById(R.id.title);
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
				setupSubscriptionCard((InAppSubscription) purchase, card);
			} else {
				setupPurchaseCard(purchase, card);
			}

			int dividerLayout = i + 1 == subscriptions.size() ? R.layout.simple_divider_item : R.layout.divider_half_item;
			View divider = inflater.inflate(dividerLayout, (ViewGroup) view, false);
			((ViewGroup) view).addView(divider);
		}
	}

	private void setupPurchaseCard(@NonNull InAppPurchase purchase, @NonNull View card) {
		TextView purchaseType = card.findViewById(R.id.purchase_type);
		purchaseType.setText(R.string.in_app_purchase_desc);

		TextView purchaseDate = card.findViewById(R.id.next_billing_date);
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
		AndroidUiHelper.updateVisibility(card.findViewById(R.id.status), false);
	}

	private void setupSubscriptionCard(@NonNull InAppSubscription subscription, @NonNull View card) {
		SubscriptionState state = subscription.getState();
		boolean autoRenewing = false;
		if (subscription.isPurchased() && subscription.getPurchaseInfo() != null) {
			autoRenewing = subscription.getPurchaseInfo().isAutoRenewing();
			state = SubscriptionState.ACTIVE;
		} else if (state != SubscriptionState.UNDEFINED) {
			autoRenewing = state == SubscriptionState.ACTIVE || state == SubscriptionState.IN_GRACE_PERIOD;
		}

		TextView subscriptionPeriod = card.findViewById(R.id.purchase_type);
		String period = app.getString(subscription.getPeriodTypeString());
		if (!Algorithms.isEmpty(period)) {
			subscriptionPeriod.setText(period);
			AndroidUiHelper.updateVisibility(subscriptionPeriod, true);
		}

		if (autoRenewing) {
			TextView nextBillingDate = card.findViewById(R.id.next_billing_date);
			String expiredTimeStr = null;
			long expiredTime = subscription.getExpireTime();
			if (expiredTime == 0) {
				expiredTime = subscription.getCalculatedExpiredTime();
			}
			if (expiredTime > 0) {
				expiredTimeStr = dateFormat.format(expiredTime);
			}
			if (!Algorithms.isEmpty(expiredTimeStr)) {
				nextBillingDate.setText(app.getString(R.string.next_billing_date, expiredTimeStr));
				AndroidUiHelper.updateVisibility(nextBillingDate, true);
			}
		} else {
			View renewContainer = card.findViewById(R.id.renewContainer);
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

			View renew = card.findViewById(R.id.renew);
			AndroidUtils.setBackground(mapActivity, renew, nightMode,
					R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_dark);
		}

		TextView status = card.findViewById(R.id.status);
		status.setText(app.getString(state.getStringRes()));
		AndroidUtils.setBackground(status, app.getUIUtilities().getIcon(getBackgroundRes(state)));
	}

	@DrawableRes
	private int getBackgroundRes(@NonNull SubscriptionState state) {
		return state == SubscriptionState.ACTIVE || state == SubscriptionState.IN_GRACE_PERIOD
				? R.drawable.bg_osmand_live_active : R.drawable.bg_osmand_live_cancelled;
	}
}