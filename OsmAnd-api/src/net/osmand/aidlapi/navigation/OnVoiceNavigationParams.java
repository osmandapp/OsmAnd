package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;

public class OnVoiceNavigationParams extends AidlParams {

	private ArrayList<String> cmds = new ArrayList<>();
	private ArrayList<String> played = new ArrayList<>();

	public OnVoiceNavigationParams() {

	}

	public OnVoiceNavigationParams(List<String> cmds, List<String> played) {
		if (cmds != null) {
			this.cmds.addAll(cmds);
		}
		if (played != null) {
			this.played.addAll(played);
		}
	}

	public OnVoiceNavigationParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<OnVoiceNavigationParams> CREATOR = new Creator<OnVoiceNavigationParams>() {
		@Override
		public OnVoiceNavigationParams createFromParcel(Parcel in) {
			return new OnVoiceNavigationParams(in);
		}

		@Override
		public OnVoiceNavigationParams[] newArray(int size) {
			return new OnVoiceNavigationParams[size];
		}
	};

	public List<String> getCommands() {
		return cmds;
	}

	public List<String> getPlayed() {
		return played;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putStringArrayList("cmds", cmds);
		bundle.putStringArrayList("played", played);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		cmds = bundle.getStringArrayList("cmds");
		if (cmds == null) {
			cmds = new ArrayList<>();
		}
		played = bundle.getStringArrayList("played");
		if (played == null) {
			played = new ArrayList<>();
		}
	}
}