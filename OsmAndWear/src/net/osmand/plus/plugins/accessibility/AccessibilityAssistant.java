package net.osmand.plus.plugins.accessibility;

import android.app.Activity;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import net.osmand.plus.OsmandApplication;

public class AccessibilityAssistant extends AccessibilityDelegateCompat implements OnPageChangeListener {

    private final Activity hostActivity;
    private final OsmandApplication app;

    private volatile boolean discourageUiUpdates;
    private volatile boolean eventsLocked;
    private volatile View focusedView;

    private final SparseArray<View> monitoredPages;
    private View visiblePage;
    private int visiblePageId;

    public AccessibilityAssistant(Activity activity) {
        hostActivity = activity;
        app = (OsmandApplication)(activity.getApplication());
        discourageUiUpdates = false;
        eventsLocked = false;
        focusedView = null;
        monitoredPages = new SparseArray<>();
        visiblePage = null;
        visiblePageId = 0;
    }

    public boolean isUiUpdateDiscouraged() {
        return discourageUiUpdates && app.accessibilityEnabled();
    }

    public View getFocusedView() {
        return focusedView;
    }

    public void lockEvents() {
        eventsLocked = true;
    }

    public void unlockEvents() {
        if (!hostActivity.getWindow().getDecorView().post(() -> eventsLocked = false))
            eventsLocked = false;
    }

    public void forgetFocus() {
        focusedView = null;
    }

    public void registerPage(View page, int id) {
        monitoredPages.put(id, page);
        if (id == visiblePageId)
            visiblePage = page;
        ViewCompat.setAccessibilityDelegate(page, this);
    }


    @Override
    public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
        return ((monitoredPages.indexOfValue(host) < 0) || (host == visiblePage)) && super.onRequestSendAccessibilityEvent(host, child, event);
    }

    @Override
    public void sendAccessibilityEvent(View host, int eventType) {
        boolean passed = !eventsLocked;
        if (passed)
            super.sendAccessibilityEvent(host, eventType);
        notifyEvent(host, eventType, passed);
    }

    @Override
    public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
        boolean passed = !eventsLocked;
        int eventType = event.getEventType();
        if (passed)
            super.sendAccessibilityEventUnchecked(host, event);
        notifyEvent(host, eventType, passed);
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        visiblePageId = position;
        visiblePage = monitoredPages.get(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }


    private void processFocusChange(View view, boolean isFocused, boolean eventPassed) {
        if (view.isClickable() && ((view instanceof ImageView) || (view instanceof ImageButton) || (view instanceof Button))) {
            discourageUiUpdates = isFocused;
        } else if (eventPassed || (Build.VERSION.SDK_INT != 17)) {
            focusedView = isFocused ? view : null;
        }
    }

    private void notifyEvent(View view, int eventType, boolean passed) {
        if (Build.VERSION.SDK_INT >= 16) {
            switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                processFocusChange(view, true, passed);
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
                processFocusChange(view, false, passed);
                break;
            default:
                break;
            }
        }
    }

}
