
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
	
	public void handleException (final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		this.handleLogEvent (Level.ERROR, exception, messageFormat, messageArguments);
	}
	
	public void handleLogEvent (
			final Level level, final Throwable exception, final String messageFormat, final Object ... messageArguments)
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
