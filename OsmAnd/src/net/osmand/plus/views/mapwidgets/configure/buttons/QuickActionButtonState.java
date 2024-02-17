package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuickActionButtonState extends MapButtonState {

	public static final String DEFAULT_BUTTON_ID = "quick_actions";

	private final CommonPreference<Boolean> statePref;
	private final CommonPreference<String> namePref;
	private final CommonPreference<String> quickActionsPref;
	private final FabMarginPreference fabMarginPref;

	private List<QuickAction> quickActions = new ArrayList<>();

	public QuickActionButtonState(@NonNull OsmandApplication app, @NonNull String id) {
		super(app, id);
		this.statePref = settings.registerBooleanPreference(id + "_state", false).makeProfile();
		this.namePref = settings.registerStringPreference(id + "_name", null).makeProfile();
		this.quickActionsPref = settings.registerStringPreference(id + "_list", null).makeProfile().storeLastModifiedTime();
		this.fabMarginPref = new FabMarginPreference(settings, id + "_fab_margin");
	}

	@Override
	public boolean isEnabled() {
		return statePref.get();
	}

	public void setEnabled(boolean enabled) {
		statePref.set(enabled);
	}

	@NonNull
	@Override
	public String getName() {
		String name = namePref.get();
		return Algorithms.isEmpty(name) ? app.getString(R.string.configure_screen_quick_action) : name;
	}

	public boolean hasCustomName() {
		return !Algorithms.isEmpty(namePref.get());
	}

	public void setName(@NonNull String name) {
		namePref.set(name);
	}

	@NonNull
	public List<QuickAction> getQuickActions() {
		return quickActions;
	}

	@Nullable
	public QuickAction getQuickAction(long id) {
		for (QuickAction action : quickActions) {
			if (action.getId() == id) {
				return action;
			}
		}
		return null;
	}

	@Nullable
	public QuickAction getQuickAction(int type, String name, @NonNull Map<String, String> params) {
		for (QuickAction action : quickActions) {
			if (action.getType() == type
					&& (action.hasCustomName(app) && action.getName(app).equals(name) || !action.hasCustomName(app))
					&& action.getParams().equals(params)) {
				return action;
			}
		}
		return null;
	}

	@NonNull
	public FabMarginPreference getFabMarginPref() {
		return fabMarginPref;
	}

	public long getLastModifiedTime() {
		return quickActionsPref.getLastModifiedTime();
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		quickActionsPref.setLastModifiedTime(lastModifiedTime);
	}

	public boolean isSingleAction() {
		return quickActions.size() == 1;
	}

	@Nullable
	@Override
	public Drawable getIcon(boolean nightMode, boolean mapIcon, @ColorInt int colorId) {
		if (isSingleAction()) {
			QuickAction action = quickActions.get(0);
			Drawable icon = uiUtilities.getPaintedIcon(action.getIconRes(app), colorId);

			if (mapIcon && action.isActionWithSlash(app)) {
				Drawable slashIcon = uiUtilities.getIcon(nightMode ? R.drawable.ic_action_icon_hide_dark : R.drawable.ic_action_icon_hide_white);
				return new LayerDrawable(new Drawable[] {icon, slashIcon});
			}
			return icon;
		}
		return super.getIcon(nightMode, mapIcon, colorId);
	}

	public void resetForMode(@NonNull ApplicationMode appMode) {
		statePref.resetModeToDefault(appMode);
		namePref.resetModeToDefault(appMode);
		quickActionsPref.resetModeToDefault(appMode);
		fabMarginPref.resetModeToDefault(appMode);
	}

	public void copyForMode(@NonNull ApplicationMode fromAppMode, @NonNull ApplicationMode toAppMode) {
		statePref.setModeValue(toAppMode, statePref.getModeValue(fromAppMode));
		namePref.setModeValue(toAppMode, namePref.getModeValue(fromAppMode));
		quickActionsPref.setModeValue(toAppMode, quickActionsPref.getModeValue(fromAppMode));
		fabMarginPref.copyForMode(fromAppMode, toAppMode);
	}

	public void saveActions(@NonNull Gson gson) {
		Type type = new TypeToken<List<QuickAction>>() {}.getType();
		quickActionsPref.set(gson.toJson(quickActions, type));
	}

	public void parseQuickActions(@NonNull Gson gson) {
		String json = quickActionsPref.get();

		List<QuickAction> resQuickActions = new ArrayList<>();
		if (!Algorithms.isEmpty(json)) {
			Type type = new TypeToken<List<QuickAction>>() {}.getType();
			List<QuickAction> quickActions = gson.fromJson(json, type);

			if (!Algorithms.isEmpty(quickActions)) {
				for (QuickAction action : quickActions) {
					if (action != null) {
						resQuickActions.add(action);
					}
				}
			}
		}
		this.quickActions = resQuickActions;
	}
}
