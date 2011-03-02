
package eu.arkitech.logback.common;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class DefaultBinarySerializer
		implements
			Serializer
{
	public Serializable deserialize (final byte[] data)
			throws Throwable
	{
		final ByteArrayInputStream stream = new ByteArrayInputStream (data);
		final ObjectInputStream decoder = new ObjectInputStream (stream);
		final Serializable object = (Serializable) decoder.readObject ();
		return (object);
	}
	
	public String getContentEncoding ()
	{
		return (this.contentEncoding);
	}
	
	public String getContentType ()
	{
		return (this.contentType);
	}
	
	public int getDefaultBufferSize ()
	{
		return (this.defaultBufferSize);
	}
	
	public byte[] serialize (final Serializable object)
			throws Throwable
	{
		final ByteArrayOutputStream stream = new ByteArrayOutputStream (this.defaultBufferSize);
		final ObjectOutputStream encoder = new ObjectOutputStream (stream);
		encoder.writeObject (object);
		encoder.close ();
		return (stream.toByteArray ());
	}
	
	public void setContentEncoding (final String contentEncoding)
	{
		this.contentEncoding = contentEncoding;
	}
	
	public void setContentType (final String contentType)
	{
		this.contentType = contentType;
	}
	
	public void setDefaultBufferSize (final int defaultBufferSize)
	{
		this.defaultBufferSize = defaultBufferSize;
	}
	
	protected String contentEncoding = "binary";
	protected String contentType = "application/x-java-serialized-object";
	protected int defaultBufferSize = 2048;
}
