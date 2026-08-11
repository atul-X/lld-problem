package loggingsystem.model;

import java.time.LocalDateTime;

public class LogMessage {
    private String message;
    private LogLevel logLevel;
    private LocalDateTime timestamp;
    private String className;

    public LogMessage(String message, LogLevel logLevel, LocalDateTime timestamp,String  className) {
        this.message = message;
        this.logLevel = logLevel;
        this.timestamp = timestamp;
        this.className = className;
    }

    public String getMessage() {
        return message;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getClassName() {
        return className;
    }
}
