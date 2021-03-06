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


import java.util.List;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.OptionHelper;
import com.google.common.base.Preconditions;
import org.xml.sax.Attributes;


public class ObjectNewInstanceAction<_Object_ extends ContextAware>
		extends Action
{
	public ObjectNewInstanceAction (final Class<? extends _Object_> objectClass)
	{
		this (objectClass, null, true, true);
	}
	
	public ObjectNewInstanceAction (final Class<? extends _Object_> objectClass, final List<? super _Object_> collector, final boolean autoRegister, final boolean autoStart)
	{
		super ();
		this.objectClass = Preconditions.checkNotNull (objectClass);
		this.collector = collector;
		this.autoRegister = autoRegister;
		this.autoStart = autoStart;
		this.object = null;
		this.objectGlobalName = null;
	}
	
	@Override
	public void begin (final InterpretationContext context, final String localName, final Attributes attributes)
			throws ActionException
	{
		Preconditions.checkState (this.object == null);
		try {
			this.object = this.objectClass.newInstance ();
		} catch (final Exception exception) {
			throw (new ActionException (exception));
		}
		this.object.setContext (this.getContext ());
		final String globalName = context.subst (attributes.getValue (Action.NAME_ATTRIBUTE));
		if (!OptionHelper.isEmpty (globalName))
			this.objectGlobalName = globalName;
		else
			this.objectGlobalName = null;
		this.objectLocalName = localName;
		context.pushObject (this.object);
	}
	
	@Override
	public void end (final InterpretationContext context, final String localName)
	{
		Preconditions.checkState (this.object != null);
		Preconditions.checkState (context.popObject () == this.object);
		this.postProcessObject ();
		this.object = null;
		this.objectGlobalName = null;
		this.objectLocalName = null;
	}
	
	protected void postProcessObject ()
	{
		if (this.autoRegister)
			this.registerObject ();
		if (this.autoStart)
			this.startObject ();
		if (this.collector != null)
			this.collector.add (this.object);
	}
	
	protected void registerObject ()
	{
		if (this.objectGlobalName != null)
			this.object.getContext ().putObject (this.objectGlobalName, this.object);
	}
	
	protected void startObject ()
	{
		if (this.object instanceof LifeCycle)
			((LifeCycle) this.object).start ();
	}
	
	protected final boolean autoRegister;
	protected final boolean autoStart;
	protected final List<? super _Object_> collector;
	protected _Object_ object;
	protected final Class<? extends _Object_> objectClass;
	protected String objectGlobalName;
	protected String objectLocalName;
}
