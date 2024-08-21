package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_SEARCH_VIEW_ACTION;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.search.ShowQuickSearchMode;

public class ShowHideSearchViewAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_SEARCH_VIEW_ACTION,
			"search.view.showhide", ShowHideSearchViewAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.quick_action_search_view_title)
			.iconRes(R.drawable.ic_action_search_dark)
			.category(QuickActionType.INTERFACE)
			.nonEditable();

	public ShowHideSearchViewAction() {
		super(TYPE);
	}

	public ShowHideSearchViewAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		mapActivity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_search_view_desc);
		parent.addView(view);
	}

}
