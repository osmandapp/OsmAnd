package net.osmand.plus.chooseplan;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class NoPurchasesCard extends BaseCard {

	private final DialogFragment target;

	public NoPurchasesCard(@NonNull FragmentActivity activity, @NonNull DialogFragment target) {
		super(activity, false);
		this.target = target;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.no_purchases_card;
	}

	@Override
	protected void updateContent() {
		TextView infoDescription = view.findViewById(R.id.info_description);
		String restorePurchases = app.getString(R.string.restore_purchases);
		String infoPurchases = String.format(app.getString(R.string.empty_purchases_description), restorePurchases);
		infoDescription.setText(infoPurchases);

		LinearLayout cardsContainer = view.findViewById(R.id.cards_container);
		cardsContainer.removeAllViews();
		cardsContainer.addView(new ExploreOsmAndPlansCard(activity, target).build(activity));
	}
}
