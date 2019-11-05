package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class ANavigationVoiceRouterMessageParams implements Parcelable{
    private boolean subscribeToUpdates = true;
    private long callbackId = -1L;

    public ANavigationVoiceRouterMessageParams() {
    }

    public long getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(long callbackId) {
        this.callbackId = callbackId;
    }

    public void setSubscribeToUpdates(boolean subscribeToUpdates) {
        this.subscribeToUpdates = subscribeToUpdates;
    }

    public boolean isSubscribeToUpdates() {
        return subscribeToUpdates;
    }

    protected ANavigationVoiceRouterMessageParams(Parcel in) {
        callbackId = in.readLong();
        subscribeToUpdates = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(callbackId);
        dest.writeByte((byte) (subscribeToUpdates ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ANavigationVoiceRouterMessageParams> CREATOR = new Creator<ANavigationVoiceRouterMessageParams>() {
        @Override
        public ANavigationVoiceRouterMessageParams createFromParcel(Parcel in) {
            return new ANavigationVoiceRouterMessageParams(in);
        }

        @Override
        public ANavigationVoiceRouterMessageParams[] newArray(int size) {
            return new ANavigationVoiceRouterMessageParams[size];
        }
    };
}
