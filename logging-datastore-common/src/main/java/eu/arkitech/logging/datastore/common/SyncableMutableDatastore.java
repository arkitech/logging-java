
package eu.arkitech.logging.datastore.common;


public interface SyncableMutableDatastore
		extends
			MutableDatastore
{
	public abstract boolean syncWrite ();
}
