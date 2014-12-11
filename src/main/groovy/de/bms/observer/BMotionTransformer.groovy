package de.bms.observer

import com.google.gson.Gson
import de.bms.BMotion

abstract class BMotionTransformer extends BMotionObserver {

    protected final Gson g = new Gson()

    def abstract List<TransformerObject> update(BMotion bms)

    @Override
    def apply(BMotion bms) {
        bms.clients.each { it.sendEvent("applyTransformers", g.toJson(update(bms))) }
    }

}