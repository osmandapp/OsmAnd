package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;

public class PurchasesFragment extends BaseOsmAndFragment {
	public static final String TAG = PurchasesFragmentEmpty.class.getName();
	private InAppPurchaseHelper purchaseHelper;
	private View mainView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Context context = requireContext();
		purchaseHelper = getInAppPurchaseHelper();
		boolean nightMode = !getMyApplication().getSettings().isLightContent();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);
		mainView = themedInflater.inflate(R.layout.purchases_layout, container, false);
		AppBarLayout appbar = mainView.findViewById(R.id.appbar);
		View toolbar = themedInflater.inflate(R.layout.profile_preference_toolbar_with_icon, container, false);
		appbar.addView(toolbar);

		TextView toolbarTitle = appbar.findViewById(R.id.toolbar_title);
		View iconToolbarContainer = appbar.findViewById(R.id.icon_toolbar);
		toolbarTitle.setText(getString(R.string.purchases));
		ImageView icon = iconToolbarContainer.findViewById(R.id.profile_icon);
		icon.setImageResource(R.drawable.ic_action_help_online);
		icon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri uri = Uri.parse("https://docs.osmand.net/en/main@latest/osmand/purchases");
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		});
		ImageButton backButton = mainView.findViewById(R.id.close_button);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity fragmentActivity = getActivity();
				if (fragmentActivity != null) {
					fragmentActivity.onBackPressed();
				}
			}
		});
		LinearLayout purchasesRestore = mainView.findViewById(R.id.restore_purchases);
		purchasesRestore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (purchaseHelper != null && !purchaseHelper.hasInventory()) {
					purchaseHelper.requestInventory();
				}
			}
		});
		LinearLayout osmandLive = mainView.findViewById(R.id.osmand_live);
		osmandLive.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				ChoosePlanDialogFragment.showOsmLiveInstance(getMyActivity().getSupportFragmentManager());
			}
		});
		return mainView;
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
}
