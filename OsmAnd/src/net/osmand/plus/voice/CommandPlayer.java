package net.osmand.plus.voice;

import java.util.List;

import alice.tuprolog.Struct;

public interface CommandPlayer {

	public String getCurrentVoice();

	public CommandBuilder newCommandBuilder();

	public List<String> playCommands(CommandBuilder builder);

	public void clear();

	public List<String> execute(List<Struct> listStruct);
	
	public void updateAudioStream(int streamType);

    public String getLanguage();
    
    public boolean supportsStructuredStreetNames();

	public void stop();
}
