package net.osmand.plus.voice;

import java.io.File;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.OsmandApplication;
import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class CommandPlayerFactory 
{

	private static TextToSpeech mTts;

	public static void onActivityInit(Activity ctx) {
		if (mTts != null) {
			mTts.shutdown();
		}
		mTts = new TextToSpeech(ctx, new OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.SUCCESS) {
					mTts = null;
				}
			}
		});
	}
	
	public static void onActivityStop(Activity ctx) {
		if (mTts != null) {
			mTts.shutdown();
			mTts = null;
		}
	}
	
	public static CommandPlayer createCommandPlayer(String voiceProvider, OsmandApplication osmandApplication, Context ctx)
		throws CommandPlayerException
	{
		if (voiceProvider != null){
			File parent = OsmandSettings.getOsmandSettings(ctx).extendOsmandPath(ResourceManager.VOICE_PATH);
			File voiceDir = new File(parent, voiceProvider);
			if(!voiceDir.exists()){
				throw new CommandPlayerException(ctx.getString(R.string.voice_data_unavailable));
			}
			if (MediaCommandPlayerImpl.isMyData(voiceDir)) {
				return new MediaCommandPlayerImpl(osmandApplication, voiceProvider);
			} else if (TTSCommandPlayerImpl.isMyData(voiceDir, mTts)) {
				return new TTSCommandPlayerImpl(osmandApplication, voiceProvider, mTts);
			}
			throw new CommandPlayerException(ctx.getString(R.string.voice_data_not_supported));
		}
		return null;
	}
}
