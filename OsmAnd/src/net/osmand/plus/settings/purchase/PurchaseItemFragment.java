package net.osmand.plus.settings.purchase;

import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.GOOGLE;

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
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.settings.purchase.data.PurchaseUiData;
import net.osmand.plus.settings.purchase.data.PurchaseUiDataUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class PurchaseItemFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener {

	public static final String TAG = PurchaseItemFragment.class.getName();

	private static final String PURCHASE_SKU_ARG = "purchase_sku_arg";
	private static final String IS_FREE_ACCOUNT_ARG = "is_free_account_arg";

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
	private static final String NO_VALUE = "—";

	private String purchaseSku;
	private PurchaseUiData purchase;
	private InAppPurchaseHelper inAppPurchaseHelper;
	private boolean isFreeAccountPurchase;

	private View view;
	private boolean isToolbarInitialized;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			if (args.containsKey(PURCHASE_SKU_ARG)) {
				purchaseSku = args.getString(PURCHASE_SKU_ARG);
			}
			if (args.containsKey(IS_FREE_ACCOUNT_ARG)) {
				isFreeAccountPurchase = args.getBoolean(IS_FREE_ACCOUNT_ARG);
			}
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		iconsCache = app.getUIUtilities();
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
		} else {
			if (isFreeAccountPurchase) {
				purchase = PurchaseUiDataUtils.createFreeAccPurchaseUiData(app);
			} else {
				purchase = PurchaseUiDataUtils.createBackupSubscriptionUiData(app);
			}
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
			SubscriptionState state = purchase.getSubscriptionState();
			long startTime = purchase.getPurchaseTime();
			long endTime = purchase.getExpireTime();
			Pair<String, String> stateStrings = parseSubscriptionState(state, startTime, endTime);
			purchaseTitle = stateStrings.first;
			purchaseDesc = stateStrings.second;
			updateInformationBlock(R.id.purchasing_period_block, purchaseTitle, purchaseDesc);
		} else {
			long purchaseTime = purchase.getPurchaseTime();
			boolean hasTime = purchaseTime > 0;
			if (hasTime) {
				purchaseDesc = dateFormat.format(purchaseTime);
				purchaseTitle = app.getString(R.string.shared_string_purchased);
				updateInformationBlock(R.id.purchasing_period_block, purchaseTitle, purchaseDesc);
			}
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.purchasing_period_block), hasTime);
		}

		String purchasedOn = getString(R.string.shared_string_purchased_on);
		InAppPurchase.PurchaseOrigin origin = purchase.getOrigin();
		String platform = isFreeAccountPurchase ? NO_VALUE : getString(origin.getStoreNameId());
		updateInformationBlock(R.id.platform_block, purchasedOn, platform);

		// Bottom buttons
		boolean manageVisible = purchase.isSubscription() && origin == GOOGLE;
		boolean liveVisible = purchase.isLiveUpdateSubscription();
		setupLiveButton(liveVisible);
		setupManageButton(manageVisible);
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

	private Pair<String, String> parseSubscriptionState(@NonNull SubscriptionState state, long startTime, long expireTime) {
		String title;
		String desc;
		switch (state) {
			case ACTIVE:
			case CANCELLED:
			case IN_GRACE_PERIOD:
				title = expireTime > 0 ? app.getString(R.string.shared_string_expires) : app.getString(R.string.shared_string_purchased);
				desc = expireTime > 0 ? dateFormat.format(expireTime) : dateFormat.format(startTime);
				break;
			case EXPIRED:
				title = app.getString(R.string.expired);
				desc = dateFormat.format(expireTime);
				break;
			case ON_HOLD:
				title = app.getString(R.string.on_hold_since, "");
				desc = dateFormat.format(startTime);
				break;
			case PAUSED:
				title = app.getString(R.string.shared_string_paused);
				desc = dateFormat.format(expireTime);
				break;
			default:
				title = app.getString(R.string.shared_string_undefined);
				desc = "";
		}
		return new Pair<>(title, desc);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable String purchaseSku) {
		Bundle args = new Bundle();
		args.putString(PURCHASE_SKU_ARG, purchaseSku);
		showInstance(manager, args);
	}

	public static void showInstance(@NonNull FragmentManager manager, boolean isFreeAccountPurchase) {
		Bundle args = new Bundle();
		args.putBoolean(IS_FREE_ACCOUNT_ARG, isFreeAccountPurchase);
		showInstance(manager, args);
	}

	private static void showInstance(@NonNull FragmentManager manager, @NonNull Bundle arguments) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			PurchaseItemFragment fragment = new PurchaseItemFragment();
			fragment.setArguments(arguments);
			fragment.show(manager, TAG);
		}
	}

}
