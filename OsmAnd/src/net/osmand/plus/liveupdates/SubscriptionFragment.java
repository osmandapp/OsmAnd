package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppHelper;
import net.osmand.plus.inapp.InAppHelper.InAppListener;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
import net.osmand.plus.liveupdates.CountrySelectionFragment.OnFragmentInteractionListener;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SubscriptionFragment extends BaseOsmAndDialogFragment implements InAppListener, OnFragmentInteractionListener {

	public static final String TAG = "SubscriptionFragment";
	private static final String EDIT_MODE_ID = "edit_mode_id";
	private static final String USER_NAME_ID = "user_name_id";
	private static final String EMAIL_ID = "email_id";
	private static final String COUNTRY_ITEM_ID = "country_id";
	private static final String HIDE_USER_NAME_ID = "hide_user_name_id";
	private static final String DONATION_ID = "donation_id";

	private OsmandSettings settings;
	private ProgressDialog dlg;
	private boolean editMode;
	private boolean donation;

	private String prevEmail;
	private CountryItem selectedCountryItem;

	private CountrySelectionFragment countrySelectionFragment = new CountrySelectionFragment();

	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	public InAppHelper getInAppHelper() {
		Activity activity = getActivity();
		if (activity instanceof OsmLiveActivity) {
			return ((OsmLiveActivity) activity).getInAppHelper();
		} else {
			return null;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(EDIT_MODE_ID, editMode);

		View view = getView();
		if (view != null) {
			EditText userNameEdit = (EditText) view.findViewById(R.id.userNameEdit);
			outState.putString(USER_NAME_ID, userNameEdit.getText().toString());
			EditText emailEdit = (EditText) view.findViewById(R.id.emailEdit);
			outState.putString(EMAIL_ID, emailEdit.getText().toString());
			CheckBox hideUserNameCheckbox = (CheckBox) view.findViewById(R.id.hideUserNameCheckbox);
			outState.putBoolean(HIDE_USER_NAME_ID, hideUserNameCheckbox.isChecked());
			CheckBox donationCheckbox = (CheckBox) view.findViewById(R.id.donationCheckbox);
			outState.putBoolean(DONATION_ID, donationCheckbox.isChecked());
			if (selectedCountryItem != null) {
				outState.putSerializable(COUNTRY_ITEM_ID, selectedCountryItem);
			}
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			editMode = savedInstanceState.getBoolean(EDIT_MODE_ID);
		}

		settings = getMyApplication().getSettings();
		prevEmail = settings.BILLING_USER_EMAIL.get();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		InAppHelper helper = getInAppHelper();
		if (helper != null) {
			helper.addListener(this);
		}

		String userName = settings.BILLING_USER_NAME.get();
		String email = settings.BILLING_USER_EMAIL.get();
		String countryDownloadName = settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get();
		boolean hideUserName = settings.BILLING_HIDE_USER_NAME.get();
		donation = !countryDownloadName.equals(OsmandSettings.BILLING_USER_DONATION_NONE_PARAMETER);

		if (savedInstanceState != null) {
			userName = savedInstanceState.getString(USER_NAME_ID);
			email = savedInstanceState.getString(EMAIL_ID);
			hideUserName = savedInstanceState.getBoolean(HIDE_USER_NAME_ID);
			donation = savedInstanceState.getBoolean(DONATION_ID);
			Object obj = savedInstanceState.getSerializable(COUNTRY_ITEM_ID);
			if (obj instanceof CountryItem) {
				selectedCountryItem = (CountryItem) obj;
				countryDownloadName = selectedCountryItem.getDownloadName();
			} else {
				countryDownloadName =
						donation ? OsmandSettings.BILLING_USER_DONATION_WORLD_PARAMETER : OsmandSettings.BILLING_USER_DONATION_NONE_PARAMETER;
			}
		}

		View view = inflater.inflate(R.layout.subscription_fragment, container, false);
		ImageButton closeButton = (ImageButton) view.findViewById(R.id.closeButton);
		if (editMode) {
			closeButton.setImageDrawable(getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_mode_back));
		} else {
			closeButton.setImageDrawable(getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_remove_dark));
		}
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		TextView title = (TextView) view.findViewById(R.id.titleTextView);
		if (editMode) {
			title.setText(getString(R.string.osm_live_subscription_settings));
		} else {
			title.setText(getString(R.string.osm_live_subscription));
		}

		final View headerLayout = view.findViewById(R.id.headerLayout);
		final View paramsLayout = view.findViewById(R.id.paramsLayout);
		AppCompatCheckBox donationCheckbox = (AppCompatCheckBox) view.findViewById(R.id.donationCheckbox);
		donationCheckbox.setChecked(donation);
		donationCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				donation = isChecked;
				paramsLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
			}
		});
		headerLayout.setVisibility(View.VISIBLE);
		paramsLayout.setVisibility(donation ? View.VISIBLE : View.GONE);

		final EditText userNameEdit = (EditText) view.findViewById(R.id.userNameEdit);
		if (!Algorithms.isEmpty(userName)) {
			userNameEdit.setText(userName);
		}

		final EditText emailEdit = (EditText) view.findViewById(R.id.emailEdit);
		if (!Algorithms.isEmpty(email)) {
			emailEdit.setText(email);
		}

		countrySelectionFragment.initCountries(getMyApplication());
		if (Algorithms.isEmpty(countryDownloadName) || countryDownloadName.equals(OsmandSettings.BILLING_USER_DONATION_NONE_PARAMETER)) {
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

		final CheckBox hideUserNameCheckbox = (CheckBox) view.findViewById(R.id.hideUserNameCheckbox);
		hideUserNameCheckbox.setChecked(hideUserName);

		View editModeBottomView = view.findViewById(R.id.editModeBottomView);
		View purchaseCard = view.findViewById(R.id.purchaseCard);
		if (editMode) {
			editModeBottomView.setVisibility(View.VISIBLE);
			purchaseCard.setVisibility(View.GONE);

			Button saveChangesButton = (Button) view.findViewById(R.id.saveChangesButton);
			saveChangesButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					InAppHelper helper = getInAppHelper();
					if (helper != null && applySettings(userNameEdit.getText().toString().trim(),
							emailEdit.getText().toString().trim(), hideUserNameCheckbox.isChecked())) {

						final Map<String, String> parameters = new HashMap<>();
						parameters.put("visibleName", settings.BILLING_HIDE_USER_NAME.get() ? "" : settings.BILLING_USER_NAME.get());
						parameters.put("preferredCountry", settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get());
						parameters.put("email", settings.BILLING_USER_EMAIL.get());
						parameters.put("cemail", prevEmail);
						parameters.put("userid", settings.BILLING_USER_ID.get());
						parameters.put("token", helper.getToken());

						showProgress();

						AndroidNetworkUtils.sendRequestAsync(getMyApplication(),
								"http://download.osmand.net/subscription/update.php",
								parameters, "Sending data...", true, true, new AndroidNetworkUtils.OnRequestResultListener() {
									@Override
									public void onResult(String result) {
										dismissProgress();
										OsmandApplication app = getMyApplication();
										if (result != null) {
											try {
												JSONObject obj = new JSONObject(result);
												if (!obj.has("error")) {
													String userId = obj.getString("userid");
													app.getSettings().BILLING_USER_ID.set(userId);
													String email = obj.getString("email");
													app.getSettings().BILLING_USER_EMAIL.set(email);
													String visibleName = obj.getString("visibleName");
													if (!Algorithms.isEmpty(visibleName)) {
														app.getSettings().BILLING_USER_NAME.set(visibleName);
														app.getSettings().BILLING_HIDE_USER_NAME.set(false);
													} else {
														app.getSettings().BILLING_HIDE_USER_NAME.set(true);
													}
													String preferredCountry = obj.getString("preferredCountry");
													app.getSettings().BILLING_USER_COUNTRY_DOWNLOAD_NAME.set(preferredCountry);

													Fragment parent = getParentFragment();
													if (parent != null && parent instanceof LiveUpdatesFragment) {
														((LiveUpdatesFragment) parent).updateSubscriptionHeader();
													}

													dismiss();
												} else {
													app.showToastMessage("Error: " + obj.getString("error"));
												}
											} catch (JSONException e) {
												app.showToastMessage(getString(R.string.shared_string_io_error));
											}
										} else {
											app.showToastMessage(getString(R.string.shared_string_io_error));
										}
									}
								});
					}
				}
			});

		} else {
			editModeBottomView.setVisibility(View.GONE);
			purchaseCard.setVisibility(View.VISIBLE);

			updatePrice(view);
			final Button subscribeButton = (Button) view.findViewById(R.id.subscribeButton);
			subscribeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					InAppHelper helper = getInAppHelper();
					if (helper != null) {
						if (applySettings(userNameEdit.getText().toString().trim(),
								emailEdit.getText().toString().trim(), hideUserNameCheckbox.isChecked())) {

							helper.purchaseLiveUpdates(getActivity(),
									settings.BILLING_USER_EMAIL.get(),
									settings.BILLING_USER_NAME.get(),
									settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get(),
									settings.BILLING_HIDE_USER_NAME.get());
						}
					}
				}
			});
		}

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
		InAppHelper helper = getInAppHelper();
		if (helper != null) {
			helper.removeListener(this);
		}
		if (dlg != null && dlg.isShowing()) {
			dlg.hide();
		}
	}

	private boolean applySettings(String userName, String email, boolean hideUserName) {
		String countryName;
		String countryDownloadName;
		if (!donation) {
			countryName = "";
			countryDownloadName = OsmandSettings.BILLING_USER_DONATION_NONE_PARAMETER;
		} else {
			countryName = selectedCountryItem != null ? selectedCountryItem.getLocalName() : "";
			countryDownloadName = selectedCountryItem != null ?
					selectedCountryItem.getDownloadName() : OsmandSettings.BILLING_USER_DONATION_WORLD_PARAMETER;
			if (Algorithms.isEmpty(email) || !AndroidUtils.isValidEmail(email)) {
				getMyApplication().showToastMessage(getString(R.string.osm_live_enter_email));
				return false;
			}
			if (Algorithms.isEmpty(userName) && !hideUserName) {
				getMyApplication().showToastMessage(getString(R.string.osm_live_enter_user_name));
				return false;
			}
		}

		settings.BILLING_USER_NAME.set(userName);
		settings.BILLING_USER_EMAIL.set(email);
		settings.BILLING_USER_COUNTRY.set(countryName);
		settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.set(countryDownloadName);
		settings.BILLING_HIDE_USER_NAME.set(hideUserName);

		return true;
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
		getMyApplication().logEvent(getActivity(), "live_osm_subscription_purchased");
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
		dlg.setCancelable(false);
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
