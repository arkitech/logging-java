
package eu.arkitech.logback.common;


import java.util.HashMap;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.action.AppenderAction;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import org.xml.sax.Attributes;


public abstract class AppenderNewInstanceAction<_Appender_ extends Appender<ILoggingEvent>>
		extends ObjectNewInstanceAction<_Appender_>
{
	public AppenderNewInstanceAction (final Class<? extends _Appender_> objectClass, final List<_Appender_> collector, final boolean autoRegister, final boolean autoStart)
	{
		super (objectClass, collector, autoRegister, autoStart);
		this.delegate = new AppenderAction<ILoggingEvent> ();
	}
	
	@Override
	public void begin (final InterpretationContext context, final String localName, final Attributes attributes)
			throws ActionException
	{
		super.begin (context, localName, attributes);
		if (this.objectGlobalName != null) {
			this.object.setName (this.objectGlobalName);
			@SuppressWarnings ("unchecked") final HashMap<String, Appender<?>> appenders = (HashMap<String, Appender<?>>) context.getObjectMap ().get (ActionConst.APPENDER_BAG);
			appenders.put (this.objectGlobalName, this.object);
		}
	}
	
	protected final AppenderAction<ILoggingEvent> delegate;
}
