package net.osmand.plus.widgets.dialogbutton;

import static net.osmand.plus.widgets.dialogbutton.DialogButtonAttributes.INVALID_ID;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public enum DialogButtonType {

	PRIMARY(
			R.attr.dlg_btn_primary_text,
			R.attr.dlg_btn_primary,
			R.attr.dlg_btn_ripple_solid
	),
	PRIMARY_HARMFUL(
			R.attr.dlg_btn_primary_text,
			R.attr.dlg_btn_primary_harmful,
			R.attr.dlg_btn_ripple_solid
	),
	SECONDARY(
			R.attr.dlg_btn_secondary_text,
			R.attr.dlg_btn_secondary,
			R.attr.dlg_btn_ripple_solid
	),
	SECONDARY_HARMFUL(
			R.attr.dlg_btn_secondary_harmful_text,
			R.attr.dlg_btn_secondary,
			R.attr.dlg_btn_ripple_solid
	),
	SECONDARY_ACTIVE(
			R.attr.dlg_btn_secondary_text,
			R.attr.dlg_btn_transparent,
			R.attr.dlg_btn_ripple_solid
	),
	STROKED(
			R.attr.dlg_btn_secondary_text,
			R.attr.dlg_btn_stroked,
			R.attr.dlg_btn_ripple
	),
	TERTIARY(
			R.attr.dlg_btn_tertiary_text,
			INVALID_ID,
			R.attr.dlg_btn_ripple_tertiary
	),
	TERTIARY_HARMFUL(
			R.attr.dlg_btn_primary_text,
			R.attr.dlg_btn_tertiary_harmful,
			R.attr.dlg_btn_ripple_solid
	);

	DialogButtonType(int contentColorAttr, int backgroundAttr, int rippleAttr) {
		this.contentColorAttr = contentColorAttr;
		this.backgroundAttr = backgroundAttr;
		this.rippleAttr = rippleAttr;
	}

	private final int contentColorAttr;
	private final int backgroundAttr;
	private final int rippleAttr;

	public int getContentColorAttr() {
		return contentColorAttr;
	}

	public int getBackgroundAttr() {
		return backgroundAttr;
	}

	public int getRippleAttr() {
		return rippleAttr;
	}

	@NonNull
	public static DialogButtonType getById(int id) {
		for (DialogButtonType buttonType : values()) {
			if (buttonType.ordinal() == id) {
				return buttonType;
			}
		}
		return PRIMARY;
	}
}
