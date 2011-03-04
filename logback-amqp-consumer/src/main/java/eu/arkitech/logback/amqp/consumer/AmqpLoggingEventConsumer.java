
package eu.arkitech.logback.amqp.consumer;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.amqp.accessors.AmqpLoggingEventAccessor;
import eu.arkitech.logback.amqp.accessors.AmqpRawConsumer;
import eu.arkitech.logback.amqp.accessors.AmqpMessage;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.LoggingEventSource;
import eu.arkitech.logback.common.Serializer;


public final class AmqpLoggingEventConsumer
		extends AmqpLoggingEventAccessor<AmqpRawConsumer>
		implements
			LoggingEventSource
{
	public AmqpLoggingEventConsumer (
			final AmqpRawConsumer accessor, final LoggingEventMutator mutator, final Serializer serializer,
			final Callbacks callbacks)
	{
		super (accessor, mutator, serializer, callbacks, accessor.monitor);
		this.buffer = this.accessor.getBuffer ();
	}
	
	public AmqpLoggingEventConsumer (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final String exchange, final String queue, final String routingKey)
	{
		this (host, port, virtualHost, username, password, exchange, queue, routingKey, null, null, null);
	}
	
	public AmqpLoggingEventConsumer (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final String exchange, final String queue, final String routingKey, final LoggingEventMutator mutator,
			final Serializer serializer, final Callbacks callbacks)
	{
		this (
				new AmqpRawConsumer (
						host, port, virtualHost, username, password, exchange, queue, routingKey, null, callbacks, null),
				mutator, serializer, callbacks);
	}
	
	public final boolean isDrained ()
	{
		return (this.buffer.isEmpty ());
	}
	
	public final ILoggingEvent pull ()
			throws InterruptedException
	{
		return (this.pull (Long.MAX_VALUE, TimeUnit.DAYS));
	}
	
	public final ILoggingEvent pull (final long timeout, final TimeUnit timeoutUnit)
			throws InterruptedException
	{
		final AmqpMessage message = this.buffer.poll (timeout, timeoutUnit);
		if (message == null)
			return (null);
		final ILoggingEvent originalEvent = this.decodeMessage (message);
		if (originalEvent == null)
			return (null);
		final ILoggingEvent clonedEvent = this.prepareEvent (originalEvent);
		if (clonedEvent == null)
			return (null);
		return (clonedEvent);
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
	
	private final BlockingQueue<AmqpMessage> buffer;
	
	public static final String defaultExchange = "logging";
	public static final String defaultRoutingKeyFormat = "logging.event.%s";
}
