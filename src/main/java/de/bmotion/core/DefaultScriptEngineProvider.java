package de.bmotion.core;

import groovy.lang.GroovyShell;

public class DefaultScriptEngineProvider implements IBMotionScriptEngineProvider {

	@Override
	public GroovyShell get() {
		return new GroovyShell();
	}

	@Override
	public String[] getImports() {
		return new String[] {};
	}

}
