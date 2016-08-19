package de.bmotion.core;

import java.util.Map;

import groovy.lang.Closure;

public class BMotionApiWrapper implements IBMotionApi {

	protected BMotion bmotion;

	public BMotionApiWrapper(BMotion bmotion) {
		this.bmotion = bmotion;
	}

	@Override
	public void log(Object message) {
		bmotion.log(message);
	}

	@Override
	public Object executeEvent(String name) throws BMotionException {
		return bmotion.executeEvent(name);
	}

	@Override
	public Object executeEvent(String name, Map<String, String> options) throws BMotionException {
		return bmotion.executeEvent(name, options);
	}

	@Override
	public Object eval(String formula) throws BMotionException {
		return bmotion.eval(formula);
	}

	@Override
	public Object eval(String formula, Map<String, Object> options) throws BMotionException {
		return bmotion.eval(formula, options);
	}

	@Override
	public void registerMethod(String name, Closure<?> func) {
		bmotion.registerMethod(name, func);
	}

	@Override
	public Object callMethod(String name, Object... args) throws BMotionException {
		return bmotion.callMethod(name, args);
	}

	@Override
	public Map<String, Closure<?>> getMethods() {
		return bmotion.getMethods();
	}

	@Override
	public Map<String, Object> getSessionData() {
		return bmotion.getSessionData();
	}

	@Override
	public Map<String, Object> getToolData() {
		return bmotion.getToolData();
	}

}
