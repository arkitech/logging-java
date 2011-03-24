
package eu.arkitech.logback.amqp.accessors;


import java.io.IOException;

import ch.qos.logback.classic.Level;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
		this.host = Preconditions.checkNotNull (!Strings.isNullOrEmpty (configuration.host) ? configuration.host : AmqpRawAccessorConfiguration.defaultHost);
		Preconditions.checkArgument (!this.host.isEmpty ());
		this.port = Preconditions.checkNotNull (((configuration.port != null) && (configuration.port.intValue () != 0)) ? configuration.port : AmqpRawAccessorConfiguration.defaultPort).intValue ();
		Preconditions.checkArgument (this.port > 0);
		this.virtualHost = Preconditions.checkNotNull (!Strings.isNullOrEmpty (configuration.virtualHost) ? configuration.virtualHost : AmqpRawAccessorConfiguration.defaultVirtualHost);
		Preconditions.checkArgument (!this.virtualHost.isEmpty ());
		this.username = Preconditions.checkNotNull (!Strings.isNullOrEmpty (configuration.username) ? configuration.username : AmqpRawAccessorConfiguration.defaultUsername);
		Preconditions.checkArgument (!this.username.isEmpty ());
		this.password = Preconditions.checkNotNull (!Strings.isNullOrEmpty (configuration.password) ? configuration.password : AmqpRawAccessorConfiguration.defaultPassword);
		Preconditions.checkArgument (!this.password.isEmpty ());
	}
	
	public final boolean isConnected ()
	{
		return (this.connection != null);
	}
	
	public abstract boolean isDrained ();
	
	protected final boolean connect ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.connection == null, "amqp message accessor is already connected");
			try {
				this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp message accessor connecting to `%s@%s:%s:%s`", this.username, this.host, this.port, this.virtualHost);
				final ConnectionFactory connectionFactory = new ConnectionFactory ();
				connectionFactory.setHost (this.host);
				connectionFactory.setPort (this.port);
				connectionFactory.setVirtualHost (this.virtualHost);
				connectionFactory.setUsername (this.username);
				connectionFactory.setPassword (this.password);
				try {
					this.connection = connectionFactory.newConnection ();
				} catch (final IOException exception) {
					this.callbacks.handleException (exception, "amqp message accessor encountered a network error while connecting; aborting!");
					this.disconnect ();
					return (false);
				}
				this.connection.addShutdownListener (new ShutdownListener () {
					@Override
					public void shutdownCompleted (final ShutdownSignalException exception)
					{
						if (!exception.isInitiatedByApplication ())
							AmqpRawAccessor.this.callbacks.handleException (exception, "amqp message accessor encountered a shutdown error; disconnecting!");
						AmqpRawAccessor.this.disconnect ();
					}
				});
				try {
					this.channel = this.connection.createChannel ();
				} catch (final IOException exception) {
					this.callbacks.handleException (exception, "amqp message accessor encountered a network error while opening the channel; aborting!");
					this.disconnect ();
					return (false);
				}
				this.callbacks.handleLogEvent (Level.INFO, null, "amqp message accessor connected to `%s@%s:%s:%s`", this.username, this.host, this.port, this.virtualHost);
				return (true);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp message accessor encountered an unknown error while connecting; aborting!");
				this.disconnect ();
				return (false);
			}
		}
	}
	
	protected final boolean disconnect ()
	{
		synchronized (this.monitor) {
			if ((this.connection == null) && (this.channel == null))
				return (false);
			Preconditions.checkState (this.connection != null, "amqp message accessor is not connected");
			try {
				this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp message accessor disconnencting");
				if (this.channel != null)
					try {
						this.channel.close ();
					} catch (final IOException exception) {
						this.callbacks.handleException (exception, "amqp message accessor encountered a network error while closing the channel; ignoring!");
					} finally {
						this.channel = null;
					}
				if (this.connection != null) {
					try {
						this.connection.close ();
					} catch (final IOException exception) {
						this.callbacks.handleException (exception, "amqp message accessor encountered a network error while closing the connection; ignoring!");
					} finally {
						this.connection = null;
					}
				}
				this.callbacks.handleLogEvent (Level.INFO, null, "amqp message accessor disconnected");
				return (true);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp message accessor encountered an unknown error while disconnecting; aborting!");
				return (false);
			} finally {
				this.channel = null;
				this.connection = null;
			}
		}
	}
	
	@Override
	protected final void finalizeLoop ()
	{
		this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp message accessor stopping");
		synchronized (this.monitor) {
			if ((this.connection != null) || (this.channel != null)) {
				this.callbacks.handleLogEvent (Level.WARN, null, "amqp message accessor should have disconnected before ending the loop; disconnecting!");
				this.disconnect ();
			}
		}
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp message accessor stopped");
	}
	
	protected final Channel getChannel ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.connection != null, "amqp message accessor is not connected");
			Preconditions.checkState (this.channel != null, "amqp message accessor is not connected (channel is not opened)");
			return (this.channel);
		}
	}
	
	@Override
	protected final void initializeLoop ()
	{
		this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp message accessor starting");
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp message accessor started");
	}
	
	protected final boolean reconnect ()
	{
		synchronized (this.monitor) {
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
