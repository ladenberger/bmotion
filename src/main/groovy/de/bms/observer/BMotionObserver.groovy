package de.bms.observer

import de.bms.BMotion

abstract class BMotionObserver {

    def abstract apply(BMotion bms)

    def BMotionObserver register(BMotion bms, String trigger = BMotion.TRIGGER_ANIMATION_CHANGED) {
        bms.registerObserver(this, trigger)
        this
    }

}