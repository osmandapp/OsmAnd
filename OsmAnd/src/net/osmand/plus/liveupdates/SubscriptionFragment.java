package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppHelper;
import net.osmand.plus.inapp.InAppHelper.InAppCallbacks;
import net.osmand.plus.liveupdates.SearchSelectionFragment.OnFragmentInteractionListener;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SubscriptionFragment extends BaseOsmAndDialogFragment implements InAppCallbacks, OnFragmentInteractionListener{

	public static final String TAG = "SubscriptionFragment";

	private InAppHelper inAppHelper;
	private OsmandSettings settings;
	private ProgressDialog dlg;

	private String userName;
	private String email;
	private String country;

	ArrayList<String> regionNames = new ArrayList<>();
	private CountrySearchSelectionFragment searchSelectionFragment
			= new CountrySearchSelectionFragment();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settings = getMyApplication().getSettings();
		inAppHelper = new InAppHelper(getMyApplication(), this);
		Activity activity = getActivity();
		if (activity instanceof OsmLiveActivity) {
			((OsmLiveActivity) activity).setInAppHelper(inAppHelper);
		}

		inAppHelper.start(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.subscription_fragment, container, false);
		ImageButton closeButton = (ImageButton) view.findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		initCountries();

		userName = settings.BILLING_USER_NAME.get();
		final EditText userNameEdit = (EditText) view.findViewById(R.id.userNameEdit);
		if (!Algorithms.isEmpty(userName)) {
			userNameEdit.setText(userName);
		}

		email = settings.BILLING_USER_EMAIL.get();
		final EditText emailEdit = (EditText) view.findViewById(R.id.emailEdit);
		if (!Algorithms.isEmpty(email)) {
			emailEdit.setText(email);
		}

		country = settings.BILLING_USER_COUNTRY.get();
		final EditText selectCountryEdit = (EditText) view.findViewById(R.id.selectCountryEdit);
		if (!Algorithms.isEmpty(country)) {
			selectCountryEdit.setText(country);
		}
		selectCountryEdit.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					SearchSelectionFragment countrySearchSelectionFragment =
							searchSelectionFragment;
					countrySearchSelectionFragment
							.show(getChildFragmentManager(), "CountriesSearchSelectionFragment");
				}
				return false;
			}
		});

		updatePrice();

		Button subscribeButton = (Button) view.findViewById(R.id.subscribeButton);
		subscribeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (inAppHelper != null) {
					userName = userNameEdit.getText().toString().trim();
					email = emailEdit.getText().toString().trim();
					country = selectCountryEdit.getText().toString().trim();

					if (Algorithms.isEmpty(userName)) {
						getMyApplication().showToastMessage("Please enter visible name");
						return;
					}
					if (Algorithms.isEmpty(email) || !AndroidUtils.isValidEmail(email)) {
						getMyApplication().showToastMessage("Please enter valid E-mail address");
						return;
					}

					settings.BILLING_USER_NAME.set(userName);
					settings.BILLING_USER_EMAIL.set(email);
					settings.BILLING_USER_COUNTRY.set(country);

					final WorldRegion world = getMyApplication().getRegions().getWorldRegion();
					String countryParam = country.equals(world.getLocaleName()) ? "" : country;

					inAppHelper.purchaseLiveUpdates(getActivity(), email, userName, countryParam);
				}
			}
		});

		setThemedDrawable((ImageView) view.findViewById(R.id.userNameIcon), R.drawable.ic_person);
		setThemedDrawable((ImageView) view.findViewById(R.id.emailIcon), R.drawable.ic_action_message);
		setThemedDrawable((ImageView) view.findViewById(R.id.countryIcon), R.drawable.ic_world_globe_dark);
		selectCountryEdit.setCompoundDrawablesWithIntrinsicBounds(
				null, null, getContentIcon(R.drawable.ic_action_arrow_drop_down), null);

		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		inAppHelper.stop();
		if (dlg != null && dlg.isShowing()) {
			dlg.hide();
		}
		Activity activity = getActivity();
		if (activity instanceof OsmLiveActivity) {
			((OsmLiveActivity) activity).setInAppHelper(null);
		}
	}

	@Override
	public void onError(String error) {
	}

	@Override
	public void onGetItems() {
		updatePrice();
	}

	@Override
	public void onItemPurchased(String sku) {

		if (InAppHelper.SKU_LIVE_UPDATES.equals(sku)) {
			Fragment parentFragment = getParentFragment();
			if (parentFragment instanceof LiveUpdatesFragment) {
				((LiveUpdatesFragment) parentFragment).updateSubscriptionBanner();
			}
		}

		dismiss();
	}

	@Override
	public void showProgress() {
		if (dlg != null) {
			dlg.dismiss();
		}
		dlg = new ProgressDialog(getActivity());
		dlg.setTitle("");
		dlg.setMessage(getString(R.string.wait_current_task_finished));
		dlg.show();
	}

	@Override
	public void dismissProgress() {
		if (dlg != null) {
			dlg.dismiss();
			dlg = null;
		}
	}

	@Override
	public void onSearchResult(String name) {
		View view = getView();
		if (view != null) {
			EditText selectCountryEdit = (EditText) view.findViewById(R.id.selectCountryEdit);
			if (selectCountryEdit != null) {
				selectCountryEdit.setText(name);
			}
		}
	}

	private void updatePrice() {
		View view = getView();
		if (view != null) {
			TextView priceTextView = (TextView) view.findViewById(R.id.priceTextView);
			if (InAppHelper.getLiveUpdatesPrice() != null) {
				priceTextView.setText(InAppHelper.getLiveUpdatesPrice());
			}
		}
	}

	private void initCountries() {
		final WorldRegion root = getMyApplication().getRegions().getWorldRegion();
		ArrayList<WorldRegion> groups = new ArrayList<>();
		groups.add(root);
		processGroup(root, groups, getActivity());
		Collections.sort(groups, new Comparator<WorldRegion>() {
			@Override
			public int compare(WorldRegion lhs, WorldRegion rhs) {
				if (lhs == root) {
					return -1;
				}
				if (rhs == root) {
					return 1;
				}
				return getHumanReadableName(lhs).compareTo(getHumanReadableName(rhs));
			}
		});
		for (WorldRegion group : groups) {
			String name = getHumanReadableName(group);
			regionNames.add(name);
		}
	}

	private static void processGroup(WorldRegion group,
									 List<WorldRegion> nameList,
									 Context context) {
		if (group.isRegionMapDownload()) {
			nameList.add(group);
		}

		if (group.getSubregions() != null) {
			for (WorldRegion g : group.getSubregions()) {
				processGroup(g, nameList, context);
			}
		}
	}

	private static String getHumanReadableName(WorldRegion group) {
		String name;
		if (group.getLevel() > 2 || (group.getLevel() == 2
				&& group.getSuperregion().getRegionId().equals(WorldRegion.RUSSIA_REGION_ID))) {
			WorldRegion parent = group.getSuperregion();
			WorldRegion parentsParent = group.getSuperregion().getSuperregion();
			if (group.getLevel() == 3) {
				if (parentsParent.getRegionId().equals(WorldRegion.RUSSIA_REGION_ID)) {
					name = parentsParent.getLocaleName() + " " + group.getLocaleName();
				} else if (!parent.getRegionId().equals(WorldRegion.UNITED_KINGDOM_REGION_ID)) {
					name = parent.getLocaleName() + " " + group.getLocaleName();
				} else {
					name = group.getLocaleName();
				}
			} else {
				name = parent.getLocaleName() + " " + group.getLocaleName();
			}
		} else {
			name = group.getLocaleName();
		}
		if (name == null) {
			name = "";
		}
		return name;
	}

	public static class CountrySearchSelectionFragment extends SearchSelectionFragment {
		@Override
		protected ArrayList<String> getList() {
			return ((SubscriptionFragment) getParentFragment()).regionNames;
		}

		@Override
		protected int getListItemIcon() {
			return R.drawable.ic_map;
		}
	}
}
