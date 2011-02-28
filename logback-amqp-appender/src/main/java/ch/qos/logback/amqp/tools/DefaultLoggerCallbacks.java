
package ch.qos.logback.amqp.tools;


import ch.qos.logback.classic.Level;
import org.slf4j.Logger;


public class DefaultLoggerCallbacks
		implements
			Callbacks
{
	public DefaultLoggerCallbacks (final Logger logger)
	{
		super ();
		this.logger = logger;
	}
	
	public void handleException (final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		this.handleLogEvent (Level.ERROR, exception, messageFormat, messageArguments);
	}
	
	public void handleLogEvent (
			final Level level, final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		final String message = String.format (messageFormat, messageArguments);
		switch (level.levelInt) {
			case Level.ERROR_INT :
				if (exception != null)
					this.logger.error (message, exception);
				else
					this.logger.error (message);
				break;
			case Level.WARN_INT :
				if (exception != null)
					this.logger.warn (message, exception);
				else
					this.logger.warn (message);
				break;
			case Level.INFO_INT :
				if (exception != null)
					this.logger.info (message, exception);
				else
					this.logger.info (message);
				break;
			case Level.DEBUG_INT :
				if (exception != null)
					this.logger.debug (message, exception);
				else
					this.logger.debug (message);
				break;
			case Level.TRACE_INT :
				if (exception != null)
					this.logger.trace (message, exception);
				else
					this.logger.trace (message);
				break;
			default:
				if (exception != null)
					this.logger.info (message, exception);
				else
					this.logger.info (message);
				break;
		}
	}
	
	protected Logger logger;
}
