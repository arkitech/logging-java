
package eu.arkitech.logback.amqp.publisher;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
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
		this.rawBuffer = this.accessor.buffer;
		this.buffer = new LinkedBlockingQueue<ILoggingEvent> ();
	}
	
	@Override
	public final boolean isDrained ()
	{
		return (this.buffer.isEmpty () && this.rawBuffer.isEmpty ());
	}
	
	@Override
	public final boolean push (final ILoggingEvent event)
			throws InterruptedException
	{
		return (this.push (event, Long.MAX_VALUE, TimeUnit.DAYS));
	}
	
	@Override
	public final boolean push (final ILoggingEvent event, final long timeout, final TimeUnit timeoutUnit)
			throws InterruptedException
	{
		return (this.buffer.offer (event, timeout, timeoutUnit));
	}
	
	@Override
	protected final void executeLoop ()
	{
		this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp event publisher shoveling events to messages");
		while (true) {
			if (this.shouldStopSoft () || this.shouldStopHard ())
				break;
			this.shovelMessage ();
		}
	}
	
	private final AmqpRawMessage encodeMessage (final ILoggingEvent event)
			throws InternalException
	{
		final String exchange;
		try {
			if (this.router != null)
				exchange = this.router.generateExchange (event);
			else
				exchange = AmqpPublisherConfiguration.defaultExchange;
		} catch (final Throwable exception) {
			throw (new InternalException ("amqp event publisher encountered an unknown error while generating the exchange for the event", exception));
		}
		final String routingKey;
		try {
			if (this.router != null)
				routingKey = this.router.generateRoutingKey (event);
			else
				routingKey = String.format (AmqpPublisherConfiguration.defaultRoutingKeyFormat, event.getLevel ().levelStr.toLowerCase ());
		} catch (final Throwable exception) {
			throw (new InternalException ("amqp event publisher encountered an unknown error while generating the routing key for the event", exception));
		}
		return (this.encodeMessage (event, exchange, routingKey));
	}
	
	private final void shovelMessage ()
	{
		try {
			final ILoggingEvent originalEvent;
			try {
				originalEvent = this.buffer.poll (this.waitTimeout, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException exception) {
				return;
			}
			if (originalEvent == null)
				return;
			final ILoggingEvent clonedEvent = this.prepareEvent (originalEvent);
			final AmqpRawMessage message = this.encodeMessage (clonedEvent);
			try {
				if (!this.rawBuffer.offer (message, this.waitTimeout, TimeUnit.MILLISECONDS))
					this.callbacks.handleLogEvent (Level.ERROR, null, "amqp event publisher (raw) buffer overflow; ignoring!");
			} catch (final InterruptedException exception) {
				return;
			}
		} catch (final InternalException exception) {
			this.callbacks.handleException (exception, "amqp publisher encountered an internal error while shoveling the message; ignoring!");
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "amqp publisher encountered an unknown error while shoveling the message; ignoring!");
		}
	}
	
	public final BlockingQueue<ILoggingEvent> buffer;
	private final BlockingQueue<AmqpRawMessage> rawBuffer;
	private final AmqpRouter router;
}
