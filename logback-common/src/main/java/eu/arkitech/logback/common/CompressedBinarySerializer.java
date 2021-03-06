/*
 * #%L
 * arkitech-logback-common
 * %%
 * Copyright (C) 2011 - 2012 Arkitech
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
		this (CompressedBinarySerializer.compressedContentType, CompressedBinarySerializer.compressedContentEncoding, CompressedBinarySerializer.compressedBufferSize, CompressedBinarySerializer.compressedLevel);
	}
	
	public CompressedBinarySerializer (final int level)
	{
		this (CompressedBinarySerializer.compressedContentType, CompressedBinarySerializer.compressedContentEncoding, CompressedBinarySerializer.compressedBufferSize, level);
	}
	
	public CompressedBinarySerializer (final String contentType, final String contentEncoding, final int bufferSize, final int level)
	{
		super (contentType, contentEncoding, bufferSize);
		this.level = level;
	}
	
	@Override
	protected InputStream decorate (final InputStream stream)
	{
		return (new CompressedInputStream (stream));
	}
	
	@Override
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
		
		@Override
		public void close ()
				throws IOException
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
		
		@Override
		public void close ()
				throws IOException
		{
			super.close ();
			this.def.end ();
		}
	}
}
