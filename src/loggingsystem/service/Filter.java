package loggingsystem.service;

import loggingsystem.model.FilterResult;
import loggingsystem.model.LogLevel;
import loggingsystem.model.LogMessage;

public interface Filter {
    FilterResult filter(LogMessage logMessage, LogLevel defaultPriority);
}
