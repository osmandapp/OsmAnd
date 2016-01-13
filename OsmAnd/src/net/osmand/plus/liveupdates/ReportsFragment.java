package net.osmand.plus.liveupdates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.liveupdates.network.GetJsonAsyncTask;
import net.osmand.plus.liveupdates.network.Protocol;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReportsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReportsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReportsFragment extends BaseOsmAndFragment {
	public static final String TITLE = "Report";
	public static final String TOTAL_CHANGES_BY_MONTH_URL = "http://builder.osmand.net/reports/total_changes_by_month.php?month=";

	private TextView contributorsTextView;
	private TextView editsTextView;

	private Spinner montReportsSpinner;
	private Spinner regionReportsSpinner;
	private MonthsForReportsAdapter monthsForReportsAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_reports, container, false);
		montReportsSpinner = (Spinner) view.findViewById(R.id.montReportsSpinner);
		monthsForReportsAdapter = new MonthsForReportsAdapter(getActivity());
		montReportsSpinner.setAdapter(monthsForReportsAdapter);

		regionReportsSpinner = (Spinner) view.findViewById(R.id.regionReportsSpinner);
		ArrayAdapter<String> regionsForReportsAdapter =
				new ArrayAdapter<String>(getActivity(), R.layout.reports_for_spinner_item,
						android.R.id.text1, new String[]{"Worldwide"});
		regionsForReportsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		regionReportsSpinner.setAdapter(regionsForReportsAdapter);

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
		regionReportsSpinner.setOnItemSelectedListener(onItemSelectedListener);
		return view;
	}

	private void requestAndUpdateUi() {
		int monthItemPosition = montReportsSpinner.getSelectedItemPosition();
		String monthUrlString = monthsForReportsAdapter.getQueryString(monthItemPosition);
		String regionUrlString = regionReportsSpinner.getSelectedItem().toString();
		GetJsonAsyncTask.OnResponseListener<Protocol.TotalChangesByMonthResponse> onResponseListener =
				new GetJsonAsyncTask.OnResponseListener<Protocol.TotalChangesByMonthResponse>() {
					@Override
					public void onResponse(Protocol.TotalChangesByMonthResponse response) {
						if (contributorsTextView != null) {
							contributorsTextView.setText(String.valueOf(response.users));
						}
						if (editsTextView != null) {
							editsTextView.setText(String.valueOf(response.changes));
						}
					}
				};
		requestData(monthUrlString, regionUrlString, onResponseListener);
	}

	private void requestData(String monthUrlString, String regionUrlString,
							 GetJsonAsyncTask.OnResponseListener<Protocol.TotalChangesByMonthResponse> onResponseListener) {
		GetJsonAsyncTask<Protocol.TotalChangesByMonthResponse> totalChangesByMontAsyncTask =
				new GetJsonAsyncTask<>(Protocol.TotalChangesByMonthResponse.class);
		totalChangesByMontAsyncTask.setOnResponseListener(onResponseListener);
		totalChangesByMontAsyncTask.execute(TOTAL_CHANGES_BY_MONTH_URL + monthUrlString);
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
			Calendar endDate = Calendar.getInstance();
			endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
			endDate.set(Calendar.HOUR_OF_DAY, endDate.getActualMaximum(Calendar.HOUR_OF_DAY));
			while (startDate.before(endDate)) {
				queryString.add(queryFormat.format(startDate.getTime()));
				add(humanFormat.format(startDate.getTime()));
				startDate.add(Calendar.MONTH, 1);
			}
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

		public String getQueryString(int position) {
			return queryString.get(position);
		}
	}

}
