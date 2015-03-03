package net.osmand.plus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import android.os.Environment;

public class WDebug {
//	private final OsmandApplication ctx;
/*	File file;
	public WDebug(OsmandApplication ctx) {
		File tPath = ctx.getAppPath(IndexConstants.TILES_INDEX_DIR);
		file = new File(tPath, "WDEBUG");
	}
	
	public WDebug() {
		String tPath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "osmand";
		file = new File(tPath, "WDEBUG");
	}*/
	
	public static void log(String str){
		try {
			String tPath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "osmand";
			File file = new File(tPath, "WDEBUG");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,true)));
			writer.write(str);
			writer.write("\n");
			writer.close();
		} catch (Exception e) {
		}

	}
	
	public static void log(String str, Exception e){
		try {
			String tPath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "osmand";
			File file = new File(tPath, "WDEBUG");
			PrintStream ps = new PrintStream(new FileOutputStream(file,true));
			ps.print(str);
			ps.print("\n");
			e.printStackTrace(ps);
			ps.close();
		} catch (Exception e1) {
		}

	}
	
	public static void log(Exception e){
		try {
			String tPath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "osmand";
			File file = new File(tPath, "WDEBUG");
			PrintStream ps = new PrintStream(new FileOutputStream(file,true));
			e.printStackTrace(ps);
			ps.close();
		} catch (Exception e1) {
		}

	}
	
}
