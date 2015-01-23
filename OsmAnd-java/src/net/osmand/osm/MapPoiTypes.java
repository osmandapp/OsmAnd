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
					if (name.equals("category")) { 
						lastCategory = new PoiCategory(this, parser.getAttributeValue("","name"));
						categories.add(lastCategory);
					} else if(name.equals("poi_type")){
						PoiType tp = new PoiType(this,
								lastCategory, parser.getAttributeValue("","name"));
						lastCategory.addPoiType(tp);
						if(lastFilter != null) {
							lastFilter.addPoiType(tp);
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
	
	public static void main(String[] args) {
		getDefault()	;
	}
}
