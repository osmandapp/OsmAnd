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

	public static void main(String[] args) {
		System.out.println(normalize("1101 DL",  "Netherlands"));
		System.out.println(normalize("1101-DL",  "Netherlands"));
		System.out.println(normalize("b288qp",   "United Kingdom"));
		System.out.println(normalize("GIR 0AA",  "United Kingdom"));
		System.out.println(normalize("IV21 2LR", "United Kingdom"));
	}

	public static String normalize(String postcode, String country) {
//		© CC BY 3.0 2016 GeoNames.org
//		with adaptations
		Map<String, List<String>> rules = new TreeMap<String, List<String>>() {{
			put("Algeria",                                     Arrays.asList("(?i)(?:DZ-?)?(\\d{5})",                         "$1"));
			put("Andorra",                                     Arrays.asList("(?i)(?:AD-?)?(\\d{3})",                         "$1"));
			put("Argentina",                                   Arrays.asList("(?i)(?:AR-?)?([A-Z]\\d{4}[A-Z]{3}|\\d{4})",     "$1"));
			put("Armenia",                                     Arrays.asList("(?i)(?:AM-?)?(\\d{6})",                         "$1"));
			put("Australia-oceania",                           Arrays.asList("(?i)(?:AU-?)?(\\d{4})",                         "$1"));
			put("Austria",                                     Arrays.asList("(?i)(?:AT-?)?(\\d{4})",                         "$1"));
			put("Azerbaijan",                                  Arrays.asList("(?i)(?:AZ-?)?(\\d{4})",                         "$1"));
			put("Bahrain",                                     Arrays.asList("(?i)(?:BH-?)?(\\d{3}\\d?)",                     "$1"));
			put("Bangladesh",                                  Arrays.asList("(?i)(?:BD-?)?(\\d{4})",                         "$1"));
			put("Barbados",                                    Arrays.asList("(?i)(?:BB-?)?(\\d{5})",                         "$1"));
			put("Belarus",                                     Arrays.asList("(?i)(?:BY-?)?(\\d{6})",                         "$1"));
			put("Belgium",                                     Arrays.asList("(?i)(?:BE-?)?(\\d{4})",                         "$1"));
			put("Bermuda",                                     Arrays.asList("(?i)(?:BM-?)?([A-Z]{2})\\W*(\\d{2})",           "$1$2"));
			put("Bosnia-herzegovina",                          Arrays.asList("(?i)(?:BA-?)?(\\d{5})",                         "$1"));
			put("Brazil",                                      Arrays.asList("(?i)(?:BR-?)?(\\d{5})\\W*(\\d{3})",             "$1-$2"));
			put("Brunei",                                      Arrays.asList("(?i)(?:BN-?)?([A-Z]{2})\\W*(\\d{4})",           "$1$2"));
			put("Bulgaria",                                    Arrays.asList("(?i)(?:BG-?)?(\\d{4})",                         "$1"));
			put("Cambodia",                                    Arrays.asList("(?i)(?:KH-?)?(\\d{5})",                         "$1"));
			put("Canada",                                      Arrays.asList("(?i)(?:CA-?)?([ABCEGHJKLMNPRSTVXY]\\d[ABCEGHJKLMNPRSTVWXYZ])\\W*(\\d[ABCEGHJKLMNPRSTVWXYZ]\\d)$", "$1 $2"));
			put("Cape-verde",                                  Arrays.asList("(?i)(?:CV-?)?(\\d{4})",                         "$1"));
			put("Chile",                                       Arrays.asList("(?i)(?:CL-?)?(\\d{7})",                         "$1"));
			put("China",                                       Arrays.asList("(?i)(?:CN-?)?(\\d{6})",                         "$1"));
			put("Christmas-island",                            Arrays.asList("(?i)(?:CX-?)?(\\d{4})",                         "$1"));
			put("Costa-rica",                                  Arrays.asList("(?i)(?:CR-?)?(\\d{4})",                         "$1"));
			put("Croatia",                                     Arrays.asList("(?i)(?:HR-?)?(\\d{5})",                         "$1"));
			put("Cuba",                                        Arrays.asList("(?i)(?:C[PU]-?)?(\\d{5})",                      "$1"));
			put("Cyprus",                                      Arrays.asList("(?i)(?:CY-?)?(\\d{4})",                         "$1"));
			put("Czech-republic",                              Arrays.asList("(?i)(?:CZ-?)?(\\d{5})",                         "$1"));
			put("Denmark",                                     Arrays.asList("(?i)(?:DK-?)?(\\d{4})",                         "$1"));
			put("Dominican-republic",                          Arrays.asList("(?i)(?:DO-?)?(\\d{5})",                         "$1"));
			put("Ecuador",                                     Arrays.asList("(?i)(?:EC-?)?(\\d{6})",                         "$1"));
			put("Egypt",                                       Arrays.asList("(?i)(?:EG-?)?(\\d{5})",                         "$1"));
			put("El-salvador",                                 Arrays.asList("(?i)(?:SV-?)?(\\d{4})",                         "$1"));
			put("Estonia",                                     Arrays.asList("(?i)(?:EE-?)?(\\d{5})",                         "$1"));
			put("Ethiopia",                                    Arrays.asList("(?i)(?:ET-?)?(\\d{4})",                         "$1"));
			put("Faroe-islands",                               Arrays.asList("(?i)(?:FO-?)?(\\d{3})",                         "$1"));
			put("Finland",                                     Arrays.asList("(?i)(?:FI-?)?(\\d{5})",                         "$1"));
			put("France",                                      Arrays.asList("(?i)(?:FR-?)?(\\d{5})",                         "$1"));
			put("French-guiana",                               Arrays.asList("(?i)(?:GF-?)?((97|98)3\\d{2})",                 "$1"));
			put("French-southern-and-antarctic-lands",         Arrays.asList("(?i)(?:PF-?)?((97|98)7\\d{2})",                 "$1"));
			put("GB",                                          Arrays.asList("(?i)(?:UK-?)?([A-Z]{1,2}[0-9]{1,2}[A-Z]?)\\W*([0-9][A-Z]{2})", "$1 $2"));
			put("Georgia",                                     Arrays.asList("(?i)(?:GE-?)?(\\d{4})",                         "$1"));
			put("Germany",                                     Arrays.asList("(?i)(?:DE-?)?(\\d{5})",                         "$1"));
			put("Greece",                                      Arrays.asList("(?i)(?:GR-?)?(\\d{5})",                         "$1"));
			put("Greenland",                                   Arrays.asList("(?i)(?:GL-?)?(\\d{4})",                         "$1"));
			put("Guadeloupe",                                  Arrays.asList("(?i)(?:GP-?)?((97|98)\\d{3})",                  "$1"));
			put("Guatemala",                                   Arrays.asList("(?i)(?:GT-?)?(\\d{5})",                         "$1"));
			put("Guinea-bissau",                               Arrays.asList("(?i)(?:GW-?)?(\\d{4})",                         "$1"));
			put("Haiti",                                       Arrays.asList("(?i)(?:HT-?)?(\\d{4})",                         "$1"));
			put("Honduras",                                    Arrays.asList("(?i)(?:HN-?)?([A-Z]{2})\\W*(\\d{4}))",          "$1$2"));
			put("Hungary",                                     Arrays.asList("(?i)(?:HU-?)?(\\d{4})",                         "$1"));
			put("Iceland",                                     Arrays.asList("(?i)(?:IS-?)?(\\d{3})",                         "$1"));
			put("India",                                       Arrays.asList("(?i)(?:IN-?)?(\\d{6})",                         "$1"));
			put("Indonesia",                                   Arrays.asList("(?i)(?:ID-?)?(\\d{5})",                         "$1"));
			put("Iran",                                        Arrays.asList("(?i)(?:IR-?)?(\\d{10})",                        "$1"));
			put("Iraq",                                        Arrays.asList("(?i)(?:IQ-?)?(\\d{5})",                         "$1"));
//			put("Ireland",                                     Arrays.asList("(?i)(?:IE-?)?([A-Z]{3}[A-Z]{4})",               "$1"));  // It's complicated
			put("Israel",                                      Arrays.asList("(?i)(?:IL-?)?(\\d{5})",                         "$1"));
			put("Italy",                                       Arrays.asList("(?i)(?:IT-?)?(\\d{5})",                         "$1"));
			put("Japan",                                       Arrays.asList("(?i)(?:JP-?)?(\\d{7})",                         "$1"));
			put("Jordan",                                      Arrays.asList("(?i)(?:JO-?)?(\\d{5})",                         "$1"));
			put("Kazakhstan",                                  Arrays.asList("(?i)(?:KZ-?)?(\\d{6})",                         "$1"));
			put("Kenya",                                       Arrays.asList("(?i)(?:KE-?)?(\\d{5})",                         "$1"));
			put("Kuwait",                                      Arrays.asList("(?i)(?:KW-?)?(\\d{5})",                         "$1"));
			put("Kyrgyzstan",                                  Arrays.asList("(?i)(?:KG-?)?(\\d{6})",                         "$1"));
			put("Laos",                                        Arrays.asList("(?i)(?:LA-?)?(\\d{5})",                         "$1"));
			put("Latvia",                                      Arrays.asList("(?i)(?:LV-?)?(\\d{4})",                         "$1"));
			put("Lebanon",                                     Arrays.asList("(?i)(?:LB-?)?(\\d{4}(\\d{4})?)",                "$1"));
			put("Lesotho",                                     Arrays.asList("(?i)(?:LS-?)?(\\d{3})",                         "$1"));
			put("Liberia",                                     Arrays.asList("(?i)(?:LR-?)?(\\d{4})",                         "$1"));
			put("Liechtenstein",                               Arrays.asList("(?i)(?:LI-?)?(\\d{4})",                         "$1"));
			put("Lithuania",                                   Arrays.asList("(?i)(?:LT-?)?(\\d{5})",                         "$1"));
			put("Luxembourg",                                  Arrays.asList("(?i)(?:LU-?)?(\\d{4})",                         "$1"));
			put("Macedonia",                                   Arrays.asList("(?i)(?:MK-?)?(\\d{4})",                         "$1"));
			put("Madagascar",                                  Arrays.asList("(?i)(?:MG-?)?(\\d{3})",                         "$1"));
			put("Malaysia",                                    Arrays.asList("(?i)(?:MY-?)?(\\d{5})",                         "$1"));
			put("Maldives",                                    Arrays.asList("(?i)(?:MV-?)?(\\d{5})",                         "$1"));
			put("Malta",                                       Arrays.asList("(?i)(?:MT-?)?([A-Z]{3})\\W*(\\d{4})",           "$1 $2"));
			put("Martinique",                                  Arrays.asList("(?i)(?:MQ-?)?(\\d{5})",                         "$1"));
			put("Mayotte",                                     Arrays.asList("(?i)(?:YT-?)?(\\d{5})",                         "$1"));
			put("Mexico",                                      Arrays.asList("(?i)(?:MX-?)?(\\d{5})",                         "$1"));
			put("Moldova",                                     Arrays.asList("(?i)(?:MD-?)?(\\d{4})",                         "$1"));
			put("Monaco",                                      Arrays.asList("(?i)(?:MC-?)?(\\d{5})",                         "$1"));
			put("Mongolia",                                    Arrays.asList("(?i)(?:MN-?)?(\\d{6})",                         "$1"));
			put("Montenegro",                                  Arrays.asList("(?i)(?:ME-?)?(\\d{5})",                         "$1"));
			put("Morocco",                                     Arrays.asList("(?i)(?:MA-?)?(\\d{5})",                         "$1"));
			put("Mozambique",                                  Arrays.asList("(?i)(?:MZ-?)?(\\d{4})",                         "$1"));
			put("Myanmar",                                     Arrays.asList("(?i)(?:MM-?)?(\\d{5})",                         "$1"));
			put("Nepal",                                       Arrays.asList("(?i)(?:NP-?)?(\\d{5})",                         "$1"));
			put("Netherlands",                                 Arrays.asList("(?i)(?:NL-?)?(\\d{4})\\W*([A-Z]{2})",           "$1$2"));
			put("New-zealand",                                 Arrays.asList("(?i)(?:NZ-?)?(\\d{4})",                         "$1"));
			put("Nicaragua",                                   Arrays.asList("(?i)(?:NI-?)?(\\d{7})",                         "$1"));
			put("Niger",                                       Arrays.asList("(?i)(?:NE-?)?(\\d{4})",                         "$1"));
			put("Nigeria",                                     Arrays.asList("(?i)(?:NG-?)?(\\d{6})",                         "$1"));
			put("North-korea",                                 Arrays.asList("(?i)(?:KP-?)?(\\d{6})",                         "$1"));
			put("Norway",                                      Arrays.asList("(?i)(?:NO-?)?(\\d{4})",                         "$1"));
			put("Oman",                                        Arrays.asList("(?i)(?:OM-?)?(\\d{3})",                         "$1"));
			put("Pakistan",                                    Arrays.asList("(?i)(?:PK-?)?(\\d{5})",                         "$1"));
			put("Papua-new-guinea",                            Arrays.asList("(?i)(?:PG-?)?(\\d{3})",                         "$1"));
			put("Paraguay",                                    Arrays.asList("(?i)(?:PY-?)?(\\d{4})",                         "$1"));
			put("Philippines",                                 Arrays.asList("(?i)(?:PH-?)?(\\d{4})",                         "$1"));
			put("Poland",                                      Arrays.asList("(?i)(?:PL-?)?(\\d{5})",                         "$1"));
			put("Portugal",                                    Arrays.asList("(?i)(?:PT-?)?(\\d{7})",                         "$1"));
			put("Puerto-rico",                                 Arrays.asList("(?i)(?:PR-?)?(\\d{9})",                         "$1"));
			put("Reunion",                                     Arrays.asList("(?i)(?:RE-?)?((97|98)(4|7|8)\\d{2})",           "$1"));
			put("Romania",                                     Arrays.asList("(?i)(?:RO-?)?(\\d{6})",                         "$1"));
			put("Russia",                                      Arrays.asList("(?i)(?:RU-?)?(\\d{6})",                         "$1"));
			put("Saint-helena-ascension-and-tristan-da-cunha", Arrays.asList("(?i)(?:SH-?)?(STHL)\\W*(1ZZ)",                  "$1 $2"));
			put("Saint-pierre-and-miquelon",                   Arrays.asList("(?i)(?:PM-?)?(97500)",                          "$1"));
			put("San-marino",                                  Arrays.asList("(?i)(?:SM-?)?(4789\\d)",                        "$1"));
			put("Saudi-arabia",                                Arrays.asList("(?i)(?:SA-?)?(\\d{5})",                         "$1"));
			put("Senegal",                                     Arrays.asList("(?i)(?:SN-?)?(\\d{5})",                         "$1"));
			put("Serbia",                                      Arrays.asList("(?i)(?:RS-?)?(\\d{6})",                         "$1"));
			put("Singapore",                                   Arrays.asList("(?i)(?:SG-?)?(\\d{6})",                         "$1"));
			put("Slovakia",                                    Arrays.asList("(?i)(?:SK-?)?(\\d{5})",                         "$1"));
			put("Slovenia",                                    Arrays.asList("(?i)(?:SI-?)?(\\d{4})",                         "$1"));
			put("Somalia",                                     Arrays.asList("(?i)(?:SO-?)?([A-Z]{2})\\W*(\\d{5})",           "$1$2"));
			put("South-africa",                                Arrays.asList("(?i)(?:ZA-?)?(\\d{4})",                         "$1"));
			put("South-korea",                                 Arrays.asList("(?i)(?:KR-?)?(?:SEOUL)?(\\d{3})\\W*(\\d{2,3})", "$1$2"));
			put("Spain",                                       Arrays.asList("(?i)(?:ES-?)?(\\d{5})",                         "$1"));
			put("Sri-lanka",                                   Arrays.asList("(?i)(?:LK-?)?(\\d{5})",                         "$1"));
			put("Sudan",                                       Arrays.asList("(?i)(?:SD-?)?(\\d{5})",                         "$1"));
			put("Swaziland",                                   Arrays.asList("(?i)(?:SZ-?)?([A-Z]\\d{3})",                    "$1"));
			put("Sweden",                                      Arrays.asList("(?i)(?:SE-?)?(\\d{5})",                         "$1"));
			put("Switzerland",                                 Arrays.asList("(?i)(?:CH-?)?(\\d{4})",                         "$1"));
			put("Taiwan",                                      Arrays.asList("(?i)(?:TW-?)?(\\d{5})",                         "$1"));
			put("Tajikistan",                                  Arrays.asList("(?i)(?:TJ-?)?(\\d{6})",                         "$1"));
			put("Thailand",                                    Arrays.asList("(?i)(?:TH-?)?(\\d{5})",                         "$1"));
			put("Tunisia",                                     Arrays.asList("(?i)(?:TN-?)?(\\d{4})",                         "$1"));
			put("Turkey",                                      Arrays.asList("(?i)(?:TR-?)?(\\d{5})",                         "$1"));
			put("Turkmenistan",                                Arrays.asList("(?i)(?:TM-?)?(\\d{6})",                         "$1"));
			put("Turks-and-caicos-islands",                    Arrays.asList("(?i)(?:TC-?)?(TKCA)\\W*(1ZZ)",                  "$1 $2"));
			put("Virgin-islands-us",                           Arrays.asList("(?i)(?:VI-?)?(\\d{5})\\W*(-\\d{4})?",           "$1$2"));
			put("Ukraine",                                     Arrays.asList("(?i)(?:UA-?)?(\\d{2})\\W*(\\d{3})",             "$1$2"));
			put("Us",                                          Arrays.asList("(?i)(?:US-?)?(\\d{5})\\W*(-\\d{4})?",           "$1$2"));
			put("Uruguay",                                     Arrays.asList("(?i)(?:UY-?)?(\\d{5})",                         "$1"));
			put("Uzbekistan",                                  Arrays.asList("(?i)(?:UZ-?)?(\\d{6})",                         "$1"));
			put("Venezuela",                                   Arrays.asList("(?i)(?:VE-?)?(\\d{4})",                         "$1"));
			put("Vietnam",                                     Arrays.asList("(?i)(?:VN-?)?(\\d{6})",                         "$1"));
			put("Zambia",                                      Arrays.asList("(?i)(?:ZM-?)?(\\d{5})",                         "$1"));
		}};
		postcode = postcode.toUpperCase();
		String result = postcode;
		if (rules.containsKey(country)) {
			Pattern pattern = Pattern.compile(rules.get(country).get(0));
			String replacement = rules.get(country).get(1);
			Matcher matcher = pattern.matcher(postcode);
			result = matcher.replaceAll(replacement);
			if (!result.equals(postcode)) {
				log.info("Normalize " + country + "'s postcode: " + postcode + " -> " + result);
			}
			if (!matcher.matches()) {
				log.info("Not matches " + country + "'s postcode regex: " + postcode);
			}
		}
		return result;
	}

}
