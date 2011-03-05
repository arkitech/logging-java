
package eu.arkitech.logback.amqp.consumer;


import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.amqp.accessors.AmqpLoggingEventAppenderSink;
import eu.arkitech.logback.common.ClassNewInstanceAction;
import eu.arkitech.logback.common.LoggingEventPump;
import org.slf4j.LoggerFactory;


public class AmqpLoggingInjector
		extends AmqpLoggingEventAppenderSink
{
	public AmqpLoggingInjector ()
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
	
	public final boolean isDrained ()
	{
		synchronized (this) {
			return ((this.consumer == null) || this.consumer.isDrained ());
		}
	}
	
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
	
	protected void reallyAppend (final ILoggingEvent event)
	{
		final Logger logger = (Logger) LoggerFactory.getLogger (event.getLoggerName ());
		logger.callAppenders (event);
	}
	
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			final boolean consumerStartSucceeded;
			final boolean pumpStartSucceeded;
			try {
				if ((this.consumer != null) || (this.pump != null))
					throw (new IllegalStateException ());
				this.consumer =
						new AmqpLoggingEventConsumer (
								this.host, this.port, this.virtualHost, this.username, this.password, this.exchange,
								this.queue, this.routingKey, this.serializer, this.mutator, this.callbacks);
				this.pump = new LoggingEventPump (this.consumer, this, this.callbacks);
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
	
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			boolean consumerStopSucceeded = false;
			final boolean pumpStopSucceeded = false;
			try {
				if (this.consumer != null)
					this.consumer.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the consumer; ignoring");
				this.consumer = null;
			}
			try {
				if (this.pump != null)
					this.pump.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the pump; ignoring");
				this.pump = null;
			}
			try {
				if (this.consumer != null)
					consumerStopSucceeded = this.consumer.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the consumer; ignoring");
			} finally {
				this.consumer = null;
			}
			try {
				if (this.pump != null)
					consumerStopSucceeded = this.pump.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the pump; ignoring");
			} finally {
				this.pump = null;
			}
			return (consumerStopSucceeded && pumpStopSucceeded);
		}
	}
	
	protected String exchange;
	protected String queue;
	protected String routingKey;
	private AmqpLoggingEventConsumer consumer;
	private LoggingEventPump pump;
	
	public static final class CreateAction
			extends ClassNewInstanceAction<AmqpLoggingInjector>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<AmqpLoggingInjector> collector, final boolean autoStart)
		{
			super (AmqpLoggingInjector.class, collector, autoStart);
		}
		
		public static boolean defaultAutoStart = true;
		public static List<AmqpLoggingInjector> defaultCollector = null;
	}
}
