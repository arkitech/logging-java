
package eu.arkitech.logback.amqp.accessors;


import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.DefaultLoggerCallbacks;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.SLoggingEvent1;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logback.common.Worker;


public abstract class AmqpAccessorWrapper<_Accessor_ extends AmqpAccessor>
		extends Worker
{
	protected AmqpAccessorWrapper (
			final _Accessor_ accessor, final LoggingEventMutator mutator, final Serializer serializer,
			final Callbacks callbacks, final Object monitor)
	{
		super (callbacks, monitor);
		this.accessor = accessor;
		this.mutator = mutator;
		this.serializer = (serializer != null) ? serializer : new DefaultBinarySerializer ();
		this.callbacks = (callbacks != null) ? callbacks : new DefaultLoggerCallbacks (this);
	}
	
	protected final ILoggingEvent decodeMessage (final AmqpMessage message)
	{
		final ILoggingEvent event;
		try {
			event = (ILoggingEvent) this.serializer.deserialize (message.content);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "amqp consumer source encountered an error while deserializing the event; aborting!");
			return (null);
		}
		return (event);
	}
	
	protected final AmqpMessage encodeMessage (final ILoggingEvent event, final String exchange, final String routingKey)
	{
		final byte[] data;
		try {
			data = this.serializer.serialize (event);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "amqp publisher sink encountered an error while serializing the event; aborting!");
			return (null);
		}
		return (new AmqpMessage (
				exchange, routingKey, this.serializer.getContentType (), this.serializer.getContentEncoding (), data));
	}
	
	protected final ILoggingEvent prepareEvent (final ILoggingEvent originalEvent)
	{
		final SLoggingEvent1 clonedEvent;
		if (!(originalEvent instanceof SLoggingEvent1))
			try {
				clonedEvent = SLoggingEvent1.build (originalEvent);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "amqp publisher sink encountered an error while cloning the event; aborting!");
				return (null);
			}
		else
			clonedEvent = (SLoggingEvent1) originalEvent;
		try {
			if (this.mutator != null)
				this.mutator.mutate (clonedEvent);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "amqp publisher sink encountered an error while mutating the event; aborting!");
			return (null);
		}
		return (clonedEvent);
	}
	
	protected final _Accessor_ accessor;
	protected final Callbacks callbacks;
	protected final LoggingEventMutator mutator;
	protected final Serializer serializer;
}
