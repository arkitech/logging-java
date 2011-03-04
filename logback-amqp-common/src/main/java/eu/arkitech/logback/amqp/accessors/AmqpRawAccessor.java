
package eu.arkitech.logback.amqp.accessors;


import java.io.IOException;

import ch.qos.logback.classic.Level;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.Worker;


public abstract class AmqpRawAccessor
		extends Worker
{
	protected AmqpRawAccessor (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final Callbacks callbacks, final Object monitor)
	{
		super (callbacks, monitor);
		synchronized (this.monitor) {
			this.host = ((host != null) && !host.isEmpty ()) ? host : AmqpRawAccessor.defaultHost;
			this.port = ((port != null) && (port != 0)) ? port : AmqpRawAccessor.defaultPort;
			this.virtualHost =
					((virtualHost != null) && !virtualHost.isEmpty ()) ? virtualHost : AmqpRawAccessor.defaultVirtualHost;
			this.username = ((username != null) && !username.isEmpty ()) ? username : AmqpRawAccessor.defaultUsername;
			this.password = ((password != null) && !password.isEmpty ()) ? password : AmqpRawAccessor.defaultPassword;
		}
	}
	
	public final boolean isConnected ()
	{
		synchronized (this.monitor) {
			return (this.connection != null);
		}
	}
	
	protected final boolean connect ()
	{
		synchronized (this.monitor) {
			if (this.connection != null)
				throw (new IllegalStateException ("amqp accessor is already connected"));
			this.callbacks.handleLogEvent (
					Level.INFO, null, "amqp accessor connecting to `%s@%s:%s:%s`", this.username, this.host, this.port,
					this.virtualHost);
			final ConnectionFactory connectionFactory = new ConnectionFactory ();
			connectionFactory.setHost (this.host);
			connectionFactory.setPort (this.port);
			connectionFactory.setVirtualHost (this.virtualHost);
			connectionFactory.setUsername (this.username);
			connectionFactory.setPassword (this.password);
			try {
				this.connection = connectionFactory.newConnection ();
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "amqp accessor encountered an error while connecting; aborting!");
				this.disconnect ();
				return (false);
			}
			this.connection.addShutdownListener (new ShutdownListener () {
				public void shutdownCompleted (final ShutdownSignalException exception)
				{
					AmqpRawAccessor.this.disconnect ();
					if (!exception.isInitiatedByApplication ())
						AmqpRawAccessor.this.callbacks.handleException (
								exception, "amqp accessor encountered an shutdown error; ignoring!");
				}
			});
			try {
				this.channel = this.connection.createChannel ();
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "amqp accessor encountered an error while opening the channel; aborting!");
				this.disconnect ();
				return (false);
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor connected");
			return (true);
		}
	}
	
	protected final void disconnect ()
	{
		synchronized (this.monitor) {
			if (this.channel != null)
				try {
					this.channel.close ();
				} catch (final IOException exception) {
					this.callbacks.handleException (
							exception, "amqp accessor encountered an error while closing the channel; ignoring!");
				} finally {
					this.channel = null;
				}
			if (this.connection != null) {
				try {
					this.connection.close ();
				} catch (final IOException exception) {
					this.callbacks.handleException (
							exception, "amqp accessor encountered an error while closing the connection; ignoring!");
				} finally {
					this.connection = null;
				}
				this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor disconnected");
			}
		}
	}
	
	protected final void executeLoop ()
	{
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor started");
		this.loop ();
	}
	
	protected final void finalizeLoop ()
	{
		this.disconnect ();
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor stopped");
	}
	
	protected final Channel getChannel ()
	{
		synchronized (this.monitor) {
			if (this.channel == null)
				throw (new IllegalStateException ("amqp accessor is not connected"));
			return (this.channel);
		}
	}
	
	protected final void initializeLoop ()
	{}
	
	protected abstract void loop ();
	
	protected final boolean reconnect ()
	{
		synchronized (this.monitor) {
			if (this.isConnected ())
				this.disconnect ();
			return (this.connect ());
		}
	}
	
	protected final boolean shouldReconnect ()
	{
		return (this.connection == null);
	}
	
	protected final String host;
	protected final String password;
	protected final int port;
	protected final String username;
	protected final String virtualHost;
	private Channel channel;
	private Connection connection;
	
	public static final String defaultHost = "127.0.0.1";
	public static final String defaultPassword = "guest";
	public static final int defaultPort = 5672;
	public static final String defaultUsername = "guest";
	public static final String defaultVirtualHost = "/";
}
