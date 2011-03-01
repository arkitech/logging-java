
package ro.volution.dev.logback.amqp.common;


import java.io.Serializable;


public interface Serializer
{
	public abstract Serializable deserialize (final byte[] object)
			throws Throwable;
	
	public abstract String getContentEncoding ();
	
	public abstract String getContentType ();
	
	public abstract byte[] serialize (final Serializable object)
			throws Throwable;
}
