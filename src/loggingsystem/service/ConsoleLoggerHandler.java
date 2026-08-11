package loggingsystem.service;

import loggingsystem.model.LogMessage;

public class ConsoleLoggerHandler implements LogHandler{

    @Override
    public void log(LogMessage logMessage) {
        System.out.println("class "+logMessage.getClassName()+" time "+ logMessage.getTimestamp()+" log level "+logMessage.getLogLevel()+ " message "+logMessage.getMessage());
    }
}
