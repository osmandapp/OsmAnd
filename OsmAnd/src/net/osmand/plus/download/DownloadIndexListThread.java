package net.osmand.plus.download;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.access.AccessibleToast;
import net.osmand.map.RegionCountry;
import net.osmand.map.RegionRegistry;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

public class DownloadIndexListThread extends Thread {
	private DownloadIndexActivity uiActivity = null;
	private IndexFileList indexFiles = null;
	private List<SrtmIndexItem> cachedSRTMFiles = new ArrayList<SrtmIndexItem>();
	private final Context ctx;

	public DownloadIndexListThread(Context ctx) {
		super("DownloadIndexes");
		this.ctx = ctx;

	}

	public void setUiActivity(DownloadIndexActivity uiActivity) {
		this.uiActivity = uiActivity;
	}

	public List<IndexItem> getCachedIndexFiles() {
		return indexFiles != null ? indexFiles.getIndexFiles() : null;
	}
	
	public List<SrtmIndexItem> getCachedSRTMFiles() {
		return cachedSRTMFiles;
	}

	public boolean isDownloadedFromInternet() {
		return indexFiles != null && indexFiles.isDownloadedFromInternet();
	}

	@Override
	public void run() {
		indexFiles = DownloadOsmandIndexesHelper.getIndexesList(ctx);
		if(OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null){
			Map<String, String> indexFileNames = new LinkedHashMap<String, String>();
			List<RegionCountry> countries = RegionRegistry.getRegionRegistry().getCountries();
			for(RegionCountry rc : countries){
				if(rc.tiles.size() > 35){
					for(RegionCountry ch : rc.getSubRegions()) {
						cachedSRTMFiles.add(new SrtmIndexItem(ch, indexFileNames));
					}
				} else {
					cachedSRTMFiles.add(new SrtmIndexItem(rc, indexFileNames));
				}
			}
		}
		if (uiActivity != null) {
			uiActivity.removeDialog(DownloadIndexActivity.DIALOG_PROGRESS_LIST);
			uiActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (indexFiles != null) {
						boolean basemapExists = uiActivity.getMyApplication().getResourceManager().containsBasemap();
						IndexItem basemap = indexFiles.getBasemap();
						if (!basemapExists && basemap != null) {
							List<DownloadEntry> downloadEntry = basemap
									.createDownloadEntry(uiActivity.getClientContext(), uiActivity.getType(), new ArrayList<DownloadEntry>());
							uiActivity.getEntriesToDownload().put(basemap, downloadEntry);
							AccessibleToast.makeText(uiActivity, R.string.basemap_was_selected_to_download, Toast.LENGTH_LONG).show();
							uiActivity.findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
						}
						uiActivity.setListAdapter(new DownloadIndexAdapter(uiActivity, uiActivity.getFilteredByType()));
						if (indexFiles.isIncreasedMapVersion()) {
							uiActivity.showDialog(DownloadIndexActivity.DIALOG_MAP_VERSION_UPDATE);
						}
					} else {
						AccessibleToast.makeText(uiActivity, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
					}
				}
			});
		}
	}
}