package loggingsystem.service;

import loggingsystem.model.LoggerConfigurations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class LogManager {
    private final Map<String,Logger> loggersMap = new ConcurrentHashMap<>();
    private final AtomicReference<LoggerConfigurations> configurations = new AtomicReference<>();

    private LogManager(){

    }

    private static class Holder {
        private static final LogManager INSTANCE = new LogManager();
    }

    public static LogManager getLogManager(){
        return Holder.INSTANCE;
    }

    public Logger getLogger(String className){
        LoggerConfigurations current = configurations.get();
        if (current == null){
            throw new IllegalStateException("LoggerConfigurations must be set via setConfigurations() before requesting a logger");
        }
        return loggersMap.computeIfAbsent(className, name -> new Logger(name, current));
    }

    public void setConfigurations(LoggerConfigurations configurations){
        this.configurations.set(configurations);
        loggersMap.clear();
    }

}
