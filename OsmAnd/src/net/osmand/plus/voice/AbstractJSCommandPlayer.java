package net.osmand.plus.voice;

import java.util.List;

import alice.tuprolog.Struct;

public class AbstractJSCommandPlayer implements CommandPlayer {
    @Override
    public String getCurrentVoice() {
        return null;
    }

    @Override
    public CommandBuilder newCommandBuilder() {
        JSCommandBuilder commandBuilder = new JSCommandBuilder(this);
        commandBuilder.setParameters("km-m", true);
        return commandBuilder;
    }

    @Override
    public void playCommands(CommandBuilder builder) {

    }

    @Override
    public void clear() {

    }

    @Override
    public List<String> execute(List<Struct> listStruct) {
        return null;
    }

    @Override
    public void updateAudioStream(int streamType) {

    }

    @Override
    public String getLanguage() {
        return "en";
    }

    @Override
    public boolean supportsStructuredStreetNames() {
        return true;
    }

    @Override
    public void stop() {

    }
}
