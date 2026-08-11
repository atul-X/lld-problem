
Design a Logger System

Design a logging framework that supports multiple log levels (DEBUG, INFO, WARN, ERROR) 
and multiple output destinations (console, file). Include log formatting and filtering by severity.

Functional Requirements
    support multiple logging mode DEBUG, INFO, WARN, ERROR.
    support multiple logging output destinations console file .
    Log Filtering by Severity.

Non-Functional Requirements
    Thread safety regarding multiple log instances.



LogHandler-interface
    FileLogger.
    ConsoleLogger.
    S3Logger.
Logger
    String className.
    

LogManager
    Map<String,ClassInstance>


Filter -interface
    -
LogLevelFilter 
    
    
LogMessage
    -Loglevel
    -message

    

LOGLEVEL ENUM
    -DEBUG, INFO, WARN, ERROR`

