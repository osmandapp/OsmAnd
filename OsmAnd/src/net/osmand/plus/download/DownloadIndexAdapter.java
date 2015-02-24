package net.osmand.plus.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class DownloadIndexAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

	private DownloadIndexFilter myFilter;
	private List<IndexItem> indexFiles = new ArrayList<IndexItem>();
	private List<IndexItemCategory> list = new ArrayList<IndexItemCategory>();
	private DownloadIndexFragment downloadFragment;

	private Map<String, String> indexFileNames = null;
	private Map<String, String> indexActivatedFileNames = null;
	private OsmandRegions osmandRegions;
	private java.text.DateFormat format;
	private OsmandApplication app;

	public DownloadIndexAdapter(DownloadIndexFragment downloadFragment, List<IndexItem> indexFiles) {
		this.downloadFragment = downloadFragment;
		
		this.indexFiles = new ArrayList<IndexItem>(indexFiles);
		app = downloadFragment.getMyApplication();
		list = new ArrayList<IndexItemCategory>(IndexItemCategory.categorizeIndexItems(app, indexFiles));
		format = downloadFragment.getMyApplication().getResourceManager().getDateFormat();
		TypedArray ta = downloadFragment.getDownloadActivity().getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
		ta.recycle();
		osmandRegions = downloadFragment.getMyApplication().getResourceManager().getOsmandRegions();
	}

	public void setLoadedFiles(Map<String, String> indexActivatedFileNames, Map<String, String> indexFileNames) {
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
		notifyDataSetInvalidated();
	}

	public void collapseTrees(final CharSequence constraint) {
		if (downloadFragment == null || downloadFragment.getDownloadActivity() == null) {
			return;
		}
		downloadFragment.getDownloadActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
					final ExpandableListView expandableListView = downloadFragment.getExpandableListView();
					for (int i = 0; i < getGroupCount(); i++) {
						int cp = getChildrenCount(i);
						if (cp < 7 && i == 0) {
							expandableListView.expandGroup(i);
						} else {
							expandableListView.collapseGroup(i);
						}
					}
			}
		});

	}

	public List<IndexItem> getIndexFiles() {
		return indexFiles;
	}
	
	
	public void setIndexFiles(List<IndexItem> indexFiles, Collection<? extends IndexItemCategory> cats) {
		this.indexFiles = new ArrayList<IndexItem>(indexFiles);
		list = new ArrayList<IndexItemCategory>(cats);
		notifyDataSetChanged();
	}
	
	@Override
	public Filter getFilter() {
		if (myFilter == null) {
			myFilter = new DownloadIndexFilter();
		}
		return myFilter;
	}

	private final class DownloadIndexFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null || constraint.length() == 0) {
				results.values = indexFiles;
				results.count = indexFiles.size();
			} else {
				String[] ors =  constraint.toString().split("\\,");
				List<List<String>> conds = new ArrayList<List<String>>();
				for(String or : ors) {
					final ArrayList<String> cond = new ArrayList<String>();
					for(String term :  or.split("\\s")) {
						final String t = term.trim().toLowerCase();
						if(t.length() > 0) {
							cond.add(t);
						}
					}
					if(cond.size() > 0) {
						conds.add(cond);
					}
				}
				List<IndexItem> filter = new ArrayList<IndexItem>();
				Context c = downloadFragment.getDownloadActivity();
				for (IndexItem item : indexFiles) {
					boolean add = true;
					String indexLC = osmandRegions.getDownloadNameIndexLowercase(item.getBasename());
					if(indexLC == null) {
						indexLC = item.getVisibleName(c, osmandRegions).toLowerCase();
					}
					for(List<String> or : conds) {
						boolean tadd = true;
						for (String var : or) {
							if (!indexLC.contains(var)) {
								tadd = false;
								break;
							}
						}
						if(!tadd) {
							add = false;
						} else {
							add = true;
							break;
						}

					}
					if (add) {
						filter.add(item);
					}

				}
				results.values = filter;
				results.count = filter.size();
			}
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			List<IndexItemCategory> clist = new ArrayList<IndexItemCategory>();
			Collection<IndexItem> items = (Collection<IndexItem>) results.values;
			if (items != null && !items.isEmpty()) {
				clist.addAll(IndexItemCategory.categorizeIndexItems(app, items));
			} else if (DownloadIndexAdapter.this.indexFiles.isEmpty()) {
				clist.add(new IndexItemCategory(app.getString(R.string.no_index_file_to_download), 1));
			} else {
				clist.add(new IndexItemCategory(app.getString(R.string.select_index_file_to_download), 1));
			}
			list = clist;
			notifyDataSetChanged();
			collapseTrees(constraint);
		}
	}

	@Override
	public int getGroupCount() {
		return list.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return list.get(groupPosition).items.size();
	}

	@Override
	public IndexItemCategory getGroup(int groupPosition) {
		return list.get(groupPosition);
	}

	@Override
	public IndexItem getChild(int groupPosition, int childPosition) {
		return list.get(groupPosition).items.get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition + (childPosition + 1) * 10000;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View v = convertView;
		IndexItemCategory group = getGroup(groupPosition);
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) downloadFragment.getDownloadActivity().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			v = inflater.inflate(net.osmand.plus.R.layout.expandable_list_item_category, parent, false);
		}
		final View row = v;
		TextView item = (TextView) row.findViewById(R.id.category_name);
		item.setText(group.name);
		item.setLinkTextColor(Color.YELLOW);
		adjustIndicator(groupPosition, isExpanded, v, app.getSettings().isLightContent());
		return row;
	}

	@Override
	public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) downloadFragment.getDownloadActivity().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			v = inflater.inflate(net.osmand.plus.R.layout.download_index_list_item, parent, false);
		}
		final View row = v;
		TextView name = (TextView) row.findViewById(R.id.name);
		TextView update = (TextView) row.findViewById(R.id.update_descr);
		update.setText("");
		TextView uptodate = (TextView) row.findViewById(R.id.uptodate_descr);
		uptodate.setText("");
		TextView description = (TextView) row.findViewById(R.id.download_descr);
		IndexItem e = (IndexItem) getChild(groupPosition, childPosition);
		OsmandApplication clctx = downloadFragment.getMyApplication();
		String eName = e.getVisibleName(clctx, osmandRegions);
		name.setText(eName.trim()); //$NON-NLS-1$
		String d = e.getDate(format) + " " + e.getSizeDescription(clctx);
		description.setText(d.trim());

		CheckBox ch = (CheckBox) row.findViewById(R.id.check_download_item);
		ch.setChecked(downloadFragment.getDownloadActivity().getEntriesToDownload().containsKey(e));
		ch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
				ch.setChecked(!ch.isChecked());
				downloadFragment.onChildClick(downloadFragment.getExpandableListView(), row, groupPosition, childPosition, getChildId(groupPosition, childPosition));
			}
		});

		if (indexFileNames != null && e.isAlreadyDownloaded(indexFileNames)) {
			if (e.getType() == DownloadActivityType.HILLSHADE_FILE
					|| e.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
				String sfName = e.getTargetFileName();
				if (indexActivatedFileNames.containsKey(sfName)) {
					name.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					// next case since present hillshade files cannot be deactivated, but are not in
					// indexActivatedFileNames
				} else if (e.getType() == DownloadActivityType.HILLSHADE_FILE) {
					name.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				} else {
					name.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
				}
			} else  {
				String sfName = e.getTargetFileName();
				final boolean updatableResource = indexActivatedFileNames.containsKey(sfName);
				String date = updatableResource ? indexActivatedFileNames.get(sfName) : indexFileNames.get(sfName);
				boolean outdated = DownloadActivity.downloadListIndexThread.checkIfItemOutdated(e);
				String updateDescr = downloadFragment.getResources().getString(R.string.local_index_installed) + ": "
						+ date;
				uptodate.setText(updateDescr);
				update.setText(updateDescr);
				uptodate.setVisibility(!outdated ? View.VISIBLE : View.GONE);
				update.setVisibility(!outdated ? View.GONE : View.VISIBLE);
			}
		}
		return row;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}