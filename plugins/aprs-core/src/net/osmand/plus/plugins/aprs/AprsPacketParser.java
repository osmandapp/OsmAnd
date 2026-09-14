package net.osmand.plus.plugins.aprs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses AX.25 frame bodies (address + control + info, FCS already verified) into {@link AprsStation}.
 */
public class AprsPacketParser {

	private static final Log LOG = PlatformUtil.getLog(AprsPacketParser.class);
	private static final Pattern QRV_PATTERN = Pattern.compile("(?i)\\bQRV\\s+([\\d.]+)");

	@Nullable
	public AprsStation parseAx25Frame(@NonNull byte[] frame) {
		int pos = 0;
		String sourceCall = null;
		while (pos + 7 <= frame.length) {
			byte[] chunk = new byte[7];
			System.arraycopy(frame, pos, chunk, 0, 7);
			pos += 7;
			String call = decodeCall(chunk);
			sourceCall = call;
			if ((chunk[6] & 0x01) != 0) {
				break;
			}
		}
		if (sourceCall == null || pos >= frame.length) {
			return null;
		}
		int control = frame[pos++] & 0xFF;
		if (control == 0x03 && pos < frame.length) {
			pos++; // PID
		}
		if (pos >= frame.length) {
			return null;
		}
		byte[] info = new byte[frame.length - pos];
		System.arraycopy(frame, pos, info, 0, info.length);
		String payload = new String(info, StandardCharsets.US_ASCII);
		return parseAprsPayload(sourceCall, payload);
	}

	@Nullable
	public AprsStation parseAprsPayload(@NonNull String sourceCall, @NonNull String payload) {
		if (payload.isEmpty()) {
			return null;
		}
		AprsStation station = new AprsStation(sourceCall);
		station.setLastHeardMs(System.currentTimeMillis());
		char type = payload.charAt(0);

		switch (type) {
			case '!':
			case '=':
				parseUncompressedPosition(station, payload, type == '=');
				break;
			case '/':
			case '@':
				parseTimestampedPosition(station, payload);
				break;
			case ';':
				parseObjectOrItem(station, payload, true);
				break;
			case '>':
				parseStatus(station, payload);
				break;
			case ':':
				parseMessage(station, payload);
				return station;
			case '_':
				parseWeatherWithoutPosition(station, payload);
				break;
			default:
				if (payload.length() > 1 && (type == '>' || type == '<')) {
					parseStatus(station, payload);
				} else {
					return null;
				}
				break;
		}
		applyQrvDetection(station);
		if (station.hasPosition()) {
			LOG.info("APRS position decoded callsign=" + station.getCallsign()
					+ " lat=" + station.getLatitude()
					+ " lon=" + station.getLongitude()
					+ " course=" + station.getCourse());
		}
		return station.hasPosition() || station.getMessageText() != null
				|| station.isWeatherStation() ? station : null;
	}

	private void parseUncompressedPosition(@NonNull AprsStation station, @NonNull String p, boolean compressed) {
		int idx = 1;
		if (compressed) {
			if (p.length() < 14) {
				return;
			}
			parseCompressedLatLon(station, p.substring(1, 14));
			idx = 14;
			if (p.length() > idx) {
				station.setSymbolTable(p.charAt(idx++));
			}
			if (p.length() > idx) {
				station.setSymbolCode(p.charAt(idx++));
			}
		} else {
			if (p.length() < 20 || p.charAt(9) != '/') {
				return;
			}
			station.setLatitude(parseLat(p.substring(1, 9)));
			station.setLongitude(parseLon(p.substring(10, 19)));
			station.setSymbolTable(p.charAt(19));
			if (p.length() > 20) {
				station.setSymbolCode(p.charAt(20));
				idx = 21;
			} else {
				idx = 20;
			}
		}
		if (p.length() > idx) {
			parseCommentTail(station, p.substring(idx));
		}
	}

	private void parseTimestampedPosition(@NonNull AprsStation station, @NonNull String p) {
		if (p.length() < 26) {
			return;
		}
		// /hhmmssh lat lon sym table code
		int idx = 8; // skip /hhmmssh + lat start
		if (p.length() >= 18) {
			station.setLatitude(parseLat(p.substring(7, 15)));
			station.setLongitude(parseLon(p.substring(15, 24)));
			if (p.length() > 24) {
				station.setSymbolTable(p.charAt(24));
			}
			if (p.length() > 25) {
				station.setSymbolCode(p.charAt(25));
				idx = 26;
			}
		}
		if (p.length() > idx) {
			String tail = p.substring(idx);
			parseCourseSpeed(station, tail);
			parseCommentTail(station, tail.length() > 7 ? tail.substring(7) : "");
		}
	}

	private void parseObjectOrItem(@NonNull AprsStation station, @NonNull String p, boolean object) {
		if (p.length() < 27) {
			return;
		}
		int posStart = object ? 10 : 2;
		if (p.length() >= posStart + 17) {
			station.setLatitude(parseLat(p.substring(posStart, posStart + 8)));
			station.setLongitude(parseLon(p.substring(posStart + 8, posStart + 17)));
			if (p.length() > posStart + 17) {
				station.setSymbolTable(p.charAt(posStart + 17));
			}
			if (p.length() > posStart + 18) {
				station.setSymbolCode(p.charAt(posStart + 18));
				parseCommentTail(station, p.substring(posStart + 19));
			}
		}
	}

	private void parseStatus(@NonNull AprsStation station, @NonNull String p) {
		if (p.length() > 1) {
			station.setComment(p.substring(1));
		}
	}

	private void parseMessage(@NonNull AprsStation station, @NonNull String p) {
		if (p.length() < 12) {
			return;
		}
		// :addressee:message
		int end = p.indexOf(':', 1);
		if (end > 1 && p.length() > end + 1) {
			station.setMessageText(p.substring(end + 1).trim());
		}
	}

	private void parseWeatherWithoutPosition(@NonNull AprsStation station, @NonNull String p) {
		station.setWeatherStation(true);
		station.setWeather(parseWeatherFields(p));
	}

	private void parseCommentTail(@NonNull AprsStation station, @NonNull String tail) {
		String rest = tail;
		if (tail.length() >= 7 && Character.isDigit(tail.charAt(0))) {
			parseCourseSpeed(station, tail);
			rest = tail.length() > 7 ? tail.substring(7) : "";
		} else if (tail.startsWith("/") && tail.length() >= 8) {
			parseCourseSpeed(station, tail.substring(1));
			rest = tail.length() > 8 ? tail.substring(8) : "";
		} else if (tail.startsWith("T")) {
			station.setWeatherStation(true);
			station.setWeather(parseWeatherFields(tail));
			return;
		}
		if (rest.startsWith("/")) {
			rest = rest.substring(1);
		}
		if (rest.startsWith("A=")) {
			parseAltitude(station, "/A=" + rest.substring(2));
			int slash = rest.indexOf(' ', 2);
			rest = slash >= 0 ? rest.substring(slash + 1) : "";
		} else if (rest.startsWith("/A=")) {
			parseAltitude(station, rest);
			rest = rest.length() > 7 ? rest.substring(7) : "";
		}
		if (rest.contains("g") || rest.contains("t") || rest.contains("r")) {
			station.setWeatherStation(true);
			station.setWeather(parseWeatherFields(rest));
		}
		if (!rest.isEmpty()) {
			station.setComment(rest);
		}
	}

	private void parseCourseSpeed(@NonNull AprsStation station, @NonNull String s) {
		try {
			if (s.length() >= 7 && s.charAt(3) == '/') {
				station.setCourse(Integer.parseInt(s.substring(0, 3)));
				station.setSpeed(Integer.parseInt(s.substring(4, 7)));
			} else if (s.length() >= 6) {
				station.setCourse(Integer.parseInt(s.substring(0, 3)));
				station.setSpeed(Integer.parseInt(s.substring(3, 6)));
			}
		} catch (NumberFormatException ignored) {
		}
	}

	private void parseAltitude(@NonNull AprsStation station, @NonNull String s) {
		if (s.startsWith("/A=") && s.length() >= 7) {
			try {
				station.setAltitude(Integer.parseInt(s.substring(4, 10)));
			} catch (NumberFormatException ignored) {
			}
		}
	}

	@NonNull
	private AprsWeather parseWeatherFields(@NonNull String s) {
		AprsWeather w = new AprsWeather();
		for (int i = 0; i < s.length() - 1; i++) {
			char id = s.charAt(i);
			int j = i + 1;
			while (j < s.length() && s.charAt(j) != ' ' && !Character.isLetter(s.charAt(j)) && j < i + 8) {
				j++;
			}
			String val = s.substring(i + 1, Math.min(j, s.length())).replaceAll("[^0-9.\\-]", "");
			if (val.isEmpty()) {
				continue;
			}
			try {
				switch (id) {
					case 'c', 'C' -> w.windDirection = Integer.parseInt(val.replaceAll("\\D", ""));
					case 's', 'S' -> w.windSpeed = Integer.parseInt(val.replaceAll("\\D", ""));
					case 'g', 'G' -> w.windGust = Integer.parseInt(val.replaceAll("\\D", ""));
					case 't', 'T' -> w.temperatureF = Float.parseFloat(val);
					case 'r', 'R' -> {
						if (w.rainfallHour < 0 || Float.isNaN(w.rainfallHour)) {
							w.rainfallHour = Float.parseFloat(val);
						} else {
							w.rainfall24h = Float.parseFloat(val);
						}
					}
					case 'b', 'B' -> w.barometerMbar = Float.parseFloat(val);
					case 'h', 'H' -> w.humidity = Integer.parseInt(val.replaceAll("\\D", ""));
					default -> { }
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return w;
	}

	private void parseCompressedLatLon(@NonNull AprsStation station, @NonNull String c) {
		if (c.length() < 13) {
			return;
		}
		try {
			double lat = 90.0 - decodeBase91(c.charAt(0), c.charAt(1), c.charAt(2)) / 380926.0;
			double lon = -180.0 + decodeBase91(c.charAt(3), c.charAt(4), c.charAt(5)) / 190463.0;
			station.setLatitude(lat);
			station.setLongitude(lon);
		} catch (Exception ignored) {
		}
	}

	private double decodeBase91(char a, char b, char c) {
		return (a - 33) * 91 * 91 + (b - 33) * 91 + (c - 33);
	}

	private double parseLat(@NonNull String s) {
		if (s.length() < 8) {
			return Double.NaN;
		}
		try {
			int deg = Integer.parseInt(s.substring(0, 2));
			double min = Double.parseDouble(s.substring(2, 7));
			double v = deg + min / 60.0;
			return s.charAt(7) == 'S' ? -v : v;
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	private double parseLon(@NonNull String s) {
		if (s.length() < 9) {
			return Double.NaN;
		}
		try {
			int deg = Integer.parseInt(s.substring(0, 3));
			double min = Double.parseDouble(s.substring(3, 8));
			double v = deg + min / 60.0;
			return s.charAt(8) == 'W' ? -v : v;
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	@NonNull
	private String decodeCall(@NonNull byte[] chunk) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			char c = (char) ((chunk[i] >> 1) & 0x7F);
			if (c != ' ') {
				sb.append(c);
			}
		}
		int ssid = (chunk[6] >> 1) & 0x0F;
		if (ssid > 0) {
			sb.append('-').append(ssid);
		}
		return sb.toString();
	}

	public void applyQrvDetection(@NonNull AprsStation station) {
		scanQrv(station, station.getComment());
		if (station.getMessageText() != null) {
			scanQrv(station, station.getMessageText());
		}
	}

	private void scanQrv(@NonNull AprsStation station, @Nullable String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		Matcher m = QRV_PATTERN.matcher(text);
		if (m.find()) {
			try {
				station.setQrvFrequencyMhz(Double.parseDouble(m.group(1)));
			} catch (NumberFormatException ignored) {
			}
		}
	}
}
