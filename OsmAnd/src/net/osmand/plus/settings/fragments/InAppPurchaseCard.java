package net.osmand.plus.settings.fragments;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class InAppPurchaseCard extends BaseCard {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

	private final InAppPurchase purchase;
	private final InAppPurchaseHelper purchaseHelper;
	private final InAppPurchases purchases;

	public InAppPurchaseCard(@NonNull FragmentActivity activity, @NonNull InAppPurchaseHelper purchaseHelper, @NonNull InAppPurchase purchase) {
		super(activity, false);
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
			icon.setImageDrawable(getIcon(R.drawable.ic_action_osmand_pro_logo_colored));
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

		boolean manageVisible = purchase instanceof InAppSubscription;
		boolean liveVisible = purchases.isLiveUpdatesSubscription(purchase);

		setupLiveButton(liveVisible);
		setupManageButton(manageVisible);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_divider), manageVisible || liveVisible);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), manageVisible && liveVisible);
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.active_color_basic);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(view.getContext(), color, 0.3f);
		AndroidUtils.setBackground(view.findViewById(R.id.selectable_list_item), drawable);
	}

	private void setupManageButton(boolean visible) {
		View manageSubscription = view.findViewById(R.id.manage_subscription);
		manageSubscription.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
				if (purchaseHelper != null) {
					purchaseHelper.manageSubscription(activity, purchase.getSku());
				}
			}
		});
		setupSelectableBackground(manageSubscription);
		ImageView icon = manageSubscription.findViewById(android.R.id.icon);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_purchases));

		TextView title = manageSubscription.findViewById(android.R.id.title);
		title.setText(R.string.manage_subscription);
		AndroidUiHelper.updateVisibility(manageSubscription, visible);
	}

	private void setupLiveButton(boolean visible) {
		View osmandLive = view.findViewById(R.id.osmand_live);
		osmandLive.setOnClickListener(v -> LiveUpdatesFragment.showInstance(activity.getSupportFragmentManager(), null));
		setupSelectableBackground(osmandLive);
		ImageView icon = osmandLive.findViewById(android.R.id.icon);
		TextView title = osmandLive.findViewById(android.R.id.title);

		title.setText(R.string.live_updates);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_osm_live));
		AndroidUiHelper.updateVisibility(osmandLive, visible);
	}

	private void setupPurchaseCard(@NonNull InAppPurchase purchase) {
		TextView purchaseType = view.findViewById(R.id.purchase_type);
		purchaseType.setText(R.string.in_app_purchase_desc);

		TextView purchaseDate = view.findViewById(R.id.next_billing_date);
		long purchaseTime = purchase.getPurchaseTime();
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
			boolean visible = true;
			if (purchases.isMapsSubscription(subscription)) {
				boolean isFullVersion = !Version.isFreeVersion(app) || InAppPurchaseHelper.isFullVersionPurchased(app, false);
				visible = !isFullVersion && !InAppPurchaseHelper.isSubscribedToAny(app, false);
			} else if (purchases.isOsmAndProSubscription(subscription)) {
				visible = !InAppPurchaseHelper.isOsmAndProAvailable(app, false);
			}
			View renewContainer = view.findViewById(R.id.renewContainer);
			renewContainer.setOnClickListener(v -> InAppPurchaseHelper.subscribe(activity, purchaseHelper, subscription.getSku()));

			AndroidUiHelper.updateVisibility(renewContainer, visible && Version.isInAppPurchaseSupported());
			AndroidUtils.setBackground(activity, renewContainer, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			AndroidUtils.setBackground(activity, view.findViewById(R.id.renew), nightMode, R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_dark);
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
}
