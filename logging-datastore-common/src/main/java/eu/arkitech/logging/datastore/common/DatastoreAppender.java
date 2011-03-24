
package eu.arkitech.logging.datastore.common;


public interface DatastoreAppender
{
	public abstract Datastore getDatastore ();
	
	public abstract boolean isStarted ();
	
	public abstract void start ();
	
	public abstract void stop ();
}
