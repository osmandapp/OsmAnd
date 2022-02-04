package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.Locale;

public class GetTextParams extends AidlParams {

	private String key;
	private String value;
	private Locale locale;

	public GetTextParams(String key, Locale locale) {
		this.key = key;
		this.locale = locale;
	}

	public GetTextParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<GetTextParams> CREATOR = new Creator<GetTextParams>() {
		@Override
		public GetTextParams createFromParcel(Parcel in) {
			return new GetTextParams(in);
		}

		@Override
		public GetTextParams[] newArray(int size) {
			return new GetTextParams[size];
		}
	};

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("key", key);
		bundle.putString("value", value);
		bundle.putSerializable("locale", locale);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		key = bundle.getString("key");
		value = bundle.getString("value");
		locale = (Locale) bundle.getSerializable("locale");
	}
}