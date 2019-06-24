package net.osmand.turnScreenOn.listener;

public interface Observable {
    void addListener(OnMessageListener listener);
    void removeListener(OnMessageListener listener);

    void notifyListeners();
}
