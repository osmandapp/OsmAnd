package net.osmand.plus.voice;

import java.io.File;
import java.util.List;
import java.util.Locale;

import net.osmand.Algoritms;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsActivity;
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
		private final Context ctx;
		private final String intentAction;
		private final Uri intentData;

		private IntentStarter(Context ctx, String intentAction) {
			this(ctx,intentAction, null);
		}

		private IntentStarter(Context ctx, String intentAction, Uri intentData) {
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
	private static final int[] TTS_VOICE_VERSION = new int[] { 100, 101 }; // !! MUST BE SORTED
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
		initializeEngine(ctx.getApplicationContext(), ctx);
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

	private void initializeEngine(final Context ctx, final Activity act)
	{
		if (mTts != null && mTtsContext != ctx) {
			internalClear();
		}
		if (mTts == null) {
			mTtsContext = ctx;
			mTts = new TextToSpeech(ctx, new OnInitListener() {
				@Override
				public void onInit(int status) {
					if (status != TextToSpeech.SUCCESS) {
						internalClear();
					} else if (mTts != null) {
						switch (mTts.isLanguageAvailable(new Locale(language)))
						{
							case TextToSpeech.LANG_MISSING_DATA:
								if (isSettingsActivity(act)) {
									Builder builder = createAlertDialog(
										R.string.tts_missing_language_data_title,
										R.string.tts_missing_language_data,
										new IntentStarter(
												act,
												TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
										act);
									builder.show();
								}
								break;
							case TextToSpeech.LANG_AVAILABLE:
								mTts.setLanguage(new Locale(language));
								break;
							case TextToSpeech.LANG_NOT_SUPPORTED:
								//maybe weird, but I didn't want to introduce parameter in around 5 methods just to do
								//this if condition
								if (isSettingsActivity(act)) {
									Builder builder = createAlertDialog(
											R.string.tts_language_not_supported_title,
											R.string.tts_language_not_supported,
											new IntentStarter(
													act,
													Intent.ACTION_VIEW, Uri.parse("market://search?q=text to speech engine"
														)),
											act);
									builder.show();
								}
								break;
						}
					}
				}

				private boolean isSettingsActivity(final Context ctx) {
					return ctx instanceof SettingsActivity;
				}
			});
		}
	}
	
	private Builder createAlertDialog(int titleResID, int messageResID,
			IntentStarter intentStarter, final Activity ctx) {
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setCancelable(true);
		builder.setNegativeButton(R.string.default_buttons_no, null);
		builder.setPositiveButton(R.string.default_buttons_yes, intentStarter);
		builder.setTitle(titleResID);
		builder.setMessage(messageResID);
		return builder;
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
