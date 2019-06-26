package net.osmand.turnScreenOn.listener;

public interface MessageObservable {
    void addListener(OnMessageListener listener);
    void removeListener(OnMessageListener listener);

    void notifyListeners();
}
