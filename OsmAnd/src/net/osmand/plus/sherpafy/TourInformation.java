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
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public class TourInformation {
	final String FILE_PREFIX = "@file:";
	
	private String name;
	private File folder;
	private String homeUrl = "";
	private String shortDescription = "";
	private String fulldescription = "";
	private String instructions = "";
	private Bitmap defaultImg = null;
	private File imgFile;
	private List<StageInformation> stageInformation = new ArrayList<TourInformation.StageInformation>();


	public TourInformation(File f) {
		this.folder = f;
		this.name = f.getName().replace('_', ' ');
	}
	
	public String getId() {
		return folder.getName();
	}
	
	public String getInstructions() {
		return instructions;
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
					homeUrl = getDefAttribute(parser, "url", "");
				} else if (tag.equals("stage")) {
					String name = getDefAttribute(parser, "name", "");
					stage = new StageInformation(this, stageInformation.size());
					stage.name = name;
				} else if (tag.equals("itinerary") && stage != null){
					String img = getDefAttribute(parser, "image", "");
					if(img.startsWith(FILE_PREFIX)) {
						stage.itineraryFile = getFile(img);
					}
					stage.itinerary = getInnerXml(parser);
				} else if (tag.equals("fullDescription")){
					fulldescription = getInnerXml(parser);
				} else if (tag.equals("instructions")){
					instructions = getInnerXml(parser);
				} else if (stage != null && tag.equals("interval")){
					stage.distance = Double.parseDouble(getDefAttribute(parser, "distance", "0"));
				} else if (stage != null && tag.equals("description")){
					stage.fullDescription = getInnerXml(parser);
				}
			} else if (tok == XmlPullParser.TEXT) {
				text = parser.getText();
			} else if (tok == XmlPullParser.END_TAG) {
				String tag = parser.getName();
				if(tag.equals("stage")) {
					stageInformation.add(stage);
					stage = null;
				} else if(stage != null && tag.equals("fullDescription")) {
					stage.fullDescription = text;
				} else if(stage != null && tag.equals("defaultImage")) {
					if(text.startsWith(FILE_PREFIX)) {
						stage.imgFile = getFile(text);
					}
				} else if(tag.equals("defaultImage")) {
					if(text.startsWith(FILE_PREFIX)) {
						imgFile = getFile(text);
					}
				} else if(stage != null && tag.equals("gpx")) {
					if(text.startsWith(FILE_PREFIX)) {
						stage.gpxFile = getFile(text);
					}
				} else if(tag.equals("shortDescription")) {
					if(stage != null) {
						stage.shortDescription = text;
					} else {
						shortDescription = text;
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

	//returns image bitmap from selected relative path
	public Bitmap getImageBitmapFromPath(String path){
		File imgFile = getFile(path);
		if (imgFile != null){
			Options opts = new Options();
//			if(imgFile.length() > 100 * 1024) {
//				opts.inSampleSize = 4;
//			}
			return BitmapFactory.decodeFile(imgFile.getAbsolutePath(), opts);
			//return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
		}
		return null;
	}
	
	
	public static class StageInformation {
		
		String itinerary = "";
		File gpxFile;
		public GPXFile gpx;
		String name = "";
		String shortDescription = "";
		String fullDescription = "";
		Bitmap img;
		File imgFile;
		private Bitmap itineraryImg;
		File itineraryFile;
		double distance;
		private TourInformation tour;
		private int order;
		
		public String getItinerary() {
			return itinerary;
		}
		
		public TourInformation getTour() {
			return tour;
		}
		
		public int getOrder() {
			return order;
		}
		
		public StageInformation(TourInformation tour, int order) {
			this.tour = tour;
			this.order = order;
		}
		
		public String getName() {
			return name;
		}
		
		public GPXFile getGpx() {
			return gpx;
		}
		
		public String getShortDescription() {
			return shortDescription;
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
		
		public Bitmap getItineraryBitmap() {
			if(itineraryImg == null && itineraryFile != null && itineraryFile.exists()) {
				itineraryImg = BitmapFactory.decodeFile(itineraryFile.getAbsolutePath());
			}
			return itineraryImg;
		}
		
		@Override
		public String toString() {
			return name;
		}

		public double getDistance() {
			return distance;
		}
		
	}

	//Returns full string from which contains XML tags from XMLParser
	public static String getInnerXml(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		StringBuilder sb = new StringBuilder();
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					if (depth > 0) {
						sb.append("</" + parser.getName() + ">");
					}
					break;
				case XmlPullParser.START_TAG:
					depth++;
					StringBuilder attrs = new StringBuilder();
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						attrs.append(parser.getAttributeName(i) + "=\""
								+ parser.getAttributeValue(i) + "\" ");
					}
					sb.append("<" + parser.getName() + " " + attrs.toString() + ">");
					break;
				default:
					sb.append(parser.getText());
					break;
			}
		}
		String content = sb.toString();
		return content;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String getHomeUrl() {
		return homeUrl;
	}

}
