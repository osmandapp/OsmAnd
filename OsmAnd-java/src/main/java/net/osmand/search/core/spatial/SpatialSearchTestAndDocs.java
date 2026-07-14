package net.osmand.search.core.spatial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.SearchAlgorithms;


//////////// TESTING //////////
// TESTING Ignore same embedded boundary city / county - deduplicate on the fly (new york x4)
// TESTING test: merge boundaries bbox - extend incomplete boundary same id ... - npt fixed as we anyway enlarge
// TESTING Filter / group some categories: Public transport stops, City&Bike - New york?
// TESTING Sort maps poi categories API search (sort bboxes?)
// TESTING query = "Church Catedral-Basílica de Nuestra Señora del Pilar"; -  POI_TYPE /\ POI (SYNONYMS!)
// TEST_ALLOW_HOUSE_POI_TYPE_INTERSECTION Review if poi doesn't have bbox don't intersect or add bbox! - Shell 2 Rožňavská (test)

////////// IN PROGRESS //////////
// TESTING INSPECTOR stats index_words_dashboard.html
// TESTING OPTIM_READ_COMMON_WORDS_ATOMS !
// REVIEW: POI / ADDRESS - France, Germany, US, Europe, China, Peru  
// TESTING: Fix 36K national park (don't index small islands > 100 POI !!!)

// TODO INDEX: Speedup load after sorting - to limit objects (store elo in index)! 
// TODO INDEX: Store Poi category index (effective intersection 'Church St. Miguel' - refactor checkAmenity)


// TODO DEDUPLICATE: same location (5-10m) 2 streets different cities (Check)
// TODO AVENUE G https://github.com/osmandapp/OsmAnd/issues/15726
// TODO OBF POI CATEGORY Bboxes too large - investigate size (introduce for categories OBF) - OsmAndPoiNameIndexDataAtom, quad tree (90% < 10K)
// TODO ANALYZE: too many wiki places on streets?
// TODO Bank abc (Bug New filter?) 


// TO DO Ivan
// REVIEW DEDUPLICATE: Review / implement similarity radius - similarityRadius = 50000 ... Route Id
// REVIEW DEDUPLICATE: Unite RouteArticle, POI by wikidata id ? - DEPTH_TO_CHECK_SAME_SEARCH_RESULTS = 20;...
// TODO DEDUPLICATE: review osm route id  combine by?
// TODO DEDUPLICATE: Test wiki / travel maps / seamarks map
// TODO UNIT TESTS: Street related to city or suburb what to show? (Fixed?) 
// TODO UNIT TESTS: (duplicate words), Бульварно-Кудрявська, NC-42, 2-га Нова (2 Нова), M2...
// TODO UNIT TESTS: Auto tests - Slow analysis (Auto test New york)
// TODO UNIT TESTS: Add test on show more '2 sokak' - Show more 1. 2 Sokak (house) 2. 2 Sokak (street) 3. 2 <WORD> Sokak (street) or 3381/2 Sokak. 4. '2.Kadriye' (city) .. Sokak

// TODO DEDUPLICATE: Index place=state, county.. + wikidata id for boundaries (regions.ocbf) & display them - analyze
// TODO DEDUPLICATE: Venezia ? - No place=city in POI is it on purpose ? 2 Wikidataids! Rating not merged. POI - relation/44741 (Q641), CITY - way/64778090 (Q33723961).
// TODO DEDUPLICATE: brand langs - 'Поїхали з нами' / 'Поехали с нами'

// TO DO Gateway
// TODO INSPECTOR: doesn't show suffixes
// TODO ANALYZE: Find slow queries on Autotests (New york, France) - large geo atoms

// TODO INDEX: Find POI Categories translations / synonyms (WEB) - Стоматол., Dentist, Stomatology, Basilica (?)
// TODO REVIEW: Abbrevations (synonyms / direction words) other languages?
// TODO REVIEW: Analyze Abbrefvations / common skip (abbrevations 1st=first) 

// TO DO - RZR
// TODO WEB: POI Categories + top poi categories
// TODO WEB: display results std way: house, interpolation results, poi...
// TODO WEB: Multithread pool, Monitor / time & memory optimize memory?
// TODO ANDROID: Integrate (include regions.ocbf) on client
// TODO ANDROID: Progress / cancel
// TODO ANDROID: memory performance 
// TODO highway=services (Not index)
 
/////////////// EXTRA FEATURES ///////////////
// TODO Store and test conscription number for some cities - issue (RZR)
// TODO Search in large parks, neighborhood same as in boundaries (index bbox POI), residential way/56238205
// TODO Japan test, housename, block_number + housenumber, neighbourhood + quarter - street + India assign houses to suburbs / neighbourhood / blocks
// TODO Postcode needs to load street and check buildings! Store postcode as bbox not as City! - '1186RZ 324' (NL, UK) 
// TODO Search near key objects (subway station artificial bbox)
// TODO Web worldwide search on missing results test "Arizona"
// TODO New Geocoding for cases ("NC 42" == "NC-42") - geo index for prefixes
// TODO Add flats: https://www.openstreetmap.org/node/5843642738
// TODO Sugggestion-correction
// TODO English postcodes
// TODO Precise Boundary 'Chernihiv sport life' mostly Kyiv - check precise boundary for filter
// TODO Short word split "Ro-ki" vs "Roki" 
// TODO Support postcode search - 14871 Pennsylvania Avenue Pine City

public class SpatialSearchTestAndDocs {

	/**
	 * Collator examples:
	 * Equals / starts from space
	 * TRUE - 's' in 'U.S. Information' (. is a space in collator)
	 * FALSE - 'us'  'U.S. Information' (no)
	 * TRUE - 'M-2' == 'M 2' (collator feature)
	 * 
	 * Tokenize:
	 * 'NA-75' - ['NA-75'] (- in between numbers),'NA 75' ['NA', '75']
	 * 'U.S. State' - ['U.S.', 'State'] (dot part of word)
	 * Friedrich-Wilhelm-Weber-Straße -  [friedrich, wilhelm, weber, straße]
	 * 
	 * Matcher
	 * 1. Exact matching always work
	 * 2. 'NA-75' matches 'NA 75' and 'NA 75' matches 'NA-75' 
	 * 
	 * Tokenizer {@link SearchAlgorithms#splitAndNormalize(String, boolean)}
	 * 
	 * Word: Characters or digits (emoji undefined status)
	 *  
	 * **Special symbols**
	 * '.' - part of the word: 'st.', '2039.' (needs to be stored inside)
	 * ''' - part of the word: 'Mcdonald's' (ignored in collator - alignChars)
	 * '-' - split not numbers, for numbers part of the word 
	 * Example: split used for user input '63/28' should keep as 1 word for building
	 * Special needs to be stored but ignored in collator
	 * 
	 * Other symbols are ignored:
	 * '#', '№', '/' ...
	 * 
	 * 1. Unnecessary split of 'NC-42', '2-B' '63/28' (housenumber reverted) causes 
	 * unnecessary complication and computation.
	 * 
	 * 2. No split causes '63/28' causes unnecessary indexing of refs like '123/1x/23y'
	 *    and missing search for '12/NameOfThePlace'
	 * 
	 * It's important to not split what has different meaning on reordering!
	 * However algorithm should support match and search for split words:
	 * DATA: '2-nd street '. SEARCH: 'Street 2', 'Street #2', Street 2-nd'
	 * DATA: 'NC 42', 'NC-42'. SEARCH: 'NC-42', 'NC 42
	 * 
	 * Index stores all single tokens except Partial Numbers and some Common.
	 * So index could have: 'NC-42', 'MC20', '2-nd' (2 letters)
	 * But not stores: '63/28', '2B', 'B2', 
	 * --------------------------------
	 * Spical cases:
	 * 1. '2nd street' is indexed as '2nd' and not 'street.
	 * 	  Limitation: user *must* input 2nd as part of search.
	 *    For input '2' or '#2' (pure number): indexes read all matching prefixes like '2nd'...
	 * 2. Data 'NC 42', ok indexed under 'NC'. (2 M)
	 *    Query: 'NC-42' will find 'NC' prefix and will match collator "NC 42" atom.
	 *    It always works with 2nd word number, if it's not number it will be 2 words.
	 * 3. Data '2 M'. Indexed only by letter. So it's not searchable as '2M'
	 * Potential issue:
	 *    '35bis' == '35 bis' - however it's only for house numbers where different tokenizer is applied!
	 * 4. Issue with space
	 *    Friedrich-Wilhelm-Weber-Straße split same as 'Friedrich Wilhelm Weber Straße' - 4 tokens
	 *    That's an issue for 'Weberstrasse' -> Weber strasse, Hemauerstraße -> Hemauer straße.
	 *    Possible solution is to prepare 2 variation during indexing 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {				
		SpatialTextSearchSettings settings = new SpatialTextSearchSettings();
		File folder = new File(System.getProperty("maps.dir"));
		LatLon location = null;
		String pattern = "Germany_b";
//		pattern = "Map";
		String pattern2 = ".....";
		String query = "Berlin hauptstrasse"; // slow
		query = "Berlin";
//		query = "Kelterstraße Kernen im Remstal";
//		query = "3 Hofäckerstraße Kernen im Remstal";
//		query = "1 W&W Platz Kornwestheim"; // duplicate word new maps needed
//		query = "1/1 Salierstraße Waiblingen"; // duplicate in house number priority 1st
		query = "24 Kelterstraße Kernen im Remstal";
		
		// poi filter
//		location = new LatLon(52.50805, 13.38176);
//		settings.SEARCH_POI = false;
//		settings.DEV_PRINT_POI_CAT_LIMIT = 100; 
//		settings.DEV_PRINT_POI_CAT_RADIUS_KM = 10;
//		query = "Gynae.";
		
		// Grainau Am Eibsee 1 36799292
		// Grainau Seehäuser Eibsee 2 - 242903848 //  Seehäuser Grainau 2, Seehäuser Eibsee 2  
		
		// Weberstraße (33164748) 49.2041 10.7035,  Von-Weber-Straße (4648613942) 49.5609 10.8685
//		query = "Weber Straße"; // +4648613942, +33164748
//		query = "WeberStraße";  // +33164748, +4648613942
//		query = "Von Weberstraße"; // +4648613942
//		location = new LatLon(48.8315, 9.3155 );
//		query = "53 Langestraße Waiblingen"; // OK - 48.8315 9.3155 !
//		query = "69 Daimler Straße Stuttgart"; //  (Daimlerstraße) 107868593 48.8015 9.2224 // 69
		

		// Building time vs no building
//		Search Stats 778.5 ms - read 754.6 ms atoms (tokens 442.4 ms, obj 1.8 ms), match 281.5 ms, comp 26.4 ms
//		Search Stats 925.5 ms - read 799.8 ms atoms (tokens 442.5 ms, obj 16.3 ms), match 280.5 ms, comp 149.5 ms
		
//		pattern = "Us_utah";
		pattern = "Us_penn";
//		pattern2 = "Us_new-york_syracuse";
//		pattern2 = "Us_virg";
//		pattern = "Map";
//		query = "Salt Lake City Pennsylvania Place UT USA";
//		query = "Salt Lake City Elephant";
//		query = "Salt Lake City Lake";
//		query = "Salt Lake City Pennsylvania Street";
//		query = "West Valley City";
//		query = "USA Salt Lake City Pennsylvania Street 41";
//		query = "Pennsylvania Avenue Pennsylvania USA"; // 31372516
//		query = "Pennsylvania Avenue Philadelphia Pennsylvania USA"; // 50193098, 26283396442
//		query = "Pennsylvania Avenue Philadelphia PA USA"; 
//		query = "Pennsylvania Avenue Philadelphia Philadelphia County Pennsylvania USA";
//		query = "Pennsylvania Avenue White Oak Allegheny County Pennsylvania USA"; // 11947214
//		query = "173 Liberty Valley Road Danville"; // enlarged
//		query = "151 Molleystown Road Pine Grove";
//		query = "6 Kent Road Pine City";
//		query = "36 Wilson Drive  Pine City"; 
//		query = "301 East Second Street Corning"; // "301 East 2nd Street Corning"
//		query = "763 Ro-Ki Boulevard Nichols"; // NO FIX yet: Roki is very short to be fixed same as Weber-Strasse
		// Important unit test
//		query = "2 South 2nd Street Saint Clair"; // to fix street matched twice 40.7194 -76.1904 // UNIT TEST !!! (25 street)
//		query = "South 2nd Street 2 Saint Clair"; // to fix street matched twice
//		query = "226 Wilkes-Barre Township Boulevard Wilkes-Barre"; // fixed type order
//		query = "5676 US-15 Montgomery"; // Test 3 matched (not 2) 
//		location = new LatLon(42.0061257, -76.5464141);
//		query = "38 Orange Street Waverly";
//		query = "441 Cook Road Addison";
//		location = new LatLon(42.0258945, -77.2365078);
//		query = "7910 County Route 5 Addison"; // Addison too far away from town
//		query = "1000 Fillmore Road State College"; // default enlarge 
		
//		query = "151 Weber Way Selinsgrove"; // Fixed: 2 word - addr:unit 
//		query = "1544 PA-61 Pottsville"; // FIXED
//		query = "138 138 Scott Avenue Bellefonte";
//		query = "17815 PA-35 Port Royal"; // CHECK!
//		query = "2039 Ridge Road Lowman"; // extend bbox hamlet // 822981342  -- unit test!
		// test default enlarge 1 -> 2.5
//		query = "1503 Stewart Road Addison"; // 
//		query = "76 North Street Waverly"; // same
//		query = "1098 Long Run Road Pine Grove"; // 2.5 enlarge 40.5943782, -76.2609811
//		query = "312 East 14th Street Elmira"; // no fix locations too close
//		query = "3374 Lower Maple Avenue Elmira";
//		query = "3760 State Route 225 Dornsife"; // red cross? unit test
//		query = "11954 East Hill Road Pine City";
		
		// Street ref "pa 75" (not stored), house "pa-75" (data)
//		query = "PA 75 27193"; //'PA75'  Data 'PA-75', 27193  4472676432
//		query = "PA 75"; // Yes - ('PA 75', 'PA-75'), YES - 'PA75' 
		// data "PA 75" - see "M-2, 2 M" example

//		pattern = "Liechtenstein_europe.obf";
//		query = "Vaduz Lettstrasse";
//		query = "Vaduz ";
//		query = "Jugendheim Malbun";

//		pattern = "Netherlands_";
//		query = "1186RZ Logger 324D Amstelveen";
//		query = "Farm";
//		query = "Huns Huns 39a-MLN 8832kd"; // Húns Húns 37482484
//		query = "11-NUON leons";
		
//		pattern = "Turkey_";
//		query = "Sokak 23018. Balikesir"; // OK
//		query = "2301. Sokak"; // Test 23018., 23018 - Fixed NameIndexCreator - parsePureIntegerSuffix
		// ALL - Search Stats 1569.2 ms - 554.0 ms 59,656 atoms (read 318.8, match 134.1), 985.8 ms compute 693,139 (loadBld 396.2, read 149.5)
        // NO INTER - Search Stats 871.5 ms - 546.4 ms 59,656 atoms (read 313.7, match 135.6), 299.9 ms compute 4,735 (loadBld 54.1, read 37.2)
//		query = "Sokak 2";// 380657094 2.Sokak, 202159401
//		location = new LatLon(40.7627, 29.8454);  
//		query = "2/1 21038 Sokak"; // 1380369156
		// "2.Sokak", "2 Sokak", "Sokak 2", "2. Sokak", "32/2 Sokak" + housenumber (?)
		
		
//		pattern = "regions.ocbf" ;
		
//		pattern = "Ukraine_kyiv-city";
//		pattern = "Test_Ukraine_kyiv-city_europe_12.obf";
//		pattern = "Ukraine_";
		// poi types
//		location = new LatLon(50.439, 30.516);
//		settings.SEARCH_POI = false;
//		settings.DEV_PRINT_POI_CAT_LIMIT = 1000; 
//		settings.DEV_PRINT_POI_CAT_RADIUS_KM = 10;
		
//		query = "bank приватбанк"; // прив.
//		query = "при.";
//		query = "Cafe";
//		query = "Aquarium.";
//		query = "Veget.";
//		query = "Mcdonald's";
//		query = "Stomat.";
		
//		location = new LatLon(50.4631,30.4553);
//		settings.OPTIM_READ_COMMON_WORDS_ATOMS = true;
//		query = "mcdonald's";
//		query = "Kyiv Глушкова 1"; // vs 'Kyiv 1'
//		query = "нова пошта Бульварно Кудрявська";
//		query = "Бульварно-кудрявс.";
//		query = "Ukraine kyiv saks.";
//		query = "пузата хата mcdonal.";
//		query = "Нова пошта 3 харків";
//		query = "Нова пошта харків";
//		query = "2 га Нова вулиця"; // unit test '2га' +, '2-га', '2', '2 га' (partial) unit test (260537333, 104438019)
//		query = "2га Нова вулиця"; 
//		query = "2 нова вулиця"; // '"25-та вулиця", "25та вулиця", "25 та вулиця", "25 вулиця" (NOT FIRST) - '25-та Садова вулиця' 150768561
//		query = "25 садова вулиця"; // 150768561 28256
//		query = "саксаг. 63 28"; // 129-Б, 129б 63/28, 63, 63-28  +'саксаг. 63 28'
//		query = "саксаг. 63/28, 2";
//		query = "саксаг. 63/28 подъезд 2";
//		query = "саксаг. Володимирська"; // intersection
//		query = "саксаг. тарас."; // intersection
//		query = "54-та Садова вулиця 8"; // interpolation
//		query = "Яр. вал 29-г";
//		query = "Школа 25 Володимирська вулиця"; // Школа 25 Володимирська вулиця ALWAYS_READ_COMMON_WORDS_ATOMS = true
//		query = "андріівський узвіз Школа "; // ALWAYS_READ_COMMON_WORDS_ATOMS = true
//		query = "Школа ";
//		query = "Школа А+";
		
//		query = "школа №25"; // test '№25', '25'? -- 'школа', 'школа №25', 'школа 25' // 63112526
//		query = "ВЕЛОwatt";
//		query = "O128894."; // FIX Osm id getOsmIdFromMapObjectId
		// 'M 2' variations data: 'M-2', 'M 2' and '2 M' 
		// POI М-2    (306998303): + ('M-2', 'M 2', '2 M')  - ('2M', 'M2', '2-M')
		// POI '2 M' (3869587585): + ('M-2', 'M 2', '2 M')  - ('2M', 'M2', '2-M') - 2 is not indexed query 2M, 2-M
		// m-n Topol 2(120393782): + ('M-2', 'M 2', '2 M')  - ('2M', 'M2', '2-M')
//		query = "2-M";
		// '2XU', '2X.' 
//		query = "360692"; // refs - 3г (not indexed, search by 3 3gh) 390094/5536x/4267x  
		
//		pattern = "Belarus_minsk";
//		query = "Независим. 48, 1";
		
//		pattern = "Australia";
//		pattern = "Oceania";
//		query = "Holmby road 18 B"; // 'Holmby 18 B', 'Holmby 18-B', 'Holmby 18B'
//		query = "Holmby Melbourne 18B";
		
//		pattern = "Slovakia";
//		pattern2 = "World_";
//		query = "Bratislava Billa";
//		settings.DEDUPLICATE_RES = false;
//		settings.ALLOW_HOUSE_POI_TYPE_INTERSECTION = false;
//		query = "Shell 2 Rožňavská";
		
//		pattern = "Us_new-york_new"; // new-york, new-jersey
//		pattern = "Us_new-"; 
		
//		location = new LatLon(40.78035, -73.96572); // central park
//		location = new LatLon(40.64946, -74.00682); // brooklyn
//		location = new LatLon(40.64946, -73.50682);
//		query = "New York The plaza";
//		query = "New York plaza"; // the plaza , riu plaza
//		query = "New York 55 st"; // 'NY s.' - 0.5s 100k, 'NY st' - 2s (700k)
		// 40.64946, -74.00682 - unit test '4th av', '4 ave', '4th avenue' 241843204, 247910224, 85393997 (..) brooklyn - not 48
		// 40.78035, -73.96572 - unit test '4th av', '4 ave', '4th avenue'  - 85393997 Park avenue
//		query = "New York 4 av 8"; 
//		query = "New York 4 av"; // 160947243
//		query = "57th street"; // central park - 265345338 east, 86216906 west, ()66926268 (west)?),
//		query = "57 street"; // central park - 265345338 east, 86216906 west, ()66926268 (west)?),
//		query = "new york 57th street manhattan";
//		query = "4th ave"; //  unit '4 ave'
//		query = "4th ave 8 paterson"; //  wrong city...
//		query = "little creek"; // little creek
		// Result 4 - 40.8407, -74.0954 [[4th, 8] Building 2 4th Street (26238417818) 40.8441 -74.0910 , [ave, paterson] STREET_TYPE Paterson Avenue (651531238) 40.8374 -74.0997 ]
		
//		query = "2nd street"; // poi types '2 street' - TODO broken
//		query = "blvd"; //  unit test  'blvd', 'boulevard' - 248280132
		
//		pattern = "Us_alaska_"; // special test slow 
//		query = "tongass national forest"; // found anyway complet match 
//		query = "tongass national"; // tongass not found without OPTIM_READ_COMMON_WORDS_ATOMS (?)
//		location = new LatLon(57.366, -150.940);
//		settings.OPTIM_READ_COMMON_WORDS_ATOMS = true;
//		settings.OPTIM_READ_COMMON_WORDS_LIMIT = 2200;
		
		// Japan addr:quarter, addr:neighbourhood, addr:block_number
		// See test - [8-8 Kinshi 3 Kinshi Sumida Tokyo], Rivière Tsumura
		// India - Satyam node/2296788005#map=18/17.805646/83.356818
		// +[Venezia, Cannaregio, 539D , Campo Saffa], +[Venezia Cannaregio 539D ] -[Venezia 539D  Campo Saffa] - expected
//		pattern = "Italy_ven";
//		pattern = "Map";
//		pattern2 = "World_basemap_2";
		// ! unit test - search full address ! no double 539d (no intersectoin)
		// Cannaregio 539D Campo Saffa, Venezia Cannaregio Campo Saffa  , 
//		query = "Venezia Cannaregio Campo Saffa ";
//		query = "Cannaregio 539D Campo Saffa";
//		query = "Campo Saffa";
		
//		pattern = "France_ile-de-france";
//		location = new LatLon(40, 5);
//		query = "Rue Bouchardon 2BIS"; // '2bis' OK, '2 BIS' OK , '2' OK, '2-BIS'
//		query = "Rue Jean Poulmarch 17bis"; //  17bis OK, 17 OK, 17 BIS - OK 'Rue Jean Poulmarch 17;17 bis' 
//		query = "Dieu 8-bis"; // 'Rue Dieu 8 bis' , '8-bis', '8 bis'
		// too many results
//		query = "rue de l'eglise"; // specific search - "rue de l'eglise", non specific "rue de"
//		query = "rue de la";
//		query = "rue la";
//		query = "rû bas du rue";

		
//		pattern = "World_basemap_2";
//		pattern2 = "Ukraine";
//		pattern = "Italy_";
//		query = "о. Пасхи"; // o
//		query = "остров Пасхи"; // o. -> остров - not supported data need to be updated
//		query = "New york";
//		query  = "Madeira"; // short_name	Madeira
//		query  = "Everest";
//		query  = "Rio de Janeiro";
//		location = new LatLon(44.0194, 10.2025);
//		query = "Venezia"; // no place - city
//		query = "Венец."; 

//		pattern = "Spain_aragon_europe_";
//		query = "Church Basílica de Nuestra Señora del Pilar"; // Church vs Roman Church
//		query = "Catedral-Basílica de Nuestra Señora del Pilar"; // 7 words! 2^7 combinations
//		query = "Square de Nuestra Señora del Pilar";  // Church vs Square
//		
//		pattern = "Peru_";
//		query ="Calle 20 188 San Isidro Lima"; // 1430799557
//		query ="Lima Calle 20 San Isidro";
//		query ="Calle 20 ";

		long t = System.nanoTime();

		List<BinaryMapIndexReader> ls = new ArrayList<BinaryMapIndexReader>();
		for (File f : folder.listFiles()) {
			if (f.getName().startsWith(pattern) || f.getName().startsWith(pattern2)) {
				SpatialTextSearch.initFile(ls, f);
			} else if(f.getName().equals(OsmandRegions.REGIONS_OCBF)){
				SpatialTextSearch.initFile(ls, f);
			}
		}
		SpatialTextSearch a = new SpatialTextSearch();
		System.out.println(String.format("Index files %.1f ms", (System.nanoTime() - t) / 1e6));

//		settings.OPTIM_DELETE_EMBEDDED_BOUNDARIES = false;
//		settings.DEDUPLICATE_RES = false;
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(MapPoiTypes.getDefault());
		SpatialSearchContext searchContext = new SpatialSearchContext(settings, ls, poiSearch, location);
		SpatialSearchResults rs = a.searchTest(query, searchContext, 10000);
		SpatialSearchResult mainResult = rs.getFirstResult();
		if (mainResult != null && mainResult.matchedTokens() < rs.tokens.size() - 2) {
			// another way to check to check to get mainResult - boundary object
			City bbox = null;
			for (MapObject o : mainResult.getObjects()) {
				if (o instanceof City c && c.getBbox31() != null) {
					// check that city is not inside maps searched
					bbox = c;
					break;
				}
			}
			if (bbox != null) {
				System.out.println("Suggest search other region - " + bbox);
			}
		}
//		settings.OPTIM_DELETE_POI_SAME_AS_CITY_STREET = false;
//		settings.DEDUPLICATE_RES = true;
//		searchContext = new SpatialSearchContext(settings, ls, poiSearch, location);
//		a.searchTest(query, searchContext, 8000);
	}

	private static void testDeduplication(String[] args) throws IOException, InterruptedException {
		SpatialTextSearchSettings settings = new SpatialTextSearchSettings();
		File folder = new File(System.getProperty("maps.dir"));
		LatLon location = null;
		String pattern = "Italy_";
		String pattern2 = "World";		
		String query = "Torrente Capraia"; // deduplicate by name and similarityRadius
		settings.LANG_DEDUPLICATE = "en";
		query = "Anello di Capraia e Montelupo"; // deduplicate by route_id 
		
		pattern = "Ukraine_";
		pattern2 = "Ukraine_";
		query = "Софійський"; // deduplicate by osmId and wikidata
		query = "Ярославів Вал";

		long t = System.nanoTime();

		List<BinaryMapIndexReader> ls = new ArrayList<BinaryMapIndexReader>();
		for (File f : folder.listFiles()) {
			if (f.getName().startsWith(pattern) || f.getName().startsWith(pattern2)) {
				SpatialTextSearch.initFile(ls, f);
			} else if(f.getName().equals(OsmandRegions.REGIONS_OCBF)){
				SpatialTextSearch.initFile(ls, f);
			}
		}
		SpatialTextSearch a = new SpatialTextSearch();
		System.out.println(String.format("Index files %.1f ms", (System.nanoTime() - t) / 1e6));
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(MapPoiTypes.getDefault());
		SpatialSearchContext searchContext = new SpatialSearchContext(settings, ls, poiSearch, location);
		SpatialSearchResults rs = a.searchTest(query, searchContext, 1000);
		if (rs.mainResults != null) {
			for (SpatialSearchResult s : rs.mainResults) {
				MapObject unitedObject = s.unitedObject;
				String out = s.toString();
				if (unitedObject != null) {
					out += " United:" + unitedObject.toString();
				}
				System.out.println(out);
			}
		}
	}
	
}
