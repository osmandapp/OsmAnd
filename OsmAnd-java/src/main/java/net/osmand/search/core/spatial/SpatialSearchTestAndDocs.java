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
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapPoiTypes.PoiTranslator;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.SearchAlgorithms;


//////////// LIVE TESTING //////////
// UNIT TESTING: Fix 36K national park - live test? (don't index small islands > 100 POI !!!)
// UNIT TESTING: "Gate D18"... (too many results check Limit results "Gate")?
// UNIT TESTING: 'tongass national forest', 'national', national forest'
// UNIT TESTING: 'rue de l'eglise', 'rue de la', 'rue de la fen.', 'rû bas du rue' (too many results) 
// UNIT TESTING: 'Venezia', 'Everest', 'Rio de Janeiro', 'остров Пасхи'
// UNIT TESTING: 25-та школа (keep failing too many results)
// UNIT TESTING: 100km+ "Мигия озеро" (non freq-common word + enlarge), - partialMatch+partialExactMatch
// UNIT TESTING: 100km+ Calle 20 188 San Isidro Lima 
// UNIT TESTING: 100km+ нова пошта краматорськ  - no brand (3, 5) 5 (5 N7846074085, N1482296639)

//////////// TESTING //////////
// UNIT TESTING: 2419 Avenue G, Dickinson, TX 77539, USA (FAILS border) - Add missing border
// UNIT TESTING: 1. 2 Sokak (house) 2. 2 Sokak (street) 3. 2 <WORD> Sokak (street) or 3381/2 Sokak. 4. '2.Kadriye' (city) .. Sokak!
// "2.Sokak", "2 Sokak", "Sokak 2", "2. Sokak", "32/2 Sokak" + housenumber (?),  2/1 21038 Sokak, Sokak 23018. Balikesir, 2301. Sokak
// UNIT TESTING: 4av - 'New York 4 av 8', '4 av', '4 avenue 8', '4th ave', '8 4 ave paterson' (26240861988)
// UNIT TESTING: (venezia district-street) 'Venezia Cannaregio Campo Saffa', 'Cannaregio 539D Campo Saffa', 'Venezia Cannaregio 539D'
// UNIT TESTING: 'Pennsylvania Avenue Philadelphia Philadelphia County Pennsylvania USA' (duplicate words) res - 39.963028, -75.174270
// UNIT TESTING: "саксаг. Володимирська"; // street intersection
// UNIT TESTING: (2 house + ref) 'саксаг. 63/28, 2' (ref + 2 +house), 'саксаг. 28', 'саксаг. 63', 'саксаг. 63/28'
// UNIT TESTING: (school): "Школа 25 Володимирська вулиця" "андріівський узвіз Школа "
// UNIT TESTING: (by id): O128894
// UNIT TESTING: (poi additional germany) Gynaecologist - from all poi types should be result ! (not like old search)
// UNIT TESTING: (City + House +- Street) 'Kyiv Глушкова 1', 'Kyiv 1'
// UNIT TESTING: POI intersection 'fuel mcdonalds', 'cafe fuel', 'fuel burger'
// UNIT TESTING: нова пошта <street>, нова пошта <city>, just <post_ref> (нова пошта 3 краматорск), 5 <>... 
// UNIT TESTING: нова пошта краматорськ  - no brand !! (3 in ref, 5 in name) 5 (5 N7846074085, N1482296639)
// UNIT TESTING: Brands See makby queries and file! // 20: 16 (brand/name Mac.by), 3 (no brand, name Mac.by), ...
// UNIT TESTING: Deduplicate brands by search 'по.' (search) - results brand langs - 'Поїхали з нами' / 'Поехали с нами'
// UNIT TESTING: POI Name / Type + Address - 'Shell 2 Rožňavská'
// UNIT TESTING: <POI Category> + Object - "Cafe вулиця Саксаганського", restaurant Antwerpen , Postcode + Type, 1181ZM cafe
//               Hotel Berlin, see below, "нова пошта вулиця Саксаганського", "нова вулиця Саксаганського"; // brand +
// UNIT TESTING DEDUPLICATE: Street related to city or suburb what to show
// UNIT TESTING: (failing) 763 Ro-Ki Boulevard Nichols

////////// IN PROGRESS //////////
// REVIEW (index_words_dashboard - common озеро): POI / ADDRESS - France, Germany, US, Europe, China, Peru
// REVIEW: Auto test New york, France, Italy (Slow?)
// DEDUPLICATE: Merge suburb+city same location (5-10m) 2 streets different cities (Aleja Bohaterów)

// TODO Result Amenity bbox (merge on category)
// TODO City > Boundary ? location

// TODO Okko Brand names
// TODO Harderwijk - Q58931 (2712801 < 46280889)

// TODO DEDUPLICATE: Venezia, Bratislava? - No place=city in POI is it on purpose ? 2 Wikidataids! Rating not merged. POI - relation/44741 (Q641), CITY - way/64778090 (Q33723961).

// TODO DEDUPLICATE: Index place=state, county.. + wikidata id for boundaries (regions.ocbf) & display them - analyze
// TEST DEDUPLICATE:  wiki / travel maps / seamarks map
// TODO DEDUPLICATE: too many houses (duplicate names) in wiki maps - obstruct search by street "Ярославів Вал"`?

// TO DO Gateway
// TODO INDEX: Find POI Categories translations / synonyms via Common words - Стоматол., Dentist, Basilica 
// TODO REVIEW: Abbrevations (synonyms / direction words) other languages?
// TODO REVIEW: Analyze Abbrevations / common skip (abbrevations 1st=first)


/////////////// EXTRA FEATURES ///////////////
// TODO 100km+: Calle 20 188 San Isidro Lima, mihia lake, нова пошта краматорськ 3, Нова Пошта (№5 not searchable by common words / name)
// SLOW: "Travessa de Santo António" x "Rua Joaquim Ribeiro de Carvalho" x "portugal" (39.7412, -8.8012 Barreira Urbanização Vale da Cabrita))
//       "Foothill Boulevard" x "Golden State Road" x "Los Angeles" x "United states of America"
// TODO FORBID (slow): to interconnect tokens between 2 words - issue "<Street> <City> <Hno>"?
// TODO Sorting before load objects (use elo and other buildings?) and limit results
// TODO INDEX: highway=services (Not index)
// TODO Suggestion based on common suffixes
// TODO Store and test conscription number for some cities - issue (RZR)
// TODO Search in large parks, neighborhood same as in boundaries (index bbox POI), residential way/56238205
// TODO Japan test, housename, block_number + housenumber, neighbourhood + quarter - street + India assign houses to suburbs / neighbourhood / blocks
// TODO Postcode needs to load street and check buildings! Store postcode as bbox not as City! - '1186RZ 324' (NL, UK) 
// TODO Search near key objects (subway station artificial bbox)
// TODO Web worldwide search on missing results test "Arizona"
// TODO New Geocoding for cases ("NC 42" == "NC-42") - geo index for prefixes
// TODO Add flats: https://www.openstreetmap.org/node/5843642738
// TODO Auto-Corrections
// TODO English postcodes
// TODO Precise Boundary 'Chernihiv sport life' mostly Kyiv - check precise boundary for filter
// TODO '4 ave 8 paterson' (OK - '8 4 ave paterson', '4th ave 8 paterson' play order of assigned numbers to bdl ref)
// TODO Short word split "Ro-ki" vs "Roki" 

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
		SpatialTextSearchSettings settings = SpatialTextSearchSettings.defaultSettings();
		File folder = new File(System.getProperty("maps.dir"));
		LatLon location = null;
		String pattern = "Germany_bad";
//		pattern = "Map";
		String pattern2 = ".....";
		String query = "Berlin hauptstrasse"; // slow
		query = "Berlin";
//		query = "Kelterstraße Kernen im Remstal";
//		query = "3 Hofäckerstraße Kernen im Remstal";
//		query = "1 W&W Platz Kornwestheim"; // duplicate word new maps needed
//		query = "1/1 Salierstraße Waiblingen"; // duplicate in house number priority 1st
		query = "24 Kelterstraße Kernen im Remstal";
		query = "2/1 Rathausplatz Esslingen am Neckar"; // not correct
		
//		pattern = "Map";
//		query = "5 to go";
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
//		location = new LatLon(40.7640, -111.8643);
//		pattern2 = "Us_new-york_syracuse";
//		pattern2 = "Us_virg";
//		pattern = "Map";
//		query = "Salt Lake City Pennsylvania Place UT USA";
//		query = "Salt Lake City Elephant";
//		query = "Salt Lake City Lake";
//		query = "Salt Lake City Pennsylvania Street";
//		query = "West Valley City";
//		query = "2110 College Avenue Elmira";
		
		pattern = "Us_penn";
//		query = "USA Salt Lake City Pennsylvania Street 41"6
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
		query = "2 South 2nd Street Saint Clair"; // to fix street matched twice 40.7194 -76.1904 // UNIT TEST !!! (25 street)
//		query = "South 2nd Street 2 Saint Clair"; // to fix street matched twice
		query = "226 Wilkes-Barre Township Boulevard Wilkes-Barre"; // fixed type order
//		query = "5676 US-15 Montgomery"; // Test 3 matched (not 2) - Data "US 15"
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
//		query = "PA 75 27193"; // +'PA75', +'PA-75', +'PA 75'  Data 'PA-75', 27193  4472676432
//		query = "PA 75"; // Yes - ('PA 75', 'PA-75'), YES - 'PA75'
//		query = "PA 21";  // 1336083883 DATA 'PA21' (+!'PA 21', +'PA-21',+'PA21') 
		
//		pattern = "Us_penn";
//		pattern = "Map";
//		query = "14871 Bly Road";
//		query = "Pennsylvania 1282 14871";
//		query = "14871 Pennsylvania Avenue Pine City";
//		query = "14871 Pennsylvania Avenue";
//		pattern = "Us_texas";
//		query = "Avenue G, Dickinson"; // 26308264745
//		query = "2419 Avenue G, Dickinson, TX USA";
//		query = "2419 Avenue G, Dickinson, 77539 TX USA";
//		query = "TX";

//		pattern = "Liechtenstein_europe.obf";
//		query = "Vaduz Lettstrasse";
//		query = "Fast food"; // "Burger Fast food";
//		query = "Bank wheelchair"; // "Burger Fast food";
//		query = "Burger Mcdonald's"; // Test 2 match
//		query = "Vegan Mai Thai"; // Test 3 match
//		query = "Vegan"; // Test Vegan results from subtype
//		query = "Trübbach 10"; // Test Vegan results
//		query = "helipad 2"; // 
//		query = "Friedenskapelle Church"; //Friedenskapelle, Friedhofskapelle (catholic), Mamerten (roman)
//		settings.DEV_PRINT_POI_CAT_RADIUS_KM  = 100;
//		settings.DEV_PRINT_POI_CAT_LIMIT = 100;
//		location = new LatLon(47, 10);
//		query = "Vaduz ";
//		query = "Jugendheim Malbun";

//		pattern = "Netherlands_";
//		location = new LatLon(52.2827, 4.8601);
//		query = "harderwijk estrado"; // 't2+0-w2-oth1-tp4' t2+0-w2-oth2-tp0  
//		query = "1186RZ Logger 324D Amstelveen";
//		query = "Farm";
//		query = "8832kd";
//		query = "Huns Huns 39a-MLN 8832kd"; // Húns Húns 37482484
//		query = "11-NUON leons";
//		pattern2 = "Gb_england";
//		query = "Gate D18"; // gate d18, "gate d-18"
//		query = "mcdonalds"; 
//		query = "mcdonalds fast food "; // 2807400942 didn't return with many maps LiVE TEST mcdonalds
//		query = "vegan cafe"; // vegan-no Popov exclude
		
 
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
//		location = new LatLon(50.436423, 30.508097);
//		settings.SEARCH_POI = false;
//		settings.DEV_PRINT_POI_CAT_LIMIT = 1000; 
//		settings.DEV_PRINT_POI_CAT_RADIUS_KM = 10;
//		query = "Cafe Fuel";
//		query = "atm bank"; 
//		query = "Aquarium";
//		query = "Fuel diesel";
		
//		location = new LatLon(48, 31);
		// "Мигия water", "Мигия озеро", "род." (1019665295,(48.0217 30.9681),)
//		pattern = "Ukraine_";
//		query = "Мигия озеро";
//		query = "Мигия water"; 
		
//		location = new LatLon(48.75, 37.5);
//		query = "нова пошта 3 краматорськ"; // (1482296639, 5 7846074085) 
//		query = "Нова пошта 3 харків";
//		query = "Нова пошта харків";
		
//		query = "shop Fuel";
//		query = "Cafe Fuel";
//		query = "bank приватбанк"; // прив.
//		query = "при.";
//		query = "Cafe";
//		query = "Aquarium.";
//		query = "Veget.";
//		query = "Mcdonalds";
//		query = "Stomat.";
		
//		location = new LatLon(50.4631,30.4553);
//		settings.OPTIM_READ_COMMON_WORDS_ATOMS = true;
//		query = "mcdonald's";
//		query = "Kyiv 1"; // vs 'Kyiv 1' 'Kyiv Глушкова 1'
//		query = "нова пошта Бульварно Кудрявська";
//		query = "Бульварно-кудрявс.";
//		query = "Ukraine kyiv saks.";
//		query = ". entr."; // check dots
//		query = "пузата хата mcdonal.";
//		
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
//		query = "25-та школа"; // 25-та школа, 25-та school
		
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
//		query = "New York av 8";
//		query = "4 ave 8";
//		query = "New York 4 av"; // 160947243
//		query = "57th street"; // central park - 265345338 east, 86216906 west, (266926268 (west)?),
//		query = "57 street"; // central park - 265345338 east, 86216906 west, (26926268 (west)?),
//		query = "new york 57th street manhattan";
//		query = "4th ave"; //  unit '4 ave'
//		query = "4th ave 8 paterson"; //  wrong city... 26240861988
//		query = "4 8 ave paterson"; //  '8 4 ave paterson' ok, '4 ave 8 paterson' not ok To fix 26240861988
//		query = "little creek"; // little creek
		// Result 4 - 40.8407, -74.0954 [[4th, 8] Building 2 4th Street (26238417818) 40.8441 -74.0910 , [ave, paterson] STREET_TYPE Paterson Avenue (651531238) 40.8374 -74.0997 ]
		
//		query = "2nd street"; // poi types '2 street' - broken
//		query = "blvd"; //  unit test  'blvd', 'boulevard' - 248280132
		
//		pattern = "Us_alaska_"; // special test slow 
//		query = "tongass national forest"; // found anyway complet match 
//		query = "tongass national"; //  LIVE TEST tongass not found without OPTIM_READ_COMMON_WORDS_ATOMS (?) 
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
//		query = "Venezia Cannaregio 539D Campo Saffa";
//		query = "Campo Saffa";
//		query = "Venezia";
		
//		pattern = "France_ile-de-france";
//		pattern = "France_";
//		location = new LatLon(40, 5);
//		query = "Eiffel"; // Tour Eiffel, Tower Eiffel, Eiffel - First always Tour Eiffel (second 'Le Jules Verne' OK) 
//		query = "Rue Bouchardon 2BIS"; // '2bis' OK, '2 BIS' OK , '2' OK, '2-BIS'
//		query = "Rue Jean Poulmarch 17bis"; //  17bis OK, 17 OK, 17 BIS - OK 'Rue Jean Poulmarch 17;17 bis' 
//		query = "Dieu 8-bis"; // 'Rue Dieu 8 bis' , '8-bis', '8 bis'
		// too many results
//		query = "rue de l'eglise"; // specific search - "rue de l'eglise", non specific "rue de"
//		query = "rue de la fen."; // all strets
//		query = "de la"; // "de la", "rue de la" only common words + high rating
//		query = "rû bas du rue";
		
//		pattern = "Us_cal";
//		pattern2 = "regions";
//		query = "Golden State Road Los Angeles United States"; // matched only 5?, United States - not found
//		query = "Sylmar United States"; // not found
//		query = "United States"; // not found 
//		query = "Golden State Road Foothill Boulevard Sylmar USA";

		
//		pattern = "World_basemap_mini";
//		pattern2 = "Ukraine_";
//		location = new LatLon(50, 30);
//		settings.DEDUPLICATE_RES = false;
//		query = "Кафе Antwerpen ";
//		query = "Ресторан Antwerpen ";
//		query = "Cafe Gulliver";
//		query = "Hotel Berlin";
//		query = "Cafe вулиця Саксаганського";
//		query = "нова пошта вулиця Саксаганського"; // brand + 
//		query = "нова вулиця Саксаганського"; // no brand
		
		// NL amstelveen
//		query = "1181ZM cafe"; // TEST missing pois (postcode) 
		
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
//		query = "Church Basílica de Nuestra Señora del Pilar"; // Church vs Roman Church UNIT TEST (7 matched)
//		query = "Catedral-Basílica de Nuestra Señora del Pilar"; // 7 words! 2^7 combinations
//		query = "Square de Nuestra Señora del Pilar";  // Church vs Square
//		
//		pattern = "Peru_";
//		query ="Calle 20 188 San Isidro Lima"; // 1430799557
//		query ="Lima Calle 20 San Isidro";
//		query ="Calle 20 ";
		
//		pattern = "Map";
//		query = "по."; //Поїхали з нами,  поехали с нами
//		pattern = "Makby";
//		pattern = "Belarus_min";
//		location = new LatLon(53.8, 27.5);
		// 20: 16 (brand/name Mac.by), 3 (no brand, name Mac.by), 1 (brand/name Мак бай, 13721164919) - Q118149500
		// top_index_brand (2, 26): [Mak.by {Q118149500;Mak by;Makby;Мак бай} (25), // 0 - "Мак.Бай", "Мак.by", "Макby"
//		query = "Mak.by"; // 21 - 16 + 1 + 3 + 1 poi type [Brand]
//		query = "Mak By"; // 21 - 16 + 1 + 3 + 1 poi type
//		query = "Мак бай"; // 18 - 16 + 1 (brand) + 1 poi type [only 6 by name]
//		query = "Мак by";  // mix - 18 - 16 + 1 (brand) + 1 poi type [only 6 by name]
//		query = "MakBy"; // 18 - 16 + 1 + 1 poi type
//		query = "mcdonald's"; // 18 all synonym


//		pattern ="usa_wilkes-barre.obf";
//		pattern ="Us_penn";
//		query = "226 Wilkes-Barre Township Boulevard Wilkes-Barre";
//		query = "226 Wilkes-Barre Township Boulevard ";// 116894954
		
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
		MapPoiTypes poiTypes = MapPoiTypes.getDefault();
		poiTypes.setPoiTranslator(new TestPoiTranslator());
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes);
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
		SpatialTextSearchSettings settings = SpatialTextSearchSettings.defaultSettings();
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
				MapObject unitedObject = s.unitedObject.getSyntheticAmenity();
				String out = s.toString();
				if (unitedObject != null) {
					out += " United:" + unitedObject.toString();
				}
				System.out.println(out);
			}
		}
	}
	
	private static class TestPoiTranslator implements PoiTranslator {
		
		@Override
		public String getTranslation(String keyName) {
			if (keyName.equals("hotel")) {
				return "отель";
			}
			return null;
		}
		
		@Override
		public String getTranslation(AbstractPoiType type) {
			return getTranslation(type.getKeyName());
		}
		
		@Override
		public String getSynonyms(String keyName) {
			if (keyName.equals("hotel")) {
				return "отель;готель;гатэль";
			} else if (keyName.equals("cafe")) {
				return "кафе";
			} else if (keyName.equals("restaurant")) {
				return "ресторан";
			}
			return null;
		}
		
		@Override
		public String getSynonyms(AbstractPoiType type) {
			return getSynonyms(type.getKeyName());
		}
		
		@Override
		public String getEnTranslation(String keyName) {
			return null;
		}
		
		@Override
		public String getEnTranslation(AbstractPoiType type) {
			return null;
		}
		
		@Override
		public String getAllLanguagesTranslationSuffix() {
			return "";
		}
	}
	
}
