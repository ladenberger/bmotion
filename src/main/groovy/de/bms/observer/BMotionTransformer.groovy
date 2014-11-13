package de.bms.observer

import de.bms.BMotion

interface BMotionTransformer {

    def List<TransformerObject> update(BMotion bms)

}