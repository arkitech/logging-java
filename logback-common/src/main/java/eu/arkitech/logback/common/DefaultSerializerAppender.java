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


public abstract class DefaultSerializerAppender
		extends DefaultAppender
{
	public LoggingEventMutator getMutator ()
	{
		return (this.mutator);
	}
	
	public Serializer getSerializer ()
	{
		return (this.serializer);
	}
	
	public void setMutator (final LoggingEventMutator mutator)
	{
		this.mutator = mutator;
	}
	
	public void setSerializer (final Serializer serializer)
	{
		this.serializer = serializer;
	}
	
	protected LoggingEventMutator mutator;
	protected Serializer serializer;
}
