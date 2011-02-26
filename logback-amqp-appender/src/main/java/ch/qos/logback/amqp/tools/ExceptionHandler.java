
package ch.qos.logback.amqp.tools;


public interface ExceptionHandler
{
	public abstract void handleException (final String message, final Throwable exception);
}
