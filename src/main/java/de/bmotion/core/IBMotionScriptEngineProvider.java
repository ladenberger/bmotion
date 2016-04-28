package de.bmotion.core;

import groovy.lang.GroovyShell;

public interface IBMotionScriptEngineProvider {

    public GroovyShell get();

    public String[] getImports();

}
