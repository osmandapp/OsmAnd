package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.shared.grid.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.shared.grid.ButtonPositionSize.POS_RIGHT;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.actions.ChangeMapOrientationAction;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.controls.maphudbuttons.CompassDrawable;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.shared.grid.ButtonPositionSize;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuickActionButtonState extends MapButtonState {

	public static final String DEFAULT_BUTTON_ID = "quick_actions";

	public static final String DEFAULT_ICON_KEY = "ic_quick_action";

	private final CommonPreference<Boolean> visibilityPref;
	private final CommonPreference<String> namePref;
	private final CommonPreference<String> quickActionsPref;

	private final MapQuickActionLayer quickActionLayer;

	private List<QuickAction> quickActions = new ArrayList<>();

	public QuickActionButtonState(@NonNull OsmandApplication app, @NonNull String id) {
		super(app, id);
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", false)).makeProfile();
		this.namePref = addPreference(settings.registerStringPreference(id + "_name", null)).makeGlobal().storeLastModifiedTime();
		this.quickActionsPref = addPreference(settings.registerStringPreference(id + "_list", null)).makeGlobal().storeLastModifiedTime();
		this.quickActionLayer = app.getOsmandMap().getMapLayers().getMapQuickActionLayer();
	}

	@Override
	public boolean isEnabled() {
		return visibilityPref.get();
	}

	public void setEnabled(boolean enabled) {
		visibilityPref.set(enabled);
	}

	@NonNull
	@Override
	public String getName() {
		String name = namePref.get();
		return Algorithms.isEmpty(name) ? app.getString(R.string.configure_screen_quick_action) : name;
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.configure_screen_quick_action);
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.map_quick_actions_button;
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
	@Override
	public CommonPreference<Boolean> getVisibilityPref() {
		return visibilityPref;
	}

	@NonNull
	public CommonPreference<String> getNamePref() {
		return namePref;
	}


	@NonNull
	public CommonPreference<String> getQuickActionsPref() {
		return quickActionsPref;
	}

	public long getLastModifiedTime() {
		return Math.max(namePref.getLastModifiedTime(), quickActionsPref.getLastModifiedTime());
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		namePref.setLastModifiedTime(lastModifiedTime);
		quickActionsPref.setLastModifiedTime(lastModifiedTime);
	}

	public boolean isSingleAction() {
		return quickActions.size() == 1;
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

	public boolean isDefaultButton() {
		return Algorithms.stringsEqual(DEFAULT_BUTTON_ID, getId());
	}

	@NonNull
	@Override
	public ButtonAppearanceParams createAppearanceParams() {
		ButtonAppearanceParams appearanceParams = super.createAppearanceParams();
		if (Algorithms.isEmpty(getSavedIconName())) {
			appearanceParams.setIconName(getDefaultIconName());
		}
		return appearanceParams;
	}

	@NonNull
	public String getDefaultIconName() {
		if (isSingleAction()) {
			int iconId = getQuickActions().get(0).getIconRes(app);
			if (iconId > 0) {
				return app.getResources().getResourceEntryName(iconId);
			}
		}
		return DEFAULT_ICON_KEY;
	}

	@Nullable
	@Override
	public Drawable getIcon(@DrawableRes int iconId, @ColorInt int color, boolean nightMode, boolean mapIcon) {
		if (mapIcon) {
			if (quickActionLayer.isWidgetVisibleForButton(getId())) {
				return super.getIcon(R.drawable.ic_action_close, color, nightMode, true);
			} else if (isSingleAction() && quickActions.get(0).isActionWithSlash(app)) {
				Drawable drawable = super.getIcon(iconId, color, nightMode, true);
				Drawable slashIcon = uiUtilities.getIcon(nightMode ? R.drawable.ic_action_icon_hide_dark : R.drawable.ic_action_icon_hide_white);
				return new LayerDrawable(new Drawable[] {drawable, slashIcon});
			} else if (isSingleAction() && quickActions.get(0) instanceof ChangeMapOrientationAction) {
				Drawable drawable = super.getIcon(iconId, 0, nightMode, true);
				if (drawable != null) {
					return new CompassDrawable(drawable);
				}
			}
		}
		return super.getIcon(iconId, color, nightMode, mapIcon);
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		int hash = id.hashCode();
		int mod = hash == Integer.MIN_VALUE ? 0 : Math.abs(hash % 3);

		boolean xMove = (mod == 0 || mod == 2);
		boolean yMove = (mod == 1 || mod == 2);

		return setupButtonPosition(position, POS_RIGHT, POS_BOTTOM, xMove, yMove);
	}
}
