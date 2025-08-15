package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
import net.osmand.plus.liveupdates.CountrySelectionFragment.OnFragmentInteractionListener;
import net.osmand.plus.liveupdates.GetJsonAsyncTask.OnErrorListener;
import net.osmand.plus.liveupdates.GetJsonAsyncTask.OnResponseListener;
import net.osmand.plus.liveupdates.Protocol.RecipientsByMonth;
import net.osmand.plus.liveupdates.Protocol.TotalChangesByMonthResponse;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

public class ReportsFragment extends BaseFullScreenFragment implements OnFragmentInteractionListener {

	public static final String DOMAIN = "https://osmand.net/";
	public static final String TOTAL_CHANGES_BY_MONTH_URL_PATTERN = DOMAIN +
			"reports/query_report?report=total_changes_by_month&month=%s&region=%s";
	public static final String USERS_RANKING_BY_MONTH = DOMAIN +
			"reports/query_report?report=ranking_users_by_month&month=%s&region=%s";
	public static final String RECIPIENTS_BY_MONTH = DOMAIN +
			"reports/query_report?report=recipients_by_month&month=%s&region=%s";


	private static final Log LOG = PlatformUtil.getLog(ReportsFragment.class);
	public static final String EDITS_FRAGMENT = "NumberOfEditsFragment";
	public static final String RECIPIENTS_FRAGMENT = "RecipientsFragment";

	private TextView contributorsTextView;
	private TextView editsTextView;
	private TextView donationsTextView;
	private TextView recipientsTextView;

	private Spinner monthReportsSpinner;
	private MonthsForReportsAdapter monthsForReportsAdapter;

	private final CountrySelectionFragment countrySelectionFragment = new CountrySelectionFragment();
	private final UsersReportFragment userReportFragment = new UsersReportFragment();
	private TextView countryNameTextView;
	private CountryItem selectedCountryItem;

	private ImageView numberOfContributorsIcon;
	private ImageView numberOfEditsIcon;
	private ImageView donationsIcon;
	private ImageView numberOfRecipientsIcon;
	private ImageView donationsTotalIcon;
	private TextView donationsTotalTitle;
	private TextView donationsTotalTextView;
	private TextView numberOfContributorsTitle;
	private TextView numberOfEditsTitle;
	private TextView numberOfRecipientsTitle;
	private TextView donationsTitle;
	private ProgressBar progressBar;
	private LinearLayout donationsTotalLayout;

	private int inactiveColor;
	private int textColorPrimary;
	private int textColorSecondary;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_reports, container, false);
		monthReportsSpinner = view.findViewById(R.id.monthReportsSpinner);
		View monthButton = view.findViewById(R.id.monthButton);
		monthReportsSpinner.setOnTouchListener((v, event) -> {
			event.offsetLocation(dpToPx(48f), 0);
			monthButton.onTouchEvent(event);
			return true;
		});
		monthsForReportsAdapter = new MonthsForReportsAdapter(getActivity());
		monthReportsSpinner.setAdapter(monthsForReportsAdapter);

		monthButton.setOnClickListener(v -> monthReportsSpinner.performClick());

		String osmLiveUrl = getString(R.string.url_osm_live_info);
		view.findViewById(R.id.show_all).setOnClickListener(v -> {
			Activity activity = getActivity();
			if (activity != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(osmLiveUrl));
				AndroidUtils.startActivityIfSafe(activity, intent);
			}
		});
		((TextView) view.findViewById(R.id.osm_live_url_label)).setText(osmLiveUrl);

		View regionReportsButton = view.findViewById(R.id.reportsButton);
		regionReportsButton.setOnClickListener(v -> {
			countrySelectionFragment.show(getChildFragmentManager(), "CountriesSearchSelectionFragment");
		});
		OnClickListener listener = v -> {
			int monthItemPosition = monthReportsSpinner.getSelectedItemPosition();
			String monthUrlString = monthsForReportsAdapter.getQueryString(monthItemPosition);
			String countryUrlString = selectedCountryItem.getDownloadName();
			boolean isRecipientsReport = v.getId() == R.id.numberOfRecipientsLayout;
			if (!countryUrlString.isEmpty() || isRecipientsReport) {
				Bundle bl = new Bundle();
				bl.putString(UsersReportFragment.URL_REQUEST,
						String.format(isRecipientsReport ? RECIPIENTS_BY_MONTH : USERS_RANKING_BY_MONTH, monthUrlString, countryUrlString));
				userReportFragment.setArguments(bl);
				userReportFragment.show(getChildFragmentManager(), isRecipientsReport ? RECIPIENTS_FRAGMENT : EDITS_FRAGMENT);
			}
		};
		view.findViewById(R.id.numberOfContributorsLayout).setOnClickListener(listener);
		view.findViewById(R.id.numberOfEditsLayout).setOnClickListener(listener);
		view.findViewById(R.id.numberOfRecipientsLayout).setOnClickListener(listener);

		countrySelectionFragment.initCountries(app);
		selectedCountryItem = countrySelectionFragment.getCountryItems().get(0);

		countryNameTextView = regionReportsButton.findViewById(android.R.id.text1);
		countryNameTextView.setText(selectedCountryItem.getLocalName());

		setThemedDrawable(view, R.id.calendarImageView, R.drawable.ic_action_data);
		setThemedDrawable(view, R.id.monthDropDownIcon, R.drawable.ic_action_arrow_drop_down);
		setThemedDrawable(view, R.id.regionIconImageView, R.drawable.ic_world_globe_dark);
		setThemedDrawable(view, R.id.countryDropDownIcon, R.drawable.ic_action_arrow_drop_down);

		numberOfContributorsIcon = view.findViewById(R.id.numberOfContributorsIcon);
		numberOfEditsIcon = view.findViewById(R.id.numberOfEditsIcon);
		numberOfRecipientsIcon = view.findViewById(R.id.numberOfRecipientsIcon);
		donationsIcon = view.findViewById(R.id.donationsIcon);
		donationsTotalIcon = view.findViewById(R.id.donationsTotalIcon);
		setThemedDrawable(numberOfContributorsIcon, R.drawable.ic_action_group2);
		setThemedDrawable(numberOfRecipientsIcon, R.drawable.ic_group);
		setThemedDrawable(donationsIcon, R.drawable.ic_action_bitcoin);
		setThemedDrawable(donationsTotalIcon, R.drawable.ic_action_bitcoin);
		setThemedDrawable(numberOfEditsIcon, R.drawable.ic_map);


		numberOfContributorsTitle = view.findViewById(R.id.numberOfContributorsTitle);
		numberOfEditsTitle = view.findViewById(R.id.numberOfEditsTitle);
		donationsTitle = view.findViewById(R.id.donationsTitle);
		numberOfRecipientsTitle = view.findViewById(R.id.numberOfRecipientsTitle);
		donationsTotalLayout = view.findViewById(R.id.donationsTotal);
		donationsTotalTitle = view.findViewById(R.id.donationsTotalTitle);
		donationsTotalTextView = view.findViewById(R.id.donationsTotalTextView);


		progressBar = view.findViewById(R.id.progress);

		contributorsTextView = view.findViewById(R.id.contributorsTextView);
		editsTextView = view.findViewById(R.id.editsTextView);
		donationsTextView = view.findViewById(R.id.donationsTextView);
		recipientsTextView = view.findViewById(R.id.recipientsTextView);

		requestAndUpdateUi();

		AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				requestAndUpdateUi();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		};
		monthReportsSpinner.setOnItemSelectedListener(onItemSelectedListener);

		inactiveColor = AndroidUtils.getColorFromAttr(container.getContext(), R.attr.plugin_details_install_header_bg);
		textColorPrimary = AndroidUtils.getColorFromAttr(container.getContext(), android.R.attr.textColorPrimary);
		textColorSecondary = AndroidUtils.getColorFromAttr(container.getContext(), android.R.attr.textColorSecondary);

		return view;
	}

	public void requestAndUpdateUi() {
		int monthItemPosition = monthReportsSpinner.getSelectedItemPosition();
		String monthUrlString = monthsForReportsAdapter.getQueryString(monthItemPosition);
		String countryUrlString = selectedCountryItem.getDownloadName();

		tryUpdateData(monthUrlString, countryUrlString);
	}

	private void tryUpdateData(String monthUrlString, String regionUrlString) {
		OnResponseListener<TotalChangesByMonthResponse> onResponseListener = response -> {
			if (response != null) {
				if (contributorsTextView != null) {
					contributorsTextView.setText(String.valueOf(response.users));
				}
				if (editsTextView != null) {
					editsTextView.setText(String.valueOf(response.changes));
				}
			}
			disableProgress();
		};
		OnErrorListener onErrorListener = error -> {
			if (contributorsTextView != null) {
				contributorsTextView.setText(R.string.data_is_not_available);
			}
			if (editsTextView != null) {
				editsTextView.setText(R.string.data_is_not_available);
			}
			disableProgress();
		};
		enableProgress();
		GetJsonAsyncTask<TotalChangesByMonthResponse> totalChangesByMontAsyncTask =
				new GetJsonAsyncTask<>(TotalChangesByMonthResponse.class);
		totalChangesByMontAsyncTask.setOnResponseListener(onResponseListener);
		totalChangesByMontAsyncTask.setOnErrorListener(onErrorListener);
		String finalUrl = String.format(TOTAL_CHANGES_BY_MONTH_URL_PATTERN, monthUrlString, regionUrlString);
		OsmAndTaskManager.executeTask(totalChangesByMontAsyncTask, finalUrl);

		GetJsonAsyncTask<RecipientsByMonth> recChangesByMontAsyncTask =
				new GetJsonAsyncTask<>(Protocol.RecipientsByMonth.class);
		OnResponseListener<RecipientsByMonth> recResponseListener = response -> {
			if (response != null) {
				if (recipientsTextView != null) {
					recipientsTextView.setText(String.valueOf(response.regionCount));
				}
				if (donationsTextView != null) {
					donationsTextView.setText(String.format("%.3f", response.regionBtc * 1000f) + " mBTC");
				}
				if (donationsTotalLayout != null &&
						donationsTotalTextView != null) {
					donationsTotalLayout.setVisibility(regionUrlString.isEmpty() ? View.VISIBLE : View.GONE);
					donationsTotalTextView.setText(String.format("%.3f", response.btc * 1000f) + " mBTC");
				}
			}
			disableProgress();
		};
		recChangesByMontAsyncTask.setOnResponseListener(recResponseListener);
		clearTextViewResult(recipientsTextView);
		clearTextViewResult(donationsTextView);
		clearTextViewResult(donationsTotalTextView);

		String recfinalUrl = String.format(RECIPIENTS_BY_MONTH, monthUrlString, regionUrlString);
		OsmAndTaskManager.executeTask(recChangesByMontAsyncTask, recfinalUrl);
	}

	private void setThemedDrawable(View parent, @IdRes int viewId, @DrawableRes int iconId) {
		((ImageView) parent.findViewById(viewId)).setImageDrawable(getContentIcon(iconId));
	}

	private void setThemedDrawable(View view, @DrawableRes int iconId) {
		((ImageView) view).setImageDrawable(getContentIcon(iconId));
	}

	private void clearTextViewResult(TextView textView) {
		if (textView != null) {
			textView.setText("-");
		}
	}

	@Override
	public void onSearchResult(CountryItem item) {
		selectedCountryItem = item;
		countryNameTextView.setText(item.getLocalName());
		requestAndUpdateUi();
	}

	private void enableProgress() {
		numberOfContributorsIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_group, inactiveColor));
		numberOfEditsIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_map, inactiveColor));
		numberOfRecipientsIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_group, inactiveColor));
		donationsIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_bitcoin, inactiveColor));
		donationsTotalIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_bitcoin, inactiveColor));

		numberOfContributorsTitle.setTextColor(inactiveColor);
		numberOfEditsTitle.setTextColor(inactiveColor);
		numberOfRecipientsTitle.setTextColor(inactiveColor);
		donationsTitle.setTextColor(inactiveColor);
		donationsTotalTitle.setTextColor(inactiveColor);

		progressBar.setVisibility(View.VISIBLE);

		contributorsTextView.setTextColor(inactiveColor);
		donationsTextView.setTextColor(inactiveColor);
		donationsTotalTextView.setTextColor(inactiveColor);
		recipientsTextView.setTextColor(inactiveColor);
		editsTextView.setTextColor(inactiveColor);
	}

	private void disableProgress() {
		numberOfContributorsIcon.setImageDrawable(getContentIcon(R.drawable.ic_group));
		numberOfEditsIcon.setImageDrawable(getContentIcon(R.drawable.ic_map));
		numberOfRecipientsIcon.setImageDrawable(getContentIcon(R.drawable.ic_group));
		donationsIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_bitcoin));
		donationsTotalIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_bitcoin));

		numberOfContributorsTitle.setTextColor(textColorSecondary);
		numberOfEditsTitle.setTextColor(textColorSecondary);
		numberOfRecipientsTitle.setTextColor(textColorSecondary);
		donationsTitle.setTextColor(textColorSecondary);
		donationsTotalTitle.setTextColor(textColorSecondary);

		progressBar.setVisibility(View.INVISIBLE);

		contributorsTextView.setTextColor(textColorPrimary);
		editsTextView.setTextColor(textColorPrimary);
		donationsTextView.setTextColor(textColorPrimary);
		donationsTotalTextView.setTextColor(textColorPrimary);
		recipientsTextView.setTextColor(textColorPrimary);
	}
}
