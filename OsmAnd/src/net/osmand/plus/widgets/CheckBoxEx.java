package net.osmand.plus.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

public class CheckBoxEx extends AppCompatCheckBox {
    private CompoundButtonExAttributes compoundButtonExAttributes = new CompoundButtonExAttributes();

    public CheckBoxEx(Context context) {
        super(context);
    }

    public CheckBoxEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        CompoundButtonExAttributes.parseAttributes(this, compoundButtonExAttributes, attrs, 0, 0);
    }

    public CheckBoxEx(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        CompoundButtonExAttributes.parseAttributes(this, compoundButtonExAttributes, attrs, defStyleAttr, 0);
    }

    public CompoundButtonExAttributes getAttributes() {
        return compoundButtonExAttributes;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        compoundButtonExAttributes.onDraw(getContext(), this);
    }
}