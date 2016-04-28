package rapture.kernel.pipeline;

import rapture.common.model.DocumentWithMeta;
import rapture.kernel.ContextFactory;
import rapture.object.storage.ObjectStorageSearchable;
/**
 * A helper class that understands the ins and outs of ObjectStorage objects, and has the correct defaults before sending to
 * Search Publisher
 * @author alanmoore
 *
 */
public class ObjectStorageSearchPublisher {
	private static ObjectStorageSearchable searchable = new ObjectStorageSearchable();
	
	public static void publishCreateMessage(DocumentWithMeta doc) {
		SearchPublisher.publishCreateMessage(ContextFactory.getAnonymousUser(), searchable, doc);
	}
}