package net.osmand.plus.quickaction;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.ViewGroup;

import java.util.HashMap;

public class QuickAction implements Parcelable {

    public interface QuickActionSelectionListener {

        void onActionSelected(QuickAction action);
    }

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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.nameRes);
        dest.writeInt(this.iconRes);
        dest.writeSerializable(this.params);
    }

    protected QuickAction(Parcel in) {
        this.id = in.readInt();
        this.nameRes = in.readInt();
        this.iconRes = in.readInt();
        this.params = (HashMap<String, String>) in.readSerializable();
    }

    public static final Parcelable.Creator<QuickAction> CREATOR = new Parcelable.Creator<QuickAction>() {
        @Override
        public QuickAction createFromParcel(Parcel source) {
            return new QuickAction(source);
        }

        @Override
        public QuickAction[] newArray(int size) {
            return new QuickAction[size];
        }
    };
}

