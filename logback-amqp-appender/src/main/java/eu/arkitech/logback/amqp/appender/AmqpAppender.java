
package eu.arkitech.logback.amqp.appender;


import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import eu.arkitech.logback.amqp.accessors.AmqpLoggingEventRouter;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.DefaultContextAwareCallbacks;
import eu.arkitech.logback.common.DefaultLoggingEventMutator;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;


public class AmqpAppender
		extends UnsynchronizedAppenderBase<ILoggingEvent>
		implements
			AmqpLoggingEventRouter
{
	public AmqpAppender ()
	{
		super ();
		this.callbacks = new DefaultContextAwareCallbacks (this);
		this.exchangeLayout = new PatternLayout ();
		this.routingKeyLayout = new PatternLayout ();
		this.exchangeLayout.setPattern (AmqpAppender.defaultExchangeKeyPattern);
		this.routingKeyLayout.setPattern (AmqpAppender.defaultRoutingKeyPattern);
		this.mutator = new DefaultLoggingEventMutator ();
	}
	
	public String generateExchange (final ILoggingEvent event)
	{
		return (this.exchangeLayout.doLayout (event));
	}
	
	public String generateRoutingKey (final ILoggingEvent event)
	{
		return (this.routingKeyLayout.doLayout (event));
	}
	
	public String getExchangePattern ()
	{
		return (this.exchangeLayout.getPattern ());
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
	
	public String getRoutingKeyPattern ()
	{
		return (this.routingKeyLayout.getPattern ());
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
		final AmqpPublisherSink sink;
		synchronized (this) {
			sink = this.sink;
		}
		return ((sink == null) || sink.isDrained ());
	}
	
	public final boolean isRunning ()
	{
		final AmqpPublisherSink sink;
		synchronized (this) {
			sink = this.sink;
		}
		return ((sink != null) && sink.isRunning ());
	}
	
	public void setContext (final Context context)
	{
		super.setContext (context);
		this.exchangeLayout.setContext (context);
		this.routingKeyLayout.setContext (context);
	}
	
	public void setExchangePattern (final String pattern)
	{
		this.exchangeLayout.setPattern (pattern);
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
	
	public void setRoutingKeyPattern (final String pattern)
	{
		this.routingKeyLayout.setPattern (pattern);
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
		this.exchangeLayout.start ();
		this.routingKeyLayout.start ();
		super.start ();
	}
	
	public void stop ()
	{
		if (!this.isStarted ())
			return;
		this.reallyStop ();
		this.exchangeLayout.stop ();
		this.routingKeyLayout.stop ();
		super.stop ();
	}
	
	protected void append (final ILoggingEvent event)
	{
		try {
			this.sink.push (event);
		} catch (final Error exception) {
			throw (exception);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "amqp appender encountered an error while processing the event; ignoring!");
		}
	}
	
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			final boolean sinkStartSucceeded;
			try {
				if (this.sink != null)
					throw (new IllegalStateException ());
				this.sink =
						new AmqpPublisherSink (
								this.host, this.port, this.virtualHost, this.username, this.password, this, this.mutator,
								this.serializer, this.callbacks);
				sinkStartSucceeded = this.sink.start ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp appender encountered an error while starting; aborting!");
				try {
					this.reallyStop ();
				} catch (final Error exception1) {}
				throw (exception);
			}
			return (sinkStartSucceeded);
		}
	}
	
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			boolean sinkStopSucceeded = false;
			try {
				if (this.sink != null)
					this.sink.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp appender encountered an error while stopping the sink; ignoring");
				this.sink = null;
			}
			try {
				if (this.sink != null)
					sinkStopSucceeded = this.sink.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp appender encountered an error while stopping the sink; ignoring");
			} finally {
				this.sink = null;
			}
			return (sinkStopSucceeded);
		}
	}
	
	protected final Callbacks callbacks;
	protected PatternLayout exchangeLayout;
	protected String host;
	protected LoggingEventMutator mutator;
	protected String password;
	protected Integer port;
	protected PatternLayout routingKeyLayout;
	protected Serializer serializer;
	protected String username;
	protected String virtualHost;
	private AmqpPublisherSink sink;
	
	public static final String defaultExchangeKeyPattern = "logging%nopex";
	public static final String defaultRoutingKeyPattern = "logging.event.%level%nopex";
}