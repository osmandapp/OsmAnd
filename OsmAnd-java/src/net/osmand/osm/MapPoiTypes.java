package net.osmand.osm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public class MapPoiTypes {
	private static MapPoiTypes DEFAULT_INSTANCE = null;
	private static final Log log = PlatformUtil.getLog(MapRenderingTypes.class);
	private String resourceName;
	private List<PoiCategory> categories = new ArrayList<PoiCategory>();
	private PoiCategory otherCategory;
	
	static final String OSM_WIKI_CATEGORY = "osmwiki";
	private PoiTranslator poiTranslator = null;
	
	public MapPoiTypes(String fileName){
		this.resourceName = fileName;
	}
	
	public interface PoiTranslator {
		
		public String getTranslation(AbstractPoiType type);
	}
	
	public static MapPoiTypes getDefaultNoInit() {
		if(DEFAULT_INSTANCE == null){
			DEFAULT_INSTANCE = new MapPoiTypes(null);
		}
		return DEFAULT_INSTANCE;
	}
	

	public static MapPoiTypes getDefault() {
		if(DEFAULT_INSTANCE == null){
			DEFAULT_INSTANCE = new MapPoiTypes(null);
			DEFAULT_INSTANCE.init();
		}
		return DEFAULT_INSTANCE;
	}

	
	public PoiCategory getOtherPoiCategory() {
		return otherCategory;
	}
	
	public PoiCategory getUserDefinedCategory() {
		return otherCategory;
	}
	
	public PoiCategory getPoiCategoryByName(String name) {
		name = name.toLowerCase();
		return getPoiCategoryByName(name, false);
	}
	

	public PoiCategory getPoiCategoryBySubtypeName(String name) {
		for(PoiCategory pc : categories) {
			PoiType pt = pc.getPoiTypeByKeyName(name);
			if(pt != null) {
				return pc;
			}
		}
		return otherCategory;
	}
	
	public Map<String, PoiType> getAllTranslatedNames() {
		Map<String, PoiType> translation = new TreeMap<String, PoiType>(); 
		for(PoiCategory pc : categories) {
			for(PoiType pt :  pc.getPoiTypes()) {
				translation.put(pt.getTranslation(), pt);
			}
		}
		return translation;
	}
	
	public Map<String, PoiType> getAllTranslatedNames(PoiCategory pc) {
		Map<String, PoiType> translation = new TreeMap<String, PoiType>();
		for (PoiType pt : pc.getPoiTypes()) {
			translation.put(pt.getTranslation(), pt);
		}
		return translation;
	}
	
	public PoiCategory getPoiCategoryByName(String name, boolean create) {
		if(name.equals("entertainment") && !create) {
			name = "leisure";
		}
		for(PoiCategory p : categories ) {
			if(p.getName().equals(name)) {
				return p;
			}
		}
		if(create) {
			PoiCategory lastCategory = new PoiCategory(this, name, categories.size());
			categories.add(lastCategory);
			return lastCategory;
		}
		return otherCategory;
	}
	
	public PoiTranslator getPoiTranslator() {
		return poiTranslator;
	}
	
	public void setPoiTranslator(PoiTranslator poiTranslator) {
		this.poiTranslator = poiTranslator;
	}
	

	public void init(){
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
						lastCategory = new PoiCategory(this, parser.getAttributeValue("","name"), categories.size());
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
						}
						lastCategory.addPoiType(tp);
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
		findDefaultOtherCategory();
	}
	
	private void findDefaultOtherCategory() {
		PoiCategory pc = getPoiCategoryByName("user_defined_other");
		if(pc == null) {
			throw new IllegalArgumentException("No poi category other");
		}
		otherCategory = pc;
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

	public String getTranslation(AbstractPoiType abstractPoiType) {
		String translation = null;
		if(poiTranslator != null) {
			translation = poiTranslator.getTranslation(abstractPoiType);
		}
		if(translation != null) {
			return translation;
		}
		return Algorithms.capitalizeFirstLetterAndLowercase(abstractPoiType.getName().replace('_', ' '));
	}


	public boolean isRegisteredType(PoiCategory t) {
		return getPoiCategoryByName(t.getKeyName()) != otherCategory;
	}


	

	
}
