
package eu.arkitech.logback.amqp.accessors;


import ch.qos.logback.classic.spi.ILoggingEvent;


public interface AmqpLoggingEventRouter
{
	public abstract String generateExchange (final ILoggingEvent event)
			throws Throwable;
	
	public abstract String generateRoutingKey (final ILoggingEvent event)
			throws Throwable;
}
