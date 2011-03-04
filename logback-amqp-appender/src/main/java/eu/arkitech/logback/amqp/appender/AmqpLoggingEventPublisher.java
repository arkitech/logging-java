
package eu.arkitech.logback.amqp.appender;


import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.amqp.accessors.AmqpLoggingEventAccessor;
import eu.arkitech.logback.amqp.accessors.AmqpLoggingEventRouter;
import eu.arkitech.logback.amqp.accessors.AmqpMessage;
import eu.arkitech.logback.amqp.accessors.AmqpRawPublisher;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.LoggingEventSink;
import eu.arkitech.logback.common.Serializer;


public final class AmqpLoggingEventPublisher
		extends AmqpLoggingEventAccessor<AmqpRawPublisher>
		implements
			LoggingEventSink
{
	public AmqpLoggingEventPublisher (
			final AmqpRawPublisher accessor, final AmqpLoggingEventRouter router, final LoggingEventMutator mutator,
			final Serializer serializer, final Callbacks callbacks)
	{
		super (accessor, mutator, serializer, callbacks, accessor.monitor);
		this.router = router;
		this.buffer = this.accessor.getBuffer ();
	}
	
	public AmqpLoggingEventPublisher (
			final String host, final Integer port, final String virtualHost, final String username, final String password)
	{
		this (host, port, virtualHost, username, password, null, null, null, null);
	}
	
	public AmqpLoggingEventPublisher (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final AmqpLoggingEventRouter router, final LoggingEventMutator mutator, final Serializer serializer,
			final Callbacks callbacks)
	{
		this (
				new AmqpRawPublisher (host, port, virtualHost, username, password, null, callbacks, null), router, mutator,
				serializer, callbacks);
	}
	
	public final boolean isDrained ()
	{
		return (this.buffer.isEmpty ());
	}
	
	public final boolean push (final ILoggingEvent event)
			throws InterruptedException
	{
		return (this.push (event, Long.MAX_VALUE, TimeUnit.DAYS));
	}
	
	public final boolean push (final ILoggingEvent originalEvent, final long timeout, final TimeUnit timeoutUnit)
			throws InterruptedException
	{
		if (originalEvent == null)
			throw (new IllegalArgumentException ());
		final ILoggingEvent clonedEvent = this.prepareEvent (originalEvent);
		if (clonedEvent == null)
			return (false);
		final AmqpMessage message = this.encodeMessage (clonedEvent);
		if (message == null)
			return (false);
		if (!this.buffer.offer (message, timeout, timeoutUnit))
			return (false);
		return (true);
	}
	
	protected void executeLoop ()
	{
		while (true) {
			if (this.shouldStopSoft ())
				this.accessor.requestStop ();
			if (this.accessor.awaitStop (this.waitTimeout))
				break;
		}
	}
	
	protected void finalizeLoop ()
	{
		this.accessor.awaitStop ();
	}
	
	protected void initializeLoop ()
	{
		this.accessor.start ();
	}
	
	private final AmqpMessage encodeMessage (final ILoggingEvent event)
	{
		final String exchange;
		try {
			if (this.router != null)
				exchange = this.router.generateExchange (event);
			else
				exchange = AmqpLoggingEventPublisher.defaultExchange;
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception,
					"amqp publisher sink encountered an error while generating the exchange for the event; aborting!");
			return (null);
		}
		final String routingKey;
		try {
			if (this.router != null)
				routingKey = this.router.generateRoutingKey (event);
			else
				routingKey =
						String.format (AmqpLoggingEventPublisher.defaultRoutingKeyFormat, event.getLevel ().levelStr.toLowerCase ());
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception,
					"amqp publisher sink encountered an error while generating the routing key for the event; aborting!");
			return (null);
		}
		return (this.encodeMessage (event, exchange, routingKey));
	}
	
	private final BlockingDeque<AmqpMessage> buffer;
	private final AmqpLoggingEventRouter router;
	
	public static final String defaultExchange = "logging";
	public static final String defaultRoutingKeyFormat = "logging.event.%s";
}
