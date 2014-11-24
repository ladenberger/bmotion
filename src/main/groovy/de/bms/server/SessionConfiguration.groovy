package de.bms.server

public class SessionConfiguration {

    def templateUrl
    def scriptPath
    def modelPath
    def tool
    def bmsSvg

    public SessionConfiguration() {}

    public SessionConfiguration(templateUrl, scriptPath, modelPath, tool, bmsSvg) {
        super();
        this.templateUrl = templateUrl
        this.scriptPath = scriptPath
        this.modelPath = modelPath
        this.tool = tool
        this.bmsSvg = bmsSvg
    }

}
