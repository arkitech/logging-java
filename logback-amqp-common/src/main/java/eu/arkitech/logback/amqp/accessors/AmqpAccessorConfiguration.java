/*
 * #%L
 * arkitech-logback-amqp-common
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

package eu.arkitech.logback.amqp.accessors;


import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;


public interface AmqpAccessorConfiguration
{
	public abstract LoggingEventMutator getMutator ();
	
	public abstract Serializer getSerializer ();
	
	public static final LoggingEventMutator defaultMutator = null;
	public static final Serializer defaultSerializer = new DefaultBinarySerializer ();
}
