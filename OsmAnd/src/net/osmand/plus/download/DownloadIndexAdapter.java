package net.osmand.plus.download;

import static net.osmand.data.IndexConstants.EXTRA_EXT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.osmand.data.IndexConstants;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
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
	private DownloadIndexActivity downloadActivity;

	private Map<String, String> indexFileNames = null;
	private Map<String, String> indexActivatedFileNames = null;

	public DownloadIndexAdapter(DownloadIndexActivity downloadActivity, List<IndexItem> indexFiles) {
		this.downloadActivity = downloadActivity;
		this.indexFiles = new ArrayList<IndexItem>(indexFiles);
		List<IndexItemCategory> cats = IndexItemCategory.categorizeIndexItems(downloadActivity.getMyApplication(), indexFiles);
		synchronized (this) {
			list.clear();
			list.addAll(cats);
		}
		updateLoadedFiles();
	}

	public void updateLoadedFiles() {
		indexActivatedFileNames = getMyApplication().getResourceManager().getIndexFileNames();
		DownloadIndexActivity.listWithAlternatives(getMyApplication().getAppPath(""),
				EXTRA_EXT, indexActivatedFileNames);
		indexFileNames = getMyApplication().getResourceManager().getIndexFileNames();
		DownloadIndexActivity.listWithAlternatives(getMyApplication().getAppPath(""),
				EXTRA_EXT, indexFileNames);
		getMyApplication().getResourceManager().getBackupIndexes(indexFileNames);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) downloadActivity.getApplication();
	}

	public void collapseTrees(final CharSequence constraint) {
		downloadActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				synchronized (DownloadIndexAdapter.this) {
					final ExpandableListView expandableListView = downloadActivity.getExpandableListView();
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
	
	
	public synchronized void setIndexFiles(List<IndexItem> indexFiles, Collection<? extends IndexItemCategory> cats) {
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
				String[] vars = constraint.toString().split("\\s");
				for (int i = 0; i < vars.length; i++) {
					vars[i] = vars[i].trim().toLowerCase();
				}
				List<IndexItem> filter = new ArrayList<IndexItem>();
				ClientContext c = downloadActivity.getMyApplication();
				for (IndexItem item : indexFiles) {
					boolean add = true;
					for (String var : vars) {
						if (var.length() > 0) {
							if (!item.getVisibleName(c).toLowerCase().contains(var) 
									/*&& !item.getDescription().toLowerCase().contains(var)*/) {
								add = false;
							}
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
					list.addAll(IndexItemCategory.categorizeIndexItems(downloadActivity.getMyApplication(), items));
				} else {
					list.add(new IndexItemCategory(downloadActivity.getResources().getString(R.string.select_index_file_to_download), 1));
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
			LayoutInflater inflater = downloadActivity.getLayoutInflater();
			v = inflater.inflate(net.osmand.plus.R.layout.download_index_list_item_category, parent, false);
		}
		final View row = v;
		TextView item = (TextView) row.findViewById(R.id.download_index_category_name);
		item.setText(group.name);
		item.setLinkTextColor(Color.YELLOW);
		adjustIndicator(groupPosition, isExpanded, v);
		return row;
	}

	@Override
	public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater inflater = downloadActivity.getLayoutInflater();
			v = inflater.inflate(net.osmand.plus.R.layout.download_index_list_item, parent, false);
		}
		final View row = v;
		TextView item = (TextView) row.findViewById(R.id.download_item);
		TextView description = (TextView) row.findViewById(R.id.download_descr);
		IndexItem e = (IndexItem) getChild(groupPosition, childPosition);
		ClientContext clctx = downloadActivity.getMyApplication();
		String eName = e.getVisibleDescription(clctx) + "\n" + e.getVisibleName(clctx);
		item.setText(eName.trim()); //$NON-NLS-1$
		String d = e.getDate() + "\n" + e.getSizeDescription(clctx);
		description.setText(d.trim());

		CheckBox ch = (CheckBox) row.findViewById(R.id.check_download_item);
		ch.setChecked(downloadActivity.getEntriesToDownload().containsKey(e));
		ch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
				ch.setChecked(!ch.isChecked());
				downloadActivity.onChildClick(downloadActivity.getListView(), row, groupPosition, childPosition, getChildId(groupPosition, childPosition));
			}
		});

		if (indexFileNames != null) {
			
			if (!e.isAlreadyDownloaded(indexFileNames)) {
				item.setTextColor(downloadActivity.getResources().getColor(R.color.index_unknown));
				item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
			} else {
				if(e.getType() == DownloadActivityType.SRTM_FILE){
					item.setTextColor(downloadActivity.getResources().getColor(R.color.act_index_uptodate)); // GREEN
					item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				} else if (e.getDate() != null) {
					String sfName = e.getTargetFileName();
					if (e.getDate().equals(indexActivatedFileNames.get(sfName))) {
						item.setText(item.getText() + "\n" + downloadActivity.getResources().getString(R.string.local_index_installed) + " : "
								+ indexActivatedFileNames.get(sfName));
						item.setTextColor(downloadActivity.getResources().getColor(R.color.act_index_uptodate)); // GREEN
						item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					} else if (e.getDate().equals(indexFileNames.get(sfName))) {
						item.setText(item.getText() + "\n" + downloadActivity.getResources().getString(R.string.local_index_installed) + " : "
								+ indexFileNames.get(sfName));
						// item.setTextColor(getResources().getColor(R.color.deact_index_uptodate)); //DARK_GREEN
						// Try fix Issue 1482: Use italic instead of dark colors for deactivated maps
						item.setTextColor(downloadActivity.getResources().getColor(R.color.act_index_uptodate));
						item.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
					} else if (indexActivatedFileNames.containsKey(sfName)) {
						item.setText(item.getText() + "\n" + downloadActivity.getResources().getString(R.string.local_index_installed) + " : "
								+ indexActivatedFileNames.get(sfName));
						item.setTextColor(downloadActivity.getResources().getColor(R.color.act_index_updateable)); // LIGHT_BLUE
						item.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					} else {
						item.setText(item.getText() + "\n" + downloadActivity.getResources().getString(R.string.local_index_installed) + " : "
								+ indexFileNames.get(sfName));
						// item.setTextColor(getResources().getColor(R.color.deact_index_updateable)); //DARK_BLUE
						// Try fix Issue 1482: Use italic instead of dark colors for deactivated maps
						item.setTextColor(downloadActivity.getResources().getColor(R.color.act_index_updateable));
						item.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
					}
				} else {
					item.setTextColor(downloadActivity.getResources().getColor(R.color.act_index_uptodate));
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