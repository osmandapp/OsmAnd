package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.net.Uri;
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

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.PurchasingCard;
import net.osmand.plus.chooseplan.TroubleshootingCard;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.liveupdates.CountrySelectionFragment;
import net.osmand.plus.liveupdates.CountrySelectionFragment.OnFragmentInteractionListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;

import java.util.List;

public class PurchasesFragment extends BaseOsmAndFragment implements InAppPurchaseListener, OnFragmentInteractionListener {

	public static final String TAG = PurchasesFragment.class.getName();

	private static final String OSMAND_PURCHASES_URL = "https://docs.osmand.net/en/main@latest/osmand/purchases";

	private OsmandApplication app;
	private InAppPurchaseHelper purchaseHelper;

	private ViewGroup cardsContainer;

	private boolean nightMode;

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved() && fragmentManager.findFragmentByTag(TAG) == null) {
			PurchasesFragment fragment = new PurchasesFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);

		View view = themedInflater.inflate(R.layout.purchases_layout, container, false);
		AndroidUtils.addStatusBarPadding21v(getActivity(), view);
		createToolbar(view, nightMode);
		cardsContainer = view.findViewById(R.id.cards_container);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateCards();

		if (purchaseHelper != null) {
			purchaseHelper.requestInventory();
		}
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		purchaseHelper = getInAppPurchaseHelper();
		if (mapActivity == null || purchaseHelper == null) {
			return;
		}
		cardsContainer.removeAllViews();

		List<InAppPurchase> mainPurchases = purchaseHelper.getEverMadeMainPurchases();
		for (int i = 0; i < mainPurchases.size(); i++) {
			InAppPurchase purchase = mainPurchases.get(i);
			cardsContainer.addView(new InAppPurchaseCard(mapActivity, purchaseHelper, purchase).build(mapActivity));
		}
		boolean promoActive = app.getSettings().BACKUP_PROMOCODE_ACTIVE.get();
		if (promoActive) {
			cardsContainer.addView(new PromoPurchaseCard(mapActivity).build(mapActivity));
		}

		BaseCard purchaseCard;
		if (Version.isPaidVersion(app) || !Algorithms.isEmpty(mainPurchases) || !promoActive) {
			purchaseCard = new TroubleshootingCard(mapActivity, purchaseHelper, false, false);
		} else {
			purchaseCard = new PurchasingCard(mapActivity, purchaseHelper, false);
		}
		cardsContainer.addView(purchaseCard.build(mapActivity));
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

	private void createToolbar(View mainView, final boolean nightMode) {
		AppBarLayout appbar = mainView.findViewById(R.id.appbar);
		View toolbar = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.profile_preference_toolbar_with_icon, appbar, false);

		View iconToolbarContainer = toolbar.findViewById(R.id.icon_toolbar);
		ImageView icon = iconToolbarContainer.findViewById(R.id.profile_icon);
		int iconColorRes = nightMode ? R.color.icon_color_primary_dark : R.color.active_buttons_and_links_text_light;
		icon.setImageDrawable(getIcon(R.drawable.ic_action_help_online, iconColorRes));
		icon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getContext() != null) {
					WikipediaDialogFragment.showFullArticle(getContext(), Uri.parse(OSMAND_PURCHASES_URL), nightMode);
				}
			}
		});
		ImageButton backButton = toolbar.findViewById(R.id.close_button);
		UiUtilities.rotateImageByLayoutDirection(backButton);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity fragmentActivity = getActivity();
				if (fragmentActivity != null) {
					fragmentActivity.onBackPressed();
				}
			}
		});

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getString(R.string.purchases));
		appbar.addView(toolbar);
	}

	@Override
	public void onError(InAppPurchaseHelper.InAppPurchaseTaskType taskType, String error) {
	}

	@Override
	public void onGetItems() {
		updateCards();
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		if (purchaseHelper != null) {
			purchaseHelper.requestInventory();
		}
	}

	@Override
	public void showProgress(InAppPurchaseHelper.InAppPurchaseTaskType taskType) {
	}

	@Override
	public void dismissProgress(InAppPurchaseHelper.InAppPurchaseTaskType taskType) {
	}

	@Override
	public void onSearchResult(CountrySelectionFragment.CountryItem name) {

	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}
}