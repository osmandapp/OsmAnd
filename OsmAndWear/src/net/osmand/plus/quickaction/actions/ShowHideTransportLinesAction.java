package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_TRANSPORT_LINES_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.transport.TransportLinesMenu;

public class ShowHideTransportLinesAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_TRANSPORT_LINES_ACTION_ID,
			"transport.showhide", ShowHideTransportLinesAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.poi_filter_public_transport)
			.iconRes(R.drawable.ic_action_transport_bus).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideTransportLinesAction() {
		super(TYPE);
	}

	public ShowHideTransportLinesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		TransportLinesMenu transportLinesMenu = new TransportLinesMenu(app);
		boolean selected = transportLinesMenu.isShowAnyTransport();
		transportLinesMenu.toggleTransportLines(mapActivity, !selected);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_transport_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isActionWithSlash(app) ? app.getString(R.string.shared_string_hide) : app.getString(R.string.shared_string_show);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		TransportLinesMenu menu = new TransportLinesMenu(app);
		return menu.isShowAnyTransport();
	}
}
