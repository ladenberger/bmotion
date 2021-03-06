package de.bmotion.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.corundumstudio.socketio.SocketIOClient;

import de.bmotion.core.objects.FormulaListObject;
import de.bmotion.core.objects.FormulaReturnObject;
import groovy.lang.Closure;

public abstract class BMotion implements IBMotionApi {

	public final static String TRIGGER_ANIMATION_CHANGED = "AnimationChanged";

	public final String id;

	public final List<SocketIOClient> clients = new ArrayList<SocketIOClient>();

	public final IBMotionScriptEngineProvider scriptEngineProvider;

	public String mode = BMotionServer.MODE_STANDALONE;

	public final Map<String, Object> sessionData = new HashMap<String, Object>();
	public final Map<String, Object> toolData = new HashMap<String, Object>();

	public final Map<String, Closure<?>> methods = new HashMap<String, Closure<?>>();

	public BMotion(String sessionId, final IBMotionScriptEngineProvider scriptEngineProvider) {
		this.id = sessionId;
		this.scriptEngineProvider = scriptEngineProvider;
		this.sessionData.put("id", id.toString());
	}

	public BMotion() {
		this(UUID.randomUUID().toString());
	}

	public BMotion(String sessionId) {
		this(sessionId, new DefaultScriptEngineProvider());
	}

	public void checkObserver(String trigger) {
		clients.forEach(client -> client.sendEvent("checkObserver", trigger, toolData));
	}

	public void log(Object message) {
		clients.forEach(client -> client.sendEvent("log", message));
	}

	public Object executeEvent(String name) throws BMotionException {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("name", name);
		return executeEvent(options);
	}

	public Object executeEvent(String name, Map<String, String> options) throws BMotionException {
		options.put("name", name);
		return executeEvent(options);
	}

	public abstract Object executeEvent(Map<String, String> options) throws BMotionException;

	public Map<String, Map<String, Object>> evaluateFormulas(Map<String, FormulaListObject> oList)
			throws BMotionException {

		return oList.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {

			FormulaListObject list = (FormulaListObject) entry.getValue();
			Map<String, Object> collect = list.getFormulas().stream()
					.collect(Collectors.toMap(obj -> obj.getFormula(), obj -> {
						FormulaReturnObject returnObj = new FormulaReturnObject();
						try {
							returnObj.setResult(eval(obj.getFormula(), obj.getOptions()));
						} catch (BMotionException e) {
							returnObj.setError(e.getMessage());
						}
						return returnObj;
					}));
			return collect;

		}));

	}

	/**
	 *
	 * This method evaluates a given formula and returns the result.
	 *
	 * @param formula
	 *            The formula to evaluate
	 * @return the result of the formula or null if no result was found or no an
	 *         exception was thrown.
	 * @throws BMotionException
	 */
	public abstract Object eval(String formula, Map<String, Object> options) throws BMotionException;

	public Object eval(String formula) throws BMotionException {
		return eval(formula, Collections.emptyMap());
	}

	public void initGroovyScript(String groovyPath) throws BMotionException {
		scriptEngineProvider.load(groovyPath, this);
	}

	public abstract void initModel(String model, Map<String, String> modelOptions, String mode) throws BMotionException;

	public void initModel(String model, Map<String, String> modelOptions) throws BMotionException {
		initModel(model, modelOptions, BMotionServer.MODE_STANDALONE);
	}

	public abstract void disconnect();

	public void registerMethod(String name, Closure<?> func) {
		methods.put(name, func);
	}

	public Object callMethod(String name, Object... args) throws BMotionException {
		Closure<?> func = methods.get(name);
		if (func == null) {
			throw new BMotionException("Cannot call method " + name + ": No such method.");
		}
		try {
			return func.call(args);
		} catch (Exception e) {
			throw new BMotionException(
					"Some error occurred while executing method " + name + ": " + e.getMessage() + ".");
		}
	}

	public Map<String, Object> getSessionData() {
		return sessionData;
	}

	public String getId() {
		return id;
	}

	public List<SocketIOClient> getClients() {
		return clients;
	}

	public Map<String, Object> getToolData() {
		return toolData;
	}

	public void sessionLoaded() {
	}

	public Map<String, Closure<?>> getMethods() {
		return methods;
	}

}