package net.osmand.plus.download;

import java.io.File;

import net.osmand.map.RegionCountry;
import net.osmand.plus.ClientContext;
import net.osmand.plus.R;

public class SrtmIndexItem extends IndexItem {
	
	private RegionCountry item;
	public SrtmIndexItem(RegionCountry item) {
		super(fileName(item), "Elevation lines", "",  item.tiles.size()+"", null);
		this.item = item;
		type = DownloadActivityType.SRTM_FILE;
	}
	
	private static String fileName(RegionCountry r) {
		if(r.parent == null) {
			return r.continentName + " " + r.name;
		} else {
			return r.parent.continentName + " " + r.parent.name + " " + r.name;
		}
	}

	@Override
	public boolean isAccepted() {
		return true;
	}
	
	@Override
	public DownloadEntry createDownloadEntry(ClientContext ctx, DownloadActivityType type) {
		File parent = ctx.getAppDir();
		final DownloadEntry entry;
		if (parent == null || !parent.exists()) {
			ctx.showToastMessage(R.string.sd_dir_not_accessible);
			entry = null;
		} else {
			entry = new DownloadEntry();
			entry.type = type;
			entry.baseName = getBasename();
//			entry.fileToSave = new File(parent, entry.baseName + toSavePostfix);
//			entry.unzip = unzipDir;
			entry.dateModified = System.currentTimeMillis();
			entry.parts = Integer.parseInt(size);
//			entry.fileToUnzip = new File(parent, entry.baseName + toCheckPostfix);
		}
		return entry;
	}
	
	@Override
	public String convertServerFileNameToLocal() {
		return fileName+".nonexistent";
	}
	
	@Override
	public String getBasename() {
		return fileName;
	}
	
	@Override
	public String getSizeDescription() {
		return size + " parts";
	}
	
	@Override
	public String getVisibleName() {
		if(item.parent == null) {
			return item.name + "\n";
		} else {
			return item.parent.name +"\n"+item.name;
		}
	}
}
