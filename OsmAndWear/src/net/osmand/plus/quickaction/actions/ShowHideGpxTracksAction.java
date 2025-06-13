package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_GPX_TRACKS_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideGpxTracksAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_GPX_TRACKS_ACTION_ID,
			"gpx.showhide", ShowHideGpxTracksAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.show_gpx).iconRes(R.drawable.ic_action_polygom_dark).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideGpxTracksAction() {
		super(TYPE);
	}

	public ShowHideGpxTracksAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		GpxSelectionHelper selectedGpxHelper = mapActivity.getMyApplication()
			.getSelectedGpxHelper();
		if (selectedGpxHelper.isAnyGpxFileSelected()) {
			selectedGpxHelper.clearAllGpxFilesToShow(true);
		} else {
			selectedGpxHelper.restoreSelectedGpxFiles();
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
			.setText(R.string.quick_action_show_hide_gpx_tracks_descr);
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
		return app.getSelectedGpxHelper().isAnyGpxFileSelected();
	}
}
