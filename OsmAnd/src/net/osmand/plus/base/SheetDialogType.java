package net.osmand.plus.base;

import net.osmand.plus.R;

public enum SheetDialogType {
	
	TOP(R.style.Animations_PopUpMenu_Top), 
	BOTTOM(R.style.Animations_PopUpMenu_Bottom);

	SheetDialogType(int animationStyleResId) {
		this.animationStyleResId = animationStyleResId;
	}

	private int animationStyleResId;

	public int getAnimationStyleResId() {
		return animationStyleResId;
	}
}
