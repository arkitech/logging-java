
package ro.volution.dev.logback.webapp;


import ro.volution.dev.logback.common.BufferedAppender;


public class EventLogAppender
		extends BufferedAppender
{
	public void start ()
	{
		if (this.isStarted ())
			return;
		super.start ();
		if (this.context.getObject (this.name) != null)
			throw (new IllegalArgumentException (String.format ("duplicate object name found `%s`", this.name)));
		this.context.putObject (this.name, this);
	}
	
	public void stop ()
	{
		if (!this.isStarted ())
			return;
		this.context.putObject (this.name, null);
		super.stop ();
	}
}
