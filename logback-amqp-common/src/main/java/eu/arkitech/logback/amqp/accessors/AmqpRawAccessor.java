
package eu.arkitech.logback.amqp.accessors;


import java.io.IOException;

import ch.qos.logback.classic.Level;
import com.google.common.base.Preconditions;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import eu.arkitech.logback.common.Worker;


public abstract class AmqpRawAccessor
		extends Worker
{
	protected AmqpRawAccessor (final AmqpRawAccessorConfiguration configuration)
	{
		super (configuration);
		this.host = Preconditions.checkNotNull (((configuration.host != null) && !configuration.host.isEmpty ()) ? configuration.host : AmqpRawAccessorConfiguration.defaultHost);
		Preconditions.checkArgument (!this.host.isEmpty ());
		this.port = Preconditions.checkNotNull (((configuration.port != null) && (configuration.port.intValue () != 0)) ? configuration.port : AmqpRawAccessorConfiguration.defaultPort).intValue ();
		Preconditions.checkArgument (this.port > 0);
		this.virtualHost = Preconditions.checkNotNull (((configuration.virtualHost != null) && !configuration.virtualHost.isEmpty ()) ? configuration.virtualHost : AmqpRawAccessorConfiguration.defaultVirtualHost);
		Preconditions.checkArgument (!this.virtualHost.isEmpty ());
		this.username = Preconditions.checkNotNull (((configuration.username != null) && !configuration.username.isEmpty ()) ? configuration.username : AmqpRawAccessorConfiguration.defaultUsername);
		Preconditions.checkArgument (!this.username.isEmpty ());
		this.password = Preconditions.checkNotNull (((configuration.password != null) && !configuration.password.isEmpty ()) ? configuration.password : AmqpRawAccessorConfiguration.defaultPassword);
		Preconditions.checkArgument (!this.password.isEmpty ());
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
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor connecting to `%s@%s:%s:%s`", this.username, this.host, this.port, this.virtualHost);
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
				@Override
				public void shutdownCompleted (final ShutdownSignalException exception)
				{
					AmqpRawAccessor.this.disconnect ();
					if (!exception.isInitiatedByApplication ())
						AmqpRawAccessor.this.callbacks.handleException (exception, "amqp accessor encountered an shutdown error; ignoring!");
				}
			});
			try {
				this.channel = this.connection.createChannel ();
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "amqp accessor encountered an error while opening the channel; aborting!");
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
					this.callbacks.handleException (exception, "amqp accessor encountered an error while closing the channel; ignoring!");
				} finally {
					this.channel = null;
				}
			if (this.connection != null) {
				try {
					this.connection.close ();
				} catch (final IOException exception) {
					this.callbacks.handleException (exception, "amqp accessor encountered an error while closing the connection; ignoring!");
				} finally {
					this.connection = null;
				}
				this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor disconnected");
			}
		}
	}
	
	@Override
	protected final void executeLoop ()
	{
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp accessor started");
		this.loop ();
	}
	
	@Override
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
	
	@Override
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
}
