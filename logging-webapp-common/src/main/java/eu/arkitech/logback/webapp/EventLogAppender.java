
package eu.arkitech.logback.webapp;


import eu.arkitech.logback.common.BlockingQueueAppender;


public class EventLogAppender
		extends BlockingQueueAppender
{
	@Override
	public void start ()
	{
		if (this.isStarted ())
			return;
		super.start ();
		if (this.context.getObject (this.name) != null)
			throw (new IllegalArgumentException (String.format ("duplicate object name found `%s`", this.name)));
		this.context.putObject (this.name, this);
	}
	
	@Override
	public void stop ()
	{
		if (!this.isStarted ())
			return;
		this.context.putObject (this.name, null);
		super.stop ();
	}
}
