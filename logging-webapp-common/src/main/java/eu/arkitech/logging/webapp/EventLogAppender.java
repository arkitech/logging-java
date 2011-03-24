
package eu.arkitech.logging.webapp;


import java.util.List;

import eu.arkitech.logback.common.AppenderNewInstanceAction;
import eu.arkitech.logback.common.BlockingQueueAppender;


public class EventLogAppender
		extends BlockingQueueAppender
{
	public static final class CreateAction
			extends AppenderNewInstanceAction<EventLogAppender>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoRegister, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<? super EventLogAppender> collector, final boolean autoRegister, final boolean autoStart)
		{
			super (EventLogAppender.class, collector, autoRegister, autoStart);
		}
		
		public static boolean defaultAutoRegister = true;
		public static boolean defaultAutoStart = true;
		public static List<? super EventLogAppender> defaultCollector = null;
	}
}
