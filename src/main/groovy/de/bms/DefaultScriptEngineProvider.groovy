package de.bms

import de.bms.server.BMotionScriptEngineProvider

public class DefaultScriptEngineProvider implements BMotionScriptEngineProvider {

    @Override
    public GroovyShell get() {
        return new GroovyShell()
    }

    @Override
    public String[] getImports() {
        return []
    }
}
