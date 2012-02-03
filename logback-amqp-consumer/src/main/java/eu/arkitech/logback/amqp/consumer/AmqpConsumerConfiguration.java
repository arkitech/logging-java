
package eu.arkitech.logback.amqp.consumer;


import com.google.common.base.Objects;
import eu.arkitech.logback.amqp.accessors.AmqpAccessorConfiguration;
import eu.arkitech.logback.amqp.accessors.AmqpRawConsumerConfiguration;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;


public class AmqpConsumerConfiguration
		extends AmqpRawConsumerConfiguration
		implements
			AmqpAccessorConfiguration
{
	protected AmqpConsumerConfiguration (final AmqpConsumerConfiguration override, final AmqpConsumerConfiguration overriden)
	{
		super (override, overriden);
		this.serializer = Objects.firstNonNull (override.serializer, overriden.serializer);
		this.mutator = Objects.firstNonNull (override.mutator, overriden.mutator);
	}
	
	protected AmqpConsumerConfiguration (final String host, final Integer port, final String virtualHost, final String username, final String password, final String exchange, final String queue, final String routingKey, final Serializer serializer, final LoggingEventMutator mutator, final Callbacks callbacks, final Object monitor)
	{
		super (host, port, virtualHost, username, password, exchange, queue, routingKey, callbacks, monitor);
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
	public final Serializer serializer;
	public static final LoggingEventMutator defaultMutator = AmqpAccessorConfiguration.defaultMutator;
	public static final Serializer defaultSerializer = AmqpAccessorConfiguration.defaultSerializer;
}
