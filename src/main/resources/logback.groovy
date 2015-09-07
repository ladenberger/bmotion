import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.*

// define the USER_HOME variable setting its value
// to that of the "user.home" system property
def USER_HOME = System.getProperty("user.home")

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %.-5level %logger{36} - %msg%n"
    }
}

appender("FILE", FileAppender) {
    file = "${USER_HOME}/.prob/bmotion.log"
    append = true
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %.-5level %logger{36} - %msg%n"
    }
}

logger("de.prob.cli", OFF)
logger("de.prob.animator", OFF)
logger("org.eclipse.jetty", ERROR)
logger("com.corundumstudio.socketio", WARN)
//root(INFO, ["STDOUT", "FILE"])
root(INFO, ["STDOUT", "FILE"])