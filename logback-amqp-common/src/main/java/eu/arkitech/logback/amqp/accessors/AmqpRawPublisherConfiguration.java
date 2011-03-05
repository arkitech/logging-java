
package eu.arkitech.logback.amqp.accessors;


import eu.arkitech.logback.common.Callbacks;


public class AmqpRawPublisherConfiguration
		extends AmqpRawAccessorConfiguration
{
	public AmqpRawPublisherConfiguration ()
	{
		this (null, null, null, null, null, null, null);
	}
	
	public AmqpRawPublisherConfiguration (final AmqpRawPublisherConfiguration override, final AmqpRawPublisherConfiguration overriden)
	{
		super (override, overriden);
	}
	
	public AmqpRawPublisherConfiguration (final String host, final Integer port, final String virtualHost, final String username, final String password, final Callbacks callbacks, final Object monitor)
	{
		super (host, port, virtualHost, username, password, callbacks, monitor);
	}
}
