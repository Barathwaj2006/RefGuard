/**
 * Structured Logger for RefGuard Backend
 * 
 * Provides consistent logging format across the application with support for:
 * - Multiple log levels (DEBUG, INFO, WARN, ERROR)
 * - Structured JSON output for production monitoring
 * - Human-readable output for development
 * - Contextual metadata attachment
 */

export type LogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

interface LogEntry {
  timestamp: string;
  level: LogLevel;
  service: string;
  message: string;
  context?: Record<string, unknown>;
  error?: {
    name: string;
    message: string;
    stack?: string;
  };
}

class Logger {
  private readonly service: string;
  private readonly isProduction: boolean;
  private readonly minLevel: LogLevel;

  private readonly levelPriority: Record<LogLevel, number> = {
    DEBUG: 0,
    INFO: 1,
    WARN: 2,
    ERROR: 3,
  };

  constructor(service: string = 'RefGuard') {
    this.service = service;
    this.isProduction = process.env.NODE_ENV === 'production';
    this.minLevel = (process.env.LOG_LEVEL as LogLevel) || (this.isProduction ? 'INFO' : 'DEBUG');
  }

  private shouldLog(level: LogLevel): boolean {
    return this.levelPriority[level] >= this.levelPriority[this.minLevel];
  }

  private formatEntry(entry: LogEntry): string {
    if (this.isProduction) {
      return JSON.stringify(entry);
    }
    
    const timestamp = entry.timestamp;
    const level = entry.level.padEnd(5);
    const contextStr = entry.context ? ` ${JSON.stringify(entry.context)}` : '';
    const errorStr = entry.error ? ` [${entry.error.name}: ${entry.error.message}]` : '';
    
    return `[${timestamp}] ${level} [${entry.service}] ${entry.message}${contextStr}${errorStr}`;
  }

  private log(level: LogLevel, message: string, context?: Record<string, unknown>, error?: Error): void {
    if (!this.shouldLog(level)) {
      return;
    }

    const entry: LogEntry = {
      timestamp: new Date().toISOString(),
      level,
      service: this.service,
      message,
      context,
      ...(error && {
        error: {
          name: error.name,
          message: error.message,
          stack: error.stack,
        },
      }),
    };

    const output = this.formatEntry(entry);

    switch (level) {
      case 'ERROR':
        console.error(output);
        break;
      case 'WARN':
        console.warn(output);
        break;
      default:
        console.log(output);
    }
  }

  debug(message: string, context?: Record<string, unknown>): void {
    this.log('DEBUG', message, context);
  }

  info(message: string, context?: Record<string, unknown>): void {
    this.log('INFO', message, context);
  }

  warn(message: string, context?: Record<string, unknown>): void {
    this.log('WARN', message, context);
  }

  error(message: string, error?: Error, context?: Record<string, unknown>): void {
    this.log('ERROR', message, context, error);
  }

  /**
   * Create a child logger with additional context
   */
  child(context: Record<string, unknown>): Logger {
    const childLogger = new Logger(this.service);
    // Store context for future use (could be extended)
    childLogger.info('Logger context initialized', context);
    return childLogger;
  }
}

// Export singleton instance for general use
export const logger = new Logger();

// Export factory for creating specialized loggers
export const createLogger = (service: string): Logger => new Logger(service);
