package de.bmotion.core;

import groovy.lang.GroovyShell;

public interface IBMotionScriptEngineProvider {

    public GroovyShell load(String groovyPath, BMotion session) throws BMotionException;

    public String[] getImports();

}
