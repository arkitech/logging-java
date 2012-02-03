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


import com.google.common.base.Objects;


public class WorkerConfiguration
		implements
			Configuration
{
	public WorkerConfiguration ()
	{
		this ((Callbacks) null, (Object) null);
	}
	
	public WorkerConfiguration (final Callbacks callbacks, final Object monitor)
	{
		super ();
		this.callbacks = callbacks;
		this.monitor = monitor;
	}
	
	public WorkerConfiguration (final WorkerConfiguration override, final WorkerConfiguration overriden)
	{
		super ();
		this.callbacks = Objects.firstNonNull (override.callbacks, overriden.callbacks);
		this.monitor = Objects.firstNonNull (override.callbacks, overriden.callbacks);
	}
	
	public final Callbacks callbacks;
	public final Object monitor;
	public static final int defaultWaitTimeout = 1000;
}
