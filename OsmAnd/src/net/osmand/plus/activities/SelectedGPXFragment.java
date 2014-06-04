package net.osmand.plus.activities;

import java.text.Collator;

import net.osmand.plus.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;

public class SelectedGPXFragment extends OsmandExpandableListFragment {

	public static final int SEARCH_ID = -1;
	public static final int ACTION_ID = 0;
	protected static final int DELETE_ACTION_ID = 1;
	private boolean selectionMode = false;
	private ActionMode actionMode;
	private SearchView searchView;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);

	}

	@Override
	public void onResume() {
		super.onResume();
	}


	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == ACTION_ID) {
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.export_fav, R.drawable.ic_action_search_light,
				R.drawable.ic_action_search_dark, MenuItem.SHOW_AS_ACTION_ALWAYS
						| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		searchView = new com.actionbarsherlock.widget.SearchView(getActivity());
		mi.setActionView(searchView);
		searchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				return true;
			}
		});
		createMenuItem(menu, ACTION_ID, R.string.export_fav, R.drawable.ic_action_gsave_light,
				R.drawable.ic_action_gsave_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	public void showProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
	}

	private void enterDeleteMode() {
		actionMode = getSherlockActivity().startActionMode(new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				createMenuItem(menu, DELETE_ACTION_ID, R.string.default_buttons_delete,
						R.drawable.ic_action_delete_light, R.drawable.ic_action_delete_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				selectionMode = false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (item.getItemId() == DELETE_ACTION_ID) {
					// TODO delete
				}
				return true;
			}

		});

	}

	

	

	

	

	class SelectedGPXAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		Filter myFilter;


		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return 0;
		}

		@Override
		public String getGroup(int groupPosition) {
			return "";
		}

		@Override
		public int getGroupCount() {
			return 0;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.expandable_list_item_category, parent, false);
				fixBackgroundRepeat(row);
			}
			adjustIndicator(groupPosition, isExpanded, row);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
//			final String model = getGroup(groupPosition);
//			label.setText(model);
//
//			if (selectionMode) {
//				ch.setVisibility(View.VISIBLE);
//				ch.setChecked(groupsToDelete.contains(model));
//
//				ch.setOnClickListener(new View.OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						if (ch.isChecked()) {
//							groupsToDelete.add(model);
//							List<FavouritePoint> fvs = helper.getFavoriteGroups().get(model);
//							if (fvs != null) {
//								favoritesToDelete.addAll(fvs);
//							}
//							favouritesAdapter.notifyDataSetInvalidated();
//						} else {
//							groupsToDelete.remove(model);
//						}
//					}
//				});
//			} else {
//				ch.setVisibility(View.GONE);
//			}
			return row;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.favourites_list_item, parent, false);
			}

//			TextView label = (TextView) row.findViewById(R.id.favourite_label);
//			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
//			final FavouritePoint model = (FavouritePoint) getChild(groupPosition, childPosition);
//			row.setTag(model);
//			if (model.isStored()) {
//				icon.setImageResource(R.drawable.list_favorite);
//			} else {
//				icon.setImageResource(R.drawable.opened_poi);
//			}
//			LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
//			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(),
//					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
//			String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
//			label.setText(distance + model.getName(), TextView.BufferType.SPANNABLE);
//			((Spannable) label.getText()).setSpan(
//					new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0, distance.length() - 1,
//					0);
//			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
//			if (selectionMode && model.isStored()) {
//				ch.setVisibility(View.VISIBLE);
//				ch.setChecked(favoritesToDelete.contains(model));
//				row.findViewById(R.id.favourite_icon).setVisibility(View.GONE);
//				ch.setOnClickListener(new View.OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						if (ch.isChecked()) {
//							favoritesToDelete.add(model);
//						} else {
//							favoritesToDelete.remove(model);
//							if (groupsToDelete.contains(model.getCategory())) {
//								groupsToDelete.remove(model.getCategory());
//								favouritesAdapter.notifyDataSetInvalidated();
//							}
//						}
//					}
//				});
//			} else {
//				row.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
//				ch.setVisibility(View.GONE);
//			}
			return row;
		}

		@Override
		public Filter getFilter() {
			if (myFilter == null) {
				myFilter = new SearchFilter();
			}
			return myFilter;
		}
	}

	private class SearchFilter extends Filter {


		public SearchFilter() {
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
//			if (constraint == null || constraint.length() == 0) {
//				results.values = helper.getFavoriteGroups();
//				results.count = 1;
//			} else {
//				TreeMap<String, List<FavouritePoint>> filter = new TreeMap<String, List<FavouritePoint>>(helper.getFavoriteGroups());
//				TreeMap<String, List<FavouritePoint>> filterLists = new TreeMap<String, List<FavouritePoint>>();
//				String cs = constraint.toString().toLowerCase();
//				Iterator<Entry<String, List<FavouritePoint>>> ti = filter.entrySet().iterator();
//				while(ti.hasNext()) {
//					Entry<String, List<FavouritePoint>> next = ti.next();
//					if(next.getKey().toLowerCase().indexOf(cs) == -1) {
//						ti.remove();
//						filterLists.put(next.getKey(), next.getValue());
//					}
//				}
//				ti = filterLists.entrySet().iterator();
//				while(ti.hasNext()) {
//					Entry<String, List<FavouritePoint>> next = ti.next();
//					final List<FavouritePoint> list = next.getValue();
//					LinkedList<FavouritePoint> ll = new LinkedList<FavouritePoint>();
//					for(FavouritePoint l : list) {
//						if(l.getName().toLowerCase().indexOf(cs) != -1) {
//							ll.add(l);
//						}
//							
//					}
//					if(ll.size() > 0) {
//						filter.put(next.getKey(), ll);
//					}
//				}
//				results.values = filter;
//				results.count = filter.size();
//			}
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
//			synchronized (adapter) {
//				favouritesAdapter.setFavoriteGroups((Map<String, List<FavouritePoint>>) results.values);
//			}
//			favouritesAdapter.notifyDataSetChanged();
//			if(constraint != null && constraint.length() > 1) {
//				collapseTrees();
//			}
		}

	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		return false;
	}

}
