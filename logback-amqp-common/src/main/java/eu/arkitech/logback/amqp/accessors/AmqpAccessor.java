
package eu.arkitech.logback.amqp.accessors;


import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Preconditions;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.SLoggingEvent1;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logback.common.Worker;
import eu.arkitech.logback.common.WorkerConfiguration;


public abstract class AmqpAccessor<_Accessor_ extends AmqpRawAccessor>
		extends Worker
{
	protected AmqpAccessor (final _Accessor_ accessor, final AmqpAccessorConfiguration configuration)
	{
		super ((WorkerConfiguration) configuration);
		this.accessor = Preconditions.checkNotNull (accessor);
		final Serializer configurationSerializer = configuration.getSerializer ();
		this.serializer = Preconditions.checkNotNull ((configurationSerializer != null) ? configurationSerializer : AmqpAccessorConfiguration.defaultSerializer);
		this.mutator = configuration.getMutator ();
	}
	
	protected final ILoggingEvent decodeMessage (final AmqpRawMessage message)
	{
		final ILoggingEvent event;
		try {
			event = (ILoggingEvent) this.serializer.deserialize (message.content);
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "amqp consumer source encountered an error while deserializing the event; aborting!");
			return (null);
		}
		return (event);
	}
	
	protected final AmqpRawMessage encodeMessage (final ILoggingEvent event, final String exchange, final String routingKey)
	{
		final byte[] data;
		try {
			data = this.serializer.serialize (event);
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "amqp publisher sink encountered an error while serializing the event; aborting!");
			return (null);
		}
		return (new AmqpRawMessage (exchange, routingKey, this.serializer.getContentType (), this.serializer.getContentEncoding (), data));
	}
	
	protected final ILoggingEvent prepareEvent (final ILoggingEvent originalEvent)
	{
		final SLoggingEvent1 clonedEvent;
		if (!(originalEvent instanceof SLoggingEvent1))
			try {
				clonedEvent = SLoggingEvent1.build (originalEvent);
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "amqp publisher sink encountered an error while cloning the event; aborting!");
				return (null);
			}
		else
			clonedEvent = (SLoggingEvent1) originalEvent;
		try {
			if (this.mutator != null)
				this.mutator.mutate (clonedEvent);
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "amqp publisher sink encountered an error while mutating the event; aborting!");
			return (null);
		}
		return (clonedEvent);
	}
	
	protected final _Accessor_ accessor;
	protected final LoggingEventMutator mutator;
	protected final Serializer serializer;
}
