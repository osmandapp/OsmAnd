package net.osmand.plus.quickaction;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

/**
 * Created by okorsun on 20.12.16.
 */

public class QuickActionItem {
    private static final int HEADER_VALUE = -1;

    private final int nameRes;
    private final int drawableRes;

    public QuickActionItem(@StringRes int nameRes, @DrawableRes int drawableRes) {
        this.nameRes = nameRes;
        this.drawableRes = drawableRes;
    }

    private QuickActionItem(){
        nameRes = HEADER_VALUE;
        drawableRes = HEADER_VALUE;
    }

    public static QuickActionItem createHeaderItem() {
        return new QuickActionItem();
    }

    public int getNameRes() {
        return nameRes;
    }

    public int getDrawableRes() {
        return drawableRes;
    }

    public boolean isHeader() {
        return nameRes == HEADER_VALUE;
    }

//    public interface QuickActionListItem{
//        int getNameRes();
//        int getDrawableRes();
//        boolean isHeader();
//    }

}
