package net.osmand.plus.quickaction;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.ViewGroup;

import net.osmand.plus.activities.MapActivity;

import java.util.HashMap;

public class QuickAction {

    public interface QuickActionSelectionListener {

        void onActionSelected(QuickAction action);
    }

    protected int type;
    protected long id;
    protected @StringRes int nameRes;
    protected @DrawableRes int iconRes;

    private String name;
    private HashMap<String, String> params;

    protected QuickAction() {
        this.id = System.currentTimeMillis();
    }

    protected QuickAction(int type) {
        this.id = System.currentTimeMillis();
        this.type = type;
    }

    public QuickAction(QuickAction quickAction) {
        this.type = quickAction.type;
        this.id = quickAction.id;
        this.nameRes = quickAction.nameRes;
        this.iconRes = quickAction.iconRes;
        this.name = quickAction.name;
        this.params = quickAction.params;
    }

    public int getNameRes() {
        return nameRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    public long getId() {
        return id;
    }

    public String getName(Context context) {
        return name == null || name.isEmpty() ? nameRes > 0 ? context.getString(nameRes) : "" : name;
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParams(HashMap<String, String> params) {
        this.params = params;
    }

    public void execute(MapActivity activity){};
    public void drawUI(ViewGroup parent){};
    public void fillParams(){};


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuickAction action = (QuickAction) o;

        if (type != action.type) return false;
        if (id != action.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type;
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + nameRes;
        result = 31 * result + iconRes;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

