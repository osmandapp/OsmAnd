package net.osmand.plus.quickaction;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_COORDINATE_GRID_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.CoordinatesGridLayerSettings;

public class ShowHideCoordinatesGridAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_COORDINATE_GRID_ACTION_ID,
			"coordinates_grid.showhide", ShowHideCoordinatesGridAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.layer_coordinates_grid)
			.iconRes(R.drawable.ic_action_world_globe)
			.nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	private CoordinatesGridLayerSettings gridLayerSettings;

	public ShowHideCoordinatesGridAction() {
		super(TYPE);
	}

	public ShowHideCoordinatesGridAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		requireGridHelper(app).toggleEnable();
		mapActivity.refreshMap();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_showhide_coordinates_grid_descr);
		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isActionWithSlash(app) ?
				app.getString(R.string.shared_string_hide) :
				app.getString(R.string.shared_string_show);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		return requireGridHelper(app).isEnabled();
	}

	@NonNull
	private CoordinatesGridLayerSettings requireGridHelper(@NonNull OsmandApplication app) {
		if (gridLayerSettings == null) {
			gridLayerSettings = new CoordinatesGridLayerSettings(app);
		}
		return gridLayerSettings;
	}
}
