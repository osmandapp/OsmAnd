package net.osmand.plus.settings.fragments;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.OsmandSettings;

public class PromoPurchaseCard extends MapBaseCard {

	public PromoPurchaseCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.inapp_purchase_card;
	}

	@Override
	protected void updateContent() {
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView purchaseType = view.findViewById(R.id.purchase_type);

		title.setText(R.string.promo);
		purchaseType.setText(R.string.promo_subscription);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_osmand_pro_logo));

		AndroidUiHelper.updateVisibility(purchaseType, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.osmand_live), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.manage_subscription), false);

		OsmandSettings settings = app.getSettings();
		long startTime = settings.BACKUP_PROMOCODE_START_TIME.get();
		long expiredTime = settings.BACKUP_PROMOCODE_EXPIRE_TIME.get();
		SubscriptionState state = settings.BACKUP_PROMOCODE_STATE.get();

		InAppPurchaseCard.setupStatus(view, state, startTime, expiredTime);
	}
}

