package net.osmand.plus.settings.enums;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum MtbClassification {
	SCALE("showMtbScale", R.string.mtb_scale, null),
	IMBA("showMtbScaleIMBATrails", R.string.mtb_imba, R.string.mtb_imba_description);

	public final String attrName;
	@StringRes
	public final int nameId;
	@Nullable
	@StringRes
	public final Integer descriptionId;

	MtbClassification(String attrName, int nameId, @Nullable Integer descriptionId) {
		this.attrName = attrName;
		this.nameId = nameId;
		this.descriptionId = descriptionId;
	}
}
