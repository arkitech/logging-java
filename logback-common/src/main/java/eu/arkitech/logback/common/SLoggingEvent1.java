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
/**
 * Logback: the reliable, generic, fast and flexible logging framework. Copyright (C) 1999-2009, QOS.ch. All rights reserved.
 * This program and the accompanying materials are dual-licensed under either the terms of the Eclipse Public License v1.0 as
 * published by the Eclipse Foundation or (per the licensee's choosing) under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation.
 */

package eu.arkitech.logback.common;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.ThrowableProxyVO;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;


public class SLoggingEvent1
		implements
			ILoggingEvent,
			Serializable
{
	@Override
	public boolean equals (final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass () != obj.getClass ())
			return false;
		final SLoggingEvent1 other = (SLoggingEvent1) obj;
		if (this.message == null) {
			if (other.message != null)
				return false;
		} else if (!this.message.equals (other.message))
			return false;
		if (this.loggerName == null) {
			if (other.loggerName != null)
				return false;
		} else if (!this.loggerName.equals (other.loggerName))
			return false;
		if (this.threadName == null) {
			if (other.threadName != null)
				return false;
		} else if (!this.threadName.equals (other.threadName))
			return false;
		if (this.timeStamp != other.timeStamp)
			return false;
		if (this.marker == null) {
			if (other.marker != null)
				return false;
		} else if (!this.marker.equals (other.marker))
			return false;
		if (this.mdcPropertyMap == null) {
			if (other.mdcPropertyMap != null)
				return false;
		} else if (!this.mdcPropertyMap.equals (other.mdcPropertyMap))
			return false;
		return true;
	}
	
	@Override
	public Object[] getArgumentArray ()
	{
		return this.argumentArray;
	}
	
	@Override
	public StackTraceElement[] getCallerData ()
	{
		return this.callerDataArray;
	}
	
	public long getContextBirthTime ()
	{
		return this.loggerContextVO.getBirthTime ();
	}
	
	public LoggerContextVO getContextLoggerRemoteView ()
	{
		return this.loggerContextVO;
	}
	
	@Override
	public String getFormattedMessage ()
	{
		if (this.formattedMessage != null) {
			return this.formattedMessage;
		}
		if (this.argumentArray != null) {
			this.formattedMessage = MessageFormatter.arrayFormat (this.message, this.argumentArray).getMessage ();
		} else {
			this.formattedMessage = this.message;
		}
		return this.formattedMessage;
	}
	
	@Override
	public Level getLevel ()
	{
		return this.level;
	}
	
	@Override
	public LoggerContextVO getLoggerContextVO ()
	{
		return this.loggerContextVO;
	}
	
	@Override
	public String getLoggerName ()
	{
		return this.loggerName;
	}
	
	@Override
	public Marker getMarker ()
	{
		return this.marker;
	}
	
	@Override
	public Map<String, String> getMdc ()
	{
		return this.mdcPropertyMap;
	}
	
	@Override
	public Map<String, String> getMDCPropertyMap ()
	{
		return this.mdcPropertyMap;
	}
	
	@Override
	public String getMessage ()
	{
		return this.message;
	}
	
	@Override
	public String getThreadName ()
	{
		return this.threadName;
	}
	
	@Override
	public IThrowableProxy getThrowableProxy ()
	{
		return this.throwableProxy;
	}
	
	@Override
	public long getTimeStamp ()
	{
		return this.timeStamp;
	}
	
	@Override
	public boolean hasCallerData ()
	{
		return this.callerDataArray != null;
	}
	
	@Override
	public int hashCode ()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.message == null) ? 0 : this.message.hashCode ());
		result = (prime * result) + ((this.threadName == null) ? 0 : this.threadName.hashCode ());
		result = (prime * result) + (int) (this.timeStamp ^ (this.timeStamp >>> 32));
		return result;
	}
	
	@Override
	public void prepareForDeferredProcessing ()
	{}
	
	@Override
	public String toString ()
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append (this.timeStamp);
		sb.append (" ");
		sb.append (this.level);
		sb.append (" [");
		sb.append (this.threadName);
		sb.append ("] ");
		sb.append (this.loggerName);
		sb.append (" - ");
		sb.append (this.getFormattedMessage ());
		return sb.toString ();
	}
	
	private void readObject (final ObjectInputStream in)
			throws IOException,
				ClassNotFoundException
	{
		in.defaultReadObject ();
		final int levelInt = in.readInt ();
		this.level = Level.toLevel (levelInt);
		final int argArrayLen = in.readInt ();
		if (argArrayLen != SLoggingEvent1.NULL_ARGUMENT_ARRAY) {
			this.argumentArray = new String[argArrayLen];
			for (int i = 0; i < argArrayLen; i++) {
				final Object val = in.readObject ();
				if (!SLoggingEvent1.NULL_ARGUMENT_ARRAY_ELEMENT.equals (val)) {
					this.argumentArray[i] = val;
				}
			}
		}
	}
	
	private void writeObject (final ObjectOutputStream out)
			throws IOException
	{
		out.defaultWriteObject ();
		out.writeInt (this.level.levelInt);
		if (this.argumentArray != null) {
			final int len = this.argumentArray.length;
			out.writeInt (len);
			for (final Object element : this.argumentArray) {
				if (element != null) {
					out.writeObject (element.toString ());
				} else {
					out.writeObject (SLoggingEvent1.NULL_ARGUMENT_ARRAY_ELEMENT);
				}
			}
		} else {
			out.writeInt (SLoggingEvent1.NULL_ARGUMENT_ARRAY);
		}
	}
	
	public Object[] argumentArray;
	public StackTraceElement[] callerDataArray;
	public transient String key;
	public transient Level level;
	public LoggerContextVO loggerContextVO;
	public String loggerName;
	public Marker marker;
	public Map<String, String> mdcPropertyMap;
	public String message;
	public String threadName;
	public IThrowableProxy throwableProxy;
	public long timeStamp;
	private transient String formattedMessage;
	
	public static SLoggingEvent1 build (final ILoggingEvent original)
	{
		// taken from `LoggingEventVO.build` but constructing a `PubLoggingEventVO`
		final SLoggingEvent1 clone = new SLoggingEvent1 ();
		clone.loggerName = original.getLoggerName ();
		clone.loggerContextVO = original.getLoggerContextVO ();
		clone.threadName = original.getThreadName ();
		clone.level = (original.getLevel ());
		clone.message = (original.getMessage ());
		clone.argumentArray = (original.getArgumentArray ());
		clone.marker = original.getMarker ();
		clone.mdcPropertyMap = original.getMDCPropertyMap ();
		clone.timeStamp = original.getTimeStamp ();
		clone.throwableProxy = ThrowableProxyVO.build (original.getThrowableProxy ());
		if (original.hasCallerData ())
			clone.callerDataArray = original.getCallerData ();
		return (clone);
	}
	
	private static final int NULL_ARGUMENT_ARRAY = -1;
	private static final String NULL_ARGUMENT_ARRAY_ELEMENT = "NULL_ARGUMENT_ARRAY_ELEMENT";
	private static final long serialVersionUID = -3385765861078946218L;
}
