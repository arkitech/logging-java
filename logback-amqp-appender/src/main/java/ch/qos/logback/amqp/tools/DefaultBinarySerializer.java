
package ch.qos.logback.amqp.tools;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public final class DefaultBinarySerializer
		implements
			Serializer
{
	public final Serializable deserialize (final byte[] data)
			throws Throwable
	{
		final ByteArrayInputStream stream = new ByteArrayInputStream (data);
		final ObjectInputStream decoder = new ObjectInputStream (stream);
		final Serializable object = (Serializable) decoder.readObject ();
		return (object);
	}
	
	public final byte[] serialize (final Serializable object)
			throws Throwable
	{
		final ByteArrayOutputStream stream = new ByteArrayOutputStream (this.defaultBufferSize);
		final ObjectOutputStream encoder = new ObjectOutputStream (stream);
		encoder.writeObject (object);
		encoder.close ();
		return (stream.toByteArray ());
	}
	
	public final String getContentType ()
	{
		return (this.contentType);
	}
	
	public final String getContentEncoding ()
	{
		return (this.contentEncoding);
	}
	
	public final int defaultBufferSize = 2048;
	public final String contentType = "application/x-java-serialized-object";
	public final String contentEncoding = "binary";
}
