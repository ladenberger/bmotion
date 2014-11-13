package de.bms.server

public class SessionConfiguration {

    def String templateUrl
    def String scriptPath
    def String modelPath
    def String tool

    public SessionConfiguration() {}

    public SessionConfiguration(String templateUrl, String scriptPath, String modelPath, String tool) {
        super();
        this.templateUrl = templateUrl
        this.scriptPath = scriptPath
        this.modelPath = modelPath
        this.tool = tool
    }

}
