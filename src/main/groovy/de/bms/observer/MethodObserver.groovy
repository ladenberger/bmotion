package de.bms.observer

import de.bms.BMotion
import groovy.transform.TupleConstructor

@TupleConstructor
class MethodObserver extends BMotionObserver {

    def _name
    def _data

    def MethodObserver(name) {
        this._name = name
    }

    def static MethodObserver make(Closure cls) {
        new MethodObserver().with cls
    }

    def MethodObserver name(name) {
        this._name = name
        this
    }

    def MethodObserver data(data) {
        this._data = data
        this
    }

    @Override
    def apply(BMotion bms) {
        bms.clients.each { it.sendEvent(_name, _data) }
    }

}
