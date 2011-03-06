
package eu.arkitech.logback.webapp;


import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.action.AppenderAction;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import eu.arkitech.logback.common.BlockingQueueAppender;
import eu.arkitech.logback.common.ClassNewInstanceAction;
import org.xml.sax.Attributes;


public class EventLogAppender
		extends BlockingQueueAppender
{
	public static final class CreateAction
			extends ClassNewInstanceAction<EventLogAppender>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoRegister, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<EventLogAppender> collector, final boolean autoRegister, final boolean autoStart)
		{
			super (EventLogAppender.class, collector, autoRegister, autoStart);
			this.delegate = new AppenderAction<ILoggingEvent> ();
		}
		
		@Override
		public void begin (final InterpretationContext context, final String name, final Attributes attributes)
				throws ActionException
		{
			super.begin (context, name, attributes);
			this.delegate.begin (context, name, attributes);
		}
		
		@Override
		public void end (final InterpretationContext context, final String name)
		{
			super.end (context, name);
			this.delegate.end (context, name);
		}
		
		protected final AppenderAction<ILoggingEvent> delegate;
		
		public static boolean defaultAutoRegister = true;
		public static boolean defaultAutoStart = true;
		public static List<EventLogAppender> defaultCollector = null;
	}
}
