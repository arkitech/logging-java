
package eu.arkitech.logback.amqp.accessors;


import ch.qos.logback.classic.Level;
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
	
	public abstract boolean isDrained ();
	
	protected final ILoggingEvent decodeMessage (final AmqpRawMessage message)
			throws InternalException
	{
		try {
			return ((ILoggingEvent) this.serializer.deserialize (message.content));
		} catch (final Throwable exception) {
			throw (new InternalException ("amqp event accessor encountered an unknown error while deserializing the event", exception));
		}
	}
	
	protected final AmqpRawMessage encodeMessage (final ILoggingEvent event, final String exchange, final String routingKey)
			throws InternalException
	{
		try {
			return (new AmqpRawMessage (exchange, routingKey, this.serializer.getContentType (), this.serializer.getContentEncoding (), this.serializer.serialize (event)));
		} catch (final Throwable exception) {
			throw (new InternalException ("amqp event accessor encountered an unknown error while serializing the event", exception));
		}
	}
	
	@Override
	protected final void finalizeLoop ()
	{
		this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp event accessor stopping");
		this.accessor.requestStop ();
		this.accessor.awaitStop ();
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp event accessor stopped");
	}
	
	@Override
	protected final void initializeLoop ()
	{
		this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp event accessor starting");
		this.accessor.start ();
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp event accessor started");
	}
	
	protected final void mutateEvent (final ILoggingEvent event)
			throws InternalException
	{
		try {
			if (this.mutator != null)
				this.mutator.mutate (event);
		} catch (final Throwable exception) {
			throw (new InternalException ("amqp event accessor encountered an unknown error while mutating the event", exception));
		}
	}
	
	protected final ILoggingEvent prepareEvent (final ILoggingEvent originalEvent)
			throws InternalException
	{
		try {
			final SLoggingEvent1 clonedEvent;
			if (!(originalEvent instanceof SLoggingEvent1))
				clonedEvent = SLoggingEvent1.build (originalEvent);
			else
				clonedEvent = (SLoggingEvent1) originalEvent;
			this.mutateEvent (clonedEvent);
			return (clonedEvent);
		} catch (final Error exception) {
			throw (new InternalException ("amqp event accessor encountered an unknown error while preparing the event", exception));
		}
	}
	
	@Override
	protected final boolean shouldStopSoft ()
	{
		return (this.isDrained () && super.shouldStopSoft ());
	}
	
	protected final _Accessor_ accessor;
	protected final LoggingEventMutator mutator;
	protected final Serializer serializer;
	
	protected static final class InternalException
			extends Exception
	{
		public InternalException (final String message, final Throwable cause)
		{
			super (message, cause);
		}
		
		private static final long serialVersionUID = 1L;
	}
}
