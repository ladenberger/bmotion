package de.bmotion.core;

import java.util.Map;

import groovy.lang.Closure;

public interface IBMotionApi {

	public void log(Object message);

	public Object executeEvent(String name) throws BMotionException;

	public Object executeEvent(String name, Map<String, String> options) throws BMotionException;

	public Object eval(String formula) throws BMotionException;

	public abstract Object eval(String formula, Map<String, Object> options) throws BMotionException;

	public void registerMethod(String name, Closure<?> func);

	public Object callMethod(String name, Object... args) throws BMotionException;

	public Map<String, Closure<?>> getMethods();

	public Map<String, Object> getSessionData();

	public Map<String, Object> getToolData();

}
