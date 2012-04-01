package net.osmand.access;

import net.osmand.access.AccessibleLayout;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

// This class serves as a delegate of accessibility service
// providing a sort of touch exploration capability
// for a View hierarchy. It means that elements will be spoken
// on touch. Thus, you can slide your finger across the screen
// and hear available controls and items.
// Lift finger up on a control to make click.
//
// This class can not be instantiated directly.
// Use static method takeCareOf() to get it's functionality
// for respective objects.
//
public class AccessibilityDelegate extends AccessibleLayout {

    private AccessibilityDelegate(Context context) {
        super(context);
    }

    // Attach itself to a target View hierarchy to intercept touch events
    // and provide on-touch accessibility feedback.
    // Target View must be an instance of FrameLayout
    // or have a parent which is an instance of ViewGroup.
    private void attach(View target) {
        ViewGroup parent;
        if (target instanceof FrameLayout) {
            parent = (ViewGroup)target;
            while (parent.getChildCount() > 0) {
                View child = parent.getChildAt(0);
                parent.removeViewAt(0);
                addView(child);
            }
            parent.addView(this, new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        } else if (target.getParent() instanceof ViewGroup) {
            parent = (ViewGroup)target.getParent();
            int position = parent.indexOfChild(target);
            ViewGroup.LayoutParams params = target.getLayoutParams();
            parent.removeViewAt(position);
            addView(target, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            parent.addView(this, position, params);
        }
    }


    // Provide touch exploration capability for individual View
    // or whole View hierarchy. The hierarchy root specified
    // as an argument must either be an instance of FrameLayout
    // or have a parent that is an instance of ViewGroup.
    public static void takeCareOf(View hierarchy) {
        final AccessibilityDelegate delegate = new AccessibilityDelegate(hierarchy.getContext());
        delegate.attach(hierarchy);
    }

    // Provide touch exploration capability for given window.
    public static void takeCareOf(Window window) {
        takeCareOf(window.getDecorView());
    }

    // Provide touch exploration capability for an activity View content.
    // Use after setContentView().
    public static void takeCareOf(Activity activity) {
        takeCareOf(activity.getWindow());
    }

    // Provide touch exploration capability for a dialog View content.
    // Use after setContentView().
    public static void takeCareOf(Dialog dialog) {
        takeCareOf(dialog.getWindow());
    }

}
