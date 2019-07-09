package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class ANavigationVoiceRouterRegistrationParams implements Parcelable{
    private boolean subscribeToUpdates = true;
    private long callbackId = -1L;

    public ANavigationVoiceRouterRegistrationParams() {
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

    protected ANavigationVoiceRouterRegistrationParams(Parcel in) {
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

    public static final Parcelable.Creator<ANavigationVoiceRouterRegistrationParams> CREATOR = new Parcelable.Creator<ANavigationVoiceRouterRegistrationParams>() {
        @Override
        public ANavigationVoiceRouterRegistrationParams createFromParcel(Parcel in) {
            return new ANavigationVoiceRouterRegistrationParams(in);
        }

        @Override
        public ANavigationVoiceRouterRegistrationParams[] newArray(int size) {
            return new ANavigationVoiceRouterRegistrationParams[size];
        }
    };
}
