package de.bms.server

interface BMotionScriptEngineProvider {

    public GroovyShell get()

    public String[] getImports()

}
