package net.osmand.data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;


public class Postcode {
	private final static Log log = PlatformUtil.getLog(Postcode.class);

	private final static Map<String, List<String>> rules = new TreeMap<String, List<String>>();

	//	© CC BY 3.0 2016 GeoNames.org
	//	with adaptations
	static {
		rules.put("Algeria",                                     Arrays.asList("(?i)(?:DZ-?)?(\\d{5})",                         "$1"));
		rules.put("Andorra",                                     Arrays.asList("(?i)(?:AD-?)?(\\d{3})",                         "$1"));
		rules.put("Argentina",                                   Arrays.asList("(?i)(?:AR-?)?([A-Z]\\d{4}[A-Z]{3}|\\d{4})",     "$1"));
		rules.put("Armenia",                                     Arrays.asList("(?i)(?:AM-?)?(\\d{6})",                         "$1"));
		rules.put("Australia-oceania",                           Arrays.asList("(?i)(?:AU-?)?(\\d{4})",                         "$1"));
		rules.put("Austria",                                     Arrays.asList("(?i)(?:AT-?)?(\\d{4})",                         "$1"));
		rules.put("Azerbaijan",                                  Arrays.asList("(?i)(?:AZ-?)?(\\d{4})",                         "$1"));
		rules.put("Bahrain",                                     Arrays.asList("(?i)(?:BH-?)?(\\d{3}\\d?)",                     "$1"));
		rules.put("Bangladesh",                                  Arrays.asList("(?i)(?:BD-?)?(\\d{4})",                         "$1"));
		rules.put("Barbados",                                    Arrays.asList("(?i)(?:BB-?)?(\\d{5})",                         "$1"));
		rules.put("Belarus",                                     Arrays.asList("(?i)(?:BY-?)?(\\d{6})",                         "$1"));
		rules.put("Belgium",                                     Arrays.asList("(?i)(?:BE-?)?(\\d{4})",                         "$1"));
		rules.put("Bermuda",                                     Arrays.asList("(?i)(?:BM-?)?([A-Z]{2})\\W*(\\d{2})",           "$1$2"));
		rules.put("Bosnia-herzegovina",                          Arrays.asList("(?i)(?:BA-?)?(\\d{5})",                         "$1"));
		rules.put("Brazil",                                      Arrays.asList("(?i)(?:BR-?)?(\\d{5})\\W*(\\d{3})",             "$1-$2"));
		rules.put("Brunei",                                      Arrays.asList("(?i)(?:BN-?)?([A-Z]{2})\\W*(\\d{4})",           "$1$2"));
		rules.put("Bulgaria",                                    Arrays.asList("(?i)(?:BG-?)?(\\d{4})",                         "$1"));
		rules.put("Cambodia",                                    Arrays.asList("(?i)(?:KH-?)?(\\d{5})",                         "$1"));
		rules.put("Canada",                                      Arrays.asList("(?i)(?:CA-?)?([ABCEGHJKLMNPRSTVXY]\\d[ABCEGHJKLMNPRSTVWXYZ])\\W*(\\d[ABCEGHJKLMNPRSTVWXYZ]\\d)$", "$1 $2"));
		rules.put("Cape-verde",                                  Arrays.asList("(?i)(?:CV-?)?(\\d{4})",                         "$1"));
		rules.put("Chile",                                       Arrays.asList("(?i)(?:CL-?)?(\\d{7})",                         "$1"));
		rules.put("China",                                       Arrays.asList("(?i)(?:CN-?)?(\\d{6})",                         "$1"));
		rules.put("Christmas-island",                            Arrays.asList("(?i)(?:CX-?)?(\\d{4})",                         "$1"));
		rules.put("Costa-rica",                                  Arrays.asList("(?i)(?:CR-?)?(\\d{4})",                         "$1"));
		rules.put("Croatia",                                     Arrays.asList("(?i)(?:HR-?)?(\\d{5})",                         "$1"));
		rules.put("Cuba",                                        Arrays.asList("(?i)(?:C[PU]-?)?(\\d{5})",                      "$1"));
		rules.put("Cyprus",                                      Arrays.asList("(?i)(?:CY-?)?(\\d{4})",                         "$1"));
		rules.put("Czech-republic",                              Arrays.asList("(?i)(?:CZ-?)?(\\d{5})",                         "$1"));
		rules.put("Denmark",                                     Arrays.asList("(?i)(?:DK-?)?(\\d{4})",                         "$1"));
		rules.put("Dominican-republic",                          Arrays.asList("(?i)(?:DO-?)?(\\d{5})",                         "$1"));
		rules.put("Ecuador",                                     Arrays.asList("(?i)(?:EC-?)?(\\d{6})",                         "$1"));
		rules.put("Egypt",                                       Arrays.asList("(?i)(?:EG-?)?(\\d{5})",                         "$1"));
		rules.put("El-salvador",                                 Arrays.asList("(?i)(?:SV-?)?(\\d{4})",                         "$1"));
		rules.put("Estonia",                                     Arrays.asList("(?i)(?:EE-?)?(\\d{5})",                         "$1"));
		rules.put("Ethiopia",                                    Arrays.asList("(?i)(?:ET-?)?(\\d{4})",                         "$1"));
		rules.put("Faroe-islands",                               Arrays.asList("(?i)(?:FO-?)?(\\d{3})",                         "$1"));
		rules.put("Finland",                                     Arrays.asList("(?i)(?:FI-?)?(\\d{5})",                         "$1"));
		rules.put("France",                                      Arrays.asList("(?i)(?:FR-?)?(\\d{5})",                         "$1"));
		rules.put("French-guiana",                               Arrays.asList("(?i)(?:GF-?)?((97|98)3\\d{2})",                 "$1"));
		rules.put("French-southern-and-antarctic-lands",         Arrays.asList("(?i)(?:PF-?)?((97|98)7\\d{2})",                 "$1"));
		rules.put("GB",                                          Arrays.asList("(?i)(?:UK-?)?([A-Z]{1,2}[0-9]{1,2}[A-Z]?)\\W*([0-9][A-Z]{2})", "$1 $2"));
		rules.put("Georgia",                                     Arrays.asList("(?i)(?:GE-?)?(\\d{4})",                         "$1"));
		rules.put("Germany",                                     Arrays.asList("(?i)(?:DE-?)?(\\d{5})",                         "$1"));
		rules.put("Greece",                                      Arrays.asList("(?i)(?:GR-?)?(\\d{5})",                         "$1"));
		rules.put("Greenland",                                   Arrays.asList("(?i)(?:GL-?)?(\\d{4})",                         "$1"));
		rules.put("Guadeloupe",                                  Arrays.asList("(?i)(?:GP-?)?((97|98)\\d{3})",                  "$1"));
		rules.put("Guatemala",                                   Arrays.asList("(?i)(?:GT-?)?(\\d{5})",                         "$1"));
		rules.put("Guinea-bissau",                               Arrays.asList("(?i)(?:GW-?)?(\\d{4})",                         "$1"));
		rules.put("Haiti",                                       Arrays.asList("(?i)(?:HT-?)?(\\d{4})",                         "$1"));
		rules.put("Honduras",                                    Arrays.asList("(?i)(?:HN-?)?([A-Z]{2})\\W*(\\d{4}))",          "$1$2"));
		rules.put("Hungary",                                     Arrays.asList("(?i)(?:HU-?)?(\\d{4})",                         "$1"));
		rules.put("Iceland",                                     Arrays.asList("(?i)(?:IS-?)?(\\d{3})",                         "$1"));
		rules.put("India",                                       Arrays.asList("(?i)(?:IN-?)?(\\d{6})",                         "$1"));
		rules.put("Indonesia",                                   Arrays.asList("(?i)(?:ID-?)?(\\d{5})",                         "$1"));
		rules.put("Iran",                                        Arrays.asList("(?i)(?:IR-?)?(\\d{10})",                        "$1"));
		rules.put("Iraq",                                        Arrays.asList("(?i)(?:IQ-?)?(\\d{5})",                         "$1"));
//		rules.	put("Ireland",                                     Arrays.asList("(?i)(?:IE-?)?([A-Z]{3}[A-Z]{4})",               "$1"));  // It's complicated
		rules.put("Israel",                                      Arrays.asList("(?i)(?:IL-?)?(\\d{5})",                         "$1"));
		rules.put("Italy",                                       Arrays.asList("(?i)(?:IT-?)?(\\d{5})",                         "$1"));
		rules.put("Japan",                                       Arrays.asList("(?i)(?:JP-?)?(\\d{7})",                         "$1"));
		rules.put("Jordan",                                      Arrays.asList("(?i)(?:JO-?)?(\\d{5})",                         "$1"));
		rules.put("Kazakhstan",                                  Arrays.asList("(?i)(?:KZ-?)?(\\d{6})",                         "$1"));
		rules.put("Kenya",                                       Arrays.asList("(?i)(?:KE-?)?(\\d{5})",                         "$1"));
		rules.put("Kuwait",                                      Arrays.asList("(?i)(?:KW-?)?(\\d{5})",                         "$1"));
		rules.put("Kyrgyzstan",                                  Arrays.asList("(?i)(?:KG-?)?(\\d{6})",                         "$1"));
		rules.put("Laos",                                        Arrays.asList("(?i)(?:LA-?)?(\\d{5})",                         "$1"));
		rules.put("Latvia",                                      Arrays.asList("(?i)(?:LV-?)?(\\d{4})",                         "$1"));
		rules.put("Lebanon",                                     Arrays.asList("(?i)(?:LB-?)?(\\d{4}(\\d{4})?)",                "$1"));
		rules.put("Lesotho",                                     Arrays.asList("(?i)(?:LS-?)?(\\d{3})",                         "$1"));
		rules.put("Liberia",                                     Arrays.asList("(?i)(?:LR-?)?(\\d{4})",                         "$1"));
		rules.put("Liechtenstein",                               Arrays.asList("(?i)(?:LI-?)?(\\d{4})",                         "$1"));
		rules.put("Lithuania",                                   Arrays.asList("(?i)(?:LT-?)?(\\d{5})",                         "$1"));
		rules.put("Luxembourg",                                  Arrays.asList("(?i)(?:LU-?)?(\\d{4})",                         "$1"));
		rules.put("Macedonia",                                   Arrays.asList("(?i)(?:MK-?)?(\\d{4})",                         "$1"));
		rules.put("Madagascar",                                  Arrays.asList("(?i)(?:MG-?)?(\\d{3})",                         "$1"));
		rules.put("Malaysia",                                    Arrays.asList("(?i)(?:MY-?)?(\\d{5})",                         "$1"));
		rules.put("Maldives",                                    Arrays.asList("(?i)(?:MV-?)?(\\d{5})",                         "$1"));
		rules.put("Malta",                                       Arrays.asList("(?i)(?:MT-?)?([A-Z]{3})\\W*(\\d{4})",           "$1 $2"));
		rules.put("Martinique",                                  Arrays.asList("(?i)(?:MQ-?)?(\\d{5})",                         "$1"));
		rules.put("Mayotte",                                     Arrays.asList("(?i)(?:YT-?)?(\\d{5})",                         "$1"));
		rules.put("Mexico",                                      Arrays.asList("(?i)(?:MX-?)?(\\d{5})",                         "$1"));
		rules.put("Moldova",                                     Arrays.asList("(?i)(?:MD-?)?(\\d{4})",                         "$1"));
		rules.put("Monaco",                                      Arrays.asList("(?i)(?:MC-?)?(\\d{5})",                         "$1"));
		rules.put("Mongolia",                                    Arrays.asList("(?i)(?:MN-?)?(\\d{6})",                         "$1"));
		rules.put("Montenegro",                                  Arrays.asList("(?i)(?:ME-?)?(\\d{5})",                         "$1"));
		rules.put("Morocco",                                     Arrays.asList("(?i)(?:MA-?)?(\\d{5})",                         "$1"));
		rules.put("Mozambique",                                  Arrays.asList("(?i)(?:MZ-?)?(\\d{4})",                         "$1"));
		rules.put("Myanmar",                                     Arrays.asList("(?i)(?:MM-?)?(\\d{5})",                         "$1"));
		rules.put("Nepal",                                       Arrays.asList("(?i)(?:NP-?)?(\\d{5})",                         "$1"));
		rules.put("Netherlands",                                 Arrays.asList("(?i)(?:NL-?)?(\\d{4})\\W*([A-Z]{2})",           "$1$2"));
		rules.put("New-zealand",                                 Arrays.asList("(?i)(?:NZ-?)?(\\d{4})",                         "$1"));
		rules.put("Nicaragua",                                   Arrays.asList("(?i)(?:NI-?)?(\\d{7})",                         "$1"));
		rules.put("Niger",                                       Arrays.asList("(?i)(?:NE-?)?(\\d{4})",                         "$1"));
		rules.put("Nigeria",                                     Arrays.asList("(?i)(?:NG-?)?(\\d{6})",                         "$1"));
		rules.put("North-korea",                                 Arrays.asList("(?i)(?:KP-?)?(\\d{6})",                         "$1"));
		rules.put("Norway",                                      Arrays.asList("(?i)(?:NO-?)?(\\d{4})",                         "$1"));
		rules.put("Oman",                                        Arrays.asList("(?i)(?:OM-?)?(\\d{3})",                         "$1"));
		rules.put("Pakistan",                                    Arrays.asList("(?i)(?:PK-?)?(\\d{5})",                         "$1"));
		rules.put("Papua-new-guinea",                            Arrays.asList("(?i)(?:PG-?)?(\\d{3})",                         "$1"));
		rules.put("Paraguay",                                    Arrays.asList("(?i)(?:PY-?)?(\\d{4})",                         "$1"));
		rules.put("Philippines",                                 Arrays.asList("(?i)(?:PH-?)?(\\d{4})",                         "$1"));
		rules.put("Poland",                                      Arrays.asList("(?i)(?:PL-?)?(\\d{5})",                         "$1"));
		rules.put("Portugal",                                    Arrays.asList("(?i)(?:PT-?)?(\\d{7})",                         "$1"));
		rules.put("Puerto-rico",                                 Arrays.asList("(?i)(?:PR-?)?(\\d{9})",                         "$1"));
		rules.put("Reunion",                                     Arrays.asList("(?i)(?:RE-?)?((97|98)(4|7|8)\\d{2})",           "$1"));
		rules.put("Romania",                                     Arrays.asList("(?i)(?:RO-?)?(\\d{6})",                         "$1"));
		rules.put("Russia",                                      Arrays.asList("(?i)(?:RU-?)?(\\d{6})",                         "$1"));
		rules.put("Saint-helena-ascension-and-tristan-da-cunha", Arrays.asList("(?i)(?:SH-?)?(STHL)\\W*(1ZZ)",                  "$1 $2"));
		rules.put("Saint-pierre-and-miquelon",                   Arrays.asList("(?i)(?:PM-?)?(97500)",                          "$1"));
		rules.put("San-marino",                                  Arrays.asList("(?i)(?:SM-?)?(4789\\d)",                        "$1"));
		rules.put("Saudi-arabia",                                Arrays.asList("(?i)(?:SA-?)?(\\d{5})",                         "$1"));
		rules.put("Senegal",                                     Arrays.asList("(?i)(?:SN-?)?(\\d{5})",                         "$1"));
		rules.put("Serbia",                                      Arrays.asList("(?i)(?:RS-?)?(\\d{6})",                         "$1"));
		rules.put("Singapore",                                   Arrays.asList("(?i)(?:SG-?)?(\\d{6})",                         "$1"));
		rules.put("Slovakia",                                    Arrays.asList("(?i)(?:SK-?)?(\\d{5})",                         "$1"));
		rules.put("Slovenia",                                    Arrays.asList("(?i)(?:SI-?)?(\\d{4})",                         "$1"));
		rules.put("Somalia",                                     Arrays.asList("(?i)(?:SO-?)?([A-Z]{2})\\W*(\\d{5})",           "$1$2"));
		rules.put("South-africa",                                Arrays.asList("(?i)(?:ZA-?)?(\\d{4})",                         "$1"));
		rules.put("South-korea",                                 Arrays.asList("(?i)(?:KR-?)?(?:SEOUL)?(\\d{3})\\W*(\\d{2,3})", "$1$2"));
		rules.put("Spain",                                       Arrays.asList("(?i)(?:ES-?)?(\\d{5})",                         "$1"));
		rules.put("Sri-lanka",                                   Arrays.asList("(?i)(?:LK-?)?(\\d{5})",                         "$1"));
		rules.put("Sudan",                                       Arrays.asList("(?i)(?:SD-?)?(\\d{5})",                         "$1"));
		rules.put("Swaziland",                                   Arrays.asList("(?i)(?:SZ-?)?([A-Z]\\d{3})",                    "$1"));
		rules.put("Sweden",                                      Arrays.asList("(?i)(?:SE-?)?(\\d{5})",                         "$1"));
		rules.put("Switzerland",                                 Arrays.asList("(?i)(?:CH-?)?(\\d{4})",                         "$1"));
		rules.put("Taiwan",                                      Arrays.asList("(?i)(?:TW-?)?(\\d{5})",                         "$1"));
		rules.put("Tajikistan",                                  Arrays.asList("(?i)(?:TJ-?)?(\\d{6})",                         "$1"));
		rules.put("Thailand",                                    Arrays.asList("(?i)(?:TH-?)?(\\d{5})",                         "$1"));
		rules.put("Tunisia",                                     Arrays.asList("(?i)(?:TN-?)?(\\d{4})",                         "$1"));
		rules.put("Turkey",                                      Arrays.asList("(?i)(?:TR-?)?(\\d{5})",                         "$1"));
		rules.put("Turkmenistan",                                Arrays.asList("(?i)(?:TM-?)?(\\d{6})",                         "$1"));
		rules.put("Turks-and-caicos-islands",                    Arrays.asList("(?i)(?:TC-?)?(TKCA)\\W*(1ZZ)",                  "$1 $2"));
		rules.put("Virgin-islands-us",                           Arrays.asList("(?i)(?:VI-?)?(\\d{5})\\W*(-\\d{4})?",           "$1$2"));
		rules.put("Ukraine",                                     Arrays.asList("(?i)(?:UA-?)?(\\d{2})\\W*(\\d{3})",             "$1$2"));
		rules.put("Us",                                          Arrays.asList("(?i)(?:US-?)?(\\d{5})\\W*(-\\d{4})?",           "$1$2"));
		rules.put("Uruguay",                                     Arrays.asList("(?i)(?:UY-?)?(\\d{5})",                         "$1"));
		rules.put("Uzbekistan",                                  Arrays.asList("(?i)(?:UZ-?)?(\\d{6})",                         "$1"));
		rules.put("Venezuela",                                   Arrays.asList("(?i)(?:VE-?)?(\\d{4})",                         "$1"));
		rules.put("Vietnam",                                     Arrays.asList("(?i)(?:VN-?)?(\\d{6})",                         "$1"));
		rules.put("Zambia",                                      Arrays.asList("(?i)(?:ZM-?)?(\\d{5})",                         "$1"));
	}

	public static void main(String[] args) {
		System.out.println(normalize("1101 DL",  "Netherlands"));
		System.out.println(normalize("1101-DL",  "Netherlands"));
		System.out.println(normalize("b288qp",   "United Kingdom"));
		System.out.println(normalize("GIR 0AA",  "United Kingdom"));
		System.out.println(normalize("IV21 2LR", "United Kingdom"));
	}

	private static boolean isCountryKnown(String country) {
		return rules.containsKey(country);
	}

	private static Pattern getPattern(String country) {
		return Pattern.compile(rules.get(country).get(0));
	}

	private static Matcher getMatcher(String postcode, String country) {
		return isCountryKnown(country) ? getPattern(country).matcher(postcode) : null;
	}

	public static String normalize(String postcode, String country) {
		postcode = postcode.toUpperCase();
		String result = postcode;
		if (isCountryKnown(country)) {
			String replacement = rules.get(country).get(1);
			Matcher matcher = getMatcher(postcode, country);
			if (matcher != null) {
				String res = matcher.replaceAll(replacement);
				if (res != null) {
					result = res.replaceAll("null", "");
					if (!result.equals(postcode)) {
						log.info("Normalize " + country + "'s postcode: " + postcode + " -> " + result);
					}
					if (!matcher.matches()) {
						log.info("Not matches " + country + "'s postcode regex: " + postcode);
					}
				}
			}
		}
		return result;
	}

	public static boolean looksLikePostcodeStart(String s, String country) {
		boolean result = false;
		if (isCountryKnown(country)) {
			Matcher matcher = getMatcher(s, country);
			result = matcher != null && matcher.find();
		}
		result = result || s.matches("(.+\\d+.*|.*\\d+.+)");
		return result;
	}

}
