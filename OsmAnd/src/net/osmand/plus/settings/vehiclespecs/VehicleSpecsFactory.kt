package net.osmand.plus.settings.vehiclespecs

import net.osmand.plus.settings.vehiclespecs.profiles.BicycleSpecs
import net.osmand.plus.settings.vehiclespecs.profiles.BoatSpecs
import net.osmand.plus.settings.vehiclespecs.profiles.CarSpecs
import net.osmand.plus.settings.vehiclespecs.profiles.MotorcycleSpecs
import net.osmand.plus.settings.vehiclespecs.profiles.TruckSpecs
import net.osmand.plus.settings.vehiclespecs.profiles.VehicleSpecs
import net.osmand.router.GeneralRouter.GeneralRouterProfile

object VehicleSpecsFactory {

    private const val DERIVED_PROFILE_TRUCK = "Truck"
    private const val DERIVED_PROFILE_MOTORCYCLE = "Motorcycle"

    @JvmStatic
    fun createSpecs(profile: GeneralRouterProfile?, derivedProfile: String?): VehicleSpecs? {
        return when (profile) {
            GeneralRouterProfile.BOAT -> BoatSpecs()
            GeneralRouterProfile.BICYCLE -> BicycleSpecs()
            GeneralRouterProfile.CAR -> when {
                DERIVED_PROFILE_TRUCK.equals(derivedProfile, ignoreCase = true) -> TruckSpecs()
                DERIVED_PROFILE_MOTORCYCLE.equals(derivedProfile, ignoreCase = true) -> MotorcycleSpecs()
                else -> CarSpecs()
            }
            else -> null
        }
    }
}