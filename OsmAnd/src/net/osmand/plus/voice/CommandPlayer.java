package net.osmand.plus.voice;

import java.util.List;

import alice.tuprolog.Struct;
import android.app.Activity;
import android.content.Context;

public interface CommandPlayer {

	public abstract String getCurrentVoice();

	public abstract CommandBuilder newCommandBuilder();

	public abstract void playCommands(CommandBuilder builder);

	public abstract void clear();

	public abstract List<String> execute(List<Struct> listStruct);
	
	public void onActivityInit(Activity ctx);
	
	public void onActvitiyStop(Context ctx);
}