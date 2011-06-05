package net.osmand.plus.activities;

import java.util.Locale;

import net.osmand.LogUtil;
import net.osmand.plus.voice.TTSOsmand;

import org.apache.commons.logging.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class InitTTSActivity extends Activity implements OnInitListener {

	private static final Log log = LogUtil
	.getLog(InitTTSActivity.class);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTts = new TextToSpeech(this, this);
		final int languageAvailable = mTts.isLanguageAvailable(Locale.FRANCE);
		log.info("Language availbility:" + languageAvailable);
	}

	private TextToSpeech mTts;

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			// success, create the TTS instance
			int result = mTts.setLanguage(Locale.US);
			TTSOsmand.mTts = mTts;
            // Try this someday for some interesting results.
            // int result mTts.setLanguage(Locale.FRANCE);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            	//TODO
            }
		} else {
			// missing data, install it
			if (mTts != null) {
				mTts.speak("Needs to install data!", TextToSpeech.QUEUE_ADD, null);
				Intent installIntent = new Intent();
				installIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			} else {
				log.info("onInit called again with status: " + status);
			}
		}
		finish();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mTts != null) {
			mTts.shutdown();
			mTts = null;
		}
	}
}
