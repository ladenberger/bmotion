package de.bms.observer

import com.google.gson.Gson
import de.bms.BMotion

class TransformersObserver extends BMotionObserver {

    def Gson g = new Gson()

    def List<BMotionTransformer> transformers = []

    def static TransformersObserver make(Closure cls) {
        new TransformersObserver().with cls
    }

    def TransformersObserver add(BMotionTransformer transformer) {
        transformers.add(transformer)
        this
    }

    @Override
    def apply(BMotion bms) {
        String json = g.toJson(transformers.collectMany { it.update(bms) })
        bms.clients.each { it.sendEvent("applyTransformers", json) }
    }

}
