
package eu.arkitech.logback.amqp.appender;


import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import eu.arkitech.logback.amqp.accessors.AmqpAccessorAppender;
import eu.arkitech.logback.amqp.accessors.AmqpRouter;
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
			throws Throwable
	{
		this.publisher.push (event);
	}
	
	@Override
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			final boolean publisherStartSucceeded;
			try {
				if (this.publisher != null)
					throw (new IllegalStateException ());
				this.publisher = new AmqpPublisher (this.buildConfiguration ());
				publisherStartSucceeded = this.publisher.start ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp appender encountered an error while starting; aborting!");
				try {
					this.reallyStop ();
				} catch (final Error exception1) {}
				throw (exception);
			}
			if (publisherStartSucceeded) {
				this.exchangeLayout.start ();
				this.routingKeyLayout.start ();
			}
			return (publisherStartSucceeded);
		}
	}
	
	@Override
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			boolean publisherStopSucceeded = false;
			try {
				if (this.publisher != null)
					this.publisher.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp appender encountered an error while stopping the publisher; ignoring");
				this.publisher = null;
			}
			try {
				if (this.publisher != null)
					publisherStopSucceeded = this.publisher.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp appender encountered an error while stopping the publisher; ignoring");
			} finally {
				this.publisher = null;
			}
			if (publisherStopSucceeded) {
				this.exchangeLayout.stop ();
				this.routingKeyLayout.stop ();
			}
			return (publisherStopSucceeded);
		}
	}
	
	protected PatternLayout exchangeLayout;
	protected PatternLayout routingKeyLayout;
	private AmqpPublisher publisher;
	
	public static final String defaultExchangeKeyPattern = "logging%nopex";
	public static final String defaultRoutingKeyPattern = "logging.event.%level%nopex";
}
