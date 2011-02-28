
package ch.qos.logback.amqp.tools;


import ch.qos.logback.classic.Level;


public interface Callbacks
{
	public abstract void handleException (
			final Throwable exception, final String messageFormat, final Object ... messageArguments);
	
	public abstract void handleLogEvent (
			final Level level, final Throwable exception, final String messageFormat, final Object ... messageArguments);
}
