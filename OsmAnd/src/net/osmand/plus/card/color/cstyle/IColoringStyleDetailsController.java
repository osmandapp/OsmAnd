package net.osmand.plus.card.color.cstyle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.card.color.ColoringStyle;

public interface IColoringStyleDetailsController {

	void bindCard(@NonNull ColoringStyleDetailsCard card);

	void setColoringStyle(@NonNull ColoringStyle coloringStyle);

	boolean shouldHideCard();

	boolean shouldShowUpperSpace();

	boolean shouldShowBottomSpace();

	boolean shouldShowSpeedAltitudeLegend();

	boolean shouldShowSlopeLegend();

	@Nullable
	String getTypeDescription();

	@Nullable
	CharSequence[] getLegendHeadlines();
}
