package net.osmand.aidlapi.navdrawer;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

public class NavDrawerFooterParams extends AidlParams {

	@NonNull
	private String packageName;
	@Nullable
	private String intent;
	@Nullable
	private String appName;

	@NonNull
	public String getPackageName() {
		return packageName;
	}

	@Nullable
	public String getIntent() {
		return intent;
	}

	@Nullable
	public String getAppName() {
		return appName;
	}

	public NavDrawerFooterParams(@NonNull String packageName, @Nullable String intent,
	                             @Nullable String appName) {
		this.packageName = packageName;
		this.intent = intent;
		this.appName = appName;
	}

	protected NavDrawerFooterParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<NavDrawerFooterParams> CREATOR = new Creator<NavDrawerFooterParams>() {
		@Override
		public NavDrawerFooterParams createFromParcel(Parcel in) {
			return new NavDrawerFooterParams(in);
		}

		@Override
		public NavDrawerFooterParams[] newArray(int size) {
			return new NavDrawerFooterParams[size];
		}
	};

	@Override
	protected void readFromBundle(Bundle bundle) {
		packageName = bundle.getString("packageName", "");
		intent = bundle.getString("intent");
		appName = bundle.getString("appName");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("packageName", packageName);
		bundle.putString("intent", intent);
		bundle.putString("appName", appName);
	}
}