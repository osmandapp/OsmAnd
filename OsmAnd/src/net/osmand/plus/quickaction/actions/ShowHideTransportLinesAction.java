package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.transport.TransportLinesMenu;

public class ShowHideTransportLinesAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(31,
			"transport.showhide", ShowHideTransportLinesAction.class)
			.nameRes(R.string.quick_action_show_hide_transport)
			.iconRes(R.drawable.ic_action_transport_bus).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideTransportLinesAction() {
		super(TYPE);
	}

	public ShowHideTransportLinesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(final MapActivity activity) {
		boolean enabled = TransportLinesMenu.isShowLines(activity.getMyApplication());
		TransportLinesMenu.toggleTransportLines(activity, !enabled, null);
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_transport_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication application) {

		return TransportLinesMenu.isShowLines(application)
				? application.getString(R.string.quick_action_transport_hide)
				: application.getString(R.string.quick_action_transport_show);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {
		return TransportLinesMenu.isShowLines(application);
	}
}
