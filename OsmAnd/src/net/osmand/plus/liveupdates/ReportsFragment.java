package net.osmand.plus.liveupdates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import net.osmand.PlatformUtil;
import net.osmand.map.WorldRegion;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;

import org.apache.commons.logging.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends BaseOsmAndFragment implements SearchSelectionFragment.OnFragmentInteractionListener {
	public static final String TITLE = "Report";
	public static final String TOTAL_CHANGES_BY_MONTH_URL_PATTERN = "http://download.osmand.net/" +
			"reports/query_report.php?report=total_changes_by_month&month=%s&region=%s";
	private static final Log LOG = PlatformUtil.getLog(ReportsFragment.class);

	private TextView contributorsTextView;
	private TextView editsTextView;

	private Spinner montReportsSpinner;
	private MonthsForReportsAdapter monthsForReportsAdapter;

	CountrySearchSelectionFragment searchSelectionFragment = new CountrySearchSelectionFragment();
	private TextView countryNameTextView;

	HashMap<String, String> queryRegionNames = new HashMap<>();
	ArrayList<String> regionNames = new ArrayList<>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		initCountries();
		View view = inflater.inflate(R.layout.fragment_reports, container, false);
		montReportsSpinner = (Spinner) view.findViewById(R.id.montReportsSpinner);
		monthsForReportsAdapter = new MonthsForReportsAdapter(getActivity());
		montReportsSpinner.setAdapter(monthsForReportsAdapter);

		View regionReportsButton = view.findViewById(R.id.reportsButton);
		regionReportsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchSelectionFragment countrySearchSelectionFragment =
						searchSelectionFragment;
				countrySearchSelectionFragment
						.show(getChildFragmentManager(), "CountriesSearchSelectionFragment");
			}
		});

		countryNameTextView = (TextView) regionReportsButton.findViewById(android.R.id.text1);
		countryNameTextView.setText(regionNames.get(0));

		setThemedDrawable(view, R.id.calendarImageView, R.drawable.ic_action_data);
		setThemedDrawable(view, R.id.regionIconImageView, R.drawable.ic_world_globe_dark);
		setThemedDrawable(view, R.id.numberOfContributorsIcon, R.drawable.ic_group);
		setThemedDrawable(view, R.id.numberOfEditsIcon, R.drawable.ic_group);

		contributorsTextView = (TextView) view.findViewById(R.id.contributorsTextView);
		editsTextView = (TextView) view.findViewById(R.id.editsTextView);

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
		montReportsSpinner.setOnItemSelectedListener(onItemSelectedListener);
		return view;
	}

	public void requestAndUpdateUi() {
		int monthItemPosition = montReportsSpinner.getSelectedItemPosition();
		String monthUrlString = monthsForReportsAdapter.getQueryString(monthItemPosition);
		String countryUrlString = queryRegionNames.get(countryNameTextView.getText().toString());
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
					}
				};
		requestData(monthUrlString, countryUrlString, onResponseListener);
	}

	private void requestData(String monthUrlString, String regionUrlString,
							 GetJsonAsyncTask.OnResponseListener<Protocol.TotalChangesByMonthResponse> onResponseListener) {
		GetJsonAsyncTask<Protocol.TotalChangesByMonthResponse> totalChangesByMontAsyncTask =
				new GetJsonAsyncTask<>(Protocol.TotalChangesByMonthResponse.class);
		totalChangesByMontAsyncTask.setOnResponseListener(onResponseListener);
		String finalUrl = String.format(TOTAL_CHANGES_BY_MONTH_URL_PATTERN, monthUrlString, regionUrlString);
		totalChangesByMontAsyncTask.execute(finalUrl);
	}

	@Override
	public void onSearchResult(String name) {
		countryNameTextView.setText(name);
		requestAndUpdateUi();
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
			queryRegionNames.put(name, group == root ? "" : group.getRegionDownloadName());
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

	public String getQueryString(int position) {
		return queryRegionNames.get(position);
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

	private static class MonthsForReportsAdapter extends ArrayAdapter<String> {
		private static final SimpleDateFormat queryFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
		@SuppressLint("SimpleDateFormat")
		private static final SimpleDateFormat humanFormat = new SimpleDateFormat("MMMM yyyy");

		ArrayList<String> queryString = new ArrayList<>();

		public MonthsForReportsAdapter(Context context) {
			super(context, android.R.layout.simple_spinner_item);
			Calendar startDate = Calendar.getInstance();
			startDate.set(Calendar.MONTH, Calendar.JUNE);
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

		public GetJsonAsyncTask(Class<P> protocolClass) {
			this.protocolClass = protocolClass;
		}

		@Override
		protected P doInBackground(String... params) {
			StringBuilder response = new StringBuilder();
			String error = NetworkUtils.sendGetRequest(params[0], null, response);
			if (error == null) {
				return gson.fromJson(response.toString(), protocolClass);
			}
			LOG.error(error);
			return null;
		}

		@Override
		protected void onPostExecute(P protocol) {
			if (onResponseListener != null) {
				onResponseListener.onResponse(protocol);
			}
		}

		public void setOnResponseListener(OnResponseListener<P> onResponseListener) {
			this.onResponseListener = onResponseListener;
		}

		public interface OnResponseListener<Protocol> {
			void onResponse(Protocol response);
		}
	}

	public static class CountrySearchSelectionFragment extends SearchSelectionFragment {
		@Override
		protected ArrayList<String> getList() {
			return ((ReportsFragment) getParentFragment()).regionNames;
		}
	}
}
