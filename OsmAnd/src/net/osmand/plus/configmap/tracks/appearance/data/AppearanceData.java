package net.osmand.plus.configmap.tracks.appearance.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.card.color.ColoringStyle;

import java.util.Objects;

public class AppearanceData {

	private Boolean showArrows = null;
	private Boolean showStartFinish = null;
	private ColoringStyle coloringStyle = null;
	@ColorInt
	private Integer customColor = null;
	private String width = null;


	private OnAppearanceModifiedListener modifiedListener;

	public AppearanceData() { }

	public AppearanceData(@NonNull AppearanceData appearanceData) {
		this.showArrows = appearanceData.shouldShowArrows();
		this.showStartFinish = appearanceData.shouldShowStartFinish();
		this.coloringStyle = appearanceData.getColoringStyle();
		this.customColor = appearanceData.getCustomColor();
		this.width = appearanceData.getWidthValue();
	}

	@NonNull
	public AppearanceData setModifiedListener(@NonNull OnAppearanceModifiedListener modifiedListener) {
		this.modifiedListener = modifiedListener;
		return this;
	}

	@Nullable
	public Boolean shouldShowArrows() {
		return showArrows;
	}

	@NonNull
	public AppearanceData setShowArrows(@Nullable Boolean showArrows) {
		this.showArrows = showArrows;
		notifyOnAppearanceModified();
		return this;
	}

	@Nullable
	public Boolean shouldShowStartFinish() {
		return showStartFinish;
	}

	@NonNull
	public AppearanceData setShowStartFinish(@Nullable Boolean showStartFinish) {
		this.showStartFinish = showStartFinish;
		notifyOnAppearanceModified();
		return this;
	}

	@Nullable
	public ColoringStyle getColoringStyle() {
		return coloringStyle;
	}

	@NonNull
	public AppearanceData setColoringStyle(@Nullable ColoringStyle coloringStyle) {
		this.coloringStyle = coloringStyle;
		notifyOnAppearanceModified();
		return this;
	}

	@Nullable
	public String getWidthValue() {
		return width;
	}

	@NonNull
	public AppearanceData setWidthValue(@Nullable String width) {
		this.width = width;
		notifyOnAppearanceModified();
		return this;
	}

	@Nullable
	public Integer getCustomColor() {
		return customColor;
	}

	@NonNull
	public AppearanceData setCustomColor(@Nullable Integer customColor) {
		this.customColor = customColor;
		notifyOnAppearanceModified();
		return this;
	}

	private void notifyOnAppearanceModified() {
		if (modifiedListener != null) {
			modifiedListener.onAppearanceModified();
		}
	}

	public interface OnAppearanceModifiedListener {
		void onAppearanceModified();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AppearanceData)) return false;

		AppearanceData that = (AppearanceData) o;

		if (!Objects.equals(shouldShowArrows(), that.shouldShowArrows()))
			return false;
		if (!Objects.equals(shouldShowStartFinish(), that.shouldShowStartFinish()))
			return false;
		if (!Objects.equals(getColoringStyle(), that.getColoringStyle()))
			return false;
		if (!Objects.equals(getCustomColor(), that.getCustomColor()))
			return false;
		return Objects.equals(getWidthValue(), that.getWidthValue());
	}

	@Override
	public int hashCode() {
		int result = shouldShowArrows() != null ? shouldShowArrows().hashCode() : 0;
		result = 31 * result + (shouldShowStartFinish() != null ? shouldShowStartFinish().hashCode() : 0);
		result = 31 * result + (getColoringStyle() != null ? getColoringStyle().hashCode() : 0);
		result = 31 * result + (getCustomColor() != null ? getCustomColor().hashCode() : 0);
		result = 31 * result + (getWidthValue() != null ? getWidthValue().hashCode() : 0);
		return result;
	}
}
