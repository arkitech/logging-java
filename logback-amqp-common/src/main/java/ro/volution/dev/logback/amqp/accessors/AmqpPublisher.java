
package ro.volution.dev.logback.amqp.accessors;


import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import ro.volution.dev.logback.common.Callbacks;


public final class AmqpPublisher
		extends AmqpAccessor
{
	public AmqpPublisher (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final Callbacks callbacks, final LinkedBlockingDeque<AmqpMessage> source)
	{
		super (host, port, virtualHost, username, password, callbacks);
		this.source = source;
	}
	
	protected final void loop ()
	{
		loop : while (true) {
			while (true) {
				synchronized (this) {
					if (this.shouldStopLoop ())
						break loop;
					if (this.reconnect ())
						break;
				}
				this.sleep ();
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp publisher showeling outbound messages");
			while (true) {
				synchronized (this) {
					if (this.shouldStopLoop ())
						break loop;
					if (this.shouldReconnect ())
						break;
				}
				final AmqpMessage message;
				try {
					message = this.source.poll (AmqpAccessor.waitTimeout, TimeUnit.MILLISECONDS);
				} catch (final InterruptedException exception) {
					continue;
				}
				if (message == null)
					continue;
				synchronized (this) {
					if (!this.publish (message))
						this.source.addFirst (message);
				}
			}
		}
		if (this.isConnected ())
			this.disconnect ();
	}
	
	private final boolean publish (final AmqpMessage message)
	{
		final Channel channel = this.getChannel ();
		final AMQP.BasicProperties properties = new AMQP.BasicProperties ();
		properties.setContentType (message.contentType);
		properties.setContentEncoding (message.contentEncoding);
		properties.setDeliveryMode (2);
		try {
			channel.basicPublish (message.exchange, message.routingKey, false, false, properties, message.content);
			return (true);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "amqp publisher encountered an error while publishing the message; requeueing!");
			return (false);
		}
	}
	
	private final LinkedBlockingDeque<AmqpMessage> source;
}
