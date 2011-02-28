
package ch.qos.logback.amqp;


import ch.qos.logback.amqp.tools.Callbacks;
import ch.qos.logback.classic.Level;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;


public abstract class AmqpAccessor
{
	protected AmqpAccessor (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final Callbacks callbacks)
	{
		super ();
		this.host = ((host != null) && !host.isEmpty ()) ? host : "127.0.0.1";
		this.port = ((port != null) && (port != 0)) ? port : 5672;
		this.virtualHost = ((virtualHost != null) && !virtualHost.isEmpty ()) ? virtualHost : "/";
		this.username = ((username != null) && !username.isEmpty ()) ? username : "guest";
		this.password = ((password != null) && !password.isEmpty ()) ? password : "guest";
		this.callbacks = callbacks;
		this.thread = null;
		this.shutdownHook = null;
		this.shouldStopLoop = true;
		this.connection = null;
		this.channel = null;
		this.shouldReconnect = true;
	}
	
	public final boolean isConnected ()
	{
		synchronized (this) {
			return (this.connection != null);
		}
	}
	
	public final boolean isRunning ()
	{
		synchronized (this) {
			return ((this.thread != null) && (this.thread.isAlive ()));
		}
	}
	
	public final void start ()
	{
		synchronized (this) {
			if (this.thread != null)
				throw (new IllegalStateException ("amqp accessor is already started"));
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor starting");
			this.thread = new Thread (new Runnable () {
				public final void run ()
				{
					AmqpAccessor.this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor started");
					AmqpAccessor.this.loop ();
					if (AmqpAccessor.this.shutdownHook != null)
						Runtime.getRuntime ().removeShutdownHook (AmqpAccessor.this.shutdownHook);
					AmqpAccessor.this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor stopped");
				}
			});
			this.thread.setName (String.format ("%s@%x", this.getClass ().getName (), System.identityHashCode (this)));
			this.thread.setDaemon (true);
			this.shutdownHook = new Thread (new Runnable () {
				public final void run ()
				{
					AmqpAccessor.this.shutdownHook = null;
					if (AmqpAccessor.this.isRunning ()) {
						if (!AmqpAccessor.this.shouldStopLoop)
							AmqpAccessor.this.stop ();
						while (AmqpAccessor.this.isRunning ())
							try {
								Thread.sleep (AmqpAccessor.waitTimeout);
							} catch (final InterruptedException exception) {
								break;
							}
					}
				}
			});
			Runtime.getRuntime ().addShutdownHook (this.shutdownHook);
			this.shouldStopLoop = false;
			this.thread.start ();
		}
	}
	
	public final void stop ()
	{
		synchronized (this) {
			if (this.thread == null)
				throw (new IllegalStateException ("amqp accessor is not started"));
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor stopping");
			this.shouldStopLoop = true;
		}
	}
	
	protected final boolean connect ()
	{
		synchronized (this) {
			if (this.connection != null)
				throw (new IllegalStateException ("amqp accessor is already connected"));
			this.callbacks.handleLogEvent (
					Level.INFO, null, "amqp accessor connecting to `%s@%s:%s:%s`", this.username, this.host, this.port,
					this.virtualHost);
			this.shouldReconnect = true;
			final ConnectionFactory connectionFactory = new ConnectionFactory ();
			connectionFactory.setHost (this.host);
			connectionFactory.setPort (this.port);
			connectionFactory.setVirtualHost (this.virtualHost);
			connectionFactory.setUsername (this.username);
			connectionFactory.setPassword (this.password);
			try {
				this.connection = connectionFactory.newConnection ();
			} catch (final Throwable exception) {
				this.connection = null;
				this.callbacks.handleException (exception, "amqp accessor encountered an error while connecting; aborting!");
			}
			if (this.connection != null)
				try {
					this.channel = this.connection.createChannel ();
				} catch (final Throwable exception) {
					this.channel = null;
					this.callbacks.handleException (
							exception, "amqp accessor encountered an error while opening the channel; aborting!");
				}
			if ((this.connection == null) || (this.channel == null)) {
				if (this.connection != null)
					this.disconnect ();
			}
			if (this.connection != null) {
				this.connection.addShutdownListener (new ShutdownListener () {
					public void shutdownCompleted (final ShutdownSignalException exception)
					{
						AmqpAccessor.this.shouldReconnect = true;
						if (!exception.isInitiatedByApplication ()) {
							AmqpAccessor.this.callbacks.handleException (
									exception, "amqp consumer encountered an shutdown error; ignoring!");
							AmqpAccessor.this.disconnect ();
						}
					}
				});
				this.shouldReconnect = false;
				this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor connected");
			}
			return (this.connection != null);
		}
	}
	
	protected final void disconnect ()
	{
		if (this.connection == null)
			throw (new IllegalStateException ("amqp accessor is not connected"));
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor disconnecting");
		this.shouldReconnect = true;
		try {
			try {
				if (this.channel != null)
					this.channel.close ();
			} finally {
				this.connection.close ();
			}
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "amqp accessor encountered an error while disconnecting; ignoring!");
		} finally {
			this.connection = null;
			this.channel = null;
		}
	}
	
	protected final Channel getChannel ()
	{
		if (this.channel == null)
			throw (new IllegalStateException ());
		return (this.channel);
	}
	
	protected abstract void loop ();
	
	protected final boolean reconnect ()
	{
		if (this.isConnected ())
			this.disconnect ();
		return (this.connect ());
	}
	
	protected final boolean shouldReconnect ()
	{
		return (this.shouldReconnect);
	}
	
	protected final boolean shouldStopLoop ()
	{
		return (this.shouldStopLoop);
	}
	
	protected final void sleep ()
	{
		try {
			Thread.sleep (AmqpAccessor.waitTimeout);
		} catch (final InterruptedException exception) {}
	}
	
	protected final Callbacks callbacks;
	protected final String host;
	protected final String password;
	protected final int port;
	protected final String username;
	protected final String virtualHost;
	private Channel channel;
	private Connection connection;
	private boolean shouldReconnect;
	private boolean shouldStopLoop;
	private Thread shutdownHook;
	private Thread thread;
	
	public static final int waitTimeout = 1000;
}
