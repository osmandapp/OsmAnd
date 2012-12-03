package net.osmand.plus.download;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.osmand.LogUtil;
import net.osmand.plus.ClientContext;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

public class SrtmIndexItem extends IndexItem {
	private static final Log log = LogUtil.getLog(SrtmIndexItem.class);

	public SrtmIndexItem(String fileName, String description, String date, String size) {
		super(fileName, description, date, size, null);
		type = DownloadActivityType.SRTM_FILE;
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
			SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
			try {
				Date d = format.parse(date);
				entry.dateModified = d.getTime();
			} catch (ParseException e1) {
				log.error("ParseException", e1);
			}
			try {
				entry.sizeMB = Double.parseDouble(size);
			} catch (NumberFormatException e1) {
				log.error("ParseException", e1);
			}
			entry.parts = 1;
			if (parts != null) {
				entry.parts = Integer.parseInt(parts);
			}
//			entry.fileToUnzip = new File(parent, entry.baseName + toCheckPostfix);
		}
		return entry;
	}
}
