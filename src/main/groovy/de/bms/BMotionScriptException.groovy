package de.bms

import groovy.util.logging.Slf4j

@Slf4j
public class BMotionScriptException {

    public static checkForScriptErrors(Throwable e, String[] scriptPaths) {
        e.getStackTrace().each { StackTraceElement t ->
            if (scriptPaths.contains(t.fileName)) {
                if(t.lineNumber != -1) {
                    log.error e.getMessage() + " (" + e.getClass().
                            getName() + ") (Line: " + (t.lineNumber - 9) + ")" + " (File: " + t.fileName + ")"
                }
            }
        }
    }

}
