package net.osmand.plus.plugins.aprs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AprsStation {

	public static final int UNKNOWN = -1;

	private final String callsign;
	private char symbolTable = '/';
	private char symbolCode = '?';
	private double latitude = Double.NaN;
	private double longitude = Double.NaN;
	private int course = UNKNOWN;
	private int speed = UNKNOWN;
	private int altitude = UNKNOWN;
	private long lastHeardMs;
	private String comment = "";
	private String messageText;
	private boolean weatherStation;
	private AprsWeather weather;
	private double qrvFrequencyMhz;

	public AprsStation(@NonNull String callsign) {
		this.callsign = callsign;
		this.lastHeardMs = System.currentTimeMillis();
		this.qrvFrequencyMhz = 0.0;
	}

	@NonNull
	public String getCallsign() {
		return callsign;
	}

	public char getSymbolTable() {
		return symbolTable;
	}

	public void setSymbolTable(char symbolTable) {
		this.symbolTable = symbolTable;
	}

	public char getSymbolCode() {
		return symbolCode;
	}

	public void setSymbolCode(char symbolCode) {
		this.symbolCode = symbolCode;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int getCourse() {
		return course;
	}

	public void setCourse(int course) {
		this.course = course;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getAltitude() {
		return altitude;
	}

	public void setAltitude(int altitude) {
		this.altitude = altitude;
	}

	public long getLastHeardMs() {
		return lastHeardMs;
	}

	public void setLastHeardMs(long lastHeardMs) {
		this.lastHeardMs = lastHeardMs;
	}

	@NonNull
	public String getComment() {
		return comment != null ? comment : "";
	}

	public void setComment(@Nullable String comment) {
		this.comment = comment != null ? comment : "";
	}

	@Nullable
	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(@Nullable String messageText) {
		this.messageText = messageText;
	}

	public boolean isWeatherStation() {
		return weatherStation;
	}

	public void setWeatherStation(boolean weatherStation) {
		this.weatherStation = weatherStation;
	}

	@Nullable
	public AprsWeather getWeather() {
		return weather;
	}

	public void setWeather(@Nullable AprsWeather weather) {
		this.weather = weather;
	}

	public double getQrvFrequencyMhz() {
		return qrvFrequencyMhz;
	}

	public void setQrvFrequencyMhz(double qrvFrequencyMhz) {
		this.qrvFrequencyMhz = qrvFrequencyMhz;
	}

	public boolean hasPosition() {
		return !Double.isNaN(latitude) && !Double.isNaN(longitude);
	}

	public void updateFrom(@NonNull AprsStation other) {
		if (other.symbolTable != 0) {
			symbolTable = other.symbolTable;
		}
		if (other.symbolCode != 0 && other.symbolCode != '?') {
			symbolCode = other.symbolCode;
		}
		if (other.hasPosition()) {
			latitude = other.latitude;
			longitude = other.longitude;
		}
		if (other.course != UNKNOWN) {
			course = other.course;
		}
		if (other.speed != UNKNOWN) {
			speed = other.speed;
		}
		if (other.altitude != UNKNOWN) {
			altitude = other.altitude;
		}
		if (other.comment != null && !other.comment.isEmpty()) {
			comment = other.comment;
		}
		if (other.messageText != null) {
			messageText = other.messageText;
		}
		if (other.weatherStation) {
			weatherStation = true;
		}
		if (other.weather != null && other.weather.hasData()) {
			weather = other.weather;
		}
		if (other.qrvFrequencyMhz > 0) {
			qrvFrequencyMhz = other.qrvFrequencyMhz;
		}
		lastHeardMs = other.lastHeardMs;
	}
}
