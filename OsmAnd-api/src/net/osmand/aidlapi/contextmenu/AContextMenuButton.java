package net.osmand.aidlapi.contextmenu;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AContextMenuButton extends AidlParams {

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

	public boolean isTintIcon() {
		return needColorizeIcon;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("buttonId", buttonId);
		bundle.putString("leftTextCaption", leftTextCaption);
		bundle.putString("rightTextCaption", rightTextCaption);
		bundle.putString("leftIconName", leftIconName);
		bundle.putString("rightIconName", rightIconName);
		bundle.putBoolean("needColorizeIcon", needColorizeIcon);
		bundle.putBoolean("enabled", enabled);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		buttonId = bundle.getInt("buttonId");
		leftTextCaption = bundle.getString("leftTextCaption");
		rightTextCaption = bundle.getString("rightTextCaption");
		leftIconName = bundle.getString("leftIconName");
		rightIconName = bundle.getString("rightIconName");
		needColorizeIcon = bundle.getBoolean("needColorizeIcon");
		enabled = bundle.getBoolean("enabled");
	}
}