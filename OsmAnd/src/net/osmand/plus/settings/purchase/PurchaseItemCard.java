package net.osmand.plus.settings.purchase;

import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.ACTIVE;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.CANCELLED;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.purchase.data.PurchaseUiData;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class PurchaseItemCard extends BaseCard {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

	private TextView tvTitle;
	private ImageView ivIcon;
	private TextView tvPurchaseType;
	private TextView tvBillingDate;
	private TextView tvStatus;

	private final PurchaseUiData purchase;
	private final InAppPurchaseHelper purchaseHelper;

	public PurchaseItemCard(@NonNull FragmentActivity activity,
	                        @NonNull InAppPurchaseHelper purchaseHelper,
	                        @NonNull PurchaseUiData purchase) {
		super(activity, false);
		this.purchase = purchase;
		this.purchaseHelper = purchaseHelper;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_purchase_item;
	}

	@Override
	protected void updateContent() {
		ivIcon = view.findViewById(R.id.icon);
		tvTitle = view.findViewById(R.id.title);
		tvPurchaseType = view.findViewById(R.id.purchase_type);
		tvBillingDate = view.findViewById(R.id.next_billing_date);
		tvStatus = view.findViewById(R.id.status);

		if (getListener() != null) {
			view.setOnClickListener(v -> notifyCardPressed());
		}
		ivIcon.setImageDrawable(getIcon(purchase.getIconId()));
		tvTitle.setText(purchase.getTitle());
		String purchaseType = purchase.getPurchaseType();
		tvPurchaseType.setText(purchaseType);
		AndroidUiHelper.updateVisibility(tvPurchaseType, !Algorithms.isEmpty(purchaseType));

		if (purchase.isSubscription() && !purchase.isPromo()) {
			prepareSubscriptionCard();
		} else if (purchase.isPromo()) {
			preparePromoCard();
		} else {
			prepareOneTimePaymentCard();
		}
	}

	private void prepareSubscriptionCard() {
		SubscriptionState state = purchase.getSubscriptionState();
		boolean autoRenewing = purchase.isAutoRenewing();
		long expiredTime = purchase.getExpireTime();
		if (autoRenewing) {
			String expiredTimeStr = null;
			if (expiredTime > 0) {
				expiredTimeStr = dateFormat.format(expiredTime);
			}
			if (!Algorithms.isEmpty(expiredTimeStr)) {
				tvBillingDate.setText(app.getString(R.string.next_billing_date, expiredTimeStr));
				AndroidUiHelper.updateVisibility(tvBillingDate, true);
			}
		} else if (state != ACTIVE && state != CANCELLED) {
			boolean renewVisible = purchase.isRenewVisible();
			View renewContainer = view.findViewById(R.id.renewContainer);
			renewContainer.setOnClickListener(v -> {
				if (purchase.getSku() != null) {
					InAppPurchaseHelper.subscribe(activity, purchaseHelper, purchase.getSku());
				}
			});
			AndroidUiHelper.updateVisibility(renewContainer, renewVisible);
			AndroidUtils.setBackground(activity, renewContainer, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			AndroidUtils.setBackground(activity, view.findViewById(R.id.renew), nightMode, R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_dark);
		}
		setupStatus(view, state, expiredTime, expiredTime);
	}

	private void prepareOneTimePaymentCard() {
		long purchaseTime = purchase.getPurchaseTime();
		if (purchaseTime > 0) {
			String dateStr = dateFormat.format(purchaseTime);
			String purchased = app.getString(R.string.shared_string_purchased);
			tvBillingDate.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, purchased, dateStr));
			AndroidUiHelper.updateVisibility(tvBillingDate, true);
		} else {
			AndroidUiHelper.updateVisibility(tvBillingDate, false);
		}
		// Don't show status for one-time payment purchase
		AndroidUiHelper.updateVisibility(tvStatus, false);
	}

	private void preparePromoCard() {
		SubscriptionState state = purchase.getSubscriptionState();
		long startTime = purchase.getPurchaseTime();
		long expiredTime = purchase.getExpireTime();
		AndroidUiHelper.updateVisibility(tvBillingDate, false);
		setupStatus(view, state, startTime, expiredTime);
	}

	@NonNull
	public PurchaseUiData getDisplayedData() {
		return purchase;
	}

	public void setupStatus(@NonNull View card, @NonNull SubscriptionState state, long startTime, long expireTime) {
		OsmandApplication app = (OsmandApplication) card.getContext().getApplicationContext();
		TextView status = view.findViewById(R.id.status);
		status.setText(getStatus(app, state, startTime, expireTime));
		AndroidUtils.setBackground(status, AppCompatResources.getDrawable(app, getStatusBackgroundRes(state)));
	}

	private String getStatus(@NonNull OsmandApplication app, @NonNull SubscriptionState state, long startTime, long expireTime) {
		switch (state) {
			case UNDEFINED:
				return app.getString(R.string.shared_string_undefined);
			case ACTIVE:
			case CANCELLED:
			case IN_GRACE_PERIOD:
				return expireTime > 0 ? app.getString(R.string.active_till, dateFormat.format(expireTime)) : app.getString(R.string.osm_live_active);
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
	private int getStatusBackgroundRes(@NonNull SubscriptionState state) {
		return state.isActive() ? R.drawable.bg_osmand_live_active : R.drawable.bg_osmand_live_cancelled;
	}

}
