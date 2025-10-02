package net.osmand.plus.configmap.routes.actions;

import static net.osmand.plus.configmap.routes.RouteUtils.showRendererSnackbarForAttr;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.osm.OsmRouteType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;

public abstract class BaseRouteQuickAction extends QuickAction {

	public BaseRouteQuickAction(QuickActionType type) {
		super(type);
	}

	public BaseRouteQuickAction(QuickAction quickAction) {
		super(quickAction);
	}

	public abstract QuickActionType getActionType();

	@NonNull
	protected String getAttrName() {
		return getOsmRouteType().getRenderingPropertyAttr();
	}

	@NonNull
	protected abstract OsmRouteType getOsmRouteType();

	@NonNull
	private String getQuickActionSummary(@NonNull Context context) {
		return context.getString(R.string.quick_action_routes_summary, getName(context));
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		String attrName = getAttrName();
		OsmandApplication app = mapActivity.getApp();
		RenderingRuleProperty property = getProperty(app);
		if (property != null) {
			switchPreference(app);
			mapActivity.refreshMapComplete();
			mapActivity.updateLayers();
		} else {
			boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
			showRendererSnackbarForAttr(mapActivity, attrName, nightMode, null);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(getQuickActionSummary(mapActivity));
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
		return isEnabled(app);
	}

	@Nullable
	protected RenderingRuleProperty getProperty(@NonNull OsmandApplication app) {
		return ConfigureMapUtils.getPropertyForAttr(app, getAttrName());
	}

	protected boolean isEnabled(@NonNull OsmandApplication app) {
		String attrName = getAttrName();
		return app.getRouteLayersHelper().isRoutesTypeEnabled(attrName);
	}

	protected void switchPreference(@NonNull OsmandApplication app) {
		String attrName = getAttrName();
		app.getRouteLayersHelper().toggleRoutesType(attrName);
	}
}
