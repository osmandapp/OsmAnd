package net.osmand.plus.routing;

import androidx.annotation.NonNull;

import net.osmand.plus.voice.CommandBuilder;

/**
 * Command to wait until voice player is initialized
 */
record VoiceCommandPending(int type, VoiceRouter voiceRouter) {

	public static final int ROUTE_CALCULATED = 1;
	public static final int ROUTE_RECALCULATED = 2;

	VoiceCommandPending(int type, @NonNull VoiceRouter voiceRouter) {
		this.type = type;
		this.voiceRouter = voiceRouter;
	}

	public void play(@NonNull CommandBuilder newCommand) {
		int left = voiceRouter.router.getLeftDistance();
		int time = voiceRouter.router.getLeftTime();
		if (left > 0) {
			if (type == ROUTE_CALCULATED) {
				newCommand.newRouteCalculated(left, time);
			} else if (type == ROUTE_RECALCULATED) {
				newCommand.routeRecalculated(left, time);
			}
			voiceRouter.play(newCommand);
		}
	}
}
