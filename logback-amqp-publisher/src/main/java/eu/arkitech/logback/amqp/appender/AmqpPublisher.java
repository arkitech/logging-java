
package eu.arkitech.logback.amqp.appender;


import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.amqp.accessors.AmqpAccessor;
import eu.arkitech.logback.amqp.accessors.AmqpRawMessage;
import eu.arkitech.logback.amqp.accessors.AmqpRawPublisher;
import eu.arkitech.logback.amqp.accessors.AmqpRouter;
import eu.arkitech.logback.common.LoggingEventSink;


public final class AmqpPublisher
		extends AmqpAccessor<AmqpRawPublisher>
		implements
			LoggingEventSink
{
	public AmqpPublisher (final AmqpPublisherConfiguration configuration)
	{
		this (null, configuration);
	}
	
	public AmqpPublisher (final AmqpRawPublisher accessor, final AmqpPublisherConfiguration configuration)
	{
		super ((accessor != null) ? accessor : new AmqpRawPublisher (configuration, null), configuration);
		this.router = configuration.router;
		this.buffer = this.accessor.getBuffer ();
	}
	
	@Override
	public final boolean isDrained ()
	{
		return (this.buffer.isEmpty ());
	}
	
	@Override
	public final boolean push (final ILoggingEvent event)
			throws InterruptedException
	{
		return (this.push (event, Long.MAX_VALUE, TimeUnit.DAYS));
	}
	
	@Override
	public final boolean push (final ILoggingEvent originalEvent, final long timeout, final TimeUnit timeoutUnit)
			throws InterruptedException
	{
		if (originalEvent == null)
			throw (new IllegalArgumentException ());
		final ILoggingEvent clonedEvent = this.prepareEvent (originalEvent);
		if (clonedEvent == null)
			return (false);
		final AmqpRawMessage message = this.encodeMessage (clonedEvent);
		if (message == null)
			return (false);
		if (!this.buffer.offer (message, timeout, timeoutUnit))
			return (false);
		return (true);
	}
	
	@Override
	protected void executeLoop ()
	{
		while (true) {
			if (this.shouldStopSoft ())
				this.accessor.requestStop ();
			if (this.accessor.awaitStop (this.waitTimeout))
				break;
		}
	}
	
	@Override
	protected void finalizeLoop ()
	{
		this.accessor.awaitStop ();
	}
	
	@Override
	protected void initializeLoop ()
	{
		this.accessor.start ();
	}
	
	private final AmqpRawMessage encodeMessage (final ILoggingEvent event)
	{
		final String exchange;
		try {
			if (this.router != null)
				exchange = this.router.generateExchange (event);
			else
				exchange = AmqpPublisherConfiguration.defaultExchange;
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "amqp publisher sink encountered an error while generating the exchange for the event; aborting!");
			return (null);
		}
		final String routingKey;
		try {
			if (this.router != null)
				routingKey = this.router.generateRoutingKey (event);
			else
				routingKey = String.format (AmqpPublisherConfiguration.defaultRoutingKeyFormat, event.getLevel ().levelStr.toLowerCase ());
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "amqp publisher sink encountered an error while generating the routing key for the event; aborting!");
			return (null);
		}
		return (this.encodeMessage (event, exchange, routingKey));
	}
	
	private final BlockingDeque<AmqpRawMessage> buffer;
	private final AmqpRouter router;
}
