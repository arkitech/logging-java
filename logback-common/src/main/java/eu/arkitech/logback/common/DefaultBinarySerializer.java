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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


public class DefaultBinarySerializer
		implements
			Serializer
{
	public DefaultBinarySerializer ()
	{
		this (DefaultBinarySerializer.defaultContentType, DefaultBinarySerializer.defaultContentEncoding, DefaultBinarySerializer.defaultBufferSize);
	}
	
	public DefaultBinarySerializer (final String contentType, final String contentEncoding, final int bufferSize)
	{
		super ();
		this.contentType = contentType;
		this.contentEncoding = contentEncoding;
		this.bufferSize = bufferSize;
	}
	
	@Override
	public Object deserialize (final byte[] data)
			throws IOException,
				ClassNotFoundException
	{
		return (this.deserialize (data, 0, data.length));
	}
	
	@Override
	public Object deserialize (final byte[] data, final int offset, final int size)
			throws IOException,
				ClassNotFoundException
	{
		final ByteArrayInputStream stream = new ByteArrayInputStream (data, offset, size);
		final InputStream decoratedStream = this.decorate (stream);
		final ObjectInputStream decoder = new ObjectInputStream (decoratedStream);
		final Object object = decoder.readObject ();
		decoder.close ();
		return (object);
	}
	
	public int getBufferSize ()
	{
		return (this.bufferSize);
	}
	
	@Override
	public String getContentEncoding ()
	{
		return (this.contentEncoding);
	}
	
	@Override
	public String getContentType ()
	{
		return (this.contentType);
	}
	
	@Override
	public byte[] serialize (final Object object)
			throws IOException
	{
		final ByteArrayOutputStream stream = new ByteArrayOutputStream (this.bufferSize);
		final OutputStream decoratedStream = this.decorate (stream);
		final ObjectOutputStream encoder = new ObjectOutputStream (decoratedStream);
		encoder.writeObject (object);
		encoder.close ();
		return (stream.toByteArray ());
	}
	
	public void setBufferSize (final int bufferSize)
	{
		this.bufferSize = bufferSize;
	}
	
	public void setContentEncoding (final String contentEncoding)
	{
		this.contentEncoding = contentEncoding;
	}
	
	public void setContentType (final String contentType)
	{
		this.contentType = contentType;
	}
	
	@SuppressWarnings ("unused")
	protected InputStream decorate (final InputStream stream)
			throws IOException
	{
		return (stream);
	}
	
	@SuppressWarnings ("unused")
	protected OutputStream decorate (final OutputStream stream)
			throws IOException
	{
		return (stream);
	}
	
	protected int bufferSize;
	protected String contentEncoding;
	protected String contentType;
	public static final int defaultBufferSize = 2048;
	public static final String defaultContentEncoding = "binary";
	public static final String defaultContentType = "application/x-java-serialized-object";
}
