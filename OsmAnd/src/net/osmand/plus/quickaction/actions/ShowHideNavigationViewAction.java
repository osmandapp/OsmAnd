package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_NAVIGATION_VIEW_ACTION;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.views.layers.MapActionsHelper;

public class ShowHideNavigationViewAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_NAVIGATION_VIEW_ACTION,
			"navigation.view.showhide", ShowHideNavigationViewAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.quick_action_navigation_view_title)
			.iconRes(R.drawable.ic_action_gdirections_dark)
			.category(QuickActionType.INTERFACE)
			.nonEditable();

	public ShowHideNavigationViewAction() {
		super(TYPE);
	}

	public ShowHideNavigationViewAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		MapActionsHelper controlsHelper = mapActivity.getMapLayers().getMapActionsHelper();
		if (controlsHelper != null) {
			controlsHelper.doRoute();
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_navigation_view_desc);
		parent.addView(view);
	}
}
