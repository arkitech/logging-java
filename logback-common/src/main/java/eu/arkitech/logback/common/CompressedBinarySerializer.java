
package eu.arkitech.logback.common;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


public class CompressedBinarySerializer
		extends DefaultBinarySerializer
{
	public CompressedBinarySerializer ()
	{
		this (
				CompressedBinarySerializer.compressedContentType, CompressedBinarySerializer.compressedContentEncoding,
				CompressedBinarySerializer.compressedBufferSize, CompressedBinarySerializer.compressedLevel);
	}
	
	public CompressedBinarySerializer (final int level)
	{
		this (
				CompressedBinarySerializer.compressedContentType, CompressedBinarySerializer.compressedContentEncoding,
				CompressedBinarySerializer.compressedBufferSize, level);
	}
	
	public CompressedBinarySerializer (
			final String contentType, final String contentEncoding, final int bufferSize, final int level)
	{
		super (contentType, contentEncoding, bufferSize);
		this.level = level;
	}
	
	protected InputStream decorate (final InputStream stream)
	{
		return (new CompressedInputStream (stream));
	}
	
	protected OutputStream decorate (final OutputStream stream)
	{
		return (new CompressedOutputStream (stream, this.level));
	}
	
	protected int level;
	
	public static final int compressedBufferSize = 1024;
	public static final String compressedContentEncoding = "binary";
	public static final String compressedContentType = "application/x-java-serialized-object+zlib";
	public static final int compressedLevel = 5;
	
	public static class CompressedInputStream
			extends InflaterInputStream
	{
		public CompressedInputStream (final InputStream stream)
		{
			super (stream, new Inflater ());
		}
		
		public void close () throws IOException
		{
			super.close ();
			this.inf.end ();
		}
	}
	
	public static class CompressedOutputStream
			extends DeflaterOutputStream
	{
		public CompressedOutputStream (final OutputStream stream, final int level)
		{
			super (stream, new Deflater (level));
		}
		
		public void close () throws IOException
		{
			super.close ();
			this.def.end ();
		}
	}
}
