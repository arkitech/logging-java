
package eu.arkitech.logback.amqp.consumer;


import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.ClassNewInstanceAction;
import eu.arkitech.logback.common.DefaultAppenderSink;
import eu.arkitech.logback.common.DefaultContextAwareCallbacks;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.LoggingEventPump;
import eu.arkitech.logback.common.Serializer;
import org.slf4j.LoggerFactory;


public class AmqpLoggingInjector
		extends DefaultAppenderSink
{
	public AmqpLoggingInjector ()
	{
		super ();
		this.callbacks = new DefaultContextAwareCallbacks (this);
	}
	
	public String getExchange ()
	{
		return (this.exchange);
	}
	
	public String getHost ()
	{
		return (this.host);
	}
	
	public LoggingEventMutator getMutator ()
	{
		return (this.mutator);
	}
	
	public String getPassword ()
	{
		return (this.password);
	}
	
	public Integer getPort ()
	{
		return (this.port);
	}
	
	public String getQueue ()
	{
		return (this.queue);
	}
	
	public String getRoutingKey ()
	{
		return (this.routingKey);
	}
	
	public Serializer getSerializer ()
	{
		return (this.serializer);
	}
	
	public String getUsername ()
	{
		return (this.username);
	}
	
	public String getVirtualHost ()
	{
		return (this.virtualHost);
	}
	
	public final boolean isDrained ()
	{
		final AmqpConsumerSource source;
		synchronized (this) {
			source = this.source;
		}
		return ((source == null) || source.isDrained ());
	}
	
	public final boolean isRunning ()
	{
		final AmqpConsumerSource source;
		synchronized (this) {
			source = this.source;
		}
		return ((source != null) && source.isRunning ());
	}
	
	public void setExchange (final String exchange)
	{
		this.exchange = exchange;
	}
	
	public void setHost (final String host)
	{
		this.host = host;
	}
	
	public void setMutator (final LoggingEventMutator mutator)
	{
		this.mutator = mutator;
	}
	
	public void setPassword (final String password)
	{
		this.password = password;
	}
	
	public void setPort (final Integer port)
	{
		this.port = port;
	}
	
	public void setQueue (final String queue)
	{
		this.queue = queue;
	}
	
	public void setRoutingKey (final String routingKey)
	{
		this.routingKey = routingKey;
	}
	
	public void setSerializer (final Serializer serializer)
	{
		this.serializer = serializer;
	}
	
	public void setUsername (final String username)
	{
		this.username = username;
	}
	
	public void setVirtualHost (final String virtualHost)
	{
		this.virtualHost = virtualHost;
	}
	
	public void start ()
	{
		if (this.isStarted ())
			return;
		this.reallyStart ();
		super.start ();
	}
	
	public void stop ()
	{
		if (!this.isStarted ())
			return;
		this.reallyStop ();
		super.stop ();
	}
	
	protected void append (final ILoggingEvent event)
	{
		final Logger logger = (Logger) LoggerFactory.getLogger (event.getLoggerName ());
		logger.callAppenders (event);
	}
	
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			final boolean sourceStartSucceeded;
			final boolean pumpStartSucceeded;
			try {
				if ((this.source != null) || (this.pump != null))
					throw (new IllegalStateException ());
				this.source =
						new AmqpConsumerSource (
								this.host, this.port, this.virtualHost, this.username, this.password, this.exchange,
								this.queue, this.routingKey, this.mutator, this.serializer, this.callbacks);
				this.pump = new LoggingEventPump (this.source, this, this.callbacks);
				sourceStartSucceeded = this.source.start ();
				pumpStartSucceeded = this.pump.start ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while starting; aborting!");
				try {
					this.reallyStop ();
				} catch (final Error exception1) {}
				throw (exception);
			}
			return (sourceStartSucceeded && pumpStartSucceeded);
		}
	}
	
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			boolean sourceStopSucceeded = false;
			final boolean pumpStopSucceeded = false;
			try {
				if (this.source != null)
					this.source.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the source; ignoring");
				this.source = null;
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
				if (this.source != null)
					sourceStopSucceeded = this.source.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the source; ignoring");
			} finally {
				this.source = null;
			}
			try {
				if (this.pump != null)
					sourceStopSucceeded = this.pump.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the pump; ignoring");
			} finally {
				this.pump = null;
			}
			return (sourceStopSucceeded && pumpStopSucceeded);
		}
	}
	
	protected final Callbacks callbacks;
	protected String exchange;
	protected String host;
	protected LoggingEventMutator mutator;
	protected String password;
	protected Integer port;
	protected String queue;
	protected String routingKey;
	protected Serializer serializer;
	protected String username;
	protected String virtualHost;
	private LoggingEventPump pump;
	private AmqpConsumerSource source;
	
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
