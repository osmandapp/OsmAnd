package net.osmand;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.osmand.LogUtil;

import org.apache.commons.logging.Log;

/**
 * Basic algorithms that are not in jdk 
 */
public class Algoritms {
	private static final int BUFFER_SIZE = 1024;
	private static final Log log = LogUtil.getLog(Algoritms.class);
	
	public static boolean isEmpty(String s){
		return s == null || s.length() == 0;
	}
	
	
	public static String capitalizeFirstLetterAndLowercase(String s) {
		if (s != null && s.length() > 1) {
			// not very efficient algorithm
			return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
		} else {
			return s;
		}
	}
	
	
	public static boolean objectEquals(Object a, Object b){
		if(a == null){
			return b == null;
		} else {
			return a.equals(b);
		}
	}
	
	public static int extractFirstIntegerNumber(String s) {
		int i = 0;
		for (int k = 0; k < s.length(); k++) {
			if (Character.isDigit(s.charAt(k))) {
				i = i * 10 + (s.charAt(k) - '0');
			} else {
				break;
			}
		}
		return i;
	}
	
	
	public static void streamCopy(InputStream in, OutputStream out) throws IOException{
		byte[] b = new byte[BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}
	
	public static void closeStream(Closeable stream){
		try {
			if(stream != null){
				stream.close();
			}
		} catch(IOException e){
			log.warn("Closing stream warn", e); //$NON-NLS-1$
		}
	}
	
	public static void updateAllExistingImgTilesToOsmandFormat(File f){
		if(f.isDirectory()){
			for(File c : f.listFiles()){
				updateAllExistingImgTilesToOsmandFormat(c);
			}
		} else if(f.getName().endsWith(".png") || f.getName().endsWith(".jpg")){ //$NON-NLS-1$ //$NON-NLS-2$
			f.renameTo(new File(f.getAbsolutePath() + ".tile")); //$NON-NLS-1$
		} else if(f.getName().endsWith(".andnav2")) { //$NON-NLS-1$
			f.renameTo(new File(f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - ".andnav2".length()) + ".tile")); //$NON-NLS-1$ //$NON-NLS-2$
		}
			
	}
	
	public static boolean removeAllFiles(File f){
		if(f.isDirectory()){
			boolean deleted = true; 
			for(File c : f.listFiles()){
				deleted &= removeAllFiles(c);
			}
			return f.delete();
		} else {
			return f.delete();
		}
	}
}
