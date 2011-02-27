
package ch.qos.logback.amqp;


import java.util.concurrent.LinkedBlockingQueue;

import ch.qos.logback.amqp.tools.Callbacks;
import ch.qos.logback.classic.Level;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;


public final class AmqpConsumer
		extends AmqpAccessor
{
	public AmqpConsumer (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final String[] exchanges, final String queue, final Callbacks callbacks,
			final LinkedBlockingQueue<AmqpMessage> sink)
	{
		super (host, port, virtualHost, username, password, callbacks);
		this.exchanges = exchanges;
		this.queue = queue;
		this.sink = sink;
	}
	
	protected final void loop ()
	{
		loop : while (true) {
			while (true) {
				synchronized (this) {
					if (this.shouldStopLoop ())
						break loop;
					if (!this.shouldReconnect ())
						break;
					if (this.connect ())
						break;
				}
				this.sleep ();
			}
			while (true) {
				synchronized (this) {
					if (this.shouldStopLoop ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
					if (this.declare ())
						break;
				}
				this.sleep ();
				continue loop;
			}
			while (true) {
				synchronized (this) {
					if (this.shouldStopLoop ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
					if (this.register ())
						break;
				}
				this.sleep ();
				continue loop;
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer showeling inbound messages");
			while (true) {
				synchronized (this) {
					if (this.shouldStopLoop ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
				}
				this.sleep ();
			}
		}
		synchronized (this) {
			if (this.isConnected ())
				this.disconnect ();
		}
	}
	
	private final void consume (final Envelope envelope, final BasicProperties properties, final byte[] content)
	{
		final AmqpMessage message =
				new AmqpMessage (
						envelope.getExchange (), envelope.getRoutingKey (), properties.getContentType (),
						properties.getContentEncoding (), content);
		try {
			this.sink.put (message);
		} catch (final InterruptedException exception) {
			this.callbacks.handleException (
					exception, "amqp consumer encountered an error while enqueueing the message; ignoring!");
		}
	}
	
	private final boolean declare ()
	{
		final Channel channel = this.getChannel ();
		for (final String exchange : this.exchanges) {
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer declaring the exchange `%s`", exchange);
			try {
				channel.exchangeDeclare (exchange, "topic", true, false, null);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while declaring the exchange `%s`; aborting!",
						exchange);
				return (false);
			}
		}
		{
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer declaring the queue `%s`", this.queue);
			try {
				channel.queueDeclare (this.queue, true, false, false, null);
			} catch (final Throwable exception) {
				this.callbacks
						.handleException (
								exception, "amqp consumer encountered an error while declaring the queue `%s`; aborting!",
								this.queue);
				return (false);
			}
		}
		for (final String exchange : this.exchanges) {
			this.callbacks.handleLogEvent (
					Level.INFO, null, "amqp consumer binding the queue `%s` to exchange `%s`", this.queue, exchange);
			try {
				channel.queueBind (this.queue, exchange, "#", null);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception,
						"amqp consumer encountered an error while binding the queue `%s` to exchange `%s`; aborting!",
						this.queue, exchange);
				return (false);
			}
		}
		return (true);
	}
	
	private final boolean register ()
	{
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer registering the consumer");
		final Channel channel = this.getChannel ();
		try {
			channel.basicConsume (this.queue, true, this.queue, true, true, null, new ConsumerCallback ());
			return (true);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "amqp consumer encountered an error while registering the consummer; aborting!");
			return (false);
		}
	}
	
	private final String[] exchanges;
	private final String queue;
	private final LinkedBlockingQueue<AmqpMessage> sink;
	
	private final class ConsumerCallback
			implements
				Consumer
	{
		public void handleCancelOk (final String consumerTag)
		{}
		
		public void handleConsumeOk (final String consumerTag)
		{}
		
		public void handleDelivery (
				final String consumerTag, final Envelope envelope, final BasicProperties properties, final byte[] content)
		{
			AmqpConsumer.this.consume (envelope, properties, content);
		}
		
		public void handleRecoverOk ()
		{}
		
		public void handleShutdownSignal (final String consumerTag, final ShutdownSignalException exception)
		{}
	}
}
