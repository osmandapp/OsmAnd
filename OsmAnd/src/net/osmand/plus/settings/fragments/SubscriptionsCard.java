package net.osmand.plus.settings.fragments;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.Period;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

public class SubscriptionsCard extends BaseCard {

	private final InAppPurchaseHelper purchaseHelper;

	@Override
	public int getCardLayoutId() {
		return R.layout.subscriptions_card;
	}

	public SubscriptionsCard(@NonNull MapActivity mapActivity, @NonNull InAppPurchaseHelper purchaseHelper) {
		super(mapActivity);
		this.purchaseHelper = purchaseHelper;
	}

	@Override
	protected void updateContent() {
		if (mapActivity == null || purchaseHelper == null) {
			return;
		}

		List<InAppSubscription> subscriptions = purchaseHelper.getEverMadeSubscriptions();
		if (Algorithms.isEmpty(subscriptions)) {
			return;
		}

		ContextThemeWrapper ctx = new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inflater = LayoutInflater.from(ctx);
		((ViewGroup) view).removeAllViews();

		for (int i = 0; i < subscriptions.size(); i++) {
			InAppSubscription subscription = subscriptions.get(i);
			SubscriptionState state = subscription.getState();
			boolean autoRenewed = SubscriptionState.ACTIVE.equals(state) || SubscriptionState.IN_GRACE_PERIOD.equals(state);

			View card = inflater.inflate(R.layout.subscription_layout, null, false);
			((ViewGroup) view).addView(card);

			TextView subscriptionPeriod = card.findViewById(R.id.subscription_type);
			String period = getSubscriptionPeriod(subscription.getSubscriptionPeriod());
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
			}

			TextView status = card.findViewById(R.id.status);
			status.setText(app.getString(state.getStringRes()));
			status.setBackgroundDrawable(ContextCompat.getDrawable(mapActivity, state.getBackgroundRes()));

			if (!autoRenewed) {
				View renewContainer = card.findViewById(R.id.renewContainer);
				AndroidUiHelper.updateVisibility(renewContainer, true);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					AndroidUtils.setBackground(ctx, renewContainer, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
				} else {
					AndroidUtils.setBackground(ctx, renewContainer, nightMode, R.drawable.btn_unstroked_light, R.drawable.btn_unstroked_dark);
				}
				final String sku = subscription.getSku();
				renewContainer.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						subscribe(sku);
					}
				});

				View renew = card.findViewById(R.id.renew);
				AndroidUtils.setBackground(ctx, renew, nightMode,
						R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_dark);
			}

			int dividerLayout = i + 1 == subscriptions.size() ? R.layout.simple_divider_item : R.layout.divider_half_item;
			View divider = inflater.inflate(dividerLayout, (ViewGroup) view, false);
			((ViewGroup) view).addView(divider);
		}
	}

	private String getHumanDate(long time, Period period) {
		Date date = new Date(time);
		int monthsCount;
		if (period == null || period.getUnit() == null) {
			return "";
		} else if (period.getUnit().equals(Period.PeriodUnit.YEAR)) {
			monthsCount = 12;
		} else {
			monthsCount = period.getNumberOfUnits();
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MONTH, monthsCount);
		date = calendar.getTime();
		SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy", app.getLocaleHelper().getPreferredLocale());
		return format.format(date);
	}

	private void subscribe(String sku) {
		if (app == null) {
			return;
		}
		if (!app.getSettings().isInternetConnectionAvailable(true)) {
			Toast.makeText(app, R.string.internet_not_available, Toast.LENGTH_LONG).show();
		} else if (mapActivity != null && purchaseHelper != null) {
			OsmandSettings settings = app.getSettings();
			purchaseHelper.purchaseLiveUpdates(mapActivity, sku,
					settings.BILLING_USER_EMAIL.get(),
					settings.BILLING_USER_NAME.get(),
					settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get(),
					settings.BILLING_HIDE_USER_NAME.get());
		}
	}

	private String getSubscriptionPeriod(Period period) {
		if (period == null || period.getUnit() == null) {
			return "";
		} else if (period.getUnit().equals(Period.PeriodUnit.YEAR)) {
			return app.getString(R.string.annual_subscription);
		} else if (period.getUnit().equals(Period.PeriodUnit.MONTH)) {
			int unitsNumber = period.getNumberOfUnits();
			if (unitsNumber == 1) {
				return app.getString(R.string.monthly_subscription);
			} else if (unitsNumber == 3) {
				return app.getString(R.string.three_months_subscription);
			}
		}
		return "";
	}
}