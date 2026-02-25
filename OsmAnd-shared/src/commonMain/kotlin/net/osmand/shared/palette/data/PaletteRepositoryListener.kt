package net.osmand.shared.palette.data

/**
 * Listener interface for receiving notifications about changes in the PaletteRepository.
 */
interface PaletteRepositoryListener {

	/**
	 * Called when a change occurs in the repository.
	 *
	 * @param event The event describing the type of change
	 * (Added, Removed, Updated, Replaced).
	 */
	fun onPaletteChanged(event: PaletteChangeEvent)
}