package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.LocationConvert;
import net.osmand.core.jni.GridConfiguration.Format;
import net.osmand.core.jni.GridConfiguration.Projection;
import net.osmand.plus.R;

public enum GridFormat {

	DMS(LocationConvert.FORMAT_SECONDS, R.string.dd_mm_ss_format),
	DM(LocationConvert.FORMAT_MINUTES, R.string.dd_mm_mmm_format),
	DECIMAL(LocationConvert.FORMAT_DEGREES, R.string.dd_ddddd_format),
	UTM(LocationConvert.UTM_FORMAT, R.string.navigate_point_format_utm);

	private final int id;
	private final int titleId;

	GridFormat(int id, int titleId) {
		this.id = id;
		this.titleId = titleId;
	}

	@NonNull
	public String getTitle(@NonNull Context context) {
		return context.getString(titleId);
	}

	@NonNull
	public Projection getProjection() {
		return switch (this) {
			case DMS, DM, DECIMAL -> Projection.WGS84;
			case UTM -> Projection.UTM;
			default -> throw new IllegalArgumentException("Unknown GridFormat: " + this);
		};
	}

	@Nullable
	public Format getFormat() {
		return switch (this) {
			case DMS -> Format.DMS;
			case DM -> Format.DM;
			case DECIMAL -> Format.Decimal;
			case UTM -> Format.values()[0];
			default -> throw new IllegalArgumentException("Unknown GridFormat: " + this);
		};
	}

	@NonNull
	public static GridFormat valueOf(int formatId) {
		for (GridFormat format : values()) {
			if (format.id == formatId) {
				return format;
			}
		}
		return values()[0];
	}
}

