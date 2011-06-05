package net.osmand.plus.voice;

import java.io.File;
import java.util.List;

import net.osmand.plus.activities.InitTTSActivity;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;

public class TTSCommandPlayerImpl extends AbstractPrologCommandPlayer {

	private static final String CONFIG_FILE = "_ttsconfig.p";

	protected TTSCommandPlayerImpl(Context ctx, String voiceProvider) throws CommandPlayerException {
		super(ctx, voiceProvider, CONFIG_FILE);
		final Intent intent = new Intent(ctx, InitTTSActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ctx.startActivity(intent);
	}

	@Override
	public void playCommands(CommandBuilder builder) {
		if (TTSOsmand.mTts != null) {
			final List<String> execute = builder.execute(); //list of strings, the speech text, play it
			StringBuilder bld = new StringBuilder();
			for (String s : execute) {
				bld.append(s).append(' ');
			}
			TTSOsmand.mTts.speak(bld.toString(), TextToSpeech.QUEUE_ADD, null);
		}
	}

	public static boolean isMyData(File voiceDir) {
		return new File(voiceDir, CONFIG_FILE).exists();
	}

}
