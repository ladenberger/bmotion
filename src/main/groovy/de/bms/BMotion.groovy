package de.bms

import com.corundumstudio.socketio.SocketIOClient
import com.google.common.base.Charsets
import com.google.common.io.Resources
import de.bms.itool.ITool
import de.bms.itool.IToolListener
import de.bms.itool.ToolRegistry
import de.bms.observer.BMotionObserver
import de.bms.observer.BMotionTransformer
import de.bms.observer.TransformersObserver
import de.bms.server.BMotionScriptEngineProvider
import de.bms.server.DefaultScriptEngineProvider
import de.bms.server.SessionConfiguration
import groovy.util.logging.Slf4j

@Slf4j
public class BMotion implements IToolListener {

    private final Map<String, Trigger> observers = [:]

    private final Map<String, Closure> methods = [:]

    private TransformersObserver transformerObserver

    public final static String TRIGGER_ANIMATION_CHANGED = "AnimationChanged"

    def boolean initialised = false

    def UUID sessionId

    def List<SocketIOClient> clients = new ArrayList<SocketIOClient>()

    def ToolRegistry toolRegistry

    def SessionConfiguration sessionConfiguration

    def templatePath

    def ITool tool

    def BMotionScriptEngineProvider scriptEngineProvider

    public BMotion(final UUID sessionId, final ITool tool, final ToolRegistry toolRegistry, final String templatePath,
                   final BMotionScriptEngineProvider scriptEngineProvider) {
        this.sessionId = sessionId
        this.tool = tool
        this.templatePath = templatePath
        this.toolRegistry = toolRegistry
        this.scriptEngineProvider = scriptEngineProvider
        this.toolRegistry.registerListener(this)
    }

    @Override
    public void animationChange(final String trigger, final ITool tool) {
        observers.get(trigger)?.observers?.each { it.apply(this) }
    }

    // ---------- BMS API
    public void registerObserver(final BMotionObserver o, String trigger = TRIGGER_ANIMATION_CHANGED) {
        registerObserver([o], trigger)
    }

    public void registerObserver(final List<BMotionObserver> o, String trigger = TRIGGER_ANIMATION_CHANGED) {
        o.each {
            (it instanceof BMotionTransformer) ? transformerObserver.add(it) :
                    observers.get(trigger)?.observers?.add(it)
        }
    }

    /**
     *
     * This method calls a JavaScript method with the given json data.
     *
     * @param cmd The JavaScript method name to be called.
     * @param json The json data.
     */
    public void apply(final String cmd, json) {
        // client.sendEvent(cmd, json)
    }

    public void apply(final BMotionObserver o) {
        o.apply(this)
    }

    public void apply(final List<BMotionObserver> o) {
        o.each { apply(it) }
    }

    /**
     *
     * This method evaluates a given formula and returns the result.
     *
     * @param formula
     *            The formula to evaluate
     * @return the result of the formula or null if no result was found or no
     *         an exception was thrown
     * @throws Exception
     */
    public Object eval(final String formula) throws Exception {
        try {
            Object evaluate = getTool().evaluate(getTool().getCurrentState(), formula);
            return evaluate;
        } catch (Exception e) {
            log.error e.getMessage()
        }
    }

    /*public Object executeOperation(final Map<String, String[]> params) {
        String id = (params.get("id") != null && params.get("id").length > 0) ? params.get("id")[0] : "";
        String op = params.get("op")[0];
        String[] parameters = params.get("predicate");
        try {
            getTool().doStep(id, op, parameters);
        } catch (ImpossibleStepException e) {
            e.printStackTrace();
        }
        return null;
    }*/

    public void registerMethod(String name, Closure cls) {
        methods.put(name, cls)
    }

    public Object callGroovyMethod(name, data) {
        Closure cls = methods.get(name)
        if (cls != null)
            return cls(data)
        return null
    }

    // ------------------

    public void initSession(SessionConfiguration sessionConfiguration) {
        log.debug "Initialising BMotion Session"
        initModel(sessionConfiguration?.modelPath)
        initObservers()
        initGroovyScript(sessionConfiguration?.scriptPath)
        this.sessionConfiguration = sessionConfiguration
        tool.refresh()
        initialised = true;
        log.debug "BMotion Session initialised"
    }

    private void initObservers() {
        this.observers.clear()
        this.methods.clear()
        this.transformerObserver = new TransformersObserver()
        def Trigger trigger = new Trigger()
        trigger.observers.add(this.transformerObserver)
        this.observers.put(TRIGGER_ANIMATION_CHANGED, trigger)
    }

    private void initModel(String modelPath, boolean force = false) {
        def String oldModelPath = this.sessionConfiguration?.modelPath
        if (modelPath != null && (!modelPath.equals(oldModelPath) || force)) {
            log.info "Loading model " + modelPath
            tool.loadModel(getTemplateFolder() + File.separator + modelPath)
            log.info "Model loaded"
        }
    }

    private void initGroovyScript(String scriptPath) {
        if (scriptPath) {
            String[] scriptPaths = scriptPath.split(",")
            String templateFolder = getTemplateFolder()
            try {
                log.info "Initialising Groovy Scripting Engine"
                scriptEngineProvider = scriptEngineProvider ?: new DefaultScriptEngineProvider()
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
    }

    public void reloadModel() {
        initModel(sessionConfiguration?.modelPath, true)
    }

    public void reloadGrooyScript() {
        initGroovyScript(sessionConfiguration?.scriptPath)
    }

    public String getTemplateFolder() {
        return new File(templatePath).getParent()
    }

    public boolean isInitialised() {
        return initialised;
    }

    public SessionConfiguration getSessionConfiguration() {
        return sessionConfiguration
    }

    public ITool getTool() {
        return tool
    }

    public String[] getScriptPaths() {
        return sessionConfiguration?.scriptPath?.split(",")
    }

}