package loggingsystem.service;

import loggingsystem.model.LogLevel;
import loggingsystem.model.LogMessage;

public interface LogHandler {
    void log(LogMessage logMessage);
}
