
package eu.arkitech.logback.amqp.consumer;


import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.amqp.accessors.AmqpAccessorAppender;
import eu.arkitech.logback.common.AppenderNewInstanceAction;
import eu.arkitech.logback.common.SourceSinkPump;
import org.slf4j.LoggerFactory;


public class AmqpConsumerAppender
		extends AmqpAccessorAppender
{
	public AmqpConsumerAppender ()
	{
		super ();
		this.startDelay = AmqpConsumerAppender.defaultStartDelay;
	}
	
	public String getExchange ()
	{
		return (this.exchange);
	}
	
	public String getQueue ()
	{
		return (this.queue);
	}
	
	public String getRoutingKey ()
	{
		return (this.routingKey);
	}
	
	public long getStartDelay ()
	{
		return (this.startDelay);
	}
	
	@Override
	public final boolean isDrained ()
	{
		synchronized (this) {
			return ((this.consumer == null) || this.consumer.isDrained ());
		}
	}
	
	@Override
	public final boolean isRunning ()
	{
		synchronized (this) {
			return ((this.consumer != null) && this.consumer.isRunning ());
		}
	}
	
	public void setExchange (final String exchange)
	{
		this.exchange = exchange;
	}
	
	public void setQueue (final String queue)
	{
		this.queue = queue;
	}
	
	public void setRoutingKey (final String routingKey)
	{
		this.routingKey = routingKey;
	}
	
	public void setStartDelay (final long startDelay)
	{
		this.startDelay = startDelay;
	}
	
	protected AmqpConsumerConfiguration buildConfiguration ()
	{
		return (new AmqpConsumerConfiguration (this.host, this.port, this.virtualHost, this.username, this.password, this.exchange, this.queue, this.routingKey, this.serializer, this.mutator, this.callbacks, null));
	}
	
	@Override
	protected void reallyAppend (final ILoggingEvent event)
	{
		final Logger logger = (Logger) LoggerFactory.getLogger (event.getLoggerName ());
		logger.callAppenders (event);
	}
	
	@Override
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			try {
				if ((this.consumer != null) || (this.pump != null))
					throw (new IllegalStateException ());
				final AmqpConsumerConfiguration configuration = this.buildConfiguration ();
				this.consumer = new AmqpConsumer (configuration);
				final boolean succeeded = this.consumer.start ();
				if (succeeded) {
					this.pump = new SourceSinkPump (configuration, this.consumer, this);
					this.pump.start (this.startDelay);
				}
				if (!succeeded) {
					this.reallyStop ();
					return (false);
				}
				return (succeeded);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer appender encountered an unknown error while starting; aborting!");
				this.reallyStop ();
				return (false);
			}
		}
	}
	
	@Override
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			try {
				boolean succeeded = true;
				if (this.consumer != null)
					this.consumer.requestStop ();
				if (this.consumer != null) {
					succeeded &= this.consumer.awaitStop ();
					this.consumer = null;
				}
				if (this.pump != null)
					this.pump.requestStop ();
				if (this.pump != null) {
					succeeded &= this.pump.awaitStop ();
					this.pump = null;
				}
				return (succeeded);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer appender encountered an unknown error while stopping; ignoring!");
				return (false);
			} finally {
				this.consumer = null;
				this.pump = null;
			}
		}
	}
	
	protected String exchange;
	protected String queue;
	protected String routingKey;
	protected long startDelay;
	private AmqpConsumer consumer;
	private SourceSinkPump pump;
	public static final long defaultStartDelay = 3000;
	
	public static final class CreateAction
			extends AppenderNewInstanceAction<AmqpConsumerAppender>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoRegister, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<? super AmqpConsumerAppender> collector, final boolean autoRegister, final boolean autoStart)
		{
			super (AmqpConsumerAppender.class, collector, autoRegister, autoStart);
		}
		
		public static boolean defaultAutoRegister = true;
		public static boolean defaultAutoStart = true;
		public static List<? super AmqpConsumerAppender> defaultCollector = null;
	}
}
