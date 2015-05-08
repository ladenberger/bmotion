package de.bms

import com.corundumstudio.socketio.SocketIOClient
import groovy.util.logging.Slf4j

@Slf4j
public abstract class BMotion {

    def final static String TRIGGER_ANIMATION_CHANGED = "AnimationChanged"

    //def final Map<String, Closure> methods = [:]

    def String mode = BMotionServer.MODE_INTEGRATED

    def String modelPath

    def boolean initialised = false

    def UUID sessionId

    def SocketIOClient client

    def final BMotionScriptEngineProvider scriptEngineProvider

    public BMotion(final UUID sessionId, final BMotionScriptEngineProvider scriptEngineProvider) {
        this.sessionId = sessionId
        this.scriptEngineProvider = scriptEngineProvider
    }

    public BMotion(final UUID sessionId) {
        this(sessionId, new DefaultScriptEngineProvider())
    }

    public void setClient(SocketIOClient client) {
        this.client = client;
    }

    public void checkObserver(String trigger, Object data) {
        if (client != null) {
            client.sendEvent('checkObserver', trigger, data)
        }
    }

    public void checkObserver(String trigger) {
        checkObserver(trigger, [])
    }

    public void checkObserver() {
        checkObserver(TRIGGER_ANIMATION_CHANGED, [])
    }

    public abstract Object executeEvent(final data) throws ImpossibleStepException

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
     * @throws IllegalFormulaException
     */
    public abstract Object eval(final String formula) throws IllegalFormulaException

    /*public void registerMethod(String name, Closure cls) {
        methods.put(name, cls)
    }

    public Object callGroovyMethod(name, data) {
        Closure cls = methods.get(name)
        if (cls != null)
            return cls(data)
        return null
    }*/

    public void initSession(String modelPath) throws BMotionException {
        this.modelPath = modelPath;
        this.initialised = true;
        initModel(modelPath)
    }

    // ------------------
    private void initModel(String modelPath, boolean force = false) throws BMotionException {
        File modelFile = new File(modelPath)
        if (modelFile.exists()) {
            log.info "BMotion Studio: Loading model " + modelPath
            loadModel(modelFile, force)
        } else {
            throw new BMotionException("Model " + modelPath + " does not exist")
        }
    }

    public abstract void loadModel(File modelFile, boolean force)

    public abstract void disconnect();

    /*private void initGroovyScript(String scriptPath) {
        if (scriptPath) {
            String[] scriptPaths = scriptPath.split(",")
            String templateFolder = getTemplateFolder()
            try {
                log.info "Initialising Groovy Scripting Engine"
                def GroovyShell shell = scriptEngineProvider.get()
                shell.setVariable("bms", this);
                shell.setVariable("templateFolder", templateFolder);
                URL url = Resources.getResource("mainscript");
                String bmsscript = Resources.toString(url, Charsets.UTF_8);
                shell.evaluate(BMotionGroovy.imports + "\n" + bmsscript)
                def aimports = BMotionGroovy.IMPORTS
                aimports += scriptEngineProvider.getImports()
                if (scriptPaths != null) {
                    for (String path : scriptPaths) {
                        String filePath = templateFolder + File.separator + path
                        shell.evaluate(aimports.join("\n") + "\n" + new File(filePath).getText(), path)
                    }
                }
                log.info "Groovy Scripting Engine Initialised"
            } catch (GroovyRuntimeException e) {
                e.printStackTrace()
            } catch (Exception e) {
                BMotionScriptException.checkForScriptErrors(e, scriptPaths)
            }
        }
    }*/

    /*public String[] getScriptPaths() {
        return sessionConfiguration?.scriptPath?.split(",")
    }*/

}