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
import java.util.WeakHashMap;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
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
	private File imgFile;
	private List<StageInformation> stageInformation = new ArrayList<TourInformation.StageInformation>();
	private List<String> maps =new ArrayList<String>();
	private String mode;

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
	
	public List<String> getMaps() {
		return maps;
	}
	
	private static WeakHashMap<File, Bitmap> androidBitmaps = new WeakHashMap<File, Bitmap>();
	private static Bitmap decodeImage(File f) {
		if(!androidBitmaps.containsKey(f)) {
			Bitmap img =null;
			if(f != null && f.exists()) {
				img = BitmapFactory.decodeFile(f.getAbsolutePath());
			}
			androidBitmaps.put(f, img);
		}
		return androidBitmaps.get(f);
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
		StageFavoriteGroup group = null;
		StageFavorite favorite = null;
		stageInformation.clear();
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				if(tag.equals("tour")) {
					name = getDefAttribute(parser, "name", name);
					mode = getDefAttribute(parser, "mode", "");
					homeUrl = getDefAttribute(parser, "url", "");
				} else if (tag.equals("stage")) {
					stage = new StageInformation(this, stageInformation.size());
					stage.name = getDefAttribute(parser, "name", "");
					stage.mode = getDefAttribute(parser, "mode", "");
				} else if (tag.equals("prerequisite")) {
					String map = getDefAttribute(parser, "map", "");
					if(!Algorithms.isEmpty(map)) {
						maps .add(map);
					}
				} else if (tag.equals("itinerary") && stage != null){
					String img = getDefAttribute(parser, "image", "");
					stage.distance = Double.parseDouble(getDefAttribute(parser, "distance", "0"));
					stage.duration = Integer.parseInt(getDefAttribute(parser, "duration", "0"));
					double slat = Double.parseDouble(getDefAttribute(parser, "startLat", "0"));
					double slon = Double.parseDouble(getDefAttribute(parser, "startLon", "0"));
					if(slat != 0 || slon != 0) {
						stage.startPoint = new LatLon(slat, slon);
					}
					if(img.startsWith(FILE_PREFIX)) {
						stage.itineraryFile = getFile(img);
					}
					stage.itinerary = getInnerXml(parser);
				} else if(stage != null && tag.equals("group")) {
					group = new StageFavoriteGroup();
					group.color = Algorithms.parseColor(getDefAttribute(parser, "color", Algorithms.colorToString(StageImageDrawable.INFO_COLOR)));
					group.name = getDefAttribute(parser, "name", "");
					group.id = getDefAttribute(parser, "id", "");
					group.order = stage.favorites.size();
					stage.favorites.add(group);
				} else if(group != null && tag.equals("favorite")) {
					favorite = new StageFavorite();
					favorite.location = new LatLon(Double.parseDouble(getDefAttribute(parser, "lat", "0")),
							Double.parseDouble(getDefAttribute(parser, "lon", "0")));
					favorite.name = getDefAttribute(parser, "name", "");
					favorite.group = group;
					favorite.order = stage.favorites.size(); 
					group.favorites.add(favorite);
					stage.favorites.add(favorite);
				} else if (tag.equals("fullDescription")){
					fulldescription = getInnerXml(parser);
				} else if (tag.equals("instructions")){
					instructions = getInnerXml(parser);
				} else if (favorite != null && tag.equals("description")){
					favorite.fullDescription = getInnerXml(parser);
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
				} else if(favorite != null && tag.equals("defaultImage")) {
					if(text.startsWith(FILE_PREFIX)) {
						favorite.imgFile = getFile(text);
					}
				} else if(stage != null && tag.equals("defaultImage")) {
					if(text.startsWith(FILE_PREFIX)) {
						stage.imgFile = getFile(text);
					}
				} else if(stage != null && tag.equals("group")) {
					group = null;
				} else if(stage != null && tag.equals("favorite")) {
					favorite = null;
				} else if(tag.equals("defaultImage")) {
					if(text.startsWith(FILE_PREFIX)) {
						imgFile = getFile(text);
					}
				} else if(stage != null && tag.equals("gpx")) {
					if(text.startsWith(FILE_PREFIX)) {
						stage.gpxFile = getFile(text);
					}
				} else if(tag.equals("shortDescription")) {
					if(favorite != null) {
						favorite.shortDescription = text;
					} else if(stage != null) {
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
		return decodeImage(imgFile);
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
	
	
	public static class StageFavoriteGroup {
		String id;
		int order;
		int color;
		String name = "";
		List<StageFavorite> favorites = new ArrayList<StageFavorite>();
		
		public int getColor() {
			return color;
		}
		
		public String getName() {
			return name;
		}
		
		public List<StageFavorite> getFavorites() {
			return favorites;
		}
		
		public int getOrder() {
			return order;
		}
	}
	
	public static class StageFavorite implements LocationPoint {
		int order;
		LatLon location;
		String name = "";
		String shortDescription = "";
		String fullDescription = "";
		File imgFile;
		StageFavoriteGroup group;
		
		public StageFavoriteGroup getGroup() {
			return group;
		}
		
		public int getOrder() {
			return order;
		}

		public LatLon getLatLon() {
			return location;
		}

		@Override
		public double getLatitude() {
			return location.getLatitude();
		}

		@Override
		public double getLongitude() {
			return location.getLongitude();
		}
		
		@Override
		public PointDescription getPointDescription(Context ctx) {
			return new PointDescription(PointDescription.POINT_TYPE_WPT, name);
		}
		
		public String getName() {
			return name;
		}

		@Override
		public int getColor() {
			if(group != null) {
				return group.color;
			}
			return 0;
		}

		public String getShortDescription() {
			return shortDescription;
		}
		
		public String getFullDescription() {
			return fullDescription;
		}
		
		public Bitmap getImage() {
			return decodeImage(imgFile);
		}

		@Override
		public boolean isVisible() {
			return true;
		}

	}
	
	public static class StageInformation {
		int duration;
		String itinerary = "";
		File gpxFile;
		GPXFile gpx;
		String name = "";
		String shortDescription = "";
		String fullDescription = "";
		File imgFile;
		File itineraryFile;
		double distance;
		LatLon startPoint = null;
		String mode;
		List<Object> favorites = new ArrayList<Object>();
		
		TourInformation tour;
		int order;
		
		public List<Object> getFavorites() {
			return favorites;
		}
		
		public StageFavoriteGroup getGroupById(String id) {
			for(Object o : favorites) {
				if(o instanceof StageFavoriteGroup) {
					if(id.equals(((StageFavoriteGroup)o).id)) {
						return (StageFavoriteGroup) o;
					}
				}
			}
			return null;
		}
		
		public String getMode() {
			if(Algorithms.isEmpty(mode)) {
				return tour.mode;
			}
			return mode;
		}
		
		public LatLon getStartPoint() {
			return startPoint;
		}
		
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
			return decodeImage(imgFile);
		}
		
		
		public Bitmap getItineraryBitmap() {
			return decodeImage(itineraryFile);
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

	public String getMode() {
		return mode;
	}

}
