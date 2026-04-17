package net.osmand.shared.routing

enum class GeneralRouterProfile {
	CAR,
	PEDESTRIAN,
	BICYCLE,
	BOAT,
	SKI,
	MOPED,
	TRAIN,
	PUBLIC_TRANSPORT,
	HORSEBACKRIDING;

	fun getBaseProfile() = name.lowercase()
}
