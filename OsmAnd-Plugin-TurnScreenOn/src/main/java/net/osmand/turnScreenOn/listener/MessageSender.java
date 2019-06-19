package net.osmand.turnScreenOn.listener;

public interface MessageSender {
    void addListener(OnMessageListener listener);
    void removeListener(OnMessageListener listener);

    void notifyListeners();
}
