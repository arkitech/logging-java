
package eu.arkitech.logback.amqp.publisher;


import java.util.List;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import eu.arkitech.logback.amqp.accessors.AmqpAccessorAppender;
import eu.arkitech.logback.amqp.accessors.AmqpRouter;
import eu.arkitech.logback.common.AppenderNewInstanceAction;
import eu.arkitech.logback.common.DefaultLoggingEventMutator;


public class AmqpPublisherAppender
		extends AmqpAccessorAppender
		implements
			AmqpRouter
{
	public AmqpPublisherAppender ()
	{
		super ();
		this.exchangeLayout = new PatternLayout ();
		this.routingKeyLayout = new PatternLayout ();
		this.exchangeLayout.setPattern (AmqpPublisherAppender.defaultExchangeKeyPattern);
		this.routingKeyLayout.setPattern (AmqpPublisherAppender.defaultRoutingKeyPattern);
		this.mutator = new DefaultLoggingEventMutator ();
	}
	
	@Override
	public String generateExchange (final ILoggingEvent event)
	{
		return (this.exchangeLayout.doLayout (event));
	}
	
	@Override
	public String generateRoutingKey (final ILoggingEvent event)
	{
		return (this.routingKeyLayout.doLayout (event));
	}
	
	public String getExchangePattern ()
	{
		return (this.exchangeLayout.getPattern ());
	}
	
	public String getRoutingKeyPattern ()
	{
		return (this.routingKeyLayout.getPattern ());
	}
	
	@Override
	public final boolean isDrained ()
	{
		synchronized (this) {
			return ((this.publisher == null) || this.publisher.isDrained ());
		}
	}
	
	@Override
	public final boolean isRunning ()
	{
		synchronized (this) {
			return ((this.publisher != null) && this.publisher.isRunning ());
		}
	}
	
	@Override
	public void setContext (final Context context)
	{
		super.setContext (context);
		this.exchangeLayout.setContext (context);
		this.routingKeyLayout.setContext (context);
	}
	
	public void setExchangePattern (final String pattern)
	{
		this.exchangeLayout.setPattern (pattern);
	}
	
	public void setRoutingKeyPattern (final String pattern)
	{
		this.routingKeyLayout.setPattern (pattern);
	}
	
	protected AmqpPublisherConfiguration buildConfiguration ()
	{
		return (new AmqpPublisherConfiguration (this.host, this.port, this.virtualHost, this.username, this.password, this, this.serializer, this.mutator, this.callbacks, null));
	}
	
	@Override
	protected final void reallyAppend (final ILoggingEvent event)
	{
		try {
			event.prepareForDeferredProcessing ();
			this.publisher.push (event);
		} catch (final InterruptedException exception) {
			this.callbacks.handleException (exception, "amqp publisher appender encountered an interruption error while pushing the message; ignoring!");
		}
	}
	
	@Override
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			try {
				if (this.publisher != null)
					throw (new IllegalStateException ());
				this.publisher = new AmqpPublisher (this.buildConfiguration ());
				boolean succeeded = this.publisher.start ();
				if (succeeded)
					try {
						this.exchangeLayout.start ();
						this.routingKeyLayout.start ();
					} catch (final Error exception) {
						this.callbacks.handleException (exception, "amqp publisher appender encountered an unknown error while starting layouts; aborting!");
						succeeded = false;
					}
				if (!succeeded) {
					this.reallyStop ();
					return (false);
				}
				return (succeeded);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp publisher appender encountered an unknown error while starting; aborting!");
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
				if (this.publisher != null)
					this.publisher.requestStop ();
				if (this.publisher != null) {
					succeeded &= this.publisher.awaitStop ();
					this.publisher = null;
				}
				try {
					this.exchangeLayout.stop ();
					this.routingKeyLayout.stop ();
				} catch (final Error exception) {
					this.callbacks.handleException (exception, "amqp publisher appender encountered an unknown error while stopping the layouts; ignoring!");
					succeeded = false;
				}
				return (succeeded);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp publisher appender encountered an unknown error while stopping; ignoring!");
				return (false);
			} finally {
				this.publisher = null;
			}
		}
	}
	
	protected PatternLayout exchangeLayout;
	protected PatternLayout routingKeyLayout;
	private AmqpPublisher publisher;
	
	public static final String defaultExchangeKeyPattern = "logging%nopex";
	public static final String defaultRoutingKeyPattern = "logging.event.%level%nopex";
	
	public static final class CreateAction
			extends AppenderNewInstanceAction<AmqpPublisherAppender>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoRegister, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<? super AmqpPublisherAppender> collector, final boolean autoRegister, final boolean autoStart)
		{
			super (AmqpPublisherAppender.class, collector, autoRegister, autoStart);
		}
		
		public static boolean defaultAutoRegister = true;
		public static boolean defaultAutoStart = true;
		public static List<? super AmqpPublisherAppender> defaultCollector = null;
	}
}
