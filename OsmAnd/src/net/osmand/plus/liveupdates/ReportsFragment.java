package net.osmand.plus.liveupdates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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

	private ImageView numberOfContributorsIcon;
	private ImageView numberOfEditsIcon;
	private TextView numberOfContributorsTitle;
	private TextView numberOfEditsTitle;
	private ProgressBar progressBar;

	private int inactiveColor;
	private int textColorPrimary;
	private int textColorSecondary;

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
		numberOfContributorsIcon = (ImageView) view.findViewById(R.id.numberOfContributorsIcon);
		setThemedDrawable(numberOfContributorsIcon, R.drawable.ic_group);
		numberOfEditsIcon = (ImageView) view.findViewById(R.id.numberOfEditsIcon);
		setThemedDrawable(numberOfEditsIcon, R.drawable.ic_map);
		numberOfContributorsTitle = (TextView) view.findViewById(R.id.numberOfContributorsTitle);
		numberOfEditsTitle = (TextView) view.findViewById(R.id.numberOfEditsTitle);
		progressBar = (ProgressBar) view.findViewById(R.id.progress);

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

		inactiveColor = getColorFromAttr(R.attr.plugin_details_install_header_bg);
		textColorPrimary = getColorFromAttr(android.R.attr.textColorPrimary);
		textColorSecondary = getColorFromAttr(android.R.attr.textColorSecondary);

		return view;
	}

	public void requestAndUpdateUi() {
		int monthItemPosition = montReportsSpinner.getSelectedItemPosition();
		String monthUrlString = monthsForReportsAdapter.getQueryString(monthItemPosition);
		String countryUrlString = queryRegionNames.get(countryNameTextView.getText().toString());

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
				onResponseListener.onResponse(protocol);
			} else {
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

	public static class CountrySearchSelectionFragment extends SearchSelectionFragment {
		@Override
		protected ArrayList<String> getList() {
			return ((ReportsFragment) getParentFragment()).regionNames;
		}

		@Override
		protected int getListItemIcon() {
			return R.drawable.ic_map;
		}
	}

	private void enableProgress() {
		numberOfContributorsIcon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_group, inactiveColor));
		numberOfEditsIcon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_map, inactiveColor));
		numberOfContributorsTitle.setTextColor(inactiveColor);
		numberOfEditsTitle.setTextColor(inactiveColor);
		progressBar.setVisibility(View.VISIBLE);
		contributorsTextView.setTextColor(inactiveColor);
		editsTextView.setTextColor(inactiveColor);
	}

	private void disableProgress() {
		numberOfContributorsIcon.setImageDrawable(getContentIcon(R.drawable.ic_group));
		numberOfEditsIcon.setImageDrawable(getContentIcon(R.drawable.ic_map));
		numberOfContributorsTitle.setTextColor(textColorSecondary);
		numberOfEditsTitle.setTextColor(textColorSecondary);
		progressBar.setVisibility(View.INVISIBLE);
		contributorsTextView.setTextColor(textColorPrimary);
		editsTextView.setTextColor(textColorPrimary);
	}

	@ColorInt
	private int getColorFromAttr(@AttrRes int colorAttribute) {
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = getActivity().getTheme();
		theme.resolveAttribute(colorAttribute, typedValue, true);
		return typedValue.data;
	}
}
