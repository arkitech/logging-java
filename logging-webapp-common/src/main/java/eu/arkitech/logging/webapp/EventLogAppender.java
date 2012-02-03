/*
 * #%L
 * arkitech-logging-webapp-common
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
