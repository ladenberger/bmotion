package de.bms

public class BMotionException extends Exception {

    public BMotionException(String msg) {
        super("BMotion Studio: " + msg);
    }

}
