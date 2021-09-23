package net.osmand.plus.voice;

import java.util.List;

public interface CommandPlayer {

	String getCurrentVoice();

	JsCommandBuilder newCommandBuilder();

	List<String> playCommands(JsCommandBuilder builder);

	void clear();

	void updateAudioStream(int streamType);

    String getLanguage();
    
    boolean supportsStructuredStreetNames();

	void stop();
}