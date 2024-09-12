package net.osmand.util;

import com.google.openlocationcode.OpenLocationCode;
import com.google.openlocationcode.OpenLocationCode.CodeArea;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.UTMPoint;

import net.osmand.data.LatLon;

import java.util.ArrayList;
import java.util.List;

public class LocationParser {

	public static class ParsedOpenLocationCode {
		private final String text;
		private String code;
		private boolean full;
		private String placeName;

		private OpenLocationCode olc;
		private LatLon latLon;

		private ParsedOpenLocationCode(String text) {
			this.text = text;
			parse();
		}

		private void parse() {
			if (!Algorithms.isEmpty(text)) {
				String[] split = text.split(" ");
				if (split.length > 0) {
					code = split[0];
					try {
						olc = new OpenLocationCode(code);
						full = olc.isFull();
						if (full) {
							CodeArea codeArea = olc.decode();
							latLon = new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude());
						} else {
							if (split.length > 1) {
								placeName = text.substring(code.length() + 1);
							}
						}
					} catch (IllegalArgumentException e) {
						code = null;
					}
				}
			}
		}

		public LatLon recover(LatLon searchLocation) {
			if (olc != null) {
				CodeArea codeArea = olc.recover(searchLocation.getLatitude(), searchLocation.getLongitude()).decode();
				latLon = new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude());
			}
			return latLon;
		}

		boolean isValidCode() {
			return !Algorithms.isEmpty(code);
		}

		public String getText() {
			return text;
		}

		public String getCode() {
			return code;
		}

		public boolean isFull() {
			return full;
		}

		public String getPlaceName() {
			return placeName;
		}

		public LatLon getLatLon() {
			return latLon;
		}
	}

	public static boolean isValidOLC(String code) {
		return OpenLocationCode.isValidCode(code);
	}
	public static ParsedOpenLocationCode parseOpenLocationCode(String locPhrase) {
		ParsedOpenLocationCode parsedCode = new ParsedOpenLocationCode(locPhrase.trim());
		return !parsedCode.isValidCode() ? null : parsedCode;
	}

	public static LatLon parseLocation(String locPhrase) {
		locPhrase = locPhrase.trim();
		boolean valid = isValidLocPhrase(locPhrase);
		if (!valid) {
			String[] split = locPhrase.split(" ");
			if (split.length == 4 && split[1].contains(".") && split[3].contains(".")) {
				locPhrase = split[1] + " " + split[3];
				valid = isValidLocPhrase(locPhrase);
			}
		}
		if (!valid) {
			return null;
		}
		locPhrase = prepareLatLonWithDecimalCommas(locPhrase);
		List<Double> d = new ArrayList<>();
		List<Object> all = new ArrayList<>();
		List<String> strings = new ArrayList<>();
		splitObjects(locPhrase, d, all, strings);
		if (d.size() == 0) {
			return null;
		}
		// detect UTM
		if (all.size() == 4 && d.size() == 3 && all.get(1) instanceof String && ((String) all.get(1)).length() == 1) {
			char ch = all.get(1).toString().charAt(0);
			if (Character.isLetter(ch)) {
				UTMPoint upoint = new UTMPoint(d.get(2), d.get(1), d.get(0).intValue(), ch);
				LatLonPoint ll = upoint.toLatLonPoint();
				return validateAndCreateLatLon(ll.getLatitude(), ll.getLongitude());
			}
		}

		if (all.size() == 3 && d.size() == 2 && all.get(1) instanceof String && ((String) all.get(1)).length() == 1) {
			char ch = all.get(1).toString().charAt(0);
			String combined = strings.get(2);
			if (Character.isLetter(ch)) {
				try {
					String east = combined.substring(0, combined.length() / 2);
					String north = combined.substring(combined.length() / 2);
					UTMPoint upoint = new UTMPoint(Double.parseDouble(north), Double.parseDouble(east), d.get(0)
							.intValue(), ch);
					LatLonPoint ll = upoint.toLatLonPoint();
					return validateAndCreateLatLon(ll.getLatitude(), ll.getLongitude());
				} catch (NumberFormatException e) {
				}
			}
		}

		//detect MGRS
		if (all.size() >= 3 && (d.size() == 2 || d.size() == 3) && all.get(1) instanceof String) {
			try {
				MGRSPoint mgrsPoint = new MGRSPoint(locPhrase);
				LatLonPoint ll = mgrsPoint.toLatLonPoint();
				return validateAndCreateLatLon(ll.getLatitude(), ll.getLongitude());
			} catch (NumberFormatException e) {
				//do nothing
			}
		}
		// try to find split lat/lon position
		int jointNumbers = 0;
		int lastJoin = 0;
		int degSplit = -1;
		int degType = -1; // 0 - degree, 1 - minutes, 2 - seconds
		boolean finishDegSplit = false;
		int northSplit = -1;
		int eastSplit = -1;
		for (int i = 1; i < all.size(); i++ ) {
			if (all.get(i - 1) instanceof Double && all.get(i) instanceof Double) {
				jointNumbers ++;
				lastJoin = i;
			}
			if (all.get(i).equals("n") || all.get(i).equals("s") ||
					all.get(i).equals("N") || all.get(i).equals("S")) {
				northSplit = i + 1;
			}
			if (all.get(i).equals("e") || all.get(i).equals("w") ||
					all.get(i).equals("E") || all.get(i).equals("W")) {
				eastSplit = i;
			}
			int dg = -1;
			if (all.get(i).equals("°")) {
				dg = 0;
			} else if (all.get(i).equals("\'") || all.get(i).equals("′")) {
				dg = 1;
			} else if (all.get(i).equals("″") || all.get(i).equals("\"")) {
				dg = 2;
			}
			if (dg != -1) {
				if (!finishDegSplit) {
					if (degType < dg) {
						degSplit = i + 1;
						degType = dg;
					} else {
						finishDegSplit = true;
						degType = dg;
					}
				} else {
					if (degType < dg) {
						degType = dg;
					} else {
						// reject delimiter
						degSplit = -1;
					}
				}
			}
		}
		int split = -1;
		if (jointNumbers == 1) {
			split = lastJoin;
		}
		if (northSplit != -1 && northSplit < all.size() -1) {
			split = northSplit;
		} else if (eastSplit != -1 && eastSplit < all.size() -1) {
			split = eastSplit;
		} else if (degSplit != -1 && degSplit < all.size() -1) {
			split = degSplit;
		}

		if (split != -1) {
			double lat = parse1Coordinate(all, 0, split);
			double lon = parse1Coordinate(all, split, all.size());
			return validateAndCreateLatLon(lat, lon);
		}
		if (d.size() == 2) {
			return validateAndCreateLatLon(d.get(0), d.get(1));
		}
		// simple url case
		if (locPhrase.contains("://")) {
			double lat = 0;
			double lon = 0;
			boolean only2decimals = true;
			for (int i = 0; i < d.size(); i++) {
				if (d.get(i).doubleValue() != d.get(i).intValue()) {
					if (lat == 0) {
						lat = d.get(i);
					} else if (lon == 0) {
						lon = d.get(i);
					} else {
						only2decimals = false;
					}
				}
			}
			if (lat != 0 && lon != 0 && only2decimals) {
				return validateAndCreateLatLon(lat, lon);
			}
		}
		// split by equal number of digits
		if (d.size() > 2 && d.size() % 2 == 0) {
			int ind = d.size() / 2 + 1;
			int splitEq = -1;
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i) instanceof Double) {
					ind --;
				}
				if (ind == 0) {
					splitEq = i;
					break;
				}
			}
			if (splitEq != -1) {
				double lat = parse1Coordinate(all, 0, splitEq);
				double lon = parse1Coordinate(all, splitEq, all.size());
				return validateAndCreateLatLon(lat, lon);
			}
		}
		return null;
	}

	private static String prepareLatLonWithDecimalCommas(String ll) {
		final int DIGITS_BEFORE_COMMA = 1, DIGITS_AFTER_COMMA = 3; // see testCommaLatLonSearch
		for (int i = DIGITS_BEFORE_COMMA, first = -1; i < ll.length() - DIGITS_AFTER_COMMA; i++) {
			if (ll.charAt(i) == ',') {
				int before = 0, after = 0;
				for (int j = i - 1; j >= i - DIGITS_BEFORE_COMMA; j--) {
					if (Character.isDigit(ll.charAt(j))) {
						before++;
					}
				}
				for (int j = i + 1; j <= i + DIGITS_AFTER_COMMA && before >= DIGITS_BEFORE_COMMA; j++) {
					if (Character.isDigit(ll.charAt(j))) {
						after++;
					}
				}
				if (before >= DIGITS_BEFORE_COMMA && after >= DIGITS_AFTER_COMMA) {
					if (first != -1) {
						return ll.substring(0, first) + "." + ll.substring(first + 1, i) + "." + ll.substring(i + 1);
					} else {
						first = i; // first suitable comma found
					}
				}
			}
		}
		return ll;
	}

	private static LatLon validateAndCreateLatLon(double lat, double lon) {
		if (Math.abs(lat) <= 90 && Math.abs(lon) <= 180) {
			return new LatLon(lat, lon);
		}
		return null;
	}

	private static boolean isValidLocPhrase(String locPhrase) {
		if (!locPhrase.isEmpty()) {
			char ch = Character.toLowerCase(locPhrase.charAt(0));
			return ch == '-' || Character.isDigit(ch) || ch == 's' || ch == 'n' || locPhrase.contains("://");
		}
		return false;
	}

	public static double parse1Coordinate(List<Object> all, int begin, int end) {
		boolean neg = false;
		double d = 0;
		int type = 0; // degree - 0, minutes - 1, seconds = 2
		Double prevDouble = null;
		for (int i = begin; i <= end; i++) {
			Object o = i == end ? "" : all.get(i);
			if (o.equals("S") || o.equals("s") || o.equals("W") || o.equals("w") || o.equals(-0.0)) {
				neg = !neg;
			}
			if (prevDouble != null) {
				if (o.equals("°")) {
					type = 0;
				} else if (o.equals("′") /*o.equals("'")*/) {
					// ' can be used as delimiter ignore it
					type = 1;
				} else if (o.equals("\"") || o.equals("″")) {
					type = 2;
				}
				if (type == 0) {
					double ld = prevDouble.doubleValue();
					if (ld < 0) {
						ld = -ld;
						neg = true;
					}
					d += ld;
				} else if (type == 1) {
					d += prevDouble.doubleValue() / 60.f;
				} else /*if (type == 1) */ {
					d += prevDouble.doubleValue() / 3600.f;
				}
				type++;
			}
			if (o instanceof Double) {
				prevDouble = (Double) o;
			} else {
				prevDouble = null;
			}
		}
		if (neg) {
			d = -d;
		}
		return d;
	}

	public static void splitObjects(String s, List<Double> d, List<Object> all, List<String> strings) {
		splitObjects(s, d, all, strings, new boolean[]{false});
	}

	public static void splitObjects(String s, List<Double> d, List<Object> all, List<String> strings, boolean[] partial) {
		boolean digit = false;
		int word = -1;
		int firstNumeralIdx = -1;
		for (int i = 0; i <= s.length(); i++) {
			char ch = i == s.length() ? ' ' : s.charAt(i);
			boolean dg = Character.isDigit(ch);
			boolean nonwh = ch != ',' && ch != ' ' && ch != ';';
			if (ch == '.' || dg || ch == '-') {
				if (!digit) {
					if (word != -1) {
						all.add(s.substring(word, i));
						strings.add(s.substring(word, i));
					}
					digit = true;
					word = i;
				} else {
					if (word == -1) {
						word = i;
					}
					// if digit
					// continue
				}
			} else {
				if (digit) {
					if (word != -1) {
						try {
							double dl = Double.parseDouble(s.substring(word, i));
							d.add(dl);
							all.add(dl);
							if (firstNumeralIdx == -1) {
								firstNumeralIdx = all.size() - 1;
							}
							strings.add(s.substring(word, i));
							digit = false;
							word = -1;
						} catch (NumberFormatException e) {
						}
					}
				}
				if (nonwh) {
					if (!Character.isLetter(ch)) {
						if (word != -1) {
							all.add(s.substring(word, i));
							strings.add(s.substring(word, i));
						}
						all.add(s.substring(i, i + 1));
						strings.add(s.substring(i, i + 1));
						word = -1;
					} else if (word == -1) {
						word = i;
					}
				} else {
					if (word != -1) {
						all.add(s.substring(word, i));
						strings.add(s.substring(word, i));
					}
					word = -1;
				}
			}
		}

		partial[0] = false;
		if (firstNumeralIdx != -1) {
			int nextTokenIdx = firstNumeralIdx + 1;
			if (all.size() <= nextTokenIdx) {
				partial[0] = true;
			}
		}
	}
}
