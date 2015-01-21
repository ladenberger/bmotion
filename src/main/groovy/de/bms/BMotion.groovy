package de.bms

import com.corundumstudio.socketio.SocketIOClient
import com.google.common.base.Charsets
import com.google.common.io.Resources
import de.bms.observer.BMotionObserver
import de.bms.observer.BMotionTransformer
import de.bms.observer.TransformersObserver
import de.bms.server.BMotionScriptEngineProvider
import de.bms.server.SessionConfiguration
import groovy.util.logging.Slf4j

@Slf4j
public abstract class BMotion {

    def final static String TRIGGER_ANIMATION_CHANGED = "AnimationChanged"

    def final Map<String, Trigger> observers = [:]

    def final Map<String, Closure> methods = [:]

    def TransformersObserver transformerObserver

    def boolean initialised = false

    def UUID sessionId

    def List<SocketIOClient> clients = new ArrayList<SocketIOClient>()

    def SessionConfiguration sessionConfiguration

    def templatePath

    def final BMotionScriptEngineProvider scriptEngineProvider

    public BMotion(final UUID sessionId, final String templatePath,
                   final BMotionScriptEngineProvider scriptEngineProvider) {
        this.sessionId = sessionId
        this.templatePath = templatePath
        this.scriptEngineProvider = scriptEngineProvider
    }

    public BMotion(final UUID sessionId, final String templatePath) {
        this(sessionId, templatePath, new DefaultScriptEngineProvider())
    }

    public void checkObserver(final String trigger) {
        log.info "Check observer, trigger: " + trigger
        observers.get(trigger)?.observers?.each { it.apply(this) }
        clients.each { it.sendEvent("checkObserver", trigger) }
    }

    public void checkObserver() {
        checkObserver(TRIGGER_ANIMATION_CHANGED)
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

    public void apply(final String name, data) {
        clients.each { it.sendEvent(name, data) }
    }

    public void apply(final BMotionObserver o) {
        o.apply(this)
    }

    public void apply(final List<BMotionObserver> o) {
        o.each { apply(it) }
    }

    public abstract Object executeEvent(final data) throws ImpossibleStepException

    public Object observe(final d) {
        def map = [:]
        d.data.each { k, v ->
            map.put(k, v.formulas.collect {
                String formula -> eval(formula)
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
        File modelFile = new File(getTemplateFolder() + File.separator + modelPath)
        if (modelFile.exists()) {
            log.info "Loading model " + modelPath
            loadModel(modelFile, force)
            log.info "Model loaded"
        }
    }

    public abstract void loadModel(File modelFile, boolean force)

    public abstract void refresh()

    private void initGroovyScript(String scriptPath) {
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

    public String[] getScriptPaths() {
        return sessionConfiguration?.scriptPath?.split(",")
    }

}