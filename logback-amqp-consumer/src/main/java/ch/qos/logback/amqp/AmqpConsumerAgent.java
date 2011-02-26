
package ch.qos.logback.amqp;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.amqp.tools.DefaultBinarySerializer;
import ch.qos.logback.amqp.tools.ExceptionHandler;
import ch.qos.logback.amqp.tools.Serializer;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import org.slf4j.LoggerFactory;


public final class AmqpConsumerAgent
		extends ContextAwareBase
		implements
			LifeCycle,
			ExceptionHandler
{
	public AmqpConsumerAgent ()
	{
		super ();
		this.serializer = new DefaultBinarySerializer ();
		this.buffer = new LinkedBlockingQueue<AmqpMessage> ();
		this.exchange = AmqpConsumerAgent.defaultExchange;
		this.queue = AmqpConsumerAgent.defaultQueue;
		this.thread = null;
		this.shutdownHook = null;
		this.shouldStop = true;
		this.consumer = null;
		this.isStarted = false;
	}
	
	public final void handleException (final String message, final Throwable exception)
	{
		this.addError (message, exception);
	}
	
	public final boolean isDrained ()
	{
		synchronized (this) {
			return (this.buffer.isEmpty ());
		}
	}
	
	public final boolean isStarted ()
	{
		synchronized (this) {
			return (this.isStarted);
		}
	}
	
	public final boolean isRunning ()
	{
		synchronized (this) {
			return (((this.thread != null) && this.thread.isAlive ()) || ((this.consumer != null) && this.consumer
					.isStarted ()));
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
			this.thread = new Thread (new Runnable () {
				public final void run ()
				{
					AmqpConsumerAgent.this.loop ();
					if (AmqpConsumerAgent.this.shutdownHook != null)
						Runtime.getRuntime ().removeShutdownHook (AmqpConsumerAgent.this.shutdownHook);
				}
			});
			this.thread.setName (String.format ("%s@%x", this.getClass ().getName (), System.identityHashCode (this)));
			this.thread.setDaemon (true);
			this.shutdownHook = new Thread (new Runnable () {
				public final void run ()
				{
					AmqpConsumerAgent.this.shutdownHook = null;
					if (AmqpConsumerAgent.this.isRunning ()) {
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
							this.host, this.port, this.virtualHost, this.username, this.password,
							new String[] {this.exchange}, this.queue, this, this.buffer);
			this.consumer.start ();
			this.isStarted = true;
		}
	}
	
	public final void stop ()
	{
		synchronized (this) {
			if (this.thread == null)
				throw (new IllegalStateException ("amqp consumer agent is not started"));
			this.shouldStop = true;
			this.consumer.stop ();
			this.isStarted = false;
		}
	}
	
	private final void loop ()
	{
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
			final Logger logger = (Logger) LoggerFactory.getLogger (event.getLoggerName ());
			logger.callAppenders (event);
		}
	}
	
	private final LinkedBlockingQueue<AmqpMessage> buffer;
	private AmqpConsumer consumer;
	private String exchange;
	private String host;
	private String password;
	private Integer port;
	private String queue;
	private final Serializer serializer;
	private boolean shouldStop;
	private Thread shutdownHook;
	private Thread thread;
	private String username;
	private String virtualHost;
	private boolean isStarted;
	
	public static final String defaultExchange = "logback";
	public static final String defaultQueue = "logback.agent";
	public static final int waitTimeout = 1000;
}
