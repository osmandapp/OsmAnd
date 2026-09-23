package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_ALONG_THE_ROUTE_ACTION;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.routepreparationmenu.ShowAlongTheRouteBottomSheet;
import net.osmand.plus.utils.UiUtilities;

public class ShowAlongTheRouteAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_ALONG_THE_ROUTE_ACTION,
			"show.along.the.route", ShowAlongTheRouteAction.class)
			.nameActionRes(R.string.shared_string_open)
			.nameRes(R.string.show_along_the_route)
			.iconRes(R.drawable.ic_action_show_along_route)
			.category(QuickActionType.NAVIGATION)
			.nonEditable();

	public ShowAlongTheRouteAction() {
		super(TYPE);
	}

	public ShowAlongTheRouteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		ShowAlongTheRouteBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), null, null);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_show_along_the_route_desc);
		parent.addView(view);
	}

}
