package net.osmand.aidl;

import net.osmand.aidl.contextmenu.AContextMenuButton;

public class AidlContextMenuButtonWrapper {

	private int buttonId;

	private String leftTextCaption;
	private String rightTextCaption;
	private String leftIconName;
	private String rightIconName;

	private boolean needColorizeIcon;
	private boolean enabled;

	public AidlContextMenuButtonWrapper(AContextMenuButton aContextMenuButton) {
		buttonId = aContextMenuButton.getButtonId();
		leftTextCaption = aContextMenuButton.getLeftTextCaption();
		rightTextCaption = aContextMenuButton.getRightTextCaption();
		leftIconName = aContextMenuButton.getLeftIconName();
		rightIconName = aContextMenuButton.getRightIconName();
		needColorizeIcon = aContextMenuButton.isTintIcon();
		enabled = aContextMenuButton.isEnabled();
	}

	public AidlContextMenuButtonWrapper(net.osmand.aidlapi.contextmenu.AContextMenuButton aContextMenuButton) {
		buttonId = aContextMenuButton.getButtonId();
		leftTextCaption = aContextMenuButton.getLeftTextCaption();
		rightTextCaption = aContextMenuButton.getRightTextCaption();
		leftIconName = aContextMenuButton.getLeftIconName();
		rightIconName = aContextMenuButton.getRightIconName();
		needColorizeIcon = aContextMenuButton.isTintIcon();
		enabled = aContextMenuButton.isEnabled();
	}

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
}