package net.osmand.plus.settings.purchase;

import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.GOOGLE;
import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.HUGEROCK_PROMO;
import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.TRIPLTEK_PROMO;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.chooseplan.PromoCompanyFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.settings.purchase.data.PurchaseUiData;
import net.osmand.plus.settings.purchase.data.PurchaseUiDataUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

public class PurchaseItemFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener {

	public static final String TAG = PurchaseItemFragment.class.getName();

	private static final String NO_VALUE = "—";
	private static final String PROMO_TYPE_KEY = "promo_type_key";
	private static final String PURCHASE_SKU_KEY = "purchase_sku_key";

	private String promoType;
	private String purchaseSku;
	private PurchaseUiData purchase;
	private InAppPurchaseHelper inAppPurchaseHelper;

	private View view;
	private boolean isToolbarInitialized;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			if (args.containsKey(PURCHASE_SKU_KEY)) {
				purchaseSku = args.getString(PURCHASE_SKU_KEY);
			}
			if (args.containsKey(PROMO_TYPE_KEY)) {
				promoType = args.getString(PROMO_TYPE_KEY);
			}
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = themedInflater.inflate(R.layout.fragment_purchase_item, container, false);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		initRefreshableData();
		if (inAppPurchaseHelper != null) {
			inAppPurchaseHelper.requestInventory(false);
			updateView();
		}
	}

	private void initRefreshableData() {
		inAppPurchaseHelper = getInAppPurchaseHelper();
		if (purchaseSku != null) {
			if (inAppPurchaseHelper != null) {
				InAppPurchase inAppPurchase = inAppPurchaseHelper.getEverMadePurchaseBySku(purchaseSku);
				if (inAppPurchase != null) {
					purchase = PurchaseUiDataUtils.createUiData(app, inAppPurchase);
				}
			}
		} else if (CollectionUtils.startsWithAny(promoType, app.getString(R.string.osmand_start))) {
			purchase = PurchaseUiDataUtils.createFreeAccPurchaseUiData(app);
		} else if (CollectionUtils.startsWithAny(promoType, app.getString(R.string.tripltek))) {
			purchase = PurchaseUiDataUtils.createTripltekPurchaseUiData(app);
		} else if (CollectionUtils.startsWithAny(promoType, app.getString(R.string.hugerock))) {
			purchase = PurchaseUiDataUtils.createHugerockPurchaseUiData(app);
		} else {
			purchase = PurchaseUiDataUtils.createBackupSubscriptionUiData(app);
		}
	}

	private void updateView() {
		if (!isToolbarInitialized) {
			createToolbar();
			isToolbarInitialized = true; // will be automatically reset after screen rotation
		}

		FragmentActivity context = getActivity();
		if (context == null || purchase == null) return;

		// Top info card
		ViewGroup cardContainer = view.findViewById(R.id.card_container);
		cardContainer.removeAllViews();
		PurchaseItemCard card = new PurchaseItemCard(context, inAppPurchaseHelper, purchase);
		cardContainer.addView(card.build(context));

		// Info blocks
		String typeTitle = getString(R.string.shared_string_type);
		String typeDesc = purchase.getPurchaseType();
		updateInformationBlock(R.id.type_block, typeTitle, typeDesc);

		String purchaseTitle;
		String purchaseDesc;
		if (purchase.isSubscription()) {
			long endTime = purchase.getExpireTime();
			long startTime = purchase.getPurchaseTime();
			SubscriptionState state = purchase.getSubscriptionState();

			Pair<String, String> pair = PurchaseUiDataUtils.parseSubscriptionState(app, state, startTime, endTime);
			purchaseTitle = pair.first;
			purchaseDesc = pair.second;
			updateInformationBlock(R.id.purchasing_period_block, purchaseTitle, purchaseDesc);
		} else {
			long purchaseTime = purchase.getPurchaseTime();
			boolean hasTime = purchaseTime > 0;
			if (hasTime) {
				purchaseDesc = PurchaseUiDataUtils.DATE_FORMAT.format(purchaseTime);
				purchaseTitle = app.getString(R.string.shared_string_purchased);
				updateInformationBlock(R.id.purchasing_period_block, purchaseTitle, purchaseDesc);
			}
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.purchasing_period_block), hasTime);
		}

		String purchasedOn = getString(R.string.shared_string_purchased_on);
		PurchaseOrigin origin = purchase.getOrigin();
		String platform = promoType == null ? NO_VALUE : getString(origin.getStoreNameId());
		updateInformationBlock(R.id.platform_block, purchasedOn, platform);

		// Bottom buttons
		boolean manageVisible = purchase.isSubscription() && origin == GOOGLE;
		boolean liveVisible = purchase.isLiveUpdateSubscription();

		setupLiveButton(liveVisible);
		setupManageButton(manageVisible);
		setupPromoDetails(origin);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_divider), manageVisible || liveVisible);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), manageVisible && liveVisible);
	}

	private void createToolbar() {
		AppBarLayout appbar = view.findViewById(R.id.appbar);
		View toolbar = themedInflater.inflate(R.layout.global_preference_toolbar, appbar, false);

		ImageButton ivBackButton = toolbar.findViewById(R.id.close_button);
		UiUtilities.rotateImageByLayoutDirection(ivBackButton);
		ivBackButton.setOnClickListener(v -> dismiss());

		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(purchase.getTitle());
		appbar.addView(toolbar);
	}

	private void updateInformationBlock(int blockId, @NonNull String title,
	                                    @NonNull String description) {
		View container = view.findViewById(blockId);
		TextView tvTitle = container.findViewById(R.id.title);
		tvTitle.setText(title);
		TextView tvDesc = container.findViewById(R.id.description);
		tvDesc.setText(description);
	}

	private void setupManageButton(boolean visible) {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		View manageSubscription = view.findViewById(R.id.manage_subscription);
		manageSubscription.setOnClickListener(v -> {
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (purchaseHelper != null) {
				purchaseHelper.manageSubscription(activity, purchase.getSku());
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
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		View osmandLive = view.findViewById(R.id.osmand_live);
		osmandLive.setOnClickListener(v -> LiveUpdatesFragment.showInstance(activity.getSupportFragmentManager(), null));
		setupSelectableBackground(osmandLive);
		ImageView icon = osmandLive.findViewById(android.R.id.icon);
		TextView title = osmandLive.findViewById(android.R.id.title);

		title.setText(R.string.live_updates);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_osm_live));
		AndroidUiHelper.updateVisibility(osmandLive, visible);
	}

	private void setupPromoDetails(@NonNull PurchaseOrigin origin) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			boolean visible = CollectionUtils.equalsToAny(origin, TRIPLTEK_PROMO, HUGEROCK_PROMO);
			View button = view.findViewById(R.id.promo_details);
			button.setOnClickListener(v -> PromoCompanyFragment.showInstance(activity.getSupportFragmentManager(), origin));

			TextView title = button.findViewById(android.R.id.title);
			title.setText(R.string.shared_string_details);

			setupSelectableBackground(button);
			AndroidUiHelper.updateVisibility(button, visible);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.promo_divider), visible);
			AndroidUiHelper.updateVisibility(button.findViewById(android.R.id.icon), false);
		}
	}

	@Override
	public void onGetItems() {
		updateView();
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		if (inAppPurchaseHelper != null) {
			inAppPurchaseHelper.requestInventory(false);
		}
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

	private void setupSelectableBackground(@NonNull View view) {
		int color = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.active_color_basic);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(view.getContext(), color, 0.3f);
		AndroidUtils.setBackground(view.findViewById(R.id.selectable_list_item), drawable);
	}

	private Drawable getActiveIcon(@DrawableRes int iconId) {
		return iconsCache.getIcon(iconId, ColorUtilities.getActiveColorId(nightMode));
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable String sku, @Nullable String promoType) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			if (!Algorithms.isEmpty(promoType)) {
				args.putString(PROMO_TYPE_KEY, promoType);
			}
			if (!Algorithms.isEmpty(sku)) {
				args.putString(PURCHASE_SKU_KEY, sku);
			}
			PurchaseItemFragment fragment = new PurchaseItemFragment();
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}