package de.bms.observer

import groovy.transform.TupleConstructor;

@TupleConstructor
class ReactTransform extends TransformerObject {

    def String bmsid

    def ReactTransform(String bmsid) {
        this.bmsid = bmsid
    }

}