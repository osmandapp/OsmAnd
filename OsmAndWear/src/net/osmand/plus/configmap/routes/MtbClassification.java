package net.osmand.plus.configmap.routes;

import static net.osmand.plus.configmap.routes.RouteUtils.SHOW_MTB_SCALE;
import static net.osmand.plus.configmap.routes.RouteUtils.SHOW_MTB_SCALE_IMBA_TRAILS;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum MtbClassification {

	SCALE(SHOW_MTB_SCALE, R.string.mtb_scale, null),
	IMBA(SHOW_MTB_SCALE_IMBA_TRAILS, R.string.mtb_imba, R.string.mtb_imba_full);

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
