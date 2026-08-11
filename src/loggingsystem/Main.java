package loggingsystem;

import loggingsystem.model.LogLevel;
import loggingsystem.model.LoggerConfigurations;
import loggingsystem.service.ConsoleLoggerHandler;
import loggingsystem.service.LogLevelPriorityFilter;
import loggingsystem.service.LogManager;
import loggingsystem.service.Logger;

import java.util.List;

public class Main {
    static void main() {
        LoggerConfigurations configurations=new LoggerConfigurations();
        configurations.setFilterList(List.of(new LogLevelPriorityFilter()));
        configurations.setLogHandlerList(List.of(new ConsoleLoggerHandler()));
        configurations.setDefaultLoggingLevel(LogLevel.DEBUG);
        LogManager logManager=LogManager.getLogManager();
        logManager.setConfigurations(configurations);
        Logger logger=logManager.getLogger(Main.class.getName());
        logger.debug("this debug message should be filtered out");
        logger.info("this info message should print");
        logger.error("this error message should print");
    }
}
