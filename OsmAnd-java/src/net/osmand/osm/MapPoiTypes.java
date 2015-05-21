package net.osmand.osm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.PlatformUtil;
import net.osmand.StringMatcher;
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
	private boolean init;
	
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
	
	public boolean isInit() {
		return init;
	}
	
	public PoiCategory getOtherPoiCategory() {
		return otherCategory;
	}
	
	public List<PoiFilter> getTopVisibleFilters() {
		List<PoiFilter> lf = new ArrayList<PoiFilter>();
		for(PoiCategory pc : categories) {
			if(pc.isTopVisible()) {
				lf.add(pc);
			}
			for(PoiFilter p : pc.getPoiFilters()) {
				if(p.isTopVisible()) {
					lf.add(p);
				}
			}
		}
		sortList(lf);
		return lf;
	}


	private void sortList(List<? extends PoiFilter> lf) {
		final Collator instance = Collator.getInstance();
		Collections.sort(lf, new Comparator<PoiFilter>() {
			@Override
			public int compare(PoiFilter object1, PoiFilter object2) {
				return instance.compare(object1.getTranslation(), object2.getTranslation());
			}
		});
	}
	
	public PoiCategory getUserDefinedCategory() {
		return otherCategory;
	}
	
	public PoiType getPoiTypeByKey(String name) {
		for(PoiCategory pc : categories) {
			PoiType pt = pc.getPoiTypeByKeyName(name);
			if(pt != null && !pt.isReference()) {
				return pt;
			}
		}
		return null;
	}
	
	public AbstractPoiType getAnyPoiTypeByKey(String name) {
		for(PoiCategory pc : categories) {
			if(pc.getKeyName().equals(name)) {
				return pc;
			}
			for(PoiFilter pf : pc.getPoiFilters()) {
				if(pf.getKeyName().equals(name)) {
					return pf;
				}	
			}
			PoiType pt = pc.getPoiTypeByKeyName(name);
			if(pt != null && !pt.isReference()) {
				return pt;
			}
		}
		return null;
	}
	
	public Map<String, PoiType> getAllTranslatedNames(boolean onlyTranslation) {
		Map<String, PoiType> translation = new TreeMap<String, PoiType>(); 
		for(PoiCategory pc : categories) {
			for(PoiType pt :  pc.getPoiTypes()) {
				if(pt.isReference()) {
					continue;
				}
				translation.put(pt.getTranslation(), pt);
				if(!onlyTranslation) {
					String kn = pt.getKeyName().replace('_', ' ');
					if(!pt.getTranslation().toLowerCase().contains(kn)) {
						translation.put(Algorithms.capitalizeFirstLetterAndLowercase(kn), pt);
					}
				}
			}
		}
		return translation;
	}
	
	public Map<String, AbstractPoiType> getAllTypesTranslatedNames(StringMatcher matcher) {
		TreeMap<String, AbstractPoiType> tm = new TreeMap<String, AbstractPoiType>(Collator.getInstance());
		Map<String, PoiType> translation = new TreeMap<String, PoiType>(); 
		for(PoiCategory pc : categories) {
			addIf(tm, pc, matcher);
			for(PoiFilter pt :  pc.getPoiFilters()) {
				addIf(tm, pt, matcher);
			}
			for(PoiType pt :  pc.getPoiTypes()) {
				if(pt.isReference()) {
					continue;
				}
				addIf(tm, pt, matcher);
			}
		}
		return tm;
	}
	
	private void addIf(Map<String, AbstractPoiType> tm, AbstractPoiType pc, StringMatcher matcher) {
		if(matcher.matches(pc.getTranslation()) || matcher.matches(pc.getKeyName().replace('_', ' '))) {
			tm.put(pc.getTranslation(), pc);
		}
	}


	public Map<String, PoiType> getAllTranslatedNames(PoiCategory pc, boolean onlyTranslation) {
		Map<String, PoiType> translation = new TreeMap<String, PoiType>();
		for (PoiType pt : pc.getPoiTypes()) {
			translation.put(pt.getTranslation(), pt);
			if (!onlyTranslation) {
				String kn = pt.getKeyName().replace('_', ' ');
				if(!pt.getTranslation().toLowerCase().contains(kn)) {
					translation.put(Algorithms.capitalizeFirstLetterAndLowercase(kn), pt);
				}
			}
		}
		return translation;
	}
	
	public PoiCategory getPoiCategoryByName(String name) {
		return getPoiCategoryByName(name, false);
	}
	
	public PoiCategory getPoiCategoryByName(String name, boolean create) {
		if(name.equals("leisure") && !create) {
			name = "entertainment";
		}
		if(name.equals("historic") && !create) {
			name = "tourism";
		}
		for(PoiCategory p : categories ) {
			if(p.getKeyName().equalsIgnoreCase(name)) {
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
		sortList(categories);
		
	}
	public void init() {
		init(null);
	}

	public void init(String resourceName) {
		InputStream is;
		long time = System.currentTimeMillis();
		List<PoiType> referenceTypes = new ArrayList<PoiType>();
		final Map<String, PoiType> allTypes = new LinkedHashMap<String, PoiType>();
		if(resourceName != null) {
			this.resourceName = resourceName;
		}
		try {
			if (this.resourceName == null) {
				is = MapPoiTypes.class.getResourceAsStream("poi_types.xml"); //$NON-NLS-1$
			} else {
				is = new FileInputStream(this.resourceName);
			}
			time = System.currentTimeMillis();
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			int tok;
			parser.setInput(is, "UTF-8");
			PoiCategory lastCategory = null;
			PoiFilter lastFilter = null;
			PoiType lastType = null;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if (name.equals("poi_category")) {
						lastCategory = new PoiCategory(this, parser.getAttributeValue("", "name"), categories.size());
						lastCategory.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
						categories.add(lastCategory);
					} else if (name.equals("poi_filter")) {
						PoiFilter tp = new PoiFilter(this, lastCategory, parser.getAttributeValue("", "name"));
						tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
						lastFilter = tp;
						lastCategory.addPoiType(tp);
					} else if (name.equals("poi_reference")) {
						PoiType tp = new PoiType(this, lastCategory, parser.getAttributeValue("", "name"));
						referenceTypes.add(tp);
						tp.setReferenceType(tp);
						if (lastFilter != null) {
							lastFilter.addPoiType(tp);
						}
						lastCategory.addPoiType(tp);
					} else if (name.equals("poi_additional")) {
						PoiType tp = new PoiType(this, lastCategory, parser.getAttributeValue("", "name"));
						tp.setOsmTag(parser.getAttributeValue("", "tag"));
						tp.setOsmValue(parser.getAttributeValue("", "value"));
						tp.setOsmTag2(parser.getAttributeValue("", "tag2"));
						tp.setOsmValue2(parser.getAttributeValue("", "value2"));
						if (lastType != null) {
							lastType.addPoiAdditional(tp);
						} else if (lastFilter != null) {
							lastFilter.addPoiAdditional(tp);
						} else if (lastCategory != null) {
							lastCategory.addPoiAdditional(tp);
						}
					} else if (name.equals("poi_type")) {
						PoiType tp = new PoiType(this, lastCategory, parser.getAttributeValue("", "name"));
						tp.setOsmTag(parser.getAttributeValue("", "tag"));
						tp.setOsmValue(parser.getAttributeValue("", "value"));
						tp.setOsmTag2(parser.getAttributeValue("", "tag2"));
						tp.setOsmValue2(parser.getAttributeValue("", "value2"));
						lastType = tp;
						if (lastFilter != null) {
							lastFilter.addPoiType(tp);
						}
						allTypes.put(tp.getKeyName(), tp);
						lastCategory.addPoiType(tp);
					}
				} else if (tok == XmlPullParser.END_TAG) {
					String name = parser.getName();
					if (name.equals("poi_filter")) {
						lastFilter = null;
					} else if (name.equals("poi_type")) {
						lastType = null;
					}
				}
			}
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
		for (PoiType gt : referenceTypes) {
			PoiType pt = allTypes.get(gt.getKeyName());
			if (pt == null || pt.getOsmTag() == null) {
				throw new IllegalStateException("Can't find poi type for poi reference '" + gt.keyName + "'");
			} else {
				gt.setReferenceType(pt);
			}
		}
		findDefaultOtherCategory();
		init = true;
		log.info("Time to init poi types " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
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
			System.out.println("Category " + p.getKeyName());
			for(PoiFilter f : p.getPoiFilters()) {
				System.out.println(" Filter " + f.getKeyName());
				print("  ", f);
			}
			print(" ", p);
		}
		
	}
	
	private static void print(String indent, PoiFilter f) {
		for(PoiType pt : f.getPoiTypes()) {
			System.out.println(indent + " Type " + pt.getKeyName() + 
					(pt.isReference() ? (" -> " + pt.getReferenceType().getCategory().getKeyName()  ): ""));
		}
	}

	public static void main(String[] args) {
		DEFAULT_INSTANCE = new MapPoiTypes("/Users/victorshcherb/osmand/repos/resources/poi/poi_types.xml");
		DEFAULT_INSTANCE.init();
//		print(DEFAULT_INSTANCE)	;
//		System.out.println("-----------------");
		List<PoiFilter> lf = DEFAULT_INSTANCE.getTopVisibleFilters();
		for(PoiFilter l : lf) {
			System.out.println("----------------- " + l.getKeyName());
			print("", l);
			Map<PoiCategory, LinkedHashSet<String>> m = 
					l.putTypes(new LinkedHashMap<PoiCategory, LinkedHashSet<String>>());
			System.out.println(m);
		}
		
	}

	public String getTranslation(AbstractPoiType abstractPoiType) {
		if(poiTranslator != null) {
			String translation = poiTranslator.getTranslation(abstractPoiType);
			if(translation != null) {
				return translation;
			}
		}
		return Algorithms.capitalizeFirstLetterAndLowercase(abstractPoiType.getKeyName().replace('_', ' '));
	}


	public boolean isRegisteredType(PoiCategory t) {
		return getPoiCategoryByName(t.getKeyName()) != otherCategory;
	}
	
}
