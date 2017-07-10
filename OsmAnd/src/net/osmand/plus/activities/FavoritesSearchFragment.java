package net.osmand.plus.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.access.AccessibilityAssistant;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesSearchFragment extends DialogFragment {

	public static final String TAG = "FavoritesSearchFragment";

	private OsmandApplication app;
	private AccessibilityAssistant accessibilityAssistant;

	private static final String FAV_SEARCH_QUERY_KEY = "fav_search_query_key";

	private Toolbar toolbar;
	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private ListView listView;
	private FavoritesSearchListAdapter listAdapter;

	private String searchQuery;
	private boolean paused;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		accessibilityAssistant = new AccessibilityAssistant(getActivity());
		boolean isLightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	@SuppressLint("PrivateResource, ValidFragment")
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final Activity activity = getActivity();
		final View view = inflater.inflate(R.layout.search_favs_fragment, container, false);

		Bundle arguments = getArguments();
		if (savedInstanceState != null) {
			searchQuery = savedInstanceState.getString(FAV_SEARCH_QUERY_KEY);
		}
		if (searchQuery == null && arguments != null) {
			searchQuery = arguments.getString(FAV_SEARCH_QUERY_KEY);
		}
		if (searchQuery == null) {
			searchQuery = "";
		}

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(app.getIconsCache().getThemedIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				}
		);

		searchEditText = (EditText) view.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.search_favorites);
		searchEditText.addTextChangedListener(
				new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}

					@Override
					public void afterTextChanged(Editable s) {
						String newQueryText = s.toString();
						if (!searchQuery.equalsIgnoreCase(newQueryText)) {
							searchQuery = newQueryText;
							listAdapter.getFilter().filter(newQueryText);
						}
					}
				}
		);

		progressBar = (ProgressBar) view.findViewById(R.id.searchProgressBar);
		clearButton = (ImageButton) view.findViewById(R.id.clearButton);
		clearButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark));
		clearButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (searchEditText.getText().length() > 0) {
							searchEditText.setText("");
							searchEditText.setSelection(0);
						}
					}
				}
		);

		listView = (ListView) view.findViewById(android.R.id.list);

		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (listView != null) {
			listView.setOnScrollListener(new AbsListView.OnScrollListener() {
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				}

				public void onScrollStateChanged(AbsListView view, int scrollState) {
					if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
						hideKeyboard();
					}
				}
			});
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					FavouritePoint point = listAdapter.getItem(position);
					if (point != null) {
						showOnMap(point);
						dismiss();
					}
				}
			});
			listAdapter = new FavoritesSearchListAdapter(getMyApplication(), getActivity());
			listAdapter.setAccessibilityAssistant(accessibilityAssistant);
			listAdapter.synchronizePoints();
			listView.setAdapter(listAdapter);
		}
		openKeyboard();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new Dialog(getActivity(), getTheme()){
			@Override
			public void onBackPressed() {
				cancel();
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();
		paused = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		hideProgressBar();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		Activity activity = getActivity();
		if (activity != null) {
			getChildFragmentManager().popBackStack();
		}
		super.onDismiss(dialog);
	}

	public void showOnMap(final FavouritePoint point) {
		getMyApplication().getSettings().FAVORITES_TAB.set(FavoritesActivity.FAV_TAB);

		final OsmandSettings settings = getMyApplication().getSettings();
		LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
		settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
				settings.getLastKnownMapZoom(),
				new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getName()),
				true,
				point); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private void openKeyboard() {
		searchEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(searchEditText);
	}

	public void hideKeyboard() {
		if (searchEditText.hasFocus()) {
			AndroidUtils.hideSoftKeyboard(getActivity(), searchEditText);
		}
	}

	private void showProgressBar() {
		updateClearButtonVisibility(false);
		progressBar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		updateClearButtonVisibility(true);
		progressBar.setVisibility(View.GONE);
	}

	private void updateClearButtonVisibility(boolean show) {
		if (show) {
			clearButton.setVisibility(View.VISIBLE);
		} else {
			clearButton.setVisibility(View.GONE);
		}
	}

	public static boolean showInstance(@NonNull FragmentActivity activity, @NonNull String searchQuery) {
		try {

			Bundle bundle = new Bundle();
			if (!Algorithms.isEmpty(searchQuery)) {
				bundle.putString(FAV_SEARCH_QUERY_KEY, searchQuery);
			}

			FavoritesSearchFragment fragment = new FavoritesSearchFragment();
			fragment.setArguments(bundle);
			fragment.show(activity.getSupportFragmentManager(), TAG);
			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	class FavoritesSearchListAdapter extends ArrayAdapter<FavouritePoint> implements Filterable {

		private static final int FAVORITE_TYPE = 0;
		private static final int HEADER_SHADOW_TYPE = 1;
		private static final int HEADER_TYPE = 2;
		private static final int FOOTER_SHADOW_TYPE = 3;

		private OsmandApplication app;
		private Activity activity;
		private AccessibilityAssistant accessibilityAssistant;
		private FavouritesDbHelper helper;

		private LatLon location;
		private Drawable arrowImage;

		List<FavouritePoint> points = new ArrayList<>();
		Filter myFilter;
		private Set<?> filter;

		public FavoritesSearchListAdapter(OsmandApplication app, Activity activity) {
			super(app, R.layout.search_list_item);
			this.app = app;
			this.activity = activity;
			this.helper = app.getFavorites();
			location = app.getSettings().getLastKnownMapLocation();
			arrowImage = ContextCompat.getDrawable(activity, R.drawable.ic_direction_arrow);
			arrowImage.mutate();
			arrowImage.setColorFilter(ContextCompat.getColor(activity, R.color.color_distance), PorterDuff.Mode.MULTIPLY);
		}

		public void setAccessibilityAssistant(AccessibilityAssistant accessibilityAssistant) {
			this.accessibilityAssistant = accessibilityAssistant;
		}

		public void synchronizePoints() {
			points.clear();
			List<FavoriteGroup> gs = helper.getFavoriteGroups();
			Set<?> flt = filter;
			for (FavoriteGroup key : gs) {
				if (flt == null || flt.contains(key)) {
					for (FavouritePoint p : key.points) {
						if (p.isVisible()) {
							points.add(p);
						}
					}
				} else {
					ArrayList<FavouritePoint> list = new ArrayList<>();
					for (FavouritePoint p : key.points) {
						if (p.isVisible() && flt.contains(p)) {
							list.add(p);
						}
					}
					points.addAll(list);
				}
			}
			Collections.sort(points, new Comparator<FavouritePoint>() {
				@Override
				public int compare(FavouritePoint p1, FavouritePoint p2) {
					int d1 = (int) (MapUtils.getDistance(p1.getLatitude(), p1.getLongitude(),
							location.getLatitude(), location.getLongitude()));
					int d2 = (int) (MapUtils.getDistance(p2.getLatitude(), p2.getLongitude(),
							location.getLatitude(), location.getLongitude()));
					return d1 < d2 ? -1 : (d1 == d2 ? 0 : 1);
				}
			});
			notifyDataSetChanged();
		}

		public LatLon getLocation() {
			return location;
		}

		public void setLocation(LatLon location) {
			this.location = location;
		}

		@Nullable
		@Override
		public FavouritePoint getItem(int position) {
			if (position > 1 && position < points.size() + 2) {
				return points.get(position - 2);
			} else {
				return null;
			}
		}

		@Override
		public int getCount() {
			if (points.size() > 0) {
				return points.size() + 3;
			} else {
				return 0;
			}
		}

		@Override
		public boolean isEnabled(int position) {
			return getItemViewType(position) == FAVORITE_TYPE;
		}

		@Override
		public int getItemViewType(int position) {
			if (position == 0) {
				return HEADER_SHADOW_TYPE;
			} else if (position == 1) {
				return HEADER_TYPE;
			} else if (position == getCount() - 1) {
				return FOOTER_SHADOW_TYPE;
			} else {
				return FAVORITE_TYPE;
			}
		}

		@Override
		public int getViewTypeCount() {
			return 4;
		}

		@NonNull
		@Override
		public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
			final FavouritePoint point = getItem(position);
			int type = getItemViewType(position);
			LinearLayout view;
			if (type == HEADER_TYPE) {
				if (convertView == null) {
					LayoutInflater inflater = (LayoutInflater) app
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					view = (LinearLayout) inflater.inflate(
							R.layout.search_favs_list_header, null);
				} else {
					view = (LinearLayout) convertView;
				}

				((TextView) view.findViewById(R.id.title)).setText(app.getString(R.string.sorted_by_distance));
			} else if (type == HEADER_SHADOW_TYPE) {
				if (convertView == null) {
					LayoutInflater inflater = (LayoutInflater) app
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					view = (LinearLayout) inflater.inflate(
							R.layout.list_shadow_header, null);
				} else {
					view = (LinearLayout) convertView;
				}
			} else if (type == FOOTER_SHADOW_TYPE) {
				if (convertView == null) {
					LayoutInflater inflater = (LayoutInflater) app
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					view = (LinearLayout) inflater.inflate(
							R.layout.list_shadow_footer, null);
				} else {
					view = (LinearLayout) convertView;
				}
			} else {
				if (convertView == null) {
					LayoutInflater inflater = (LayoutInflater) app
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					view = (LinearLayout) inflater.inflate(
							R.layout.search_favs_list_item, null);
				} else {
					view = (LinearLayout) convertView;
				}

				if (point != null) {
					ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
					TextView title = (TextView) view.findViewById(R.id.title);
					TextView subtitle = (TextView) view.findViewById(R.id.subtitle);

					imageView.setImageDrawable(FavoriteImageDrawable.getOrCreate(activity, point.getColor(), false));
					title.setText(point.getName());

					int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
							location.getLatitude(), location.getLongitude()));
					String distance = OsmAndFormatter.getFormattedDistance(dist, app) + "  ";
					ImageView direction = (ImageView) view.findViewById(R.id.direction);
					direction.setImageDrawable(arrowImage);
					TextView distanceText = (TextView) view.findViewById(R.id.distance);
					distanceText.setText(distance);
					distanceText.setTextColor(app.getResources().getColor(R.color.color_distance));

					subtitle.setText(point.getCategory().length() == 0 ? app.getString(R.string.shared_string_favorites) : point.getCategory());
				}
			}
			View divider = view.findViewById(R.id.divider);
			if (divider != null) {
				if (position > getCount() - 3) {
					divider.setVisibility(View.GONE);
				} else {
					divider.setVisibility(View.VISIBLE);
				}
			}
			ViewCompat.setAccessibilityDelegate(view, accessibilityAssistant);
			return view;
		}

		@NonNull
		@Override
		public Filter getFilter() {
			if (myFilter == null) {
				myFilter = new FavoritesFilter();
			}
			return myFilter;
		}

		void setFilterResults(Set<?> values) {
			this.filter = values;
		}
	}

	class FavoritesFilter extends Filter {

		FavouritesDbHelper helper;

		FavoritesFilter() {
			helper = app.getFavorites();
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			String favorites = app.getString(R.string.shared_string_favorites).toLowerCase();
			if (constraint == null || constraint.length() == 0) {
				results.values = null;
				results.count = 1;
			} else {
				Set<Object> filter = new HashSet<>();
				String cs = constraint.toString().toLowerCase();
				for (FavoriteGroup g : helper.getFavoriteGroups()) {
					String gName;
					if (Algorithms.isEmpty(g.name)) {
						gName = favorites;
					} else {
						gName = g.name.toLowerCase();
					}
					if (g.visible && gName.contains(cs)) {
						filter.add(g);
					} else {
						for (FavouritePoint fp : g.points) {
							if (fp.isVisible() && fp.getName().toLowerCase().contains(cs)) {
								filter.add(fp);
							}
						}
					}
				}
				results.values = filter;
				results.count = filter.size();
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			synchronized (listAdapter) {
				listAdapter.setFilterResults((Set<?>) results.values);
				listAdapter.synchronizePoints();
			}
			listAdapter.notifyDataSetChanged();
		}
	}
}
