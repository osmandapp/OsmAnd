package net.osmand.plus.feedback;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

public enum RateUsState {
	INITIAL_STATE,
	IGNORED,
	LIKED,
	DISLIKED_WITH_MESSAGE,
	DISLIKED_WITHOUT_MESSAGE,
	DISLIKED_OR_IGNORED_AGAIN;

	@NonNull
	public static RateUsState getNewState(@NonNull OsmandApplication app, @NonNull RateUsState requiredState) {
		RateUsState currentState = app.getSettings().RATE_US_STATE.get();
		switch (requiredState) {
			case INITIAL_STATE:
			case LIKED:
			case DISLIKED_OR_IGNORED_AGAIN:
				return requiredState;
			case IGNORED:
			case DISLIKED_WITH_MESSAGE:
			case DISLIKED_WITHOUT_MESSAGE:
				return currentState == INITIAL_STATE ? requiredState : DISLIKED_OR_IGNORED_AGAIN;
		}
		return requiredState;
	}
}
