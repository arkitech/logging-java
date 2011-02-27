
package ch.qos.logback.amqp;


import java.util.concurrent.LinkedBlockingDeque;

import ch.qos.logback.amqp.tools.DefaultBinarySerializer;
import ch.qos.logback.amqp.tools.DefaultContextAwareCallbacks;
import ch.qos.logback.amqp.tools.DefaultMutator;
import ch.qos.logback.amqp.tools.Mutator;
import ch.qos.logback.amqp.tools.PubLoggingEventVO;
import ch.qos.logback.amqp.tools.Serializer;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.UnsynchronizedAppenderBase;


public final class AmqpAppender
		extends UnsynchronizedAppenderBase<ILoggingEvent>
{
	public AmqpAppender ()
	{
		super ();
		this.serializer = new DefaultBinarySerializer ();
		this.buffer = new LinkedBlockingDeque<AmqpMessage> ();
		this.exchangeLayout = new PatternLayout ();
		this.routingKeyLayout = new PatternLayout ();
		this.exchangeLayout.setPattern (AmqpAppender.defaultExchangeKeyPattern);
		this.routingKeyLayout.setPattern (AmqpAppender.defaultRoutingKeyPattern);
		this.mutator = new DefaultMutator ();
		this.publisher = null;
	}
	
	public final Mutator getMutator ()
	{
		return (this.mutator);
	}
	
	public final boolean isDrained ()
	{
		return (this.buffer.isEmpty ());
	}
	
	public final boolean isRunning ()
	{
		final AmqpPublisher publisher = this.publisher;
		return (((publisher != null) && publisher.isRunning ()) || super.isStarted ());
	}
	
	public final void setContext (final Context context)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		super.setContext (context);
		this.exchangeLayout.setContext (context);
		this.routingKeyLayout.setContext (context);
	}
	
	public final void setExchangePattern (final String pattern)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		this.exchangeLayout.setPattern (pattern);
	}
	
	public final void setHost (final String host)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		this.host = host;
	}
	
	public final void setMutator (final Mutator mutator)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		this.mutator = mutator;
	}
	
	public final void setPassword (final String password)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		this.password = password;
	}
	
	public final void setPort (final Integer port)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		this.port = port;
	}
	
	public final void setRoutingKeyPattern (final String pattern)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		this.routingKeyLayout.setPattern (pattern);
	}
	
	public final void setUsername (final String username)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		this.username = username;
	}
	
	public final void setVirtualHost (final String virtualHost)
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		this.virtualHost = virtualHost;
	}
	
	public final void start ()
	{
		if (this.isStarted ())
			throw (new IllegalStateException ("amqp appender is already started"));
		if (this.publisher != null)
			throw (new IllegalStateException ("amqp appender has passed its life-cycle"));
		this.exchangeLayout.start ();
		this.routingKeyLayout.start ();
		this.publisher =
				new AmqpPublisher (
						this.host, this.port, this.virtualHost, this.username, this.password,
						new DefaultContextAwareCallbacks (this), this.buffer);
		this.publisher.start ();
		super.start ();
	}
	
	public final void stop ()
	{
		if (!this.isStarted ())
			throw (new IllegalStateException ("amqp appender is not started"));
		this.exchangeLayout.stop ();
		this.routingKeyLayout.stop ();
		this.publisher.stop ();
		super.stop ();
	}
	
	protected final void append (final ILoggingEvent originalEvent)
	{
		final PubLoggingEventVO event = this.prepare (originalEvent);
		byte[] data;
		try {
			data = this.serializer.serialize (event);
		} catch (final Throwable exception) {
			data = null;
			this.addError ("amqp appender encountered an error while serializing the event; ignoring!", exception);
		}
		if (data != null) {
			final String exchange = this.exchangeLayout.doLayout (originalEvent);
			final String routingKey = this.routingKeyLayout.doLayout (originalEvent);
			final AmqpMessage message =
					new AmqpMessage (
							exchange, routingKey, this.serializer.getContentType (), this.serializer.getContentEncoding (),
							data);
			this.buffer.add (message);
		}
	}
	
	private final PubLoggingEventVO prepare (final ILoggingEvent originalEvent)
	{
		final PubLoggingEventVO newEvent = PubLoggingEventVO.build (originalEvent);
		if (this.mutator != null)
			this.mutator.mutate (newEvent);
		return (newEvent);
	}
	
	private final LinkedBlockingDeque<AmqpMessage> buffer;
	private final PatternLayout exchangeLayout;
	private String host;
	private Mutator mutator;
	private String password;
	private Integer port;
	private AmqpPublisher publisher;
	private final PatternLayout routingKeyLayout;
	private final Serializer serializer;
	private String username;
	private String virtualHost;
	
	public static final String defaultExchangeKeyPattern = "logback";
	public static final String defaultRoutingKeyPattern = "%level.%logger";
}
