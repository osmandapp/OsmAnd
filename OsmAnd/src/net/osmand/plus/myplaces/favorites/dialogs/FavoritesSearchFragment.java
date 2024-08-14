package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.plugins.accessibility.AccessibilityAssistant;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesSearchFragment extends DialogFragment {

	public static final String TAG = "FavoritesSearchFragment";

	private OsmandApplication app;
	private AccessibilityAssistant accessibilityAssistant;

	public static final String FAV_SEARCH_QUERY_KEY = "fav_search_query_key";

	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private ListView listView;
	private FavoritesSearchListAdapter listAdapter;

	private String searchQuery;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		accessibilityAssistant = new AccessibilityAssistant(requireActivity());
		boolean isLightTheme = app.getSettings().isLightContent();
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	@SuppressLint("PrivateResource, ValidFragment")
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Activity activity = requireActivity();
		View view = inflater.inflate(R.layout.search_favs_fragment, container, false);

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

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		Drawable icBack = app.getUIUtilities().getThemedIcon(AndroidUtils.getNavigationIconResId(activity));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		searchEditText = view.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.search_favorites);
		searchEditText.addTextChangedListener(
				new SimpleTextWatcher() {
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

		progressBar = view.findViewById(R.id.searchProgressBar);
		clearButton = view.findViewById(R.id.clearButton);
		clearButton.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
		clearButton.setOnClickListener(v -> {
					if (searchEditText.getText().length() > 0) {
						searchEditText.setText("");
						searchEditText.setSelection(0);
					}
				}
		);

		listView = view.findViewById(android.R.id.list);

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (listView != null) {
			listView.setOnScrollListener(new OnScrollListener() {
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				}

				public void onScrollStateChanged(AbsListView view, int scrollState) {
					if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
						hideKeyboard();
					}
				}
			});
			listView.setOnItemClickListener((parent, view1, position, id) -> {
				FavouritePoint point = listAdapter.getItem(position);
				if (point != null) {
					showOnMap(point);
					dismiss();
				}
			});
			listAdapter = new FavoritesSearchListAdapter(getMyApplication());
			if (!Algorithms.isEmpty(searchQuery)) {
				listAdapter.getFilter().filter(searchQuery);
				searchEditText.setText(searchQuery);
			}
			listAdapter.setAssistant(accessibilityAssistant);
			listAdapter.synchronizePoints();
			listView.setAdapter(listAdapter);
		}
		openKeyboard();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new Dialog(requireActivity(), getTheme()) {
			@Override
			public void onBackPressed() {
				cancel();
			}
		};
	}

	@Override
	public void onPause() {
		super.onPause();
		hideProgressBar();
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		Activity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = getChildFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
			}
		}
		super.onDismiss(dialog);
	}

	public void showOnMap(FavouritePoint point) {
		getMyApplication().getSettings().FAVORITES_TAB.set(MyPlacesActivity.FAV_TAB);

		OsmandSettings settings = getMyApplication().getSettings();
		LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
		settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
				settings.getLastKnownMapZoom(),
				new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getName()),
				true,
				point);

		Bundle bundle = new Bundle();
		bundle.putString(FAV_SEARCH_QUERY_KEY, searchQuery);
		MapActivity.launchMapActivityMoveToTop(requireActivity(), bundle, null, null);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) requireActivity().getApplication();
	}

	private void openKeyboard() {
		searchEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(requireActivity(), searchEditText);
	}

	public void hideKeyboard() {
		if (searchEditText.hasFocus()) {
			AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
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

	class FavoritesSearchListAdapter extends ArrayAdapter<FavouritePoint> {

		private static final int FAVORITE_TYPE = 0;
		private static final int HEADER_SHADOW_TYPE = 1;
		private static final int HEADER_TYPE = 2;
		private static final int FOOTER_SHADOW_TYPE = 3;

		private final OsmandApplication app;
		private final FavouritesHelper helper;
		private AccessibilityAssistant assistant;

		private LatLon location;

		private final List<FavouritePoint> points = new ArrayList<>();
		private Filter myFilter;
		private Set<?> filter;

		@ColorRes
		private final int enabledColor;
		@ColorRes
		private final int disabledColor;
		@ColorRes
		private final int disabledIconColor;

		public FavoritesSearchListAdapter(@NonNull OsmandApplication app) {
			super(app, R.layout.search_list_item);
			this.app = app;
			this.helper = app.getFavoritesHelper();
			location = app.getSettings().getLastKnownMapLocation();
			boolean light = app.getSettings().isLightContent();
			enabledColor = ColorUtilities.getPrimaryTextColorId(!light);
			disabledColor = ColorUtilities.getSecondaryTextColorId(!light);
			disabledIconColor = ColorUtilities.getDefaultIconColorId(!light);
		}

		public void setAssistant(AccessibilityAssistant assistant) {
			this.assistant = assistant;
		}

		public void synchronizePoints() {
			points.clear();
			List<FavoriteGroup> gs = helper.getFavoriteGroups();
			Set<?> flt = filter;
			for (FavoriteGroup key : gs) {
				if (flt == null || flt.contains(key)) {
					points.addAll(key.getPoints());
				} else {
					ArrayList<FavouritePoint> list = new ArrayList<>();
					for (FavouritePoint p : key.getPoints()) {
						if (flt.contains(p)) {
							list.add(p);
						}
					}
					points.addAll(list);
				}
			}
			Collections.sort(points, (p1, p2) -> {
				if (p1.isVisible() && p2.isVisible() || !p1.isVisible() && !p2.isVisible()) {
					int d1 = (int) (MapUtils.getDistance(p1.getLatitude(), p1.getLongitude(),
							location.getLatitude(), location.getLongitude()));
					int d2 = (int) (MapUtils.getDistance(p2.getLatitude(), p2.getLongitude(),
							location.getLatitude(), location.getLongitude()));
					return Integer.compare(d1, d2);
				} else {
					return (p1.isVisible() == p2.isVisible()) ? 0 : (p1.isVisible() ? -1 : 1);
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
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			FavouritePoint point = getItem(position);
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
					boolean visible = point.isVisible();
					ImageView imageView = view.findViewById(R.id.imageView);
					TextView title = view.findViewById(R.id.title);
					TextView subtitle = view.findViewById(R.id.subtitle);
					int color = visible
							? app.getFavoritesHelper().getColorWithCategory(point, getColor(R.color.color_favorite))
							: getColor(disabledIconColor);
					imageView.setImageDrawable(PointImageUtils.getFromPoint(app, color, false, point));
					title.setText(point.getDisplayName(app));
					title.setTypeface(Typeface.DEFAULT, visible ? Typeface.NORMAL : Typeface.ITALIC);
					title.setTextColor(getColor(visible ? enabledColor : disabledColor));

					int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
							location.getLatitude(), location.getLongitude()));
					String distance = OsmAndFormatter.getFormattedDistance(dist, app) + "  ";
					ImageView direction = view.findViewById(R.id.direction);
					direction.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_direction_arrow, visible ? R.color.color_distance : disabledColor));
					TextView distanceText = view.findViewById(R.id.distance);
					distanceText.setText(distance);
					distanceText.setTextColor(visible ? getColor(enabledColor) : getColor(disabledColor));
					subtitle.setText(point.getCategory().length() == 0 ? app.getString(R.string.shared_string_favorites) : point.getCategoryDisplayName(app));
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
			ViewCompat.setAccessibilityDelegate(view, assistant);
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

		@ColorInt
		protected int getColor(@ColorRes int resId) {
			return ColorUtilities.getColor(getContext(), resId);
		}
	}

	private class FavoritesFilter extends Filter {

		private final FavouritesHelper helper;

		private FavoritesFilter() {
			helper = app.getFavoritesHelper();
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null || constraint.length() == 0) {
				results.values = null;
				results.count = 1;
			} else {
				Set<Object> filter = new HashSet<>();
				String query = constraint.toString().toLowerCase().trim();
				NameStringMatcher matcher = new NameStringMatcher(query, CHECK_CONTAINS);

				for (FavoriteGroup group : helper.getFavoriteGroups()) {
					String groupName = group.getDisplayName(app);
					if (matcher.matches(groupName)) {
						filter.add(group);
					} else {
						for (FavouritePoint point : group.getPoints()) {
							if (matcher.matches(point.getName()) || matcher.matches(point.getDisplayName(app))) {
								filter.add(point);
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
