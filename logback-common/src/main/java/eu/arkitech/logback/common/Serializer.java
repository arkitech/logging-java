
package eu.arkitech.logback.common;


public interface Serializer
{
	public abstract Object deserialize (final byte[] data)
			throws Throwable;
	
	public abstract Object deserialize (final byte[] data, final int offset, final int size)
			throws Throwable;
	
	public abstract String getContentEncoding ();
	
	public abstract String getContentType ();
	
	public abstract byte[] serialize (final Object object)
			throws Throwable;
}
