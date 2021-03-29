package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
import net.osmand.plus.liveupdates.CountrySelectionFragment.OnFragmentInteractionListener;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SubscriptionFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener, OnFragmentInteractionListener {

	public static final String TAG = "SubscriptionFragment";
	private static final String USER_NAME_ID = "user_name_id";
	private static final String EMAIL_ID = "email_id";
	private static final String COUNTRY_ITEM_ID = "country_id";
	private static final String HIDE_USER_NAME_ID = "hide_user_name_id";
	private static final String DONATION_ID = "donation_id";

	private OsmandSettings settings;
	private ProgressDialog dlg;
	private boolean donation;

	private String prevEmail;
	private CountryItem selectedCountryItem;

	private CountrySelectionFragment countrySelectionFragment = new CountrySelectionFragment();

	@Nullable
	public InAppPurchaseHelper getInAppPurchaseHelper() {
		Activity activity = getActivity();
		if (activity instanceof OsmandInAppPurchaseActivity) {
			return ((OsmandInAppPurchaseActivity) activity).getPurchaseHelper();
		} else {
			return null;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
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

		settings = getMyApplication().getSettings();
		prevEmail = settings.BILLING_USER_EMAIL.get();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
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
		Drawable icBack = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(app));
		closeButton.setImageDrawable(icBack);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		TextView title = (TextView) view.findViewById(R.id.titleTextView);
		title.setText(getString(R.string.osm_live_subscription_settings));

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
							.show(getChildFragmentManager(), CountrySelectionFragment.TAG);
				}
				return false;
			}
		});

		final CheckBox hideUserNameCheckbox = (CheckBox) view.findViewById(R.id.hideUserNameCheckbox);
		hideUserNameCheckbox.setChecked(hideUserName);

		Button saveChangesButton = (Button) view.findViewById(R.id.saveChangesButton);
		saveChangesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				InAppPurchaseHelper purchaseHelper = getInAppPurchaseHelper();
				if (purchaseHelper != null && applySettings(userNameEdit.getText().toString().trim(),
						emailEdit.getText().toString().trim(), hideUserNameCheckbox.isChecked())) {

					final Map<String, String> parameters = new HashMap<>();
					parameters.put("visibleName", settings.BILLING_HIDE_USER_NAME.get() ? "" : settings.BILLING_USER_NAME.get());
					parameters.put("preferredCountry", settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get());
					parameters.put("email", settings.BILLING_USER_EMAIL.get());
					parameters.put("cemail", prevEmail);
					parameters.put("userid", settings.BILLING_USER_ID.get());
					parameters.put("token", settings.BILLING_USER_TOKEN.get());

					showProgress(null);

					AndroidNetworkUtils.sendRequestAsync(getMyApplication(),
							"https://osmand.net/subscription/update",
							parameters, "Sending data...", true, true, new AndroidNetworkUtils.OnRequestResultListener() {
								@Override
								public void onResult(String result) {
									dismissProgress(null);
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

		setThemedDrawable((ImageView) view.findViewById(R.id.userNameIcon), R.drawable.ic_action_user);
		setThemedDrawable((ImageView) view.findViewById(R.id.emailIcon), R.drawable.ic_action_message);
		setThemedDrawable((ImageView) view.findViewById(R.id.countryIcon), R.drawable.ic_world_globe_dark);
		selectCountryEdit.setCompoundDrawablesWithIntrinsicBounds(
				null, null, getContentIcon(R.drawable.ic_action_arrow_drop_down), null);

		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
	public void onError(InAppPurchaseTaskType taskType, String error) {
	}

	@Override
	public void onGetItems() {
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		dismissAllowingStateLoss();
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
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
	public void dismissProgress(InAppPurchaseTaskType taskType) {
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
}
