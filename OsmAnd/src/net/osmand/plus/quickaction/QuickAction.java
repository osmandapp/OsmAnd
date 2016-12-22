package net.osmand.plus.quickaction;


import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.ViewGroup;

import java.util.HashMap;

public class QuickAction {

    protected int id;
    protected @StringRes int nameRes;
    protected @DrawableRes int iconRes;

    private HashMap<String, String> params;

    protected QuickAction() {
    }

    public QuickAction(QuickAction quickAction) {
        this.id = quickAction.id;
        this.nameRes = quickAction.nameRes;
        this.iconRes = quickAction.iconRes;
        this.params = quickAction.params;
    }

    public int getNameRes() {
        return nameRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    public int getId() {
        return id;
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public void execute(){};
    public void drawUI(ViewGroup parent){};
}

