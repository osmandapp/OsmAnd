package net.osmand.plus.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.AttributeSet;

public class RadioButtonEx extends AppCompatRadioButton {
    private CompoundButtonExAttributes compoundButtonExAttributes = new CompoundButtonExAttributes();

    public RadioButtonEx(Context context) {
        super(context);
    }

    public RadioButtonEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        CompoundButtonExAttributes.parseAttributes(this, compoundButtonExAttributes, attrs, 0, 0);
    }

    public RadioButtonEx(Context context, AttributeSet attrs, int defStyleAttr) {
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