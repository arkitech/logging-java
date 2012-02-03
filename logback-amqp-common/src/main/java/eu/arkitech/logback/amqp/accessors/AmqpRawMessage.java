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


public final class AmqpRawMessage
{
	public AmqpRawMessage (final String exchange, final String routingKey, final String contentType, final String contentEncoding, final byte[] content)
	{
		super ();
		this.exchange = exchange;
		this.routingKey = routingKey;
		this.contentType = contentType;
		this.contentEncoding = contentEncoding;
		this.content = content;
	}
	
	public final byte[] content;
	public final String contentEncoding;
	public final String contentType;
	public final String exchange;
	public final String routingKey;
}
