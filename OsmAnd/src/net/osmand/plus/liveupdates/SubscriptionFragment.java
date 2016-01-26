package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppHelper;
import net.osmand.plus.inapp.InAppHelper.InAppCallbacks;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
import net.osmand.plus.liveupdates.CountrySelectionFragment.OnFragmentInteractionListener;
import net.osmand.util.Algorithms;

public class SubscriptionFragment extends BaseOsmAndDialogFragment implements InAppCallbacks, OnFragmentInteractionListener {

	public static final String TAG = "SubscriptionFragment";

	private InAppHelper inAppHelper;
	private OsmandSettings settings;
	private ProgressDialog dlg;

	private String userName;
	private String email;
	private CountryItem selectedCountryItem;

	private CountrySelectionFragment countrySelectionFragment = new CountrySelectionFragment();

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

		countrySelectionFragment.initCountries(getMyApplication());
		String countryDownloadName = settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get();
		if (Algorithms.isEmpty(countryDownloadName)) {
			selectedCountryItem = countrySelectionFragment.getCountryItems().get(0);
		} else {
			selectedCountryItem = countrySelectionFragment.getCountryItem(countryDownloadName);
		}

		final EditText selectCountryEdit = (EditText) view.findViewById(R.id.selectCountryEdit);
		if (selectedCountryItem != null) {
			selectCountryEdit.setText(selectedCountryItem.getLocalName());
		}
		selectCountryEdit.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					CountrySelectionFragment countryCountrySelectionFragment =
							countrySelectionFragment;
					countryCountrySelectionFragment
							.show(getChildFragmentManager(), "CountriesSearchSelectionFragment");
				}
				return false;
			}
		});

		updatePrice(view);

		final Button subscribeButton = (Button) view.findViewById(R.id.subscribeButton);
		final CheckBox hideUserNameCheckbox = (CheckBox) view.findViewById(R.id.hideUserNameCheckbox);
		boolean hideUserName = settings.BILLING_HIDE_USER_NAME.get();
		hideUserNameCheckbox.setChecked(hideUserName);
		subscribeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (inAppHelper != null) {
					userName = userNameEdit.getText().toString().trim();
					email = emailEdit.getText().toString().trim();
					String countryName = selectedCountryItem != null ? selectedCountryItem.getLocalName() : "";
					String countryDownloadName = selectedCountryItem != null ? selectedCountryItem.getDownloadName() : "";

					if (Algorithms.isEmpty(email) || !AndroidUtils.isValidEmail(email)) {
						getMyApplication().showToastMessage("Please enter valid E-mail address");
						return;
					}
					if (Algorithms.isEmpty(userName) && !hideUserNameCheckbox.isChecked()) {
						getMyApplication().showToastMessage("Please enter Public Name");
						return;
					}

					settings.BILLING_USER_NAME.set(userName);
					settings.BILLING_USER_EMAIL.set(email);
					settings.BILLING_USER_COUNTRY.set(countryName);
					settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.set(countryDownloadName);
					settings.BILLING_HIDE_USER_NAME.set(hideUserNameCheckbox.isChecked());

					inAppHelper.purchaseLiveUpdates(getActivity(), email, userName, countryDownloadName);
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
		updatePrice(getView());
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
	public void onSearchResult(CountryItem item) {
		selectedCountryItem = item;
		View view = getView();
		if (view != null) {
			EditText selectCountryEdit = (EditText) view.findViewById(R.id.selectCountryEdit);
			if (selectCountryEdit != null) {
				selectCountryEdit.setText(item.getLocalName());
			}
		}
	}

	private void updatePrice(View view) {
		if (view == null) {
			view = getView();
		}
		if (view != null) {
			TextView priceTextView = (TextView) view.findViewById(R.id.priceTextView);
			if (InAppHelper.getLiveUpdatesPrice() != null) {
				priceTextView.setText(InAppHelper.getLiveUpdatesPrice());
			}
		}
	}
}
