package net.osmand.aidl.contextmenu;

import android.os.Parcel;
import android.os.Parcelable;

public class UpdateContextMenuButtonsParams implements Parcelable {
	private ContextMenuButtonsParams buttonsParams;

	public UpdateContextMenuButtonsParams(ContextMenuButtonsParams widget) {
		this.buttonsParams = widget;
	}

	public UpdateContextMenuButtonsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<UpdateContextMenuButtonsParams> CREATOR = new
			Creator<UpdateContextMenuButtonsParams>() {
				public UpdateContextMenuButtonsParams createFromParcel(Parcel in) {
					return new UpdateContextMenuButtonsParams(in);
				}

				public UpdateContextMenuButtonsParams[] newArray(int size) {
					return new UpdateContextMenuButtonsParams[size];
				}
			};

	public ContextMenuButtonsParams getContextMenuButtonsParams() {
		return buttonsParams;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(buttonsParams, flags);
	}

	private void readFromParcel(Parcel in) {
		buttonsParams = in.readParcelable(ContextMenuButtonsParams.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
