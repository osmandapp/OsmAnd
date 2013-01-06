package net.osmand.plus.voice;

import java.io.File;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import android.app.Activity;

public class CommandPlayerFactory 
{
	public static CommandPlayer createCommandPlayer(String voiceProvider, OsmandApplication osmandApplication, Activity ctx)
		throws CommandPlayerException
	{
		if (voiceProvider != null){
			File parent = osmandApplication.getInternalAPI().getAppDir(ResourceManager.VOICE_PATH);
			File voiceDir = new File(parent, voiceProvider);
			if(!voiceDir.exists()){
				throw new CommandPlayerException(ctx.getString(R.string.voice_data_unavailable));
			}
			
			if (MediaCommandPlayerImpl.isMyData(voiceDir)) {
				return new MediaCommandPlayerImpl(osmandApplication, voiceProvider);
			} else if (TTSCommandPlayerImpl.isMyData(voiceDir)) {
				return new TTSCommandPlayerImpl(ctx, voiceProvider);
			}
			throw new CommandPlayerException(ctx.getString(R.string.voice_data_not_supported));
		}
		return null;
	}
}
