package net.osmand.plus.views.mapwidgets.configure.add;

import android.os.Bundle;

import net.osmand.aidl.AidlMapWidgetWrapper;
import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetParams;

import java.util.Collections;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import static net.osmand.aidl.OsmandAidlApi.WIDGET_ID_PREFIX;

public class WidgetDataHolder {

	public static final String KEY_GROUP_NAME = "group_name";
	public static final String KEY_WIDGET_ID = "widget_id";
	public static final String KEY_EXTERNAL_PROVIDER_PACKAGE = "external_provider_package";

	private final OsmandApplication app;

	private WidgetGroup widgetGroup;
	private WidgetParams widgetParams;

	private ConnectedApp connectedApp;
	private String aidlWidgetId;
	private String externalProviderPackage;
	private AidlMapWidgetWrapper aidlWidgetData;

	public WidgetDataHolder(@NonNull OsmandApplication app, @NonNull Bundle bundle) {
		this.app = app;

		if (bundle.containsKey(KEY_GROUP_NAME)) {
			widgetGroup = WidgetGroup.valueOf(bundle.getString(KEY_GROUP_NAME));
		} else if (bundle.containsKey(KEY_EXTERNAL_PROVIDER_PACKAGE)) {
			aidlWidgetId = bundle.getString(KEY_WIDGET_ID);
			externalProviderPackage = bundle.getString(KEY_EXTERNAL_PROVIDER_PACKAGE);
			connectedApp = app.getAidlApi().getConnectedApp(externalProviderPackage);
			if (connectedApp != null) {
				String sourceId = aidlWidgetId.replaceFirst(WIDGET_ID_PREFIX, "");
				aidlWidgetData = connectedApp.getWidgets().get(sourceId);
			}
		} else {
			widgetParams = WidgetParams.getById(bundle.getString(KEY_WIDGET_ID));
		}
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
		} else if (widgetParams != null) {
			return getString(widgetParams.titleId);
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
		} else if (widgetParams != null) {
			return widgetParams.getIconId(nightMode);
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
		} else if (widgetParams != null && widgetParams.descId != 0) {
			return getString(widgetParams.descId);
		} else if (connectedApp != null) {
			return connectedApp.getName();
		}
		return null;
	}

	@Nullable
	public List<WidgetParams> getWidgetsList() {
		if (widgetGroup != null) {
			return widgetGroup.getWidgets();
		} else if (widgetParams != null) {
			return Collections.singletonList(widgetParams);
		}
		return null;
	}

	@Nullable
	public AidlMapWidgetWrapper getAidlWidgetData() {
		return aidlWidgetData;
	}

	@StringRes
	public int getSecondaryDescriptionId() {
		if (widgetGroup != null) {
			return widgetGroup.getSecondaryDescriptionId();
		} else if (widgetParams != null) {
			return widgetParams.getSecondaryDescriptionId();
		}
		return 0;
	}

	@DrawableRes
	public int getSecondaryIconId() {
		if (widgetGroup != null) {
			return widgetGroup.getSecondaryIconId();
		} else if (widgetParams != null) {
			return widgetParams.getSecondaryIconId();
		}
		return 0;
	}

	@Nullable
	public String getDocsUrl() {
		if (widgetGroup != null) {
			return widgetGroup.docsUrl;
		} else if (widgetParams != null) {
			return widgetParams.getDocsUrl();
		}
		return null;
	}

	public void saveState(@NonNull Bundle outState) {
		if (widgetGroup != null) {
			outState.putString(KEY_GROUP_NAME, widgetGroup.name());
		} else if (widgetParams != null) {
			outState.putString(KEY_WIDGET_ID, widgetParams.id);
		} else {
			outState.putString(KEY_WIDGET_ID, aidlWidgetId);
			outState.putString(KEY_EXTERNAL_PROVIDER_PACKAGE, externalProviderPackage);
		}
	}

	@NonNull
	private String getString(@StringRes int stringId) {
		return app.getString(stringId);
	}
}