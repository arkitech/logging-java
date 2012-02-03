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


import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;


public class BlockingQueueAppender
		extends UnsynchronizedAppenderBase<ILoggingEvent>
		implements
			BlockingQueue<ILoggingEvent>
{
	public BlockingQueueAppender ()
	{
		this (new LinkedBlockingQueue<ILoggingEvent> ());
	}
	
	public BlockingQueueAppender (final BlockingQueue<ILoggingEvent> queue)
	{
		this.queue = queue;
	}
	
	@Override
	public boolean add (final ILoggingEvent e)
	{
		return (this.add (e));
	}
	
	@Override
	public boolean addAll (final Collection<? extends ILoggingEvent> c)
	{
		return (this.queue.addAll (c));
	}
	
	@Override
	public void clear ()
	{
		this.queue.clear ();
	}
	
	@Override
	public boolean contains (final Object o)
	{
		return (this.queue.contains (o));
	}
	
	@Override
	public boolean containsAll (final Collection<?> c)
	{
		return (this.queue.containsAll (c));
	}
	
	@Override
	public int drainTo (final Collection<? super ILoggingEvent> c)
	{
		return (this.queue.drainTo (c));
	}
	
	@Override
	public int drainTo (final Collection<? super ILoggingEvent> c, final int maxElements)
	{
		return (this.queue.drainTo (c, maxElements));
	}
	
	@Override
	public ILoggingEvent element ()
	{
		return (this.queue.element ());
	}
	
	@Override
	public boolean isEmpty ()
	{
		return (this.queue.isEmpty ());
	}
	
	@Override
	public Iterator<ILoggingEvent> iterator ()
	{
		return (this.queue.iterator ());
	}
	
	@Override
	public boolean offer (final ILoggingEvent e)
	{
		return (this.queue.offer (e));
	}
	
	@Override
	public boolean offer (final ILoggingEvent e, final long timeout, final TimeUnit unit)
			throws InterruptedException
	{
		return (this.queue.offer (e, timeout, unit));
	}
	
	@Override
	public ILoggingEvent peek ()
	{
		return (this.queue.peek ());
	}
	
	@Override
	public ILoggingEvent poll ()
	{
		return (this.queue.poll ());
	}
	
	@Override
	public ILoggingEvent poll (final long timeout, final TimeUnit unit)
			throws InterruptedException
	{
		return (this.queue.poll (timeout, unit));
	}
	
	@Override
	public void put (final ILoggingEvent e)
			throws InterruptedException
	{
		this.queue.put (e);
	}
	
	@Override
	public int remainingCapacity ()
	{
		return (this.queue.remainingCapacity ());
	}
	
	@Override
	public ILoggingEvent remove ()
	{
		return (this.queue.remove ());
	}
	
	@Override
	public boolean remove (final Object o)
	{
		return (this.queue.remove (o));
	}
	
	@Override
	public boolean removeAll (final Collection<?> c)
	{
		return (this.queue.removeAll (c));
	}
	
	@Override
	public boolean retainAll (final Collection<?> c)
	{
		return (this.queue.retainAll (c));
	}
	
	@Override
	public int size ()
	{
		return (this.queue.size ());
	}
	
	@Override
	public ILoggingEvent take ()
			throws InterruptedException
	{
		return (this.queue.take ());
	}
	
	@Override
	public Object[] toArray ()
	{
		return (this.queue.toArray ());
	}
	
	@Override
	public <T> T[] toArray (final T[] a)
	{
		return (this.queue.toArray (a));
	}
	
	@Override
	protected void append (final ILoggingEvent event)
	{
		this.queue.add (event);
	}
	
	private final BlockingQueue<ILoggingEvent> queue;
}
