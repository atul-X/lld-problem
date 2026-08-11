package loggingsystem.model;

import loggingsystem.service.ConsoleLoggerHandler;
import loggingsystem.service.Filter;
import loggingsystem.service.LogHandler;
import loggingsystem.service.LogLevelPriorityFilter;

import java.util.List;

public class LoggerConfigurations {
    private List<LogHandler> logHandlerList;
    private List<Filter> filterList;
    private LogLevel defaultLoggingLevel;

    public LoggerConfigurations() {
        this.logHandlerList=List.of(new ConsoleLoggerHandler());
        this.filterList=List.of(new LogLevelPriorityFilter());
        defaultLoggingLevel=LogLevel.DEBUG;
    }

    public List<LogHandler> getLogHandlerList() {
        return logHandlerList;
    }

    public void setLogHandlerList(List<LogHandler> logHandlerList) {
        this.logHandlerList = logHandlerList;
    }

    public List<Filter> getFilterList() {
        return filterList;
    }

    public void setFilterList(List<Filter> filterList) {
        this.filterList = filterList;
    }

    public LogLevel getDefaultLoggingLevel() {
        return defaultLoggingLevel;
    }

    public void setDefaultLoggingLevel(LogLevel defaultLoggingLevel) {
        this.defaultLoggingLevel = defaultLoggingLevel;
    }
}
