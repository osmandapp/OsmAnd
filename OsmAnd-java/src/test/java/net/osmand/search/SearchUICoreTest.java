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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

@RunWith(Parameterized.class)
public class SearchUICoreTest {

	private static final String SEARCH_RESOURCES_PATH = "src/test/resources/search/";
	private static boolean TEST_EXTRA_RESULTS = true;
	
	private final File testFile;

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
		} catch (IOException | XmlPullParserException e) {
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
	    File obfFile = new File(testFile.getParentFile(), testFile.getName().replace(".json", ".obf"));
		File obfZipFile = new File(testFile.getParentFile(), testFile.getName().replace(".json", ".obf.gz"));
		String sourceJsonText = Algorithms.getFileAsString(testFile);
		Assert.assertNotNull(sourceJsonText);
		Assert.assertTrue(sourceJsonText.length() > 0);

		JSONObject sourceJson = new JSONObject(sourceJsonText);
		JSONArray phrasesJson = sourceJson.optJSONArray("phrases");
		String singlePhrase = sourceJson.optString("phrase", null);
		List<String> phrases = new ArrayList<>();
		if (singlePhrase != null) {
			phrases.add(singlePhrase);
		}
		if (phrasesJson != null) {
			for (int i = 0; i < phrasesJson.length(); i++) {
				String phrase = phrasesJson.optString(i);
				if (phrase != null) {
					phrases.add(phrase);
				}
			}
		}
		JSONObject settingsJson = sourceJson.getJSONObject("settings");
		BinaryMapIndexReader reader = null;
		boolean useData = settingsJson.optBoolean("useData", true);
		if (useData) {
			boolean obfZipFileExists = obfZipFile.exists();
			if (!obfZipFileExists) {
				System.out.printf("Could not find obf file: %s%n", obfZipFile.getPath());
				return;
			}
			//Assert.assertTrue(obfZipFileExists);

			GZIPInputStream gzin = new GZIPInputStream(new FileInputStream(obfZipFile));
			FileOutputStream fous = new FileOutputStream(obfFile);
			Algorithms.streamCopy(gzin, fous);
			fous.close();
			gzin.close();

			reader = new BinaryMapIndexReader(new RandomAccessFile(obfFile.getPath(), "r"), obfFile);
		}
		 boolean disabled = settingsJson.optBoolean("disabled", false);
		 if (disabled) {
			 return;
		 }
		List<List<String>> results = new ArrayList<>();
		for (int i = 0; i < phrases.size(); i++) {
			results.add(new ArrayList<String>());
		}
		if (sourceJson.has("results")) {
			parseResults(sourceJson, "results", results);
		}
		if (TEST_EXTRA_RESULTS && sourceJson.has("extra-results")) {
			parseResults(sourceJson, "extra-results", results);
		}

		Assert.assertEquals(phrases.size(), results.size());
		if (phrases.size() != results.size()) {
			return;
		}

		SearchSettings s = SearchSettings.parseJSON(settingsJson);
		if (reader != null) {
			s.setOfflineIndexes(Collections.singletonList(reader));
		}

		final SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false);
		core.init();

		ResultMatcher<SearchResult> rm = new ResultMatcher<SearchResult>() {
			@Override
			public boolean publish(SearchResult object) {
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};

		boolean simpleTest = true;
		SearchPhrase emptyPhrase = SearchPhrase.emptyPhrase(s);
		for (int k = 0; k < phrases.size(); k++) {
			String text = phrases.get(k);
			List<String> result = results.get(k);
			List<SearchResult> searchResults;
			SearchPhrase phrase;
			String[] arr = text.split("[\\\\{}]");
			if (arr.length > 0 && arr[0].equals("POI_TYPE:")) {
				SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = true;
				phrase = emptyPhrase.generateNewPhrase("", s);
				searchResults = getSearchResult(phrase, rm, core);
				for (SearchResult searchResult : searchResults) {
					if (arr.length > 1 && arr[1].equals(searchResult.localeName)) {
						String fullText = "";
						if (arr.length > 2) {
							fullText = arr[2];
						}
						phrase = emptyPhrase.generateNewPhrase(fullText, s);
						phrase.getWords().add(new SearchWord(searchResult.localeName, searchResult));
						searchResults = getSearchResult(phrase, rm, core);
						break;
					}
				}
			} else {
				phrase = emptyPhrase.generateNewPhrase(text, s);
				searchResults = getSearchResult(phrase, rm, core);
			}

			for (int i = 0; i < result.size(); i++) {
				String expected = result.get(i);
				SearchResult res = i >= searchResults.size() ? null : searchResults.get(i);
				if (simpleTest && expected.indexOf('[') != -1) {
					expected = expected.substring(0, expected.indexOf('[')).trim();
				}
				// String present = result.toString();
				String present = res == null ? ("#MISSING " + (i + 1)) : formatResult(simpleTest, res, phrase);
				if (!Algorithms.stringsEqual(expected, present)) {
					System.out.printf("Phrase: %s%n", phrase);
					System.out.printf("Mismatch for '%s' != '%s'. Result: %n", expected, present);
					System.out.println("CURRENT RESULTS: ");
					for (SearchResult r : searchResults) {
						System.out.printf("\t\"%s\",%n", formatResult(false, r, phrase));
					
					}
					System.out.println("EXPECTED : ");
					for (String r : result) {
						System.out.printf("\t\"%s\",%n", r);
					}
				}
				Assert.assertEquals(expected, present);
			}
		}

		obfFile.delete();
	}

	private List<SearchResult> getSearchResult(SearchPhrase phrase, ResultMatcher<SearchResult> rm, SearchUICore core){
		SearchResultMatcher matcher = new SearchResultMatcher(rm, phrase, 1, new AtomicInteger(1), -1);
		core.searchInternal(phrase, matcher);
		SearchResultCollection collection = new SearchResultCollection(phrase);
		collection.addSearchResults(matcher.getRequestResults(), true, true);
		if (matcher.totalLimit != -1 && matcher.count > matcher.totalLimit) {
			collection.setUseLimit(true);
		}
		
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
		if (r.location != null) {
			dist = MapUtils.getDistance(r.location, phrase.getLastTokenLocation());
		}
		return String.format(Locale.US, "%s [[%d, %s, %.3f, %.2f km]]", r.toString(),
				r.getFoundWordCount(), r.objectType.toString(),
				r.getUnknownPhraseMatchWeight(),
				dist / 1000);
	}

	static class TestSearchTranslator implements MapPoiTypes.PoiTranslator {

		private final Map<String, String> enPhrases;
		private final Map<String, String> phrases;
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
		public String getAllLanguagesTranslationSuffix() {
			return "all languages";
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
	}
}
