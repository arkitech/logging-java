
package ch.qos.logback.amqp;


import ch.qos.logback.amqp.tools.ExceptionHandler;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;


public abstract class AmqpAccessor
{
	protected AmqpAccessor (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final ExceptionHandler exceptionHandler)
	{
		super ();
		this.host = ((host != null) && !host.isEmpty ()) ? host : "127.0.0.1";
		this.port = ((port != null) && (port != 0)) ? port : 5672;
		this.virtualHost = ((virtualHost != null) && !virtualHost.isEmpty ()) ? virtualHost : "/";
		this.username = ((username != null) && !username.isEmpty ()) ? username : "guest";
		this.password = ((password != null) && !password.isEmpty ()) ? password : "guest";
		this.exceptionHandler = exceptionHandler;
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
	
	public final boolean isStarted ()
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
			this.thread = new Thread (new Runnable () {
				public final void run ()
				{
					AmqpAccessor.this.loop ();
					if (AmqpAccessor.this.shutdownHook != null)
						Runtime.getRuntime ().removeShutdownHook (AmqpAccessor.this.shutdownHook);
				}
			});
			this.thread.setName (String.format ("%s@%x", this.getClass ().getName (), System.identityHashCode (this)));
			this.thread.setDaemon (true);
			this.shutdownHook = new Thread (new Runnable () {
				public final void run ()
				{
					AmqpAccessor.this.shutdownHook = null;
					if (AmqpAccessor.this.isStarted ()) {
						AmqpAccessor.this.stop ();
						while (AmqpAccessor.this.isStarted ())
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
			this.shouldStopLoop = true;
		}
	}
	
	protected final boolean connect ()
	{
		synchronized (this) {
			if (this.connection != null)
				throw (new IllegalStateException ("amqp accessor is already connected"));
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
				this.exceptionHandler.handleException (
						"amqp accessor encountered an error while connecting; aborting!", exception);
			}
			if (this.connection != null)
				try {
					this.channel = this.connection.createChannel ();
				} catch (final Throwable exception) {
					this.channel = null;
					this.exceptionHandler.handleException (
							"amqp accessor encountered an error while opening a channel; aborting!", exception);
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
						if (!exception.isInitiatedByApplication ())
							AmqpAccessor.this.exceptionHandler.handleException (
									"amqp consumer encountered an shutdown error; ignoring!", exception);
					}
				});
				this.shouldReconnect = false;
			}
			return (this.connection != null);
		}
	}
	
	protected final void disconnect ()
	{
		if (this.connection == null)
			throw (new IllegalStateException ("amqp accessor is not connected"));
		this.shouldReconnect = true;
		try {
			try {
				if (this.channel != null)
					this.channel.close ();
			} finally {
				this.connection.close ();
			}
		} catch (final Throwable exception) {
			this.exceptionHandler.handleException (
					"amqp accessor encountered an error while disconnecting; ignoring!", exception);
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
	
	protected final ExceptionHandler exceptionHandler;
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
