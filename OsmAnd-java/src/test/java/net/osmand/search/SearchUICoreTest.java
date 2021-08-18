package net.osmand.search;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.*;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

@RunWith(Parameterized.class)
public class SearchUICoreTest {

	private static final String SEARCH_RESOURCES_PATH = "src/test/resources/search/";
	private static boolean TEST_EXTRA_RESULTS = true;
	
	private File testFile;

    public SearchUICoreTest(String name, File file) {
        this.testFile = file;
    }
	

	@BeforeClass
	public static void setUp() {
		defaultSetup();
	}


	static void defaultSetup() {
		MapPoiTypes.setDefault(new MapPoiTypes("src/test/resources/poi_types.xml"));
		MapPoiTypes poiTypes = MapPoiTypes.getDefault();
		Map<String, String> enPhrases = new HashMap<>();
		Map<String, String> phrases = new HashMap<>();
		try {
			enPhrases = Algorithms.parseStringsXml(new File("src/test/resources/phrases/en/phrases.xml"));
			//phrases = Algorithms.parseStringsXml(new File("src/test/resources/phrases/ru/phrases.xml"));
			phrases = enPhrases;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}

		poiTypes.setPoiTranslator(new TestSearchTranslator(phrases, enPhrases));
	}
	
	
	@Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> data() throws IOException {
    	final File[] files = new File(SEARCH_RESOURCES_PATH).listFiles();
    	ArrayList<Object[]> arrayList = new ArrayList<>();
    	if (files != null) {
			for (File file : files) {
				String fileName = file.getName();
				if (fileName.endsWith(".json")) {
					String name = fileName.substring(0, fileName.length() - ".json".length());
					arrayList.add(new Object[] { name, file });
				}
			}
		}
    	return arrayList;
    }

    @Test
	public void testSearch() throws IOException, JSONException {
	}

	private List<SearchResult> getSearchResult(SearchPhrase phrase, ResultMatcher<SearchResult> rm, SearchUICore core){
		SearchResultMatcher matcher = new SearchResultMatcher(rm, phrase, 1, new AtomicInteger(1), -1);
		core.searchInternal(phrase, matcher);
		SearchResultCollection collection = new SearchResultCollection(phrase);
		collection.addSearchResults(matcher.getRequestResults(), true, true);

		return collection.getCurrentSearchResults();
	}

	private void parseResults(JSONObject sourceJson, String tag, List<List<String>> results) {
		List<String> result = results.get(0);
		JSONArray resultsArr = sourceJson.getJSONArray(tag);
		boolean hasInnerArray = resultsArr.length() > 0 && resultsArr.optJSONArray(0) != null;
		for (int i = 0; i < resultsArr.length(); i++) {
			if (hasInnerArray) {
				JSONArray innerArray = resultsArr.optJSONArray(i);
				if (innerArray != null && results.size() > i) {
					result = results.get(i);
					for (int k = 0; k < innerArray.length(); k++) {
						result.add(innerArray.getString(k));
					}
				}
			} else {
				result.add(resultsArr.getString(i));
			}
		}
	}

	private String formatResult(boolean simpleTest, SearchResult r, SearchPhrase phrase) {
		if (simpleTest) {
			return r.toString().trim();
		}
		double dist = 0;
		if(r.location != null) {
			dist = MapUtils.getDistance(r.location, phrase.getLastTokenLocation());
		}
		return String.format("%s [[%d, %s, %.3f, %.2f km]]", r.toString(), 
				r.getFoundWordCount(), r.objectType.toString(),
				r.getUnknownPhraseMatchWeight(),
				dist / 1000
				);
	}

	static class TestSearchTranslator implements MapPoiTypes.PoiTranslator {

		private Map<String, String> enPhrases;
		private Map<String, String> phrases;
		public TestSearchTranslator(Map<String, String> phrases, Map<String, String> enPhrases) {
			this.phrases = phrases;
			this.enPhrases = enPhrases;
		}

		@Override
		public String getTranslation(AbstractPoiType type) {
			AbstractPoiType baseLangType = type.getBaseLangType();
			if (baseLangType != null) {
				return getTranslation(baseLangType) + " (" + type.getLang().toLowerCase() + ")";
			}
			return getTranslation(type.getIconKeyName());
		}

		@Override
		public String getTranslation(String keyName) {
			String val = phrases.get("poi_" + keyName);
			if (val != null) {
				int ind = val.indexOf(';');
				if (ind > 0) {
					return val.substring(0, ind);
				}
			}
			return val;
		}

		@Override
		public String getSynonyms(AbstractPoiType type) {
			AbstractPoiType baseLangType = type.getBaseLangType();
			if (baseLangType != null) {
				return getSynonyms(baseLangType);
			}
			return getSynonyms(type.getIconKeyName());
		}


		@Override
		public String getSynonyms(String keyName) {
			String val = phrases.get("poi_" + keyName);
			if (val != null) {
				int ind = val.indexOf(';');
				if (ind > 0) {
					return val.substring(ind + 1);
				}
				return "";
			}
			return null;
		}

		@Override
		public String getEnTranslation(AbstractPoiType type) {
			AbstractPoiType baseLangType = type.getBaseLangType();
			if (baseLangType != null) {
				return getEnTranslation(baseLangType) + " (" + type.getLang().toLowerCase() + ")";
			}
			return getEnTranslation(type.getIconKeyName());
		}

		@Override
		public String getEnTranslation(String keyName) {
			if (enPhrases.isEmpty()) {
				return Algorithms.capitalizeFirstLetter(keyName.replace('_', ' '));
			}
			String val = enPhrases.get("poi_" + keyName);
			if (val != null) {
				int ind = val.indexOf(';');
				if (ind > 0) {
					return val.substring(0, ind);
				}
			}
			return val;
		}
	};
}
