package net.osmand.plus.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

public class CompoundButtonExAttributes {
    private ApplicationMode appMode;
    private boolean profileDependent;
    private boolean nightMode;

    static void parseAttributes(CompoundButton compoundButton, CompoundButtonExAttributes target,
                                AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) {
            return;
        }

        TypedArray resolvedAttrs = compoundButton.getContext().getTheme().obtainStyledAttributes(attrs,
                R.styleable.CompoundButtonExAttributes, defStyleAttr, defStyleRes);
        applyAttributes(compoundButton, resolvedAttrs, target);
        resolvedAttrs.recycle();
    }

    private static void applyAttributes(CompoundButton compoundButton, TypedArray resolvedAttributes,
                                CompoundButtonExAttributes target) {
        if (!resolvedAttributes.hasValue(R.styleable.CompoundButtonExAttributes_profileDependent)
                || compoundButton.isInEditMode()) {
            return;
        }

        boolean profileDependent = resolvedAttributes.getBoolean(R.styleable.CompoundButtonExAttributes_profileDependent, false);
        boolean nightMode = resolvedAttributes.getBoolean(R.styleable.CompoundButtonExAttributes_nightMode, false);
        target.setProfileDependent(profileDependent);
        target.setNightMode(nightMode);
    }

    public void setProfileDependent(boolean profileDependent) {
        this.profileDependent = profileDependent;
        if (!profileDependent) {
            appMode = null;
        }
    }

    public void setProfileDependent(ApplicationMode dependedMode) {
        this.appMode = dependedMode;
        this.profileDependent = true;
    }

    public boolean isProfileDependent() {
        return profileDependent;
    }

    public ApplicationMode getAppMode() {
        return appMode;
    }

    public void setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    void onDraw(Context ctx, CompoundButton compoundButton) {
        if (profileDependent && appMode == null) {
            OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
            appMode = app.getSettings().getApplicationMode();
        }
        UiUtilities.setupCompoundButton(ctx, compoundButton, nightMode, appMode);
    }
}
