package net.osmand.osm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class MapPoiTypes {
	private static MapPoiTypes DEFAULT_INSTANCE = null;
	private static final Log log = PlatformUtil.getLog(MapRenderingTypes.class);
	private String resourceName;
	private List<PoiCategory> categories = new ArrayList<PoiCategory>(); 
	
	public MapPoiTypes(String fileName){
		this.resourceName = fileName;
	}
	
	public static MapPoiTypes getDefault() {
		if(DEFAULT_INSTANCE == null){
			DEFAULT_INSTANCE = new MapPoiTypes(null);
			DEFAULT_INSTANCE.init();
		}
		return DEFAULT_INSTANCE;
	}

	protected void init(){
		InputStream is;
		try {
			if(resourceName == null){
				is = MapRenderingTypes.class.getResourceAsStream("poi_types.xml"); //$NON-NLS-1$
			} else {
				is = new FileInputStream(resourceName);
			}
			long time = System.currentTimeMillis();
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			int tok;
			parser.setInput(is, "UTF-8");
			PoiCategory lastCategory = null;
			PoiFilter lastFilter = null;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if (name.equals("poi_category")) { 
						lastCategory = new PoiCategory(this, parser.getAttributeValue("","name"));
						categories.add(lastCategory);
					} else if (name.equals("poi_filter")) {
						PoiFilter tp = new PoiFilter(this, lastCategory,
								parser.getAttributeValue("", "name"));
						lastFilter = tp;
						lastCategory.addPoiType(tp);
					} else if(name.equals("poi_type")){
						PoiType tp = new PoiType(this,
								lastCategory, parser.getAttributeValue("","name"));
						tp.setOsmTag(parser.getAttributeValue("","tag"));
						tp.setOsmValue(parser.getAttributeValue("","value"));
						tp.setOsmTag2(parser.getAttributeValue("","tag2"));
						tp.setOsmValue2(parser.getAttributeValue("","value2"));
						
						if(lastFilter != null) {
							lastFilter.addPoiType(tp);
						} else {
							lastCategory.addPoiType(tp);
						}
					}
				} else if (tok == XmlPullParser.END_TAG) {
					String name = parser.getName();
					if (name.equals("poi_filter")) { 
						lastFilter = null;
					}
				}
			}
			log.info("Time to init poi types" + (System.currentTimeMillis() - time)); //$NON-NLS-1$
			is.close();
		} catch (IOException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		} catch (XmlPullParserException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public List<PoiCategory> getCategories() {
		return categories;
	}
	
	private static void print(MapPoiTypes df) {
		List<PoiCategory> pc = df.getCategories();
		for(PoiCategory p : pc) {
			System.out.println("Category " + p.getName());
			for(PoiFilter f : p.getPoiFilters()) {
				System.out.println(" Filter " + f.getName());
				print("  ", f);
			}
			print(" ", p);
		}
		
	}
	
	private static void print(String indent, PoiFilter f) {
		for(PoiType pt : f.getPoiTypes()) {
			System.out.println(indent + " Type " + pt.getName());
		}
	}

	public static void main(String[] args) {
		print(getDefault())	;
	}

	
}
