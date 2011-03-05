
package eu.arkitech.logback.amqp.consumer;


import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.amqp.accessors.AmqpAccessorAppender;
import eu.arkitech.logback.common.ClassNewInstanceAction;
import eu.arkitech.logback.common.LoggingEventPump;
import org.slf4j.LoggerFactory;


public class AmqpConsumerAppender
		extends AmqpAccessorAppender
{
	public AmqpConsumerAppender ()
	{
		super ();
	}
	
	public String getExchange ()
	{
		return (this.exchange);
	}
	
	public String getQueue ()
	{
		return (this.queue);
	}
	
	public String getRoutingKey ()
	{
		return (this.routingKey);
	}
	
	@Override
	public final boolean isDrained ()
	{
		synchronized (this) {
			return ((this.consumer == null) || this.consumer.isDrained ());
		}
	}
	
	@Override
	public final boolean isRunning ()
	{
		synchronized (this) {
			return ((this.consumer != null) && this.consumer.isRunning ());
		}
	}
	
	public void setExchange (final String exchange)
	{
		this.exchange = exchange;
	}
	
	public void setQueue (final String queue)
	{
		this.queue = queue;
	}
	
	public void setRoutingKey (final String routingKey)
	{
		this.routingKey = routingKey;
	}
	
	protected AmqpConsumerConfiguration buildConfiguration ()
	{
		return (new AmqpConsumerConfiguration (this.host, this.port, this.virtualHost, this.username, this.password, this.exchange, this.queue, this.routingKey, this.serializer, this.mutator, this.callbacks, null));
	}
	
	@Override
	protected void reallyAppend (final ILoggingEvent event)
	{
		final Logger logger = (Logger) LoggerFactory.getLogger (event.getLoggerName ());
		logger.callAppenders (event);
	}
	
	@Override
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			final boolean consumerStartSucceeded;
			final boolean pumpStartSucceeded;
			try {
				if ((this.consumer != null) || (this.pump != null))
					throw (new IllegalStateException ());
				final AmqpConsumerConfiguration configuration = this.buildConfiguration ();
				this.consumer = new AmqpConsumer (configuration);
				this.pump = new LoggingEventPump (configuration, this.consumer, this);
				consumerStartSucceeded = this.consumer.start ();
				pumpStartSucceeded = this.pump.start ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while starting; aborting!");
				try {
					this.reallyStop ();
				} catch (final Error exception1) {}
				throw (exception);
			}
			return (consumerStartSucceeded && pumpStartSucceeded);
		}
	}
	
	@Override
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			boolean consumerStopSucceeded = false;
			final boolean pumpStopSucceeded = false;
			try {
				if (this.consumer != null)
					this.consumer.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while stopping the consumer; ignoring");
				this.consumer = null;
			}
			try {
				if (this.pump != null)
					this.pump.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while stopping the pump; ignoring");
				this.pump = null;
			}
			try {
				if (this.consumer != null)
					consumerStopSucceeded = this.consumer.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while stopping the consumer; ignoring");
			} finally {
				this.consumer = null;
			}
			try {
				if (this.pump != null)
					consumerStopSucceeded = this.pump.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while stopping the pump; ignoring");
			} finally {
				this.pump = null;
			}
			return (consumerStopSucceeded && pumpStopSucceeded);
		}
	}
	
	protected String exchange;
	protected String queue;
	protected String routingKey;
	private AmqpConsumer consumer;
	private LoggingEventPump pump;
	
	public static final class CreateAction
			extends ClassNewInstanceAction<AmqpConsumerAppender>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<AmqpConsumerAppender> collector, final boolean autoStart)
		{
			super (AmqpConsumerAppender.class, collector, autoStart);
		}
		
		public static boolean defaultAutoStart = true;
		public static List<AmqpConsumerAppender> defaultCollector = null;
	}
}
