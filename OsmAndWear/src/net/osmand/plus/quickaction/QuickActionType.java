package net.osmand.plus.quickaction;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.lang.reflect.InvocationTargetException;

public class QuickActionType {

	public static final int CREATE_CATEGORY = 0;
	public static final int CONFIGURE_MAP = 1;
	public static final int NAVIGATION = 2;
	public static final int CONFIGURE_SCREEN = 3;
	public static final int SETTINGS = 4;
	public static final int MAP_INTERACTIONS = 5;
	public static final int MY_PLACES = 6;
	public static final int INTERFACE = 7;

	private final int id;
	private final String stringId;
	private boolean actionEditable;
	private boolean forceUseExtendedName;
	@StringRes
	private int nameRes;
	@StringRes
	private int nameActionRes;
	@DrawableRes
	private int iconRes;
	private Class<? extends QuickAction> cl;
	private int category;

	public QuickActionType(int id, String stringId) {
		this.id = id;
		this.stringId = stringId;
	}

	public QuickActionType(int id, String stringId, Class<? extends QuickAction> cl) {
		this.id = id;
		this.stringId = stringId;
		this.cl = cl;
		this.actionEditable = cl != null;
	}

	public QuickActionType nameRes(int nameRes) {
		this.nameRes = nameRes;
		return this;
	}

	public QuickActionType nameActionRes(int nameActionRes) {
		this.nameActionRes = nameActionRes;
		return this;
	}

	public QuickActionType category(int cat) {
		this.category = cat;
		return this;
	}

	public QuickActionType iconRes(int iconRes) {
		this.iconRes = iconRes;
		return this;
	}

	public QuickActionType nonEditable() {
		actionEditable = false;
		return this;
	}

	public QuickActionType forceUseExtendedName() {
		forceUseExtendedName = true;
		return this;
	}

	@NonNull
	public QuickAction createNew() {
		if(cl != null) {
			try {
				return cl.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new UnsupportedOperationException(e);
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@NonNull
	public QuickAction createNew(@NonNull QuickAction action) {
		if (cl != null) {
			try {
				return cl.getConstructor(QuickAction.class).newInstance(action);
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				throw new UnsupportedOperationException(e);
			}
		} else {
			return new QuickAction(action);
		}
	}

	public int getId() {
		return id;
	}

	public String getStringId() {
		return stringId;
	}

	public boolean isActionEditable() {
		return actionEditable;
	}

	public boolean shouldUseExtendedName() {
		return !actionEditable || forceUseExtendedName;
	}

	public int getNameRes() {
		return nameRes;
	}

	public int getActionNameRes() {
		return nameActionRes;
	}

	public int getIconRes() {
		return iconRes;
	}

	public int getCategory() {
		return category;
	}

	@NonNull
	public String getFullName(@NonNull OsmandApplication app) {
		String quickActionTypeName;
		if (getActionNameRes() != 0) {
			String name = app.getString(getNameRes());
			String actionName = app.getString(getActionNameRes());
			quickActionTypeName = app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, name);
		} else {
			quickActionTypeName = app.getString(getNameRes());
		}
		return quickActionTypeName;
	}
}
