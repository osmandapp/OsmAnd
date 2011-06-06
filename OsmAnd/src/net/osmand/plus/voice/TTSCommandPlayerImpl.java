package net.osmand.plus.voice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import net.osmand.Algoritms;
import net.osmand.plus.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class TTSCommandPlayerImpl extends AbstractPrologCommandPlayer {

	private static final String CONFIG_FILE = "_ttsconfig.p";
	private TextToSpeech mTts;
	private String language;

	protected TTSCommandPlayerImpl(Context ctx, String voiceProvider, TextToSpeech mTts) throws CommandPlayerException {
		super(ctx, voiceProvider, CONFIG_FILE);
		if (mTts == null) {
			throw new CommandPlayerException(ctx.getString(R.string.voice_data_unavailable));
		}
		final File config = new File(voiceDir, CONFIG_FILE);
		if (config.exists()) {
			//we check if the language is available
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(config));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line.matches("language(.*)")) {
						language = line.substring("language(".length(), line.lastIndexOf(')'));
						break;
					}
				}
			} catch (IOException e) {
				//error occurred while parsing config file
			} finally {
				Algoritms.closeStream(reader);
			}
			if (language == null) {
				throw new CommandPlayerException(ctx.getString(R.string.voice_data_unavailable));
			}
			switch (mTts.isLanguageAvailable(new Locale(language)))
			{
				case TextToSpeech.LANG_MISSING_DATA:
					Intent installIntent = new Intent();
					installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
					throw new CommandPlayerException(ctx.getString(R.string.voice_data_unavailable, installIntent));
				case TextToSpeech.LANG_AVAILABLE:
					//ok;
					break;
				case TextToSpeech.LANG_NOT_SUPPORTED:
					throw new CommandPlayerException(ctx.getString(R.string.voice_data_not_supported));
			}
		}
	}

	@Override
	public void playCommands(CommandBuilder builder) {
		if (mTts != null) {
			final List<String> execute = builder.execute(); //list of strings, the speech text, play it
			StringBuilder bld = new StringBuilder();
			for (String s : execute) {
				bld.append(s).append(' ');
			}
			mTts.speak(bld.toString(), TextToSpeech.QUEUE_ADD, null);
		}
	}

	@Override
	public void onActivityInit(Activity ctx) {
		if (mTts != null) {
			mTts.shutdown();
		}
		mTts = new TextToSpeech(ctx, new OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.SUCCESS) {
					mTts = null;
				} else {
					mTts.setLanguage(new Locale(language));
				}
			}
		});
	}
	
	@Override
	public void onActvitiyStop(Activity ctx) {
		internalClear();
	}

	private void internalClear() {
		if (mTts != null) {
			mTts.shutdown();
			mTts = null;
		}
	}
	
	@Override
	public void clear() {
		super.clear();
		internalClear();
	}
	
	public static boolean isMyData(File voiceDir, TextToSpeech mTts) throws CommandPlayerException {
		if (mTts == null) {
			throw new CommandPlayerException(Resources.getSystem().getString(R.string.voice_data_unavailable));
		}
		return new File(voiceDir, CONFIG_FILE).exists();
	}

}
