package net.osmand.plus.sherpafy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.plus.GPXUtilities.GPXFile;

import org.xmlpull.v1.XmlPullParser;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class TourInformation {
	final String FILE_PREFIX = "@file:";
	
	private String name;
	private File folder;
	private String shortDescription = "";
	private String fulldescription = "";
	private Bitmap defaultImg = null;
	private File imgFile;
	private List<StageInformation> stageInformation = new ArrayList<TourInformation.StageInformation>();

	public TourInformation(File f) {
		this.folder = f;
		this.name = f.getName().replace('_', ' ');
	}
	
	
	private static Reader getUTF8Reader(InputStream f) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(f);
		assert bis.markSupported();
		bis.mark(3);
		boolean reset = true;
		byte[] t = new byte[3];
		bis.read(t);
		if (t[0] == ((byte) 0xef) && t[1] == ((byte) 0xbb) && t[2] == ((byte) 0xbf)) {
			reset = false;
		}
		if (reset) {
			bis.reset();
		}
		return new InputStreamReader(bis, "UTF-8");
	}
	
	public void loadFullInformation() throws Exception {
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		final Reader reader = getUTF8Reader(new FileInputStream(new File(folder, "inventory.xml")));
		parser.setInput(reader); //$NON-NLS-1$
		int tok;
		String text = "";
		StageInformation stage = null;
		stageInformation.clear();
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				if(tag.equals("tour")) {
					name = getDefAttribute(parser, "name", name);
				} else if (tag.equals("stage")) {
					String name = getDefAttribute(parser, "name", "");
					stage = new StageInformation();
					stage.name = name;
				}
			} else if (tok == XmlPullParser.TEXT) {
				text = parser.getText();
			} else if (tok == XmlPullParser.END_TAG) {
				String tag = parser.getName();
				if(tag.equals("stage")) {
					stageInformation.add(stage);
					stage = null;
				} else if(stage != null && tag.equals("description")) {
					stage.description = text;
				} else if(stage != null && tag.equals("fullDescription")) {
					stage.fullDescription = text;
				} else if(stage != null && tag.equals("image")) {
					if(text.startsWith(FILE_PREFIX)) {
						stage.imgFile = getFile(text);
					}
				} else if(stage != null && tag.equals("gpx")) {
					if(text.startsWith(FILE_PREFIX)) {
						stage.gpxFile = getFile(text);
					}
				} else if(tag.equals("fullDescription")) {
					fulldescription = text;
				} else if(tag.equals("shortDescription")) {
					shortDescription = text;
				} else if(tag.equals("defaultImage")) {
					if(text.startsWith(FILE_PREFIX)) {
						imgFile = getFile(text);
					}
				}
				text = "";
			}
		}
		reader.close();
	}


	private File getFile(String text) {
		return new File(folder, text.substring(FILE_PREFIX.length()));
	}
	
	private String getDefAttribute(XmlPullParser parser, String string, String def) {
		String vl = parser.getAttributeValue("", string);
		if(vl != null && vl.length() > 0) {
			return vl;
		}
		return def;
		
	}


	public String getShortDescription() {
		return shortDescription;
	}
	
	public String getFulldescription() {
		return fulldescription;
	}
	
	public String getName() {
		return name;
	}
	
	public File getFolder() {
		return folder;
	}
	
	public List<StageInformation> getStageInformation() {
		return stageInformation;
	}


	public Bitmap getImageBitmap() {
		if(defaultImg == null && imgFile != null && imgFile.exists()) {
			defaultImg = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
		}
		return defaultImg;
	}
	
	public static class StageInformation {
		
		File gpxFile;
		public GPXFile gpx;
		String name = "";
		String description = "";
		String fullDescription = "";
		Bitmap img = null;
		File imgFile;
		
		public String getName() {
			return name;
		}
		
		public GPXFile getGpx() {
			return gpx;
		}
		
		public String getDescription() {
			return description;
		}
		
		public String getFullDescription() {
			return fullDescription;
		}
		
		public File getGpxFile() {
			return gpxFile;
		}
		

		public Bitmap getImageBitmap() {
			if(img == null && imgFile != null && imgFile.exists()) {
				img = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
			}
			return img;
		}
		
	}

}
