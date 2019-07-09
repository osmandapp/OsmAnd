package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class ANavigationVoiceRouterMessageParams implements Parcelable {
	private List<String> voiceCommands;

	protected ANavigationVoiceRouterMessageParams(Parcel in) {
		readFromParcel(in);
	}

	public ANavigationVoiceRouterMessageParams(List<String> commands) {
		this.voiceCommands = commands;
	}

	private void readFromParcel(Parcel in) {
		in.readStringList(voiceCommands);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringList(voiceCommands);
	}

	public static final Parcelable.Creator<ANavigationVoiceRouterMessageParams> CREATOR = new Parcelable.Creator<ANavigationVoiceRouterMessageParams>() {
		@Override
		public ANavigationVoiceRouterMessageParams createFromParcel(Parcel in) {
			return new ANavigationVoiceRouterMessageParams(in);
		}

		@Override
		public ANavigationVoiceRouterMessageParams[] newArray(int size) {
			return new ANavigationVoiceRouterMessageParams[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}
}
