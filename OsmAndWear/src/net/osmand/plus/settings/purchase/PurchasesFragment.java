package net.osmand.plus.settings.purchase;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.chooseplan.ExploreOsmAndPlansCard;
import net.osmand.plus.chooseplan.NoPurchasesCard;
import net.osmand.plus.chooseplan.TroubleshootingCard;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.settings.purchase.data.PurchaseUiData;
import net.osmand.plus.settings.purchase.data.PurchaseUiDataUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.List;

public class PurchasesFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener, CardListener {

	public static final String TAG = PurchasesFragment.class.getName();

	private InAppPurchaseHelper purchaseHelper;

	private ViewGroup cardsContainer;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_purchases, container, false);
		createToolbar(view, nightMode);
		cardsContainer = view.findViewById(R.id.cards_container);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateCards();

		if (purchaseHelper != null) {
			purchaseHelper.requestInventory(false);
		}
	}

	private void updateCards() {
		FragmentActivity activity = getActivity();
		purchaseHelper = getInAppPurchaseHelper();
		if (activity == null || purchaseHelper == null) {
			return;
		}
		cardsContainer.removeAllViews();

		List<InAppPurchase> mainPurchases = purchaseHelper.getEverMadeMainPurchases();
		for (int i = 0; i < mainPurchases.size(); i++) {
			themedInflater.inflate(R.layout.list_item_divider, cardsContainer);
			PurchaseUiData purchase = PurchaseUiDataUtils.createUiData(app, mainPurchases.get(i));
			PurchaseItemCard purchaseCard = new PurchaseItemCard(activity, purchaseHelper, purchase);
			purchaseCard.setListener(PurchasesFragment.this);
			cardsContainer.addView(purchaseCard.build(activity));
		}
		boolean showBackupSubscription = PurchaseUiDataUtils.shouldShowBackupSubscription(app, mainPurchases);
		if (showBackupSubscription) {
			themedInflater.inflate(R.layout.list_item_divider, cardsContainer);
			PurchaseUiData purchase = PurchaseUiDataUtils.createBackupSubscriptionUiData(app);
			PurchaseItemCard purchaseCard = new PurchaseItemCard(activity, purchaseHelper, purchase);
			purchaseCard.setListener(PurchasesFragment.this);
			cardsContainer.addView(purchaseCard.build(activity));
		}

		boolean needToShowFreeAccountSubscriptionCard = PurchaseUiDataUtils.shouldShowFreeAccRegistration(app);
		if (needToShowFreeAccountSubscriptionCard) {
			themedInflater.inflate(R.layout.list_item_divider, cardsContainer);
			PurchaseUiData purchase = PurchaseUiDataUtils.createFreeAccPurchaseUiData(app);
			PurchaseItemCard purchaseCard = new PurchaseItemCard(activity, purchaseHelper, purchase);
			purchaseCard.setListener(PurchasesFragment.this);
			cardsContainer.addView(purchaseCard.build(activity));
		}

		if (Version.isTripltekBuild()) {
			themedInflater.inflate(R.layout.list_item_divider, cardsContainer);
			PurchaseUiData purchase = PurchaseUiDataUtils.createTripltekPurchaseUiData(app);
			PurchaseItemCard purchaseCard = new PurchaseItemCard(activity, purchaseHelper, purchase);
			purchaseCard.setListener(PurchasesFragment.this);
			cardsContainer.addView(purchaseCard.build(activity));
		} else if (Version.isHugerockBuild()) {
			themedInflater.inflate(R.layout.list_item_divider, cardsContainer);
			PurchaseUiData purchase = PurchaseUiDataUtils.createHugerockPurchaseUiData(app);
			PurchaseItemCard purchaseCard = new PurchaseItemCard(activity, purchaseHelper, purchase);
			purchaseCard.setListener(PurchasesFragment.this);
			cardsContainer.addView(purchaseCard.build(activity));
		}

		boolean hasMainPurchases = !Algorithms.isEmpty(mainPurchases);
		if (!needToShowFreeAccountSubscriptionCard && (!Version.isPaidVersion(app) || (!hasMainPurchases && !showBackupSubscription))) {
			themedInflater.inflate(R.layout.list_item_divider, cardsContainer);
			cardsContainer.addView(new NoPurchasesCard(activity, this).build(activity));
		} else {
			themedInflater.inflate(R.layout.list_item_divider, cardsContainer);
			cardsContainer.addView(new ExploreOsmAndPlansCard(activity, this).build(activity));
		}
		cardsContainer.addView(new TroubleshootingCard(activity, purchaseHelper, false).build(activity));
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

	private void createToolbar(View mainView, boolean nightMode) {
		AppBarLayout appbar = mainView.findViewById(R.id.appbar);
		View toolbar = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.profile_preference_toolbar_with_icon, appbar, false);

		View iconToolbarContainer = toolbar.findViewById(R.id.icon_toolbar);
		ImageView icon = iconToolbarContainer.findViewById(R.id.profile_icon);
		int iconColorRes = nightMode ? R.color.icon_color_primary_dark : R.color.active_buttons_and_links_text_light;
		icon.setImageDrawable(getIcon(R.drawable.ic_action_help_online, iconColorRes));
		icon.setOnClickListener(v -> {
			if (getContext() != null) {
				AndroidUtils.openUrl(getContext(), R.string.docs_purchases, nightMode);
			}
		});
		ImageButton backButton = toolbar.findViewById(R.id.close_button);
		UiUtilities.rotateImageByLayoutDirection(backButton);
		backButton.setOnClickListener(v -> dismiss());

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getString(R.string.purchases));
		appbar.addView(toolbar);
	}

	@Override
	public void onGetItems() {
		updateCards();
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		if (purchaseHelper != null) {
			purchaseHelper.requestInventory(false);
		}
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof PurchaseItemCard) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				PurchaseItemCard purchaseCard = (PurchaseItemCard) card;
				PurchaseUiData purchase = purchaseCard.getDisplayedData();

				String sku = purchase.isPromo() ? null : purchase.getSku();
				String promoType = sku == null ? purchase.getTitle() : null;
				FragmentManager fragmentManager = activity.getSupportFragmentManager();
				PurchaseItemFragment.showInstance(fragmentManager, sku, promoType);
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			PurchasesFragment fragment = new PurchasesFragment();
			fragment.show(manager, TAG);
		}
	}
}