package net.osmand.aidl.contextmenu;

import android.os.Parcel;
import android.os.Parcelable;

public class AContextMenuButton implements Parcelable {

	private int buttonId;

	private String leftTextCaption;
	private String rightTextCaption;
	private String leftIconName;
	private String rightIconName;

	private boolean needColorizeIcon;
	private boolean enabled;

	public AContextMenuButton(int buttonId, String leftTextCaption, String rightTextCaption, String leftIconName, String rightIconName, boolean needColorizeIcon, boolean enabled) {
		this.buttonId = buttonId;
		this.leftTextCaption = leftTextCaption;
		this.rightTextCaption = rightTextCaption;
		this.leftIconName = leftIconName;
		this.rightIconName = rightIconName;
		this.needColorizeIcon = needColorizeIcon;
		this.enabled = enabled;
	}

	protected AContextMenuButton(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AContextMenuButton> CREATOR = new Creator<AContextMenuButton>() {
		@Override
		public AContextMenuButton createFromParcel(Parcel in) {
			return new AContextMenuButton(in);
		}

		@Override
		public AContextMenuButton[] newArray(int size) {
			return new AContextMenuButton[size];
		}
	};

	public int getButtonId() {
		return buttonId;
	}

	public String getLeftTextCaption() {
		return leftTextCaption;
	}

	public String getRightTextCaption() {
		return rightTextCaption;
	}

	public String getLeftIconName() {
		return leftIconName;
	}

	public String getRightIconName() {
		return rightIconName;
	}

	public boolean isNeedColorizeIcon() {
		return needColorizeIcon;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public static Creator<AContextMenuButton> getCREATOR() {
		return CREATOR;
	}

	@Override
	public void writeToParcel(Parcel dest, int f) {
		dest.writeInt(buttonId);
		dest.writeString(leftTextCaption);
		dest.writeString(rightTextCaption);
		dest.writeString(leftIconName);
		dest.writeString(rightIconName);
		dest.writeInt(needColorizeIcon ? 1 : 0);
		dest.writeInt(enabled ? 1 : 0);
	}

	private void readFromParcel(Parcel in) {
		buttonId = in.readInt();
		leftTextCaption = in.readString();
		rightTextCaption = in.readString();
		leftIconName = in.readString();
		rightIconName = in.readString();
		needColorizeIcon = in.readInt() != 0;
		enabled = in.readInt() != 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}
}