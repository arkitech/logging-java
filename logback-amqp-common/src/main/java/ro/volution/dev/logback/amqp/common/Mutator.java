
package ro.volution.dev.logback.amqp.common;

import ro.volution.dev.logback.amqp.common.SerializableLoggingEvent1;


public interface Mutator
{
	public abstract void mutate (final SerializableLoggingEvent1 event);
}
