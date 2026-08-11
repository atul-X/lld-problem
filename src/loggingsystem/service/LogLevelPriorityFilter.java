package loggingsystem.service;

import loggingsystem.model.FilterResult;
import loggingsystem.model.LogLevel;
import loggingsystem.model.LogMessage;

import java.util.HashMap;
import java.util.Map;

public class LogLevelPriorityFilter implements Filter{
    Map<LogLevel,Integer> priorityOrder=new  HashMap<>();

    public LogLevelPriorityFilter() {
        priorityOrder.put(LogLevel.DEBUG,0);
        priorityOrder.put(LogLevel.INFO,1);
        priorityOrder.put(LogLevel.ERROR,2);
    }

    @Override
    public FilterResult filter(LogMessage logMessage,LogLevel defaultPriority) {
        Integer messageLogLevelPriority=priorityOrder.get(logMessage.getLogLevel());
        Integer defaultLogLevel=priorityOrder.get(defaultPriority);
        if(messageLogLevelPriority<defaultLogLevel){
            return FilterResult.REJECTED;
        }else {
            return FilterResult.APPROVED;
        }
    }
}
