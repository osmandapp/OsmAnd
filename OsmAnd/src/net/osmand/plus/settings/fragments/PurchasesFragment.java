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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.chooseplan.NoPurchasesCard;
import net.osmand.plus.chooseplan.TroubleshootingCard;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
import net.osmand.plus.liveupdates.CountrySelectionFragment.OnFragmentInteractionListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.List;

public class PurchasesFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener, OnFragmentInteractionListener {

	public static final String TAG = PurchasesFragment.class.getName();

	private static final String OSMAND_PURCHASES_URL = "https://docs.osmand.net/en/main@latest/osmand/purchases";

	private OsmandApplication app;
	private InAppPurchaseHelper purchaseHelper;

	private ViewGroup cardsContainer;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		nightMode = !app.getSettings().isLightContent();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);

		View view = themedInflater.inflate(R.layout.purchases_layout, container, false);
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
			InAppPurchase purchase = mainPurchases.get(i);
			cardsContainer.addView(new InAppPurchaseCard(activity, purchaseHelper, purchase).build(activity));
		}
		boolean promoActive = app.getSettings().BACKUP_PROMOCODE_ACTIVE.get();
		if (promoActive) {
			cardsContainer.addView(new PromoPurchaseCard(activity).build(activity));
		}

		if (!Version.isPaidVersion(app) || Algorithms.isEmpty(mainPurchases)) {
			cardsContainer.addView(new NoPurchasesCard(activity, false).build(activity));
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

	private void createToolbar(View mainView, final boolean nightMode) {
		AppBarLayout appbar = mainView.findViewById(R.id.appbar);
		View toolbar = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.profile_preference_toolbar_with_icon, appbar, false);

		View iconToolbarContainer = toolbar.findViewById(R.id.icon_toolbar);
		ImageView icon = iconToolbarContainer.findViewById(R.id.profile_icon);
		int iconColorRes = nightMode ? R.color.icon_color_primary_dark : R.color.active_buttons_and_links_text_light;
		icon.setImageDrawable(getIcon(R.drawable.ic_action_help_online, iconColorRes));
		icon.setOnClickListener(v -> {
			if (getContext() != null) {
				AndroidUtils.openUrl(getContext(), Uri.parse(OSMAND_PURCHASES_URL), nightMode);
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
	public void onError(InAppPurchaseTaskType taskType, String error) {
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
	public void showProgress(InAppPurchaseTaskType taskType) {
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
	}

	@Override
	public void onSearchResult(CountryItem name) {

	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			PurchasesFragment fragment = new PurchasesFragment();
			fragment.show(manager, TAG);
		}
	}
}