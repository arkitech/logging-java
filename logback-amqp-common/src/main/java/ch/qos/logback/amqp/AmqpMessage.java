
package ch.qos.logback.amqp;


public final class AmqpMessage
{
	public AmqpMessage (
			final String exchange, final String routingKey, final String contentType, final String contentEncoding,
			final byte[] content)
	{
		super ();
		this.exchange = exchange;
		this.routingKey = routingKey;
		this.contentType = contentType;
		this.contentEncoding = contentEncoding;
		this.content = content;
	}
	
	public final byte[] content;
	public final String contentEncoding;
	public final String contentType;
	public final String exchange;
	public final String routingKey;
}
