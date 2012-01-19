package net.osmand.plus.voice;

import java.util.List;

import alice.tuprolog.Struct;

public interface CommandPlayer {

	public abstract String getCurrentVoice();

	public abstract CommandBuilder newCommandBuilder();

	public abstract void playCommands(CommandBuilder builder);

	public abstract void clear();

	public abstract List<String> execute(List<Struct> listStruct);
}
