
package eu.arkitech.logback.amqp.consumer;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.spi.LifeCycle;
import eu.arkitech.logback.amqp.accessors.AmqpConsumer;
import eu.arkitech.logback.amqp.accessors.AmqpMessage;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.DefaultContextAwareCallbacks;
import eu.arkitech.logback.common.Serializer;
import org.slf4j.LoggerFactory;


public class AmqpConsumerAgent
		extends ContextAwareBase
		implements
			LifeCycle
{
	public AmqpConsumerAgent ()
	{
		super ();
		this.callbacks = new DefaultContextAwareCallbacks (this);
		this.buffer = new LinkedBlockingQueue<AmqpMessage> ();
		this.exchange = AmqpConsumerAgent.defaultExchange;
		this.queue = AmqpConsumerAgent.defaultQueue;
		this.routingKey = AmqpConsumerAgent.defaultRoutingKey;
		this.serializer = new DefaultBinarySerializer ();
		this.filter = null;
		this.thread = null;
		this.shutdownHook = null;
		this.shouldStop = true;
		this.consumer = null;
		this.isStarted = false;
	}
	
	public final String getExchange ()
	{
		return (this.exchange);
	}
	
	public final Filter<ILoggingEvent> getFilter ()
	{
		return (this.filter);
	}
	
	public final String getHost ()
	{
		return (this.host);
	}
	
	public final String getPassword ()
	{
		return (this.password);
	}
	
	public final Integer getPort ()
	{
		return (this.port);
	}
	
	public final String getQueue ()
	{
		return (this.queue);
	}
	
	public final String getRoutingKey ()
	{
		return (this.routingKey);
	}
	
	public final Serializer getSerializer ()
	{
		return (this.serializer);
	}
	
	public final String getUsername ()
	{
		return (this.username);
	}
	
	public final String getVirtualHost ()
	{
		return (this.virtualHost);
	}
	
	public final boolean isDrained ()
	{
		synchronized (this) {
			return (this.buffer.isEmpty ());
		}
	}
	
	public final boolean isRunning ()
	{
		synchronized (this) {
			return (((this.thread != null) && this.thread.isAlive ()) || ((this.consumer != null) && this.consumer
					.isRunning ()));
		}
	}
	
	public final boolean isStarted ()
	{
		synchronized (this) {
			return (this.isStarted);
		}
	}
	
	public final void setContext (final Context context)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp appender is already started"));
			super.setContext (context);
		}
	}
	
	public final void setExchange (final String exchange)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.exchange = exchange;
		}
	}
	
	public final void setFilter (final Filter<ILoggingEvent> filter)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp appender is already started"));
			this.filter = filter;
		}
	}
	
	public final void setHost (final String host)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.host = host;
		}
	}
	
	public final void setPassword (final String password)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.password = password;
		}
	}
	
	public final void setPort (final Integer port)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.port = port;
		}
	}
	
	public final void setQueue (final String queue)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.queue = queue;
		}
	}
	
	public final void setRoutingKey (final String routingKey)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.routingKey = routingKey;
		}
	}
	
	public final void setSerializer (final Serializer serializer)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.serializer = serializer;
		}
	}
	
	public final void setUsername (final String username)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.username = username;
		}
	}
	
	public final void setVirtualHost (final String virtualHost)
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.virtualHost = virtualHost;
		}
	}
	
	public final void start ()
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp consumer agent is already started"));
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer agent starting");
			this.preStart ();
			this.thread = new Thread (new Runnable () {
				public final void run ()
				{
					AmqpConsumerAgent.this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer agent started");
					AmqpConsumerAgent.this.loop ();
					if (AmqpConsumerAgent.this.shutdownHook != null)
						Runtime.getRuntime ().removeShutdownHook (AmqpConsumerAgent.this.shutdownHook);
					AmqpConsumerAgent.this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer agent stopped");
				}
			});
			this.thread.setName (String.format ("%s@%x", this.getClass ().getName (), System.identityHashCode (this)));
			this.thread.setDaemon (true);
			this.shutdownHook = new Thread (new Runnable () {
				public final void run ()
				{
					AmqpConsumerAgent.this.shutdownHook = null;
					if (AmqpConsumerAgent.this.isRunning ()) {
						if (!AmqpConsumerAgent.this.shouldStop)
							AmqpConsumerAgent.this.stop ();
						while (AmqpConsumerAgent.this.isRunning ())
							try {
								Thread.sleep (AmqpConsumerAgent.waitTimeout);
							} catch (final InterruptedException exception) {
								break;
							}
					}
				}
			});
			Runtime.getRuntime ().addShutdownHook (this.shutdownHook);
			this.shouldStop = false;
			this.thread.start ();
			this.consumer =
					new AmqpConsumer (
							this.host, this.port, this.virtualHost, this.username, this.password, this.exchange, this.queue,
							this.routingKey, this.callbacks, this.buffer);
			this.consumer.start ();
			this.isStarted = true;
			this.postStart ();
		}
	}
	
	public final void stop ()
	{
		synchronized (this) {
			if (this.thread == null)
				throw (new IllegalStateException ("amqp consumer agent is not started"));
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer agent stopping");
			this.preStop ();
			this.shouldStop = true;
			this.consumer.stop ();
			this.isStarted = false;
			this.postStop ();
		}
	}
	
	protected void postStart ()
	{}
	
	protected void postStop ()
	{}
	
	protected void preStart ()
	{}
	
	protected void preStop ()
	{}
	
	protected void process (final ILoggingEvent event)
	{
		final Logger logger = (Logger) LoggerFactory.getLogger (event.getLoggerName ());
		logger.callAppenders (event);
	}
	
	private final void loop ()
	{
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer agent showling logging events");
		while (true) {
			if (this.shouldStop)
				break;
			final AmqpMessage message;
			try {
				message = this.buffer.poll (AmqpConsumerAgent.waitTimeout, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException exception) {
				continue;
			}
			if (message == null)
				continue;
			final ILoggingEvent event;
			try {
				event = (ILoggingEvent) this.serializer.deserialize (message.content);
			} catch (final Throwable exception) {
				this.addError (
						"amqp consumer agent encountered an error while deserializing the message; ignoring!", exception);
				continue;
			}
			try {
				if (this.filter != null)
					if (this.filter.decide (event) == FilterReply.DENY)
						continue;
				this.process (event);
			} catch (final Throwable exception) {
				this.addError (
						"amqp consumer agent encountered an error while processing the logging event; ignoring!", exception);
			}
		}
	}
	
	protected final Callbacks callbacks;
	private final LinkedBlockingQueue<AmqpMessage> buffer;
	private AmqpConsumer consumer;
	private String exchange;
	private Filter<ILoggingEvent> filter;
	private String host;
	private boolean isStarted;
	private String password;
	private Integer port;
	private String queue;
	private String routingKey;
	private Serializer serializer;
	private boolean shouldStop;
	private Thread shutdownHook;
	private Thread thread;
	private String username;
	private String virtualHost;
	
	public static final String defaultExchange = "logback";
	public static final String defaultQueue = "";
	public static final String defaultRoutingKey = "#";
	public static final int waitTimeout = 1000;
}
