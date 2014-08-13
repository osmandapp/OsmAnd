package net.osmand.plus.sherpafy;

import java.io.File;

import net.osmand.IndexConstants;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.util.Algorithms;
import android.content.Context;

public class TourDownloadType extends DownloadActivityType {
	
	public static final TourDownloadType TOUR = new TourDownloadType(R.string.download_tours, "tour");

	public TourDownloadType(int resource, String... tags) {
		super(resource, tags);
	}
	
	public boolean isAccepted(String fileName) {
		return true;
	}
	
	public File getDownloadFolder(OsmandApplication ctx, IndexItem indexItem) {
		return ctx.getAppPath(IndexConstants.TOURS_INDEX_DIR);
	}
	
	public boolean isZipStream(OsmandApplication ctx, IndexItem indexItem) {
		return true;
	}
	
	public boolean isZipFolder(OsmandApplication ctx, IndexItem indexItem) {
		return true;
	}
	
	public boolean preventMediaIndexing(OsmandApplication ctx, IndexItem indexItem) {
		return true;
	}
	
	public String getUnzipExtension(OsmandApplication ctx, IndexItem indexItem) {
		return "";
	}
	
	public String getBaseUrl(OsmandApplication ctx, String fileName) {
		return "http://" + SherpafyCustomization.TOUR_SERVER + "/download_tour.php?event=2&"
				+ Version.getVersionAsURLParam(ctx) + "&file=" + fileName;
	}
	
	public String getUrlSuffix(OsmandApplication ctx) {
		String accessCode = "";
		if (ctx.getAppCustomization() instanceof SherpafyCustomization) {
			accessCode = ((SherpafyCustomization) ctx.getAppCustomization()).getAccessCode();
		}
		return "&tour=yes" + (Algorithms.isEmpty(accessCode) ? "" : "&code=" + accessCode);
	}

	public String getVisibleDescription(IndexItem indexItem, Context ctx) {
		return "";
	}
	
	@Override
	public String getBasename(IndexItem indexItem) {
		String fileName = indexItem.getFileName().replace('_', ' ');
		if(fileName.indexOf('.') != -1) {
			return fileName.substring(0, fileName.indexOf('.'));
		}
		return fileName;
	}
	
	public String getVisibleName(IndexItem indexItem, Context ctx, OsmandRegions osmandRegions) {
		return getBasename(indexItem).replace('_', ' ') + "\n" + indexItem.getDescription();
	}
	
	public String getTargetFileName(IndexItem item) {
		return getBasename(item);
	}

}
