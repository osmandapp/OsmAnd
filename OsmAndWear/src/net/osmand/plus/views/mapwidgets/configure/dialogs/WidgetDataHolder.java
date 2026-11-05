package net.osmand.plus.views.mapwidgets.configure.dialogs;

import android.os.Bundle;

import net.osmand.aidl.AidlMapWidgetWrapper;
import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.Collections;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import static net.osmand.aidl.OsmandAidlApi.WIDGET_ID_PREFIX;

public class WidgetDataHolder {

	public static final String KEY_WIDGETS_PANEL_ID = "widgets_panel_id";
	public static final String KEY_GROUP_NAME = "group_name";
	public static final String KEY_WIDGET_TYPE = "widget_type_name";
	public static final String KEY_EXTERNAL_WIDGET_ID = "aidl_widget_id";
	public static final String KEY_EXTERNAL_PROVIDER_PACKAGE = "external_provider_package";

	private final OsmandApplication app;

	private final WidgetsPanel widgetsPanel;

	private WidgetGroup widgetGroup;
	private WidgetType widgetType;

	private ConnectedApp connectedApp;
	private String aidlWidgetId;
	private String externalProviderPackage;
	private AidlMapWidgetWrapper aidlWidgetData;

	public WidgetDataHolder(@NonNull OsmandApplication app, @NonNull Bundle bundle) {
		this.app = app;

		widgetsPanel = WidgetsPanel.valueOf(bundle.getString(KEY_WIDGETS_PANEL_ID));
		if (bundle.containsKey(KEY_GROUP_NAME)) {
			widgetGroup = WidgetGroup.valueOf(bundle.getString(KEY_GROUP_NAME));
		} else if (bundle.containsKey(KEY_EXTERNAL_PROVIDER_PACKAGE)) {
			aidlWidgetId = bundle.getString(KEY_EXTERNAL_WIDGET_ID);
			externalProviderPackage = bundle.getString(KEY_EXTERNAL_PROVIDER_PACKAGE);
			connectedApp = app.getAidlApi().getConnectedApp(externalProviderPackage);
			if (connectedApp != null) {
				String sourceId = aidlWidgetId.replaceFirst(WIDGET_ID_PREFIX, "");
				aidlWidgetData = connectedApp.getWidgets().get(sourceId);
			}
		} else {
			widgetType = WidgetType.valueOf(bundle.getString(KEY_WIDGET_TYPE));
		}
	}

	@NonNull
	public WidgetsPanel getWidgetsPanel() {
		return widgetsPanel;
	}

	@Nullable
	public WidgetGroup getWidgetGroup() {
		return widgetGroup;
	}

	@Nullable
	public String getAidlWidgetId() {
		return aidlWidgetId;
	}

	@NonNull
	public String getTitle() {
		if (widgetGroup != null) {
			return getString(widgetGroup.titleId);
		} else if (widgetType != null) {
			return getString(widgetType.titleId);
		} else if (aidlWidgetData != null) {
			return aidlWidgetData.getMenuTitle();
		}
		return "";
	}

	public int getWidgetsCount() {
		return widgetGroup != null ? widgetGroup.getWidgets().size() : 1;
	}

	/**
	 * @return existing icon id or 0
	 */
	@DrawableRes
	public int getIconId(boolean nightMode) {
		if (widgetGroup != null) {
			return widgetGroup.getIconId(nightMode);
		} else if (widgetType != null) {
			return widgetType.getIconId(nightMode);
		} else if (aidlWidgetData != null) {
			String iconName = aidlWidgetData.getMenuIconName();
			return AndroidUtils.getDrawableId(app, iconName);
		}
		return 0;
	}

	@Nullable
	public String getDescription() {
		if (widgetGroup != null && widgetGroup.descId != 0) {
			return getString(widgetGroup.descId);
		} else if (widgetType != null && widgetType.descId != 0) {
			return getString(widgetType.descId);
		} else if (connectedApp != null) {
			return connectedApp.getName();
		}
		return null;
	}

	@Nullable
	public List<WidgetType> getWidgetsList() {
		if (widgetGroup != null) {
			return widgetGroup.getWidgets();
		} else if (widgetType != null) {
			return Collections.singletonList(widgetType);
		}
		return null;
	}

	@Nullable
	public WidgetType getMainWidget() {
		if (widgetGroup != null) {
			for (WidgetType widget : widgetGroup.getWidgets()) {
				if (widget.isMainWidgetOfGroup()) {
					return widget;
				}
			}
		} else if (widgetType != null) {
			return widgetType;
		}
		return null;
	}

	@Nullable
	public AidlMapWidgetWrapper getAidlWidgetData() {
		return aidlWidgetData;
	}

	@Nullable
	public String getSecondaryDescription() {
		if (widgetGroup != null) {
			return widgetGroup.getSecondaryDescription(app);
		} else if (widgetType != null) {
			return widgetType.getSecondaryDescription(app);
		}
		return null;
	}

	@DrawableRes
	public int getSecondaryIconId() {
		if (widgetGroup != null) {
			return widgetGroup.getSecondaryIconId();
		} else if (widgetType != null) {
			return widgetType.getSecondaryIconId();
		}
		return 0;
	}

	@StringRes
	public int getDocsUrl() {
		if (widgetGroup != null) {
			return widgetGroup.docsUrlId;
		} else if (widgetType != null) {
			return widgetType.docsUrlId;
		}
		return 0;
	}

	public void saveState(@NonNull Bundle outState) {
		outState.putString(KEY_WIDGETS_PANEL_ID, widgetsPanel.name());
		if (widgetGroup != null) {
			outState.putString(KEY_GROUP_NAME, widgetGroup.name());
		} else if (widgetType != null) {
			outState.putString(KEY_WIDGET_TYPE, widgetType.name());
		} else {
			outState.putString(KEY_EXTERNAL_WIDGET_ID, aidlWidgetId);
			outState.putString(KEY_EXTERNAL_PROVIDER_PACKAGE, externalProviderPackage);
		}
	}

	@NonNull
	private String getString(@StringRes int stringId) {
		return app.getString(stringId);
	}
}