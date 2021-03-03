package net.osmand.plus.quickaction;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.lang.reflect.InvocationTargetException;

public class QuickActionType {

	public static final int CREATE_CATEGORY = 0;
	public static final int CONFIGURE_MAP = 1;
	public static final int NAVIGATION = 2;
	public static final int CONFIGURE_SCREEN = 3;

	private final int id;
	private final String stringId;
	private boolean actionEditable;
	private @StringRes int nameRes;
	private @StringRes int nameActionRes;
	private @DrawableRes int iconRes;
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



	public QuickAction createNew() {
		if(cl != null) {
			try {
				return cl.newInstance();
			} catch (InstantiationException e) {
				throw new UnsupportedOperationException(e);
			} catch (IllegalAccessException e) {
				throw new UnsupportedOperationException(e);
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public QuickAction createNew(QuickAction q) {
		if(cl != null) {
			try {
				return cl.getConstructor(QuickAction.class).newInstance(q);
			} catch (InstantiationException e) {
				throw new UnsupportedOperationException(e);
			} catch (IllegalAccessException e) {
				throw new UnsupportedOperationException(e);
			} catch (NoSuchMethodException e) {
				throw new UnsupportedOperationException(e);
			} catch (InvocationTargetException e) {
				throw new UnsupportedOperationException(e);
			}
		} else {
			return new QuickAction(q);
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
}
