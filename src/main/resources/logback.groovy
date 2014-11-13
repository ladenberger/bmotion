import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.WARN

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %.-5level %logger{36} - %msg%n"
    }
}

logger("de.prob.cli", ERROR)
logger("org.eclipse.jetty", ERROR)
logger("com.corundumstudio.socketio", WARN)
root(INFO, ["STDOUT"])