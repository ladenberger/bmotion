package de.bmotion.core;

import groovy.lang.GroovyShell;

public class DefaultScriptEngineProvider implements IBMotionScriptEngineProvider {

	@Override
	public GroovyShell load(String groovyPath, BMotion session) {
		return new GroovyShell();
	}

	@Override
	public String[] getImports() {
		return new String[] {};
	}

}
