
package eu.arkitech.logback.amqp.consumer;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.amqp.accessors.AmqpAccessor;
import eu.arkitech.logback.amqp.accessors.AmqpRawConsumer;
import eu.arkitech.logback.amqp.accessors.AmqpRawMessage;
import eu.arkitech.logback.common.LoggingEventSource;


public final class AmqpConsumer
		extends AmqpAccessor<AmqpRawConsumer>
		implements
			LoggingEventSource
{
	public AmqpConsumer (final AmqpConsumerConfiguration configuration)
	{
		this (null, configuration);
	}
	
	public AmqpConsumer (final AmqpRawConsumer accessor, final AmqpConsumerConfiguration configuration)
	{
		super ((accessor != null) ? accessor : new AmqpRawConsumer (configuration, null), configuration);
		this.buffer = this.accessor.getBuffer ();
	}
	
	@Override
	public final boolean isDrained ()
	{
		return (this.buffer.isEmpty ());
	}
	
	@Override
	public final ILoggingEvent pull ()
			throws InterruptedException
	{
		return (this.pull (Long.MAX_VALUE, TimeUnit.DAYS));
	}
	
	@Override
	public final ILoggingEvent pull (final long timeout, final TimeUnit timeoutUnit)
			throws InterruptedException
	{
		final AmqpRawMessage message = this.buffer.poll (timeout, timeoutUnit);
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
	
	private final BlockingQueue<AmqpRawMessage> buffer;
	
	public static final String defaultExchange = "logging";
	public static final String defaultRoutingKeyFormat = "logging.event.%s";
}
