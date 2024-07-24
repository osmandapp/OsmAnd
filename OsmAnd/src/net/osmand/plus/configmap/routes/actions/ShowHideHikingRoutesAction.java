package net.osmand.plus.configmap.routes.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_HIKING_ROUTES_ACTION;

import androidx.annotation.NonNull;

import net.osmand.osm.OsmRouteType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.render.RenderingRuleProperty;

import java.util.Arrays;

public class ShowHideHikingRoutesAction extends BaseRouteQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_HIKING_ROUTES_ACTION,
			"hiking.routes.showhide", ShowHideHikingRoutesAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.rendering_attr_hikingRoutesOSMC_name)
			.iconRes(R.drawable.ic_action_trekking_dark)
			.category(QuickActionType.CONFIGURE_MAP);

	private String previousValue;

	public ShowHideHikingRoutesAction() {
		super(TYPE);
	}

	public ShowHideHikingRoutesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected void switchPreference(@NonNull OsmandApplication app) {
		boolean previouslyEnabled = isEnabled(app);
		String attrName = getAttrName();
		OsmandSettings settings = app.getSettings();
		CommonPreference<String> preference = settings.getCustomRenderProperty(attrName);
		RenderingRuleProperty property = getProperty(app);

		if (previouslyEnabled) {
			previousValue = preference.get();
			preference.set("");
		} else if (previousValue == null && property != null) {
			preference.set(property.getPossibleValues()[0]);
		} else {
			preference.set(previousValue);
		}
	}

	@Override
	protected boolean isEnabled(@NonNull OsmandApplication app) {
		String attrName = getAttrName();
		OsmandSettings settings = app.getSettings();
		CommonPreference<String> preference = settings.getCustomRenderProperty(attrName);
		RenderingRuleProperty property = getProperty(app);
		return property != null && Arrays.asList(property.getPossibleValues()).contains(preference.get());
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@NonNull
	@Override
	protected OsmRouteType getOsmRouteType() {
		return OsmRouteType.HIKING;
	}
}
