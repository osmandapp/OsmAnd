package net.osmand.aidlapi.contextmenu;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class UpdateContextMenuButtonsParams extends AidlParams {

	private ContextMenuButtonsParams buttonsParams;

	public UpdateContextMenuButtonsParams(ContextMenuButtonsParams widget) {
		this.buttonsParams = widget;
	}

	public UpdateContextMenuButtonsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<UpdateContextMenuButtonsParams> CREATOR = new Creator<UpdateContextMenuButtonsParams>() {
		@Override
		public UpdateContextMenuButtonsParams createFromParcel(Parcel in) {
			return new UpdateContextMenuButtonsParams(in);
		}

		@Override
		public UpdateContextMenuButtonsParams[] newArray(int size) {
			return new UpdateContextMenuButtonsParams[size];
		}
	};

	public ContextMenuButtonsParams getContextMenuButtonsParams() {
		return buttonsParams;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("buttonsParams", buttonsParams);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(ContextMenuButtonsParams.class.getClassLoader());
		buttonsParams = bundle.getParcelable("buttonsParams");
	}
}