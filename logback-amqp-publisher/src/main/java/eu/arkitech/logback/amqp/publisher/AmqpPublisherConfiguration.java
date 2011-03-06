
package eu.arkitech.logback.amqp.publisher;


import com.google.common.base.Objects;
import eu.arkitech.logback.amqp.accessors.AmqpAccessorConfiguration;
import eu.arkitech.logback.amqp.accessors.AmqpRawPublisherConfiguration;
import eu.arkitech.logback.amqp.accessors.AmqpRouter;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;


public class AmqpPublisherConfiguration
		extends AmqpRawPublisherConfiguration
		implements
			AmqpAccessorConfiguration
{
	protected AmqpPublisherConfiguration (final AmqpPublisherConfiguration override, final AmqpPublisherConfiguration overriden)
	{
		super (override, overriden);
		this.router = Objects.firstNonNull (override.router, overriden.router);
		this.serializer = Objects.firstNonNull (override.serializer, overriden.serializer);
		this.mutator = Objects.firstNonNull (override.mutator, overriden.mutator);
	}
	
	protected AmqpPublisherConfiguration (final String host, final Integer port, final String virtualHost, final String username, final String password, final AmqpRouter router, final Serializer serializer, final LoggingEventMutator mutator, final Callbacks callbacks, final Object monitor)
	{
		super (host, port, virtualHost, username, password, callbacks, monitor);
		this.router = router;
		this.serializer = serializer;
		this.mutator = mutator;
	}
	
	@Override
	public LoggingEventMutator getMutator ()
	{
		return (this.mutator);
	}
	
	@Override
	public Serializer getSerializer ()
	{
		return (this.serializer);
	}
	
	public final LoggingEventMutator mutator;
	public final AmqpRouter router;
	public final Serializer serializer;
	
	public static final String defaultExchange = "logging";
	public static final LoggingEventMutator defaultMutator = AmqpAccessorConfiguration.defaultMutator;
	public static final String defaultRoutingKeyFormat = "logging.event.%s";
	public static final Serializer defaultSerializer = AmqpAccessorConfiguration.defaultSerializer;
}
