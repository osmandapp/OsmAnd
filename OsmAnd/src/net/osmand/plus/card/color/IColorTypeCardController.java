package net.osmand.plus.card.color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IColorTypeCardController {

	void bindCard(@NonNull ColorTypeCard card);

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
