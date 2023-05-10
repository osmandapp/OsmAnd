package net.osmand.plus.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import net.osmand.plus.OsmandApplication

abstract class BaseOsmAndAndroidAutoScreen(carContext: CarContext) : Screen(carContext) {

    protected val app: OsmandApplication
        get() {
            return carContext.applicationContext as OsmandApplication
        }
    protected var contentLimit: Int = 0
        private set

    init {
        initContentLimit()
    }

    private fun initContentLimit() {
        val manager = carContext.getCarService(
            ConstraintManager::class.java)
        contentLimit = DEFAULT_CONTENT_LIMIT.coerceAtMost(
            manager.getContentLimit(getConstraintLimitType()))
    }

    protected open fun getConstraintLimitType(): Int {
        return ConstraintManager.CONTENT_LIMIT_TYPE_LIST
    }

    companion object {
        private const val DEFAULT_CONTENT_LIMIT = 12
    }
}