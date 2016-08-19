package de.bmotion.core;

import java.util.Map;

import groovy.lang.Closure;

public interface IBMotionApi {

	/**
	 * 
	 * Logs the given message on the client side. An arbitrary object can be
	 * passed as a message with the premise that the object is serializable.
	 * 
	 * @param message
	 *            An arbitrary serializable message object
	 */
	public void log(Object message);

	/**
	 * 
	 * Executes an event for the given name.
	 * 
	 * @param name
	 *            The name of the event that should be executed
	 * @return The return value of the event (e.g. classical-B operations may
	 *         have return values)
	 * @throws BMotionException
	 */
	public Object executeEvent(String name) throws BMotionException;

	/**
	 * 
	 * Executes an event for the given name and options.
	 * 
	 * @param name
	 *            The name of the event that should be executed
	 * @param options
	 *            The options for the event (e.g. an additional predicate)
	 * @return The return value of the event (e.g. classical-B operations may
	 *         have return values)
	 * @throws BMotionException
	 */
	public Object executeEvent(String name, Map<String, String> options) throws BMotionException;

	/**
	 * 
	 * Evaluates the given formula in the current state and returns the value.
	 * 
	 * @param formula
	 *            The formula that should be evaluated in the current state
	 * @return The result of the formula
	 * @throws BMotionException
	 */
	public Object eval(String formula) throws BMotionException;

	/**
	 * 
	 * Evaluates the given formula with options in the current state and returns
	 * the value.
	 * 
	 * @param formula
	 *            The formula that should be evaluated in the current state
	 * @param options
	 *            The options for the evaluation (e.g. translate flag)
	 * @return The result of the formula
	 * @throws BMotionException
	 */
	public Object eval(String formula, Map<String, Object> options) throws BMotionException;

	/**
	 * 
	 * Registers a method on the server side.
	 * 
	 * @param name
	 *            The name of the method.
	 * @param func
	 *            The functional body of the method as a {@link Closure}
	 */
	public void registerMethod(String name, Closure<?> func);

	/**
	 * 
	 * Calls a registered method on the server side.
	 * 
	 * @param name
	 *            The name of the method.
	 * @param args
	 *            The arguments for the method
	 * @return The return value of the method
	 * @throws BMotionException
	 */
	public Object callMethod(String name, Object... args) throws BMotionException;

	/**
	 * 
	 * Returns a list of registered server side methods.
	 * 
	 * @return A list of registered server side methods
	 */
	public Map<String, Closure<?>> getMethods();

	/**
	 * 
	 * Returns session related data.
	 * 
	 * @return Session related data
	 */
	public Map<String, Object> getSessionData();

	/**
	 * 
	 * Returns tool related data.
	 * 
	 * @return Tool related data
	 */
	public Map<String, Object> getToolData();

}
