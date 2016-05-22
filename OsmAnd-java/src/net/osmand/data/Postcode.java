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
		Map<String, List<String>> rules = new TreeMap<String, List<String>>() {{
			put("Austria",           Arrays.asList("(?i)(AT-)?(\\d{4})"                                       , "$2"));
			put("Australia",         Arrays.asList("(?i)(AU-)?(\\d{4})"                                       , "$2"));
			put("Australia-oceania", Arrays.asList("(?i)(AU-)?(\\d{4})"                                       , "$2"));
			put("Belgium",           Arrays.asList("(?i)(BG-)?(\\d{4})"                                       , "$2"));
			put("Brazil",            Arrays.asList("(?i)(BR-)?(\\d{5})\\W*(\\d{3})"                           , "$2-$3"));
			put("Canada",            Arrays.asList("(?i)(CA-)?([A-Z]\\d[A-Z])\\W*(\\d[A-Z]\\d)"               , "$2 $3"));
			put("China",             Arrays.asList("(?i)(CN-)?(\\d{6})"                                       , "$2"));
			put("Germany",           Arrays.asList("(?i)(DE-)?(\\d{5})"                                       , "$2"));
			put("Denmark",           Arrays.asList("(?i)(DK-)?(\\d{4})"                                       , "$2"));
			put("Spain",             Arrays.asList("(?i)(ES-)?(\\d{5})"                                       , "$2"));
			put("Finland",           Arrays.asList("(?i)(FI-)?(\\d{5})"                                       , "$2"));
			put("France",            Arrays.asList("(?i)(FR-)?(\\d{5})"                                       , "$2"));
			put("Croatia",           Arrays.asList("(?i)(HR-)?(\\d{5})"                                       , "$2"));
			put("Hungary",           Arrays.asList("(?i)(HU-)?(\\d{4})"                                       , "$2"));
//			put("Ireland",           Arrays.asList("(?i)(IE-)?(\\d{4})"                                       , "$2"));
			put("Netherlands",       Arrays.asList("(?i)(NL-)?(\\d{4})\\W*([A-Z]{2})"                         , "$2$3"));
			put("United Kingdom",    Arrays.asList("(?i)(UK-)?([A-Z]{1,2}[0-9]{1,2}[A-Z]?)\\W*([0-9][A-Z]{2})", "$1 $2"));
		}};
		String result = postcode;
		if (rules.containsKey(country)) {
			Pattern pattern = Pattern.compile(rules.get(country).get(0));
			String replacement = rules.get(country).get(1);
			Matcher matcher = pattern.matcher(postcode);
			result = matcher.replaceAll(replacement);
			if (!result.equals(postcode)) {
				log.info("Normalize " + country + " code: " + postcode + " -> " + result);
			}
			if (!matcher.matches()) {
				log.info("Not matches " + country + " code: " + postcode);
			}
		}
		return result;
	}

}
