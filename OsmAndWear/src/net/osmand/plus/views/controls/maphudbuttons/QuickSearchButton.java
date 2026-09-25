package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.plus.search.ShowQuickSearchMode.NEW_IF_EXPIRED;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.helpers.MapFragmentsHelper;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickSearchButtonState;

public class QuickSearchButton extends MapButton {

	private final QuickSearchButtonState buttonState;

	public QuickSearchButton(@NonNull Context context) {
		this(context, null);
	}

	public QuickSearchButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public QuickSearchButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getQuickSearchButtonState();

		setOnClickListener(v -> {
			MapFragmentsHelper fragmentsHelper = mapActivity.getFragmentsHelper();
			fragmentsHelper.dismissCardDialog();
			fragmentsHelper.showQuickSearch(NEW_IF_EXPIRED, false);
		});
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	protected boolean shouldShow() {
		return !routeDialogOpened && visibilityHelper.shouldShowTopButtons();
	}
}