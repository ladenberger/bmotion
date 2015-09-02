package de.bms

import com.corundumstudio.socketio.SocketIOClient
import groovy.util.logging.Slf4j

@Slf4j
public abstract class BMotion {

    def final static String TRIGGER_ANIMATION_CHANGED = "AnimationChanged"

    def String mode = BMotionServer.MODE_INTEGRATED

    def final UUID id

    def List<SocketIOClient> clients = []

    def clientData = [:]

    def final BMotionScriptEngineProvider scriptEngineProvider

    public BMotion(final UUID id, final BMotionScriptEngineProvider scriptEngineProvider) {
        this.id = id
        this.scriptEngineProvider = scriptEngineProvider
    }

    public BMotion() {
        this(UUID.randomUUID(), new DefaultScriptEngineProvider())
    }

    public void checkObserver(String trigger, Object data) {
        this.clients.each {
            it.sendEvent('checkObserver', trigger, data)
        }
    }

    public abstract Object executeEvent(final data)

    public Object evaluateFormulas(final d) throws BMotionException {
        def map = [:]
        d.data.each { k, v ->
            map.put(k, v.formulas.collect { String formula -> eval(formula)
            })
        }
        return map
    }

    /**
     *
     * This method evaluates a given formula and returns the result.
     *
     * @param formula
     *            The formula to evaluate
     * @return the result of the formula or null if no result was found or no
     *         an exception was thrown.
     * @throws BMotionException
     */
    public abstract Object eval(final String formula) throws BMotionException

    public void initSession(String modelPath) {
        initModel(modelPath)
    }

    public abstract void initModel(String modelPath)

    public abstract void disconnect();

}