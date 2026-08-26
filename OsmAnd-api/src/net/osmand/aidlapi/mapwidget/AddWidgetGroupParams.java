package net.osmand.aidlapi.mapwidget;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AddWidgetGroupParams extends AidlParams {

	private AWidgetGroup group;

	public AddWidgetGroupParams(AWidgetGroup group) {
		this.group = group;
	}

	public AddWidgetGroupParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AddWidgetGroupParams> CREATOR = new Creator<AddWidgetGroupParams>() {
		@Override
		public AddWidgetGroupParams createFromParcel(Parcel in) {
			return new AddWidgetGroupParams(in);
		}

		@Override
		public AddWidgetGroupParams[] newArray(int size) {
			return new AddWidgetGroupParams[size];
		}
	};

	public AWidgetGroup getGroup() {
		return group;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("group", group);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AWidgetGroup.class.getClassLoader());
		group = bundle.getParcelable("group");
	}
}
