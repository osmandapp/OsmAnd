package net.osmand.plus.base;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;

public class SuggestExternalDirectoryDialog {
	
	
	public static boolean showDialog(Activity a, final DialogInterface.OnClickListener otherListener,
			final CallbackWithObject<String> selector){
		final boolean showOther = otherListener != null;
		final OsmandApplication app = (OsmandApplication) a.getApplication();
		Builder bld = new AlertDialog.Builder(a);
		HashSet<String> externalMounts;
		if(Build.VERSION.SDK_INT < OsmandSettings.VERSION_DEFAULTLOCATION_CHANGED) {
			externalMounts = getExternalMounts();
		} else {
			externalMounts = new HashSet<String>(app.getSettings().getWritableSecondaryStorageDirectorys());
		}
		String apath = app.getSettings().getExternalStorageDirectory().getAbsolutePath();
		externalMounts.add(app.getSettings().getDefaultExternalStorageLocation());
		externalMounts.add(apath);
		final String[] extMounts = new String[showOther ?  externalMounts.size()+1 : externalMounts.size()];
		externalMounts.toArray(extMounts);
		if (showOther) {
			extMounts[extMounts.length - 1] = a.getString(R.string.other_location);
		}
		if (extMounts.length > 1) {
			int checkedItem = 0;
			for (int j = 0; j < extMounts.length; j++) {
				if (extMounts[j].equals(apath)) {
					checkedItem = j;
					break;
				}
			}
			bld.setTitle(R.string.application_dir);
			bld.setSingleChoiceItems(extMounts, checkedItem, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(showOther && which == extMounts.length -1) {
						otherListener.onClick(dialog, which);
					} else {
						dialog.dismiss();
						if(selector != null) {
							selector.processResult(extMounts[which]);	
						} else {
							app.getSettings().setExternalStorageDirectory(extMounts[which]);
							app.getResourceManager().resetStoreDirectory();
						}
					}
				}
			});
			bld.setPositiveButton(R.string.default_buttons_ok, null);
			bld.show();
			return true;
		} 
		return false;
	}
	
	public static HashSet<String> getExternalMounts() {
	    final HashSet<String> out = new HashSet<String>();
	    String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
	    String s = "";
	    try {
	        final Process process = new ProcessBuilder().command("mount")
	                .redirectErrorStream(true).start();
	        process.waitFor();
	        final InputStream is = process.getInputStream();
	        final byte[] buffer = new byte[1024];
	        while (is.read(buffer) != -1) {
	            s = s + new String(buffer);
	        }
	        is.close();
	    } catch (final Exception e) {
	        e.printStackTrace();
	    }

	    // parse output
	    final String[] lines = s.split("\n");
	    for (String line : lines) {
	        if (!line.toLowerCase(Locale.US).contains("asec")) {
	            if (line.matches(reg)) {
	                String[] parts = line.split(" ");
	                for (String part : parts) {
	                    if (part.startsWith("/"))
	                        if (!part.toLowerCase(Locale.US).contains("vold"))
	                            out.add(part);
	                }
	            }
	        }
	    }
	    return out;
	}

}
