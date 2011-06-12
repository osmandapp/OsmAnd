package net.osmand.plus.voice;

import java.io.File;
import java.util.List;
import java.util.Locale;

import net.osmand.Algoritms;
import net.osmand.plus.R;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class TTSCommandPlayerImpl extends AbstractPrologCommandPlayer {

	private final class IntentStarter implements
			DialogInterface.OnClickListener {
		private final Activity ctx;
		private final String intentAction;
		private final Uri intentData;

		private IntentStarter(Activity ctx, String intentAction) {
			this(ctx,intentAction, null);
		}

		private IntentStarter(Activity ctx, String intentAction, Uri intentData) {
			this.ctx = ctx;
			this.intentAction = intentAction;
			this.intentData = intentData;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			Intent installIntent = new Intent();
			installIntent.setAction(intentAction);
			if (intentData != null) {
				installIntent.setData(intentData);
			}
			ctx.startActivity(installIntent);
		}
	}

	private static final String CONFIG_FILE = "_ttsconfig.p";
	private static final int TTS_VOICE_VERSION = 100;
	private TextToSpeech mTts;
	private Context mTtsContext;
	private String language;

	protected TTSCommandPlayerImpl(Activity ctx, String voiceProvider)
			throws CommandPlayerException {
		super(ctx, voiceProvider, CONFIG_FILE, TTS_VOICE_VERSION);
		final Term langVal = solveSimplePredicate("language");
		if (langVal instanceof Struct) {
			language = ((Struct) langVal).getName();
		}
		if (Algoritms.isEmpty(language)) {
			throw new CommandPlayerException(
					ctx.getString(R.string.voice_data_corrupted));
		}
		onActivityInit(ctx);
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
	public void onActivityInit(final Activity ctx) {
		if (mTts != null && mTtsContext != ctx) {
			//clear only, if the mTts was initialized in another context.
			//Unfortunately, for example from settings to map first the map is initialized than
			//the settingsactivity is destroyed...
			internalClear();
		}
		if (mTts == null) {
			mTtsContext = ctx;
			mTts = new TextToSpeech(ctx, new OnInitListener() {
				@Override
				public void onInit(int status) {
					if (status != TextToSpeech.SUCCESS) {
						internalClear();
					} else {
						switch (mTts.isLanguageAvailable(new Locale(language)))
						{
							case TextToSpeech.LANG_MISSING_DATA:
								internalClear();
								Builder builder = createAlertDialog(
									R.string.tts_missing_language_data_title,
									R.string.tts_missing_language_data,
									new IntentStarter(
											ctx,
											TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
									ctx);
								builder.show();
								break;
							case TextToSpeech.LANG_AVAILABLE:
								mTts.setLanguage(new Locale(language));
								break;
							case TextToSpeech.LANG_NOT_SUPPORTED:
								internalClear();
								builder = createAlertDialog(
										R.string.tts_language_not_supported_title,
										R.string.tts_language_not_supported,
										new IntentStarter(
												ctx,
												Intent.ACTION_VIEW, Uri.parse("market://search?q=text to speech engine"
													)),
										ctx);
								builder.show();
								break;
						}
					}
				}

	
			});
		}
	}
	
	private Builder createAlertDialog(int titleResID, int messageResID, IntentStarter intentStarter, final Activity ctx) {
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setCancelable(true);
		builder.setNegativeButton(R.string.default_buttons_no, null);
		builder.setPositiveButton(R.string.default_buttons_yes, intentStarter);
		builder.setTitle(titleResID);
		builder.setMessage(messageResID);
		return builder;
		}
		
	@Override
	public void onActvitiyStop(Context ctx) {
		//stop only when the context is the same
		if (mTtsContext == ctx) {
			internalClear();
		}
	}

	private void internalClear() {
		if (mTts != null) {
			mTts.shutdown();
			mTtsContext = null;
			mTts = null;
		}
	}
	
	@Override
	public void clear() {
		super.clear();
		internalClear();
	}
	
	public static boolean isMyData(File voiceDir) throws CommandPlayerException {
		return new File(voiceDir, CONFIG_FILE).exists();
	}

}
