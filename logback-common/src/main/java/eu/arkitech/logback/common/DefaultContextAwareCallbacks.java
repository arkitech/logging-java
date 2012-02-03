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


import ch.qos.logback.classic.Level;
import ch.qos.logback.core.spi.ContextAware;


public class DefaultContextAwareCallbacks
		implements
			Callbacks
{
	public DefaultContextAwareCallbacks (final ContextAware delegate)
	{
		super ();
		this.delegate = delegate;
	}
	
	@Override
	public void handleException (final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		this.handleLogEvent (Level.ERROR, exception, messageFormat, messageArguments);
	}
	
	@Override
	public void handleLogEvent (final Level level, final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		final String message = String.format (messageFormat, messageArguments);
		switch (level.levelInt) {
			case Level.ERROR_INT :
				if (exception != null)
					this.delegate.addError (message, exception);
				else
					this.delegate.addError (message);
				break;
			case Level.WARN_INT :
				if (exception != null)
					this.delegate.addWarn (message, exception);
				else
					this.delegate.addWarn (message);
				break;
			case Level.INFO_INT :
				if (exception != null)
					this.delegate.addInfo (message, exception);
				else
					this.delegate.addInfo (message);
				break;
			default:
				if (exception != null)
					this.delegate.addInfo (message, exception);
				else
					this.delegate.addInfo (message);
				break;
		}
	}
	
	protected ContextAware delegate;
}
