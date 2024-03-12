package net.osmand.plus.card.base.headed;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public interface IHeadedCardController {

	void bindComponent(@NonNull IHeadedContentCard card);

	@NonNull
	String getCardTitle();

	@NonNull
	String getCardSummary();

	@NonNull
	View getCardContentView(@NonNull FragmentActivity activity, boolean nightMode);

}