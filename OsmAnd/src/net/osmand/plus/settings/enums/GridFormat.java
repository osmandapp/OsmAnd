package net.osmand.plus.settings.enums;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.LocationConvert;
import net.osmand.core.jni.GridConfiguration.Format;
import net.osmand.core.jni.GridConfiguration.Projection;
import net.osmand.plus.R;
import net.osmand.plus.settings.coordinates.CoordinateFormatIds;
import net.osmand.util.CollectionUtils;

import java.util.List;

public enum GridFormat implements EnumWithTitleId {

	DMS(LocationConvert.FORMAT_SECONDS, R.string.dd_mm_ss_format),
	DM(LocationConvert.FORMAT_MINUTES, R.string.dd_mm_mmm_format),
	DIGITAL(LocationConvert.FORMAT_DEGREES, R.string.dd_ddddd_format),
	UTM(LocationConvert.UTM_FORMAT, R.string.navigate_point_format_utm),
	MGRS(LocationConvert.MGRS_FORMAT, R.string.navigate_point_format_mgrs);

	private final int id;
	private final int titleId;

	GridFormat(int id, int titleId) {
		this.id = id;
		this.titleId = titleId;
	}

	@Override
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public Projection getProjection() {
		return switch (this) {
			case DMS, DM, DIGITAL -> Projection.WGS84;
			case UTM -> Projection.UTM;
			case MGRS -> Projection.MGRS;
			default -> throw new IllegalArgumentException("Unknown GridFormat: " + this);
		};
	}

	@Nullable
	public Format getFormat() {
		return switch (this) {
			case DMS -> Format.DMS;
			case DM -> Format.DM;
			case DIGITAL -> Format.Decimal;
			case UTM, MGRS -> Format.values()[0];
			default -> throw new IllegalArgumentException("Unknown GridFormat: " + this);
		};
	}

	public boolean needSuffixes() {
		return !CollectionUtils.equalsToAny(this, UTM, MGRS);
	}

	@NonNull
	public String getCoordinateFormatId() {
		return switch (this) {
			case DMS -> CoordinateFormatIds.BUILTIN_DMS;
			case DM -> CoordinateFormatIds.BUILTIN_DDM;
			case DIGITAL -> CoordinateFormatIds.BUILTIN_DDD;
			case UTM -> CoordinateFormatIds.BUILTIN_UTM;
			case MGRS -> CoordinateFormatIds.BUILTIN_MGRS;
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

	@Nullable
	public static GridFormat fromCoordinateFormatId(@Nullable String formatId) {
		String normalized = CoordinateFormatIds.normalize(formatId);
		if (CoordinateFormatIds.BUILTIN_DMS.equals(normalized)) {
			return DMS;
		} else if (CoordinateFormatIds.BUILTIN_DDM.equals(normalized)) {
			return DM;
		} else if (CoordinateFormatIds.BUILTIN_DDD.equals(normalized)) {
			return DIGITAL;
		} else if (CoordinateFormatIds.BUILTIN_UTM.equals(normalized)) {
			return UTM;
		} else if (CoordinateFormatIds.BUILTIN_MGRS.equals(normalized)) {
			return MGRS;
		}
		return null;
	}

	@NonNull
	public static List<String> getSupportedCoordinateFormatIds() {
		return List.of(DMS.getCoordinateFormatId(),
				DM.getCoordinateFormatId(),
				DIGITAL.getCoordinateFormatId(),
				UTM.getCoordinateFormatId(),
				MGRS.getCoordinateFormatId());
	}
}

