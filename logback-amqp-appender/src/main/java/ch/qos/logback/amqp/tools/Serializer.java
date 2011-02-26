
package ch.qos.logback.amqp.tools;


import java.io.Serializable;


public interface Serializer
{
	public abstract Serializable deserialize (final byte[] object)
			throws Throwable;
	
	public abstract byte[] serialize (final Serializable object)
			throws Throwable;
	
	public abstract String getContentType ();
	
	public abstract String getContentEncoding ();
}
