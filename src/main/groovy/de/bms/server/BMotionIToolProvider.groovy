package de.bms.server

import de.bms.itool.ITool
import de.bms.itool.ToolRegistry

interface BMotionIToolProvider {

    public ITool get(String tool, ToolRegistry toolRegistry)

}
