
package eu.arkitech.logback.amqp.accessors;


import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;


public interface AmqpAccessorConfiguration
{
	public abstract LoggingEventMutator getMutator ();
	
	public abstract Serializer getSerializer ();
	
	public static final LoggingEventMutator defaultMutator = null;
	public static final Serializer defaultSerializer = new DefaultBinarySerializer ();
}
