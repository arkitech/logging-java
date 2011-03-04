
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
	
	public boolean add (final ILoggingEvent e)
	{
		return (this.add (e));
	}
	
	public boolean addAll (final Collection<? extends ILoggingEvent> c)
	{
		return (this.queue.addAll (c));
	}
	
	public void clear ()
	{
		this.queue.clear ();
	}
	
	public boolean contains (final Object o)
	{
		return (this.queue.contains (o));
	}
	
	public boolean containsAll (final Collection<?> c)
	{
		return (this.queue.containsAll (c));
	}
	
	public int drainTo (final Collection<? super ILoggingEvent> c)
	{
		return (this.queue.drainTo (c));
	}
	
	public int drainTo (final Collection<? super ILoggingEvent> c, final int maxElements)
	{
		return (this.queue.drainTo (c, maxElements));
	}
	
	public ILoggingEvent element ()
	{
		return (this.queue.element ());
	}
	
	public boolean isEmpty ()
	{
		return (this.queue.isEmpty ());
	}
	
	public Iterator<ILoggingEvent> iterator ()
	{
		return (this.queue.iterator ());
	}
	
	public boolean offer (final ILoggingEvent e)
	{
		return (this.queue.offer (e));
	}
	
	public boolean offer (final ILoggingEvent e, final long timeout, final TimeUnit unit)
			throws InterruptedException
	{
		return (this.queue.offer (e, timeout, unit));
	}
	
	public ILoggingEvent peek ()
	{
		return (this.queue.peek ());
	}
	
	public ILoggingEvent poll ()
	{
		return (this.queue.poll ());
	}
	
	public ILoggingEvent poll (final long timeout, final TimeUnit unit)
			throws InterruptedException
	{
		return (this.queue.poll (timeout, unit));
	}
	
	public void put (final ILoggingEvent e)
			throws InterruptedException
	{
		this.queue.put (e);
	}
	
	public int remainingCapacity ()
	{
		return (this.queue.remainingCapacity ());
	}
	
	public ILoggingEvent remove ()
	{
		return (this.queue.remove ());
	}
	
	public boolean remove (final Object o)
	{
		return (this.queue.remove (o));
	}
	
	public boolean removeAll (final Collection<?> c)
	{
		return (this.queue.removeAll (c));
	}
	
	public boolean retainAll (final Collection<?> c)
	{
		return (this.queue.retainAll (c));
	}
	
	public int size ()
	{
		return (this.queue.size ());
	}
	
	public ILoggingEvent take ()
			throws InterruptedException
	{
		return (this.queue.take ());
	}
	
	public Object[] toArray ()
	{
		return (this.queue.toArray ());
	}
	
	public <T> T[] toArray (final T[] a)
	{
		return (this.queue.toArray (a));
	}
	
	protected void append (final ILoggingEvent event)
	{
		this.queue.add (event);
	}
	
	private final BlockingQueue<ILoggingEvent> queue;
}
