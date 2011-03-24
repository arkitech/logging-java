
package eu.arkitech.logback.amqp.consumer;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.amqp.accessors.AmqpAccessor;
import eu.arkitech.logback.amqp.accessors.AmqpRawConsumer;
import eu.arkitech.logback.amqp.accessors.AmqpRawMessage;
import eu.arkitech.logback.common.LoggingEventSource;
import eu.arkitech.logback.common.WorkerThread.State;


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
		this.rawBuffer = this.accessor.buffer;
		this.buffer = new LinkedBlockingQueue<ILoggingEvent> ();
	}
	
	@Override
	public final boolean isDrained ()
	{
		return (this.buffer.isEmpty () && this.rawBuffer.isEmpty () && !this.accessor.isRunning ());
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
		return (this.buffer.poll (timeout, timeoutUnit));
	}
	
	@Override
	protected final void executeLoop ()
	{
		this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp event consumer shoveling messages to events");
		boolean accessorRequestedStop = false;
		while (true) {
			if (this.shouldStopSoft () || this.shouldStopHard ())
				break;
			if ((this.getState () == State.Stopping) && !accessorRequestedStop) {
				this.accessor.requestStop ();
				accessorRequestedStop = true;
			}
			this.shovelMessage ();
		}
	}
	
	private final void shovelMessage ()
	{
		try {
			final AmqpRawMessage message;
			try {
				message = this.rawBuffer.poll (this.waitTimeout, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException exception) {
				return;
			}
			if (message == null)
				return;
			final ILoggingEvent originalEvent = this.decodeMessage (message);
			final ILoggingEvent clonedEvent = this.prepareEvent (originalEvent);
			try {
				if (!this.buffer.offer (clonedEvent, this.waitTimeout, TimeUnit.MILLISECONDS))
					this.callbacks.handleLogEvent (Level.ERROR, null, "amqp event consumer (event) buffer overflow; ignoring!");
			} catch (final InterruptedException exception) {
				return;
			}
		} catch (final InternalException exception) {
			this.callbacks.handleException (exception, "amqp event consumer encountered an internal error while shoveling the message; ignoring!");
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "amqp event consumer encountered an unknown error while shoveling the message; ignoring!");
		}
	}
	
	public final BlockingQueue<ILoggingEvent> buffer;
	private final BlockingQueue<AmqpRawMessage> rawBuffer;
	
	public static final String defaultExchange = "logging";
	public static final String defaultRoutingKeyFormat = "logging.event.%s";
}
