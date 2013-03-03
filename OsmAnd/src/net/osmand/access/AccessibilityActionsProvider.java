package net.osmand.access;

// This interface is intended for defining prioritized actions
// to be performed in touch exploration mode. Implementations
// should do nothing and return false when accessibility is disabled.
public interface AccessibilityActionsProvider {
    public boolean onClick();
    public boolean onLongClick();
}
