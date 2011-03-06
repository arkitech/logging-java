
package eu.arkitech.logging.datastore.common;


public interface SyncableImmutableDatastore
		extends
			ImmutableDatastore
{
	public abstract boolean syncRead ();
}
