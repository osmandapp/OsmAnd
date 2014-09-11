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
	private final List<IndexItem> indexFiles;
	private final List<IndexItemCategory> list = new ArrayList<IndexItemCategory>();
	private DownloadIndexFragment downloadFragment;

	private Map<String, String> indexFileNames = null;
	private Map<String, String> indexActivatedFileNames = null;
	private int okColor;
	private int defaultColor;
	private int updateColor;
	private OsmandRegions osmandRegions;
	private java.text.DateFormat format;

	public DownloadIndexAdapter(DownloadIndexFragment downloadFragment, List<IndexItem> indexFiles) {
		this.downloadFragment = downloadFragment;
		this.indexFiles = new ArrayList<IndexItem>(indexFiles);
		List<IndexItemCategory> cats = IndexItemCategory.categorizeIndexItems(downloadFragment.getMyApplication(), indexFiles);
		synchronized (this) {
			list.clear();
			list.addAll(cats);
		}
		format = downloadFragment.getMyApplication().getResourceManager().getDateFormat();
		okColor = downloadFragment.getResources().getColor(R.color.color_ok);
		TypedArray ta = downloadFragment.getDownloadActivity().getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
		defaultColor = ta.getColor(0, downloadFragment.getResources().getColor(R.color.color_unknown));
		ta.recycle();
		updateColor = downloadFragment.getResources().getColor(R.color.color_update);
		osmandRegions = downloadFragment.getMyApplication().getResourceManager().getOsmandRegions();
	}

	public void setLoadedFiles(Map<String, String> indexActivatedFileNames, Map<String, String> indexFileNames) {
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
		notifyDataSetInvalidated();
	}

	public void collapseTrees(final CharSequence constraint) {
		downloadFragment.getDownloadActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				synchronized (DownloadIndexAdapter.this) {
					final ExpandableListView expandableListView = downloadFragment.getExpandableListView();
					for (int i = 0; i < getGroupCount(); i++) {
						int cp = getChildrenCount(i);
						if (cp < 7) {
							expandableListView.expandGroup(i);
						} else {
							expandableListView.collapseGroup(i);
						}
					}
				}
			}
		});

	}

	public List<IndexItem> getIndexFiles() {
		return indexFiles;
	}
	
	
	public void setIndexFiles(List<IndexItem> indexFiles, Collection<? extends IndexItemCategory> cats) {
		this.indexFiles.clear();
		this.indexFiles.addAll(indexFiles);
		list.clear();
		list.addAll(cats);
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
			synchronized (DownloadIndexAdapter.this) {
				list.clear();
				Collection<IndexItem> items = (Collection<IndexItem>) results.values;
				if (items != null && !items.isEmpty()) {
					list.addAll(IndexItemCategory.categorizeIndexItems(downloadFragment.getMyApplication(), items));
				} else if(DownloadIndexAdapter.this.indexFiles.isEmpty()){
					list.add(new IndexItemCategory(downloadFragment.getResources().getString(R.string.no_index_file_to_download), 1));
				} else {
					list.add(new IndexItemCategory(downloadFragment.getResources().getString(R.string.select_index_file_to_download), 1));
				}
			}
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
		adjustIndicator(groupPosition, isExpanded, v);
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
		TextView item = (TextView) row.findViewById(R.id.download_item);
		TextView description = (TextView) row.findViewById(R.id.download_descr);
		IndexItem e = (IndexItem) getChild(groupPosition, childPosition);
		OsmandApplication clctx = downloadFragment.getMyApplication();
		String eName = e.getVisibleDescription(clctx) + "\n" + e.getVisibleName(clctx, osmandRegions);
		item.setText(eName.trim()); //$NON-NLS-1$
		String d = e.getDate(format) + "\n" + e.getSizeDescription(clctx);
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

		if (indexFileNames != null) {
			
			if (!e.isAlreadyDownloaded(indexFileNames)) {
				item.setTextColor(defaultColor);
				item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
			} else {
				if(e.getType() == DownloadActivityType.HILLSHADE_FILE
						|| e.getType() == DownloadActivityType.SRTM_COUNTRY_FILE){
					item.setTextColor(okColor); // GREEN
					String sfName = e.getTargetFileName();
					if (indexActivatedFileNames.containsKey(sfName)) {
						item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					// next case since present hillshade files cannot be deactivated, but are not in indexActivatedFileNames
					} else if (e.getType() == DownloadActivityType.HILLSHADE_FILE) {
						item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					} else {
						item.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
					}
				} else if (e.getDate(format) != null) {
					String sfName = e.getTargetFileName();
					if (e.getDate(format).equals(indexActivatedFileNames.get(sfName))) {
						item.setText(item.getText() + "\n" + downloadFragment.getResources().getString(R.string.local_index_installed) + " : "
								+ indexActivatedFileNames.get(sfName));
						item.setTextColor(okColor); // GREEN
						item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					} else if (e.getDate(format).equals(indexFileNames.get(sfName))) {
						item.setText(item.getText() + "\n" + downloadFragment.getResources().getString(R.string.local_index_installed) + " : "
								+ indexFileNames.get(sfName));
						item.setTextColor(okColor);
						item.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
					} else if (indexActivatedFileNames.containsKey(sfName)) {
						item.setText(item.getText() + "\n" + downloadFragment.getResources().getString(R.string.local_index_installed) + " : "
								+ indexActivatedFileNames.get(sfName));
						item.setTextColor(updateColor); // LIGHT_BLUE
						item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					} else {
						item.setText(item.getText() + "\n" + downloadFragment.getResources().getString(R.string.local_index_installed) + " : "
								+ indexFileNames.get(sfName));
						item.setTextColor(updateColor);
						item.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
					}
				} else {
					item.setTextColor(okColor);
					item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				}
			}
		}
		return row;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}