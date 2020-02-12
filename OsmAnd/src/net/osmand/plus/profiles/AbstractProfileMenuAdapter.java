package net.osmand.plus.profiles;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import net.osmand.plus.ApplicationMode;

public abstract class AbstractProfileMenuAdapter<T extends ProfileAbstractViewHolder> extends RecyclerView.Adapter<T> {
	protected ProfilePressedListener profilePressedListener;
	protected ButtonPressedListener buttonPressedListener;

	public void setProfilePressedListener(@Nullable ProfilePressedListener profilePressedListener) {
		this.profilePressedListener = profilePressedListener;
	}

	public void setButtonPressedListener(@Nullable ButtonPressedListener buttonPressedListener) {
		this.buttonPressedListener = buttonPressedListener;
	}

	public interface ProfilePressedListener {
		void onProfilePressed(ApplicationMode item);
	}
	
	public interface ButtonPressedListener {
		void onButtonPressed();
	}
}
