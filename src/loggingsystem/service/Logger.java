package loggingsystem.service;

import loggingsystem.model.FilterResult;
import loggingsystem.model.LogLevel;
import loggingsystem.model.LogMessage;
import loggingsystem.model.LoggerConfigurations;

import java.time.LocalDateTime;
import java.util.List;

public class Logger {
    private String classInstance;
    private LoggerConfigurations  configurations;




    public Logger(String classInstance,LoggerConfigurations configurations) {
        this.classInstance = classInstance;
        this.configurations= configurations;
    }
    public void info(String message){
        log(message,LogLevel.INFO);

    }
    public void debug(String message){
        log(message,LogLevel.DEBUG);

    }
    public void error(String message){
        log(message,LogLevel.ERROR);
    }
    private void log(String message,LogLevel logLevel){
        if (configurations.getLogHandlerList().isEmpty()){
            throw new IllegalStateException("No log handlers configured for logger '"+classInstance+"'");
        }
        LogMessage logMessage=new LogMessage(message,logLevel, LocalDateTime.now(),classInstance);
        for (Filter filter:configurations.getFilterList()){
            if (filter.filter(logMessage,configurations.getDefaultLoggingLevel())==FilterResult.REJECTED){
                return;
            }
        }
        for (LogHandler handler:configurations.getLogHandlerList()){
            handler.log(logMessage);
        }
    }
}