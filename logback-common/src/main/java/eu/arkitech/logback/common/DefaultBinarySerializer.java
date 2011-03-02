
package eu.arkitech.logback.common;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class DefaultBinarySerializer
		implements
			Serializer
{
	public Object deserialize (final byte[] data)
			throws Throwable
	{
		return (this.deserialize (data, 0, data.length));
	}
	
	public Object deserialize (final byte[] data, final int offset, final int size)
			throws Throwable
	{
		final ByteArrayInputStream stream = new ByteArrayInputStream (data, offset, size);
		final ObjectInputStream decoder = new ObjectInputStream (stream);
		final Object object = decoder.readObject ();
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
	
	public byte[] serialize (final Object object)
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
