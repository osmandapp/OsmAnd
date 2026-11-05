package net.osmand.plus.profiles;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.settings.backend.ApplicationMode;

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
