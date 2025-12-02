package motion.Util;

/**
 * Author: Valerie Barker
 * Observer interface allows objects to observe other objects
 * In order to observe an observable object, call:
 * observable_object.subscribe(this);
 */
public interface Observer {
    /**
     * Called by observable object to notify our observer object
     * of updated information.
     * @param viewee the updated observable object
     */
    public abstract void update(Observable viewee);

}