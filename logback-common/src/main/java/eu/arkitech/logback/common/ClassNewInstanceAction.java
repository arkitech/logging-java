
package eu.arkitech.logback.common;


import java.util.List;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import org.xml.sax.Attributes;


public class ClassNewInstanceAction<_Object_ extends ContextAware>
		extends Action
{
	public ClassNewInstanceAction (final Class<? extends _Object_> objectClass)
	{
		this (objectClass, null, true);
	}
	
	public ClassNewInstanceAction (
			final Class<? extends _Object_> objectClass, final List<_Object_> collector, final boolean autoStart)
	{
		super ();
		this.objectClass = objectClass;
		this.collector = collector;
		this.autoStart = autoStart;
		this.object = null;
	}
	
	public void begin (final InterpretationContext context, final String name, final Attributes attributes)
			throws ActionException
	{
		if (this.object != null)
			throw (new IllegalStateException ());
		try {
			this.object = this.objectClass.newInstance ();
		} catch (final Exception exception) {
			throw (new ActionException (exception));
		}
		this.object.setContext (this.getContext ());
		context.pushObject (this.object);
	}
	
	public void end (final InterpretationContext context, final String name)
	{
		if (this.object == null)
			throw (new IllegalStateException ());
		if (context.popObject () != this.object)
			throw (new IllegalStateException ());
		if (this.autoStart)
			this.startObject ();
		if (this.collector != null)
			this.collector.add (this.object);
		this.object = null;
	}
	
	protected void startObject ()
	{
		if (this.object instanceof LifeCycle)
			((LifeCycle) this.object).start ();
	}
	
	protected final boolean autoStart;
	protected final List<_Object_> collector;
	protected _Object_ object;
	protected final Class<? extends _Object_> objectClass;
}
