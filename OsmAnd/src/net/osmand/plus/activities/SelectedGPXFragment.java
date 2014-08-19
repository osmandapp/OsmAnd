package net.osmand.plus.activities;

import gnu.trove.list.array.TIntArrayList;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

public class SelectedGPXFragment extends OsmandExpandableListFragment {

	public static final int SEARCH_ID = -1;
//	private SearchView searchView;
	private OsmandApplication app;
	private GpxSelectionHelper selectedGpxHelper;
	private SelectedGPXAdapter adapter;
	private boolean lightContent;
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		app = (OsmandApplication) activity.getApplication();
		selectedGpxHelper = app.getSelectedGpxHelper();
	}
	
	

	@Override
	public void onResume() {
		super.onResume();
		getListView().setFastScrollEnabled(true);
		lightContent = app.getSettings().isLightContent();
		if (adapter == null) {
			adapter = new SelectedGPXAdapter(getListView());
			setAdapter(adapter);
		}
		adapter.setDisplayGroups(selectedGpxHelper.getDisplayGroups());
		selectedGpxHelper.setUiListener(SelectedGPXFragment.class, 
					new Runnable() {
			
			@Override
			public void run() {
				adapter.setDisplayGroups(selectedGpxHelper.getDisplayGroups());				
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		selectedGpxHelper.setUiListener(SelectedGPXFragment.class, null);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View vs = super.onCreateView(inflater, container, savedInstanceState);
		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				long packedPos = ((ExpandableListContextMenuInfo) menuInfo).packedPosition;
				int group = ExpandableListView.getPackedPositionGroup(packedPos);
				int child = ExpandableListView.getPackedPositionChild(packedPos);
				if (child >= 0 && group >= 0) {
					showContextMenu(adapter.getChild(group, child));
				}
			}
		});
		TextView tv = new TextView(getSherlockActivity());
		tv.setText(R.string.none_selected_gpx);
		tv.setTextSize(24);
		//((ViewGroup)getExpandableListView().getParent()).addView(tv); 
		getExpandableListView().setEmptyView(tv);
		return vs;
	}
	
	private void showContextMenu(final GpxDisplayItem gpxDisplayItem) {
		Builder builder = new AlertDialog.Builder(getActivity());
		final ContextMenuAdapter adapter = new ContextMenuAdapter(getActivity());
		basicFileOperation(gpxDisplayItem, adapter);

		String[] values = adapter.getItemNames();
		builder.setItems(values, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OnContextMenuClick clk = adapter.getClickAdapter(which);
				if (clk != null) {
					clk.onContextMenuClick(adapter.getItemId(which), which, false, dialog);
				}
			}

		});
		builder.show();
	}
	
	private void basicFileOperation(final GpxDisplayItem gpxDisplayItem, ContextMenuAdapter adapter) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.show_gpx_route) {
					OsmandSettings settings = getMyApplication().getSettings();
					settings.setMapLocationToShow(gpxDisplayItem.locationStart.lat, gpxDisplayItem.locationStart.lon,
							settings.getLastKnownMapZoom(), Html.fromHtml(gpxDisplayItem.name).toString());
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			}
		};
		if (gpxDisplayItem.locationStart != null) {
			adapter.item(R.string.show_gpx_route).listen(listener).reg();
		}
		OsmandPlugin.onContextMenuActivity(getSherlockActivity(), this, gpxDisplayItem, adapter);
	}


	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
	
	protected void saveAsFavorites(final GpxDisplayGroup model) {
		Builder b = new AlertDialog.Builder(getActivity());
		final EditText editText = new EditText(getActivity());
		String name = model.getName();
		if(name.indexOf('\n') > 0) {
			name = name.substring(0, name.indexOf('\n'));
		}
		editText.setText(name);
		editText.setPadding(7, 3, 7, 3);
		b.setTitle(R.string.save_as_favorites_points);
		b.setView(editText);
		b.setPositiveButton(R.string.default_buttons_save, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				saveFavoritesImpl(model.getModifiableList(), editText.getText().toString());

			}
		});
		b.setNegativeButton(R.string.default_buttons_cancel, null);
		b.show();
	}

	protected void saveFavoritesImpl(List<GpxDisplayItem> modifiableList, String category) {
		FavouritesDbHelper fdb = getMyApplication().getFavorites();
		for(GpxDisplayItem i : modifiableList) {
			if (i.locationStart != null) {
				FavouritePoint fp = new FavouritePoint(i.locationStart.lat, i.locationStart.lon, i.locationStart.name,
						category);
				fdb.addFavourite(fp);
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_light,
//				R.drawable.ic_action_search_dark, MenuItem.SHOW_AS_ACTION_ALWAYS
//						| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
//		searchView = new com.actionbarsherlock.widget.SearchView(getActivity());
//		mi.setActionView(searchView);
//		searchView.setOnQueryTextListener(new OnQueryTextListener() {
//
//			@Override
//			public boolean onQueryTextSubmit(String query) {
//				return true;
//			}
//
//			@Override
//			public boolean onQueryTextChange(String newText) {
//				return true;
//			}
//		});
//		createMenuItem(menu, ACTION_ID, R.string.export_fav, R.drawable.ic_action_gsave_light,
//				R.drawable.ic_action_gsave_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	public void showProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
	}

	private void selectSplitDistance(final GpxDisplayGroup model) {
		Builder bld = new AlertDialog.Builder(getActivity());
		int[] checkedItem = new int[] {!model.isSplitDistance() && !model.isSplitTime()? 0 : -1};
		List<String> options = new ArrayList<String>();
		final List<Double> distanceSplit = new ArrayList<Double>();
		final TIntArrayList timeSplit = new TIntArrayList();
		View view = getActivity().getLayoutInflater().inflate(R.layout.selected_track_edit, null);
		
		options.add(app.getString(R.string.none));
		distanceSplit.add(-1d);
		timeSplit.add(-1);
		addOptionSplit(30, true, options, distanceSplit, timeSplit, checkedItem, model); // 100 feet, 50 yards, 50 m 
		addOptionSplit(60, true, options, distanceSplit, timeSplit, checkedItem, model); // 200 feet, 100 yards, 100 m
		addOptionSplit(150, true, options, distanceSplit, timeSplit, checkedItem, model); // 500 feet, 200 yards, 200 m
		addOptionSplit(300, true, options, distanceSplit, timeSplit, checkedItem, model); // 1000 feet, 500 yards, 500 m
		addOptionSplit(600, true, options, distanceSplit, timeSplit, checkedItem, model); // 2000 feet, 1000 yards, 1km
		addOptionSplit(1500, true, options, distanceSplit, timeSplit, checkedItem, model); // 1mi, 2km
		addOptionSplit(3000, true, options, distanceSplit, timeSplit, checkedItem, model); // 2mi, 5km
		addOptionSplit(8000, true, options, distanceSplit, timeSplit, checkedItem, model); // 5mi, 10km
		
		addOptionSplit(15, false, options, distanceSplit, timeSplit, checkedItem, model);
		addOptionSplit(30, false, options, distanceSplit, timeSplit, checkedItem, model);
		addOptionSplit(60, false, options, distanceSplit, timeSplit, checkedItem, model);
		addOptionSplit(120, false, options, distanceSplit, timeSplit, checkedItem, model);
		addOptionSplit(150, false, options, distanceSplit, timeSplit, checkedItem, model);
		addOptionSplit(300, false, options, distanceSplit, timeSplit, checkedItem, model);
		addOptionSplit(600, false, options, distanceSplit, timeSplit, checkedItem, model);
		addOptionSplit(900, false, options, distanceSplit, timeSplit, checkedItem, model);
		final CheckBox vis = (CheckBox) view.findViewById(R.id.Visibility);
		vis.setChecked(true);
		final Spinner sp = (Spinner) view.findViewById(R.id.Spinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, options);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp.setAdapter(adapter);
		if(checkedItem[0] > 0) {
			sp.setSelection(checkedItem[0]);
		}
		
		bld.setView(view);
		bld.setNegativeButton(R.string.default_buttons_cancel, null); 
		bld.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(!vis.isChecked()) {
					getMyApplication().getSelectedGpxHelper().selectGpxFile(model.getGpx(), false, false);
					SelectedGPXFragment.this.adapter.setDisplayGroups(selectedGpxHelper.getDisplayGroups());
					getMyApplication().getSelectedGpxHelper().runUiListeners();
				} else {
					updateSplit(model, distanceSplit, timeSplit, sp.getSelectedItemPosition() );
				}
				
			}
		});
		
		bld.show();
		
	}
	
	private void updateSplit(final GpxDisplayGroup model, final List<Double> distanceSplit,
			final TIntArrayList timeSplit, final int which) {
		new AsyncTask<Void, Void, Void>() {
			
			protected void onPostExecute(Void result) {
				adapter.notifyDataSetChanged();
				getSherlockActivity().setProgressBarIndeterminateVisibility(false);
			};
			
			protected void onPreExecute() {
				getSherlockActivity().setProgressBarIndeterminateVisibility(true);
			};

			@Override
			protected Void doInBackground(Void... params) {
				if(which == 0) {
					model.noSplit(app);
				} else if(distanceSplit.get(which) > 0) {
					model.splitByDistance(app, distanceSplit.get(which));
				} else if(timeSplit.get(which) > 0) {
					model.splitByTime(app, timeSplit.get(which));
				}
				return null;
			}
		}.execute((Void)null);
	}

	private void addOptionSplit(int value, boolean distance, List<String> options, List<Double> distanceSplit,
			TIntArrayList timeSplit, int[] checkedItem, GpxDisplayGroup model) {
		if (distance) {
			double dvalue = OsmAndFormatter.calculateRoundedDist(value, app);
			options.add(OsmAndFormatter.getFormattedDistance((float) dvalue, app));
			distanceSplit.add(dvalue);
			timeSplit.add(-1);
			if (Math.abs(model.getSplitDistance() - dvalue) < 1) {
				checkedItem[0] = distanceSplit.size() - 1;
			}
		} else {
			if (value < 60) {
				options.add(value + " " + app.getString(R.string.int_seconds));
			} else if (value % 60 == 0) {
				options.add((value / 60) + " " + app.getString(R.string.int_min));
			} else {
				options.add((value / 60f) + " " + app.getString(R.string.int_min));
			}
			distanceSplit.add(-1d);
			timeSplit.add(value);
			if (model.getSplitTime() == value) {
				checkedItem[0] = distanceSplit.size() - 1;
			}
		}
		
	}

	class SelectedGPXAdapter extends OsmandBaseExpandableListAdapter implements SectionIndexer, AbsListView.OnScrollListener {

		Filter myFilter;
		private List<GpxDisplayGroup> displayGroups = new ArrayList<GpxDisplayGroup>();
		private ExpandableListView expandableListView;
		private boolean groupScroll = true;
		private int maxNumberOfSections = 1;
		private double itemsInSection;
		
		public SelectedGPXAdapter(ExpandableListView lv) {
			this.expandableListView = lv;
			this.expandableListView.setOnScrollListener(this);

		}
		
		@Override
	    public void onScrollStateChanged(AbsListView view, int scrollState) {
//	        this.manualScroll = scrollState == SCROLL_STATE_TOUCH_SCROLL;
	    }

	    @Override
	    public void onScroll(AbsListView view, 
	                         int firstVisibleItem, 
	                         int visibleItemCount, 
	                         int totalItemCount) {}

	    @Override
	    public int getPositionForSection(int section) {
	    	if(groupScroll) {
	    		return expandableListView.getFlatListPosition(
	                       ExpandableListView.getPackedPositionForGroup(section));
	    	} else {
	        	return (int) (section * itemsInSection);
	        }
	    }

	    // Gets called when scrolling the list manually
	    @Override
		public int getSectionForPosition(int position) {
			// Get the packed position of the provided flat one and find the corresponding group
			if (groupScroll) {
				return ExpandableListView
						.getPackedPositionGroup(expandableListView.getExpandableListPosition(position));
			} else {
				int m = Math.min(maxNumberOfSections - 1, (int) (position / itemsInSection));
				return m;
			}
		}
	    
		@Override
		public Object[] getSections() {
			String[] ar ;
			if (groupScroll) {
				ar = new String[getGroupCount()];
				for (int i = 0; i < getGroupCount(); i++) {
					ar[i] = (i + 1) +".";
				}
			} else {
				int total = getGroupCount();
				for (int i = 0; i < getGroupCount(); i++) {
					if (expandableListView.isGroupExpanded(i)) {
						total += getChildrenCount(i);
					}
				}
				maxNumberOfSections = Math.max(1, Math.min(25, total));
				itemsInSection = ((double) total) / maxNumberOfSections;
				ar = new String[maxNumberOfSections];
				for (int i = 0; i < ar.length; i++) {
					ar[i] = ((i + 1) * 100 / maxNumberOfSections) + "%";
				}
			}
			return ar;
		}
		
		public void setDisplayGroups(List<GpxDisplayGroup> displayGroups) {
			this.displayGroups = displayGroups;
			notifyDataSetChanged();
		}
		
	



		@Override
		public GpxDisplayItem getChild(int groupPosition, int childPosition) {
			GpxDisplayGroup group = getGroup(groupPosition);
			return group.getModifiableList().get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return getGroup(groupPosition).getModifiableList().size();
		}

		@Override
		public GpxDisplayGroup getGroup(int groupPosition) {
			return displayGroups.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return displayGroups.size();
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
				row = inflater.inflate(R.layout.expandable_list_item_category_btn, parent, false);
				fixBackgroundRepeat(row);
			}
			adjustIndicator(groupPosition, isExpanded, row);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final GpxDisplayGroup model = getGroup(groupPosition);
			label.setText(model.getGroupName());
			final ImageView ch = (ImageView) row.findViewById(R.id.check_item);
			
			if(model.getType() == GpxDisplayItemType.TRACK_SEGMENT) {
				ch.setVisibility(View.VISIBLE);
				ch.setImageDrawable(getActivity().getResources().getDrawable(
						app.getSettings().isLightContent() ? R.drawable.ic_action_settings_light
								: R.drawable.ic_action_settings_dark));
				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						selectSplitDistance(model);
					}

				});
			} else if(model.getType() == GpxDisplayItemType.TRACK_POINTS || 
					model.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
				ch.setVisibility(View.VISIBLE);
				ch.setImageDrawable(getActivity().getResources().getDrawable(
						app.getSettings().isLightContent() ? R.drawable.ic_action_fav_light
								: R.drawable.ic_action_fav_dark));
				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						saveAsFavorites(model);
					}

				});
			} else {
				ch.setVisibility(View.INVISIBLE);
			}
			return row;
		}
		


		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.gpx_item_list_item, parent, false);
			}
			GpxDisplayItem child = getChild(groupPosition, childPosition);
			TextView label = (TextView) row.findViewById(R.id.name);
			TextView description = (TextView) row.findViewById(R.id.description);
			TextView additional = (TextView) row.findViewById(R.id.additional);
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			if(child.splitMetric >= 0 && child.splitName != null) {
				additional.setVisibility(View.VISIBLE);
				icon.setVisibility(View.INVISIBLE);
				additional.setText(child.splitName);
			} else {
				icon.setVisibility(View.VISIBLE);
				additional.setVisibility(View.INVISIBLE);
				if (child.group.getType() == GpxDisplayItemType.TRACK_SEGMENT) {
					icon.setImageResource(!lightContent ? R.drawable.ic_action_polygom_dark
							: R.drawable.ic_action_polygom_light);
				} else if (child.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
					icon.setImageResource(!lightContent ? R.drawable.ic_action_markers_dark
							: R.drawable.ic_action_markers_light);
				} else {
					int groupColor = child.group.getColor();
					if(child.locationStart != null) {
						groupColor = child.locationStart.getColor(groupColor);
					}
					if(groupColor == 0) {
						groupColor = getResources().getColor(R.color.gpx_track);
					}
					icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(),  groupColor));
				}
			}
			row.setTag(child);
				
			label.setText(Html.fromHtml(child.name.replace("\n", "<br/>")));
			if (child.expanded && !Algorithms.isEmpty(child.description)) {
				String d = child.description;
				if (child.group.getType() == GpxDisplayItemType.TRACK_SEGMENT) {
					d += "<br/>" + getString(R.string.local_index_gpx_info_show);
				}
				description.setText(Html.fromHtml(d));
				description.setVisibility(View.VISIBLE);
			} else {
				description.setVisibility(View.GONE);
			}

			return row;
		}

	}


	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		GpxDisplayItem child = adapter.getChild(groupPosition, childPosition);
		if(child.group.getType() == GpxDisplayItemType.TRACK_POINTS ||
				child.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
			ContextMenuAdapter qa = new ContextMenuAdapter(v.getContext());
			qa.setAnchor(v);
			String name = getString(R.string.favorite) + ": " + child.name;
			LatLon location = new LatLon(child.locationStart.lat, child.locationStart.lon);
			OsmandSettings settings = getMyApplication().getSettings();
			MapActivityActions.createDirectionsActions(qa, location, child.locationStart, name, settings.getLastKnownMapZoom(), getActivity(),
					true, false);
			MapActivityActions.showObjectContextMenu(qa, getActivity(), null);
		} else {
			child.expanded = !child.expanded;
			adapter.notifyDataSetInvalidated();
		}
		return true;
	}

	
}
