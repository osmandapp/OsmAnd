package net.osmand.aidlapi.mapwidget;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveWidgetGroupParams extends AidlParams {

	private String id;
	private boolean removeWidgets;

	public RemoveWidgetGroupParams(String id) {
		this(id, false);
	}

	/**
	 * @param id            group id to remove.
	 * @param removeWidgets if true, the group's widgets are removed too;
	 *                      otherwise they are kept but become ungrouped.
	 */
	public RemoveWidgetGroupParams(String id, boolean removeWidgets) {
		this.id = id;
		this.removeWidgets = removeWidgets;
	}

	public RemoveWidgetGroupParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveWidgetGroupParams> CREATOR = new Creator<RemoveWidgetGroupParams>() {
		@Override
		public RemoveWidgetGroupParams createFromParcel(Parcel in) {
			return new RemoveWidgetGroupParams(in);
		}

		@Override
		public RemoveWidgetGroupParams[] newArray(int size) {
			return new RemoveWidgetGroupParams[size];
		}
	};

	public String getId() {
		return id;
	}

	public boolean isRemoveWidgets() {
		return removeWidgets;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
		bundle.putBoolean("removeWidgets", removeWidgets);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		removeWidgets = bundle.getBoolean("removeWidgets");
	}
}
