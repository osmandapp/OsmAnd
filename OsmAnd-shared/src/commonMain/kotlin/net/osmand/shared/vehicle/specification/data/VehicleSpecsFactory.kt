package net.osmand.shared.vehicle.specification.data

import net.osmand.shared.routing.GeneralRouterProfile
import net.osmand.shared.vehicle.specification.domain.profiles.BicycleSpecs
import net.osmand.shared.vehicle.specification.domain.profiles.BoatSpecs
import net.osmand.shared.vehicle.specification.domain.profiles.CarSpecs
import net.osmand.shared.vehicle.specification.domain.profiles.MotorcycleSpecs
import net.osmand.shared.vehicle.specification.domain.profiles.TruckSpecs
import net.osmand.shared.vehicle.specification.domain.profiles.VehicleSpecs
import kotlin.jvm.JvmStatic

object VehicleSpecsFactory {

    private const val DERIVED_PROFILE_TRUCK = "Truck"
    private const val DERIVED_PROFILE_MOTORCYCLE = "Motorcycle"

    @JvmStatic
    fun createSpecifications(profile: GeneralRouterProfile?, derivedProfile: String?): VehicleSpecs? {
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