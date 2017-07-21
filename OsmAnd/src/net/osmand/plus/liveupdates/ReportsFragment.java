package net.osmand.plus.liveupdates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;

import org.apache.commons.logging.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ReportsFragment extends BaseOsmAndFragment implements CountrySelectionFragment.OnFragmentInteractionListener {
	public static final int TITLE = R.string.report;
	public static final String DOMAIN = "http://download.osmand.net/";
	public static final String TOTAL_CHANGES_BY_MONTH_URL_PATTERN = DOMAIN +
			"reports/query_report.php?report=total_changes_by_month&month=%s&region=%s";
	public static final String USERS_RANKING_BY_MONTH =  DOMAIN +
			"reports/query_report.php?report=ranking_users_by_month&month=%s&region=%s";
	public static final String RECIPIENTS_BY_MONTH =  DOMAIN +
			"reports/query_report.php?report=recipients_by_month&month=%s&region=%s";


	private static final Log LOG = PlatformUtil.getLog(ReportsFragment.class);
	public static final String OSM_LIVE_URL = "https://osmand.net/osm_live";

	private TextView contributorsTextView;
	private TextView editsTextView;
	private TextView donationsTextView;
	private TextView recipientsTextView;

	private Spinner monthReportsSpinner;
	private MonthsForReportsAdapter monthsForReportsAdapter;

	private CountrySelectionFragment countrySelectionFragment = new CountrySelectionFragment();
	private UsersReportFragment userReportFragment = new UsersReportFragment();
	private TextView countryNameTextView;
	private CountryItem selectedCountryItem;

	private ImageView numberOfContributorsIcon;
	private ImageView numberOfEditsIcon;
	private ImageView donationsIcon;
	private ImageView numberOfRecipientsIcon;
	private TextView numberOfContributorsTitle;
	private TextView numberOfEditsTitle;
	private TextView numberOfRecipientsTitle;
	private TextView donationsTitle;
	private ProgressBar progressBar;

	private int inactiveColor;
	private int textColorPrimary;
	private int textColorSecondary;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_reports, container, false);
		monthReportsSpinner = (Spinner) view.findViewById(R.id.monthReportsSpinner);
		final View monthButton = view.findViewById(R.id.monthButton);
		monthReportsSpinner.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				event.offsetLocation(AndroidUtils.dpToPx(getActivity(), 48f), 0);
				monthButton.onTouchEvent(event);
				return true;
			}
		});
		monthsForReportsAdapter = new MonthsForReportsAdapter(getActivity());
		monthReportsSpinner.setAdapter(monthsForReportsAdapter);

		monthButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				monthReportsSpinner.performClick();
			}
		});

		view.findViewById(R.id.show_all).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(OSM_LIVE_URL));
				startActivity(intent);
			}
		});
		((TextView) view.findViewById(R.id.osm_live_url_label)).setText(OSM_LIVE_URL);

		View regionReportsButton = view.findViewById(R.id.reportsButton);
		regionReportsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				countrySelectionFragment.show(getChildFragmentManager(), "CountriesSearchSelectionFragment");
			}
		});
		OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int monthItemPosition = monthReportsSpinner.getSelectedItemPosition();
				String monthUrlString = monthsForReportsAdapter.getQueryString(monthItemPosition);
				String countryUrlString = selectedCountryItem.getDownloadName();
				if (countryUrlString.length() > 0) {
					Bundle bl = new Bundle();
					bl.putString(UsersReportFragment.URL_REQUEST,
							String.format(USERS_RANKING_BY_MONTH, monthUrlString, countryUrlString));
					userReportFragment.setArguments(bl);
					userReportFragment.show(getChildFragmentManager(), "NumberOfEditsFramgnet");
				}
			}
		};
		view.findViewById(R.id.numberOfContributorsLayout).setOnClickListener(listener);
		view.findViewById(R.id.numberOfEditsLayout).setOnClickListener(listener);

		countrySelectionFragment.initCountries(getMyApplication());
		selectedCountryItem = countrySelectionFragment.getCountryItems().get(0);
		

		countryNameTextView = (TextView) regionReportsButton.findViewById(android.R.id.text1);
		countryNameTextView.setText(selectedCountryItem.getLocalName());

		setThemedDrawable(view, R.id.calendarImageView, R.drawable.ic_action_data);
		setThemedDrawable(view, R.id.monthDropDownIcon, R.drawable.ic_action_arrow_drop_down);
		setThemedDrawable(view, R.id.regionIconImageView, R.drawable.ic_world_globe_dark);
		setThemedDrawable(view, R.id.countryDropDownIcon, R.drawable.ic_action_arrow_drop_down);

		numberOfContributorsIcon = (ImageView) view.findViewById(R.id.numberOfContributorsIcon);
		numberOfEditsIcon = (ImageView) view.findViewById(R.id.numberOfEditsIcon);
		numberOfRecipientsIcon = (ImageView) view.findViewById(R.id.numberOfRecipientsIcon);
		donationsIcon = (ImageView) view.findViewById(R.id.donationsIcon);
		setThemedDrawable(numberOfContributorsIcon, R.drawable.ic_action_group2);
		setThemedDrawable(numberOfRecipientsIcon, R.drawable.ic_group);
		setThemedDrawable(donationsIcon, R.drawable.ic_action_bitcoin);
		setThemedDrawable(numberOfEditsIcon, R.drawable.ic_map);
		
		
		numberOfContributorsTitle = (TextView) view.findViewById(R.id.numberOfContributorsTitle);
		numberOfEditsTitle = (TextView) view.findViewById(R.id.numberOfEditsTitle);
		donationsTitle = (TextView) view.findViewById(R.id.donationsTitle);
		numberOfRecipientsTitle = (TextView) view.findViewById(R.id.numberOfRecipientsTitle);
		
		progressBar = (ProgressBar) view.findViewById(R.id.progress);

		contributorsTextView = (TextView) view.findViewById(R.id.contributorsTextView);
		editsTextView = (TextView) view.findViewById(R.id.editsTextView);
		donationsTextView = (TextView) view.findViewById(R.id.donationsTextView);
		recipientsTextView = (TextView) view.findViewById(R.id.recipientsTextView);

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

		inactiveColor = getColorFromAttr(R.attr.plugin_details_install_header_bg);
		textColorPrimary = getColorFromAttr(android.R.attr.textColorPrimary);
		textColorSecondary = getColorFromAttr(android.R.attr.textColorSecondary);

		return view;
	}

	public void requestAndUpdateUi() {
		int monthItemPosition = monthReportsSpinner.getSelectedItemPosition();
		String monthUrlString = monthsForReportsAdapter.getQueryString(monthItemPosition);
		String countryUrlString = selectedCountryItem.getDownloadName();

		tryUpdateData(monthUrlString, countryUrlString);
	}

	private void tryUpdateData(String monthUrlString, String regionUrlString) {
		GetJsonAsyncTask.OnResponseListener<Protocol.TotalChangesByMonthResponse> onResponseListener =
				new GetJsonAsyncTask.OnResponseListener<Protocol.TotalChangesByMonthResponse>() {
					@Override
					public void onResponse(Protocol.TotalChangesByMonthResponse response) {
						if (response != null) {
							if (contributorsTextView != null) {
								contributorsTextView.setText(String.valueOf(response.users));
							}
							if (editsTextView != null) {
								editsTextView.setText(String.valueOf(response.changes));
							}
						}
						disableProgress();
					}
				};
		GetJsonAsyncTask.OnErrorListener onErrorListener =
				new GetJsonAsyncTask.OnErrorListener() {
					@Override
					public void onError(String error) {
						if (contributorsTextView != null) {
							contributorsTextView.setText(R.string.data_is_not_available);
						}
						if (editsTextView != null) {
							editsTextView.setText(R.string.data_is_not_available);
						}
						disableProgress();
					}
				};
		enableProgress();
		GetJsonAsyncTask<Protocol.TotalChangesByMonthResponse> totalChangesByMontAsyncTask =
				new GetJsonAsyncTask<>(Protocol.TotalChangesByMonthResponse.class);
		totalChangesByMontAsyncTask.setOnResponseListener(onResponseListener);
		totalChangesByMontAsyncTask.setOnErrorListener(onErrorListener);
		String finalUrl = String.format(TOTAL_CHANGES_BY_MONTH_URL_PATTERN, monthUrlString, regionUrlString);
		totalChangesByMontAsyncTask.execute(finalUrl);
		
		GetJsonAsyncTask<Protocol.RecipientsByMonth> recChangesByMontAsyncTask =
				new GetJsonAsyncTask<>(Protocol.RecipientsByMonth.class);
		GetJsonAsyncTask.OnResponseListener<Protocol.RecipientsByMonth> recResponseListener =
				new GetJsonAsyncTask.OnResponseListener<Protocol.RecipientsByMonth>() {
					@Override
					public void onResponse(Protocol.RecipientsByMonth response) {
						if (response != null) {
							if (recipientsTextView != null) {
								recipientsTextView.setText(String.valueOf(response.regionCount));
							}
							if (donationsTextView != null) {
								donationsTextView.setText(String.format("%.3f", response.regionBtc*1000.0) + " mBTC");
							}
						}
						disableProgress();
					}
				};
		recChangesByMontAsyncTask.setOnResponseListener(recResponseListener);
		
		if (recipientsTextView != null) {
			recipientsTextView.setText("-");
		}
		if (donationsTextView != null) {
			donationsTextView.setText("-");
		}
		String recfinalUrl = String.format(RECIPIENTS_BY_MONTH, monthUrlString, regionUrlString);
		recChangesByMontAsyncTask.execute(recfinalUrl);
	}

	@Override
	public void onSearchResult(CountryItem item) {
		selectedCountryItem = item;
		countryNameTextView.setText(item.getLocalName());
		requestAndUpdateUi();
	}

	private static class MonthsForReportsAdapter extends ArrayAdapter<String> {
		private static final SimpleDateFormat queryFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
		@SuppressLint("SimpleDateFormat")
		private static final SimpleDateFormat humanFormat = new SimpleDateFormat("LLLL yyyy");

		ArrayList<String> queryString = new ArrayList<>();

		public MonthsForReportsAdapter(Context context) {
			super(context, android.R.layout.simple_spinner_item);
			Calendar startDate = Calendar.getInstance();
			startDate.set(Calendar.MONTH, Calendar.SEPTEMBER);
			startDate.set(Calendar.YEAR, 2015);
			startDate.set(Calendar.DAY_OF_MONTH, 1);
			startDate.set(Calendar.HOUR_OF_DAY, 0);
			Calendar endDate = Calendar.getInstance();
			while (startDate.before(endDate)) {
				queryString.add(queryFormat.format(endDate.getTime()));
				add(humanFormat.format(endDate.getTime()));
				endDate.add(Calendar.MONTH, -1);
			}
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

		public String getQueryString(int position) {
			return queryString.get(position);
		}
	}

	public static class GetJsonAsyncTask<P> extends AsyncTask<String, Void, P> {
		private static final Log LOG = PlatformUtil.getLog(GetJsonAsyncTask.class);
		private final Class<P> protocolClass;
		private final Gson gson = new Gson();
		private OnResponseListener<P> onResponseListener;
		private OnErrorListener onErrorListener;
		private volatile String error;

		public GetJsonAsyncTask(Class<P> protocolClass) {
			this.protocolClass = protocolClass;
		}

		@Override
		protected P doInBackground(String... params) {
			StringBuilder response = new StringBuilder();
			error = NetworkUtils.sendGetRequest(params[0], null, response);
			if (error == null) {
				try {
					return gson.fromJson(response.toString(), protocolClass);
				} catch (JsonSyntaxException e) {
					error = e.getLocalizedMessage();
				}
			}
			LOG.error(error);
			return null;
		}

		@Override
		protected void onPostExecute(P protocol) {
			if (protocol != null) {
				if(onResponseListener != null) {
					onResponseListener.onResponse(protocol);
				}
			} else if (onErrorListener != null) {
				onErrorListener.onError(error);
			}
		}

		public void setOnResponseListener(OnResponseListener<P> onResponseListener) {
			this.onResponseListener = onResponseListener;
		}

		public void setOnErrorListener(OnErrorListener onErrorListener) {
			this.onErrorListener = onErrorListener;
		}

		public interface OnResponseListener<Protocol> {
			void onResponse(Protocol response);
		}

		public interface OnErrorListener {
			void onError(String error);
		}
	}

	private void enableProgress() {
		numberOfContributorsIcon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_group, inactiveColor));
		numberOfEditsIcon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_map, inactiveColor));
		numberOfRecipientsIcon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_group, inactiveColor));
		donationsIcon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_bitcoin, inactiveColor));
		
		numberOfContributorsTitle.setTextColor(inactiveColor);
		numberOfEditsTitle.setTextColor(inactiveColor);
		numberOfRecipientsTitle.setTextColor(inactiveColor);
		donationsTitle.setTextColor(inactiveColor);
		
		progressBar.setVisibility(View.VISIBLE);

		contributorsTextView.setTextColor(inactiveColor);
		donationsTextView.setTextColor(inactiveColor);
		recipientsTextView.setTextColor(inactiveColor);
		editsTextView.setTextColor(inactiveColor);
	}

	private void disableProgress() {
		numberOfContributorsIcon.setImageDrawable(getContentIcon(R.drawable.ic_group));
		numberOfEditsIcon.setImageDrawable(getContentIcon(R.drawable.ic_map));
		numberOfRecipientsIcon.setImageDrawable(getContentIcon(R.drawable.ic_group));
		donationsIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_bitcoin));
		
		numberOfContributorsTitle.setTextColor(textColorSecondary);
		numberOfEditsTitle.setTextColor(textColorSecondary);
		numberOfRecipientsTitle.setTextColor(textColorSecondary);
		donationsTitle.setTextColor(textColorSecondary);
		
		progressBar.setVisibility(View.INVISIBLE);

		contributorsTextView.setTextColor(textColorPrimary);
		editsTextView.setTextColor(textColorPrimary);
		donationsTextView.setTextColor(textColorPrimary);
		recipientsTextView.setTextColor(textColorPrimary);
	}

	@ColorInt
	private int getColorFromAttr(@AttrRes int colorAttribute) {
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = getActivity().getTheme();
		theme.resolveAttribute(colorAttribute, typedValue, true);
		return typedValue.data;
	}
}
