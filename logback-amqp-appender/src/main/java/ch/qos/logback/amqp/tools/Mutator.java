
package ch.qos.logback.amqp.tools;


public interface Mutator
{
	public abstract void mutate (final PubLoggingEventVO event);
}
