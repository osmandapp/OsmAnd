package net.osmand.plus.settings.fragments;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Period;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;

public class SubscriptionsListCard extends BaseCard {

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
		if (purchaseHelper == null || Algorithms.isEmpty(purchaseHelper.getEverMadeSubscriptions())) {
			AndroidUiHelper.updateVisibility(view, false);
			return;
		} else {
			AndroidUiHelper.updateVisibility(view, true);
		}

		LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		((ViewGroup) view).removeAllViews();

		List<InAppSubscription> subscriptions = purchaseHelper.getEverMadeSubscriptions();
		for (int i = 0; i < subscriptions.size(); i++) {
			InAppSubscription subscription = subscriptions.get(i);
			SubscriptionState state = subscription.getState();
			boolean autoRenewed = state == SubscriptionState.ACTIVE || state == SubscriptionState.IN_GRACE_PERIOD;

			View card = inflater.inflate(R.layout.subscription_layout, null, false);
			((ViewGroup) view).addView(card);

			TextView subscriptionPeriod = card.findViewById(R.id.subscription_type);
			String period = app.getString(subscription.getPeriodTypeString());
			if (!Algorithms.isEmpty(period)) {
				subscriptionPeriod.setText(period);
				AndroidUiHelper.updateVisibility(subscriptionPeriod, true);
			}

			if (autoRenewed) {
				TextView nextBillingDate = card.findViewById(R.id.next_billing_date);
				String date = getHumanDate(subscription.getPurchaseTime(), subscription.getSubscriptionPeriod());
				if (!Algorithms.isEmpty(date)) {
					nextBillingDate.setText(app.getString(R.string.next_billing_date, date));
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
						ChoosePlanDialogFragment.subscribe(app, mapActivity, purchaseHelper, sku);
					}
				});

				View renew = card.findViewById(R.id.renew);
				AndroidUtils.setBackground(mapActivity, renew, nightMode,
						R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_dark);
			}

			TextView status = card.findViewById(R.id.status);
			status.setText(app.getString(state.getStringRes()));
			AndroidUtils.setBackground(status, app.getUIUtilities().getIcon(state.getBackgroundRes()));

			int dividerLayout = i + 1 == subscriptions.size() ? R.layout.simple_divider_item : R.layout.divider_half_item;
			View divider = inflater.inflate(dividerLayout, (ViewGroup) view, false);
			((ViewGroup) view).addView(divider);
		}
	}

	private String getHumanDate(long time, Period period) {
		if (period == null || period.getUnit() == null) {
			return "";
		}
		Date date = new Date(time);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(period.getUnit().getCalendarIdx(), period.getNumberOfUnits());
		date = calendar.getTime();
		return dateFormat.format(date);
	}
}