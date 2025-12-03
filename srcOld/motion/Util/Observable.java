package motion.Util;

/**
 * Author: Valerie Barker
 * Observable interface allows objects to be observed by other objects
 */
public interface Observable {

    /**
     * Subscribes an observer to this object. Subscribed objects are added to
     * observer list. When updating information you wish to be observed, call
     * for (Observer o : observers) update(this);
     * @param subscriber an Observer that wishes to observe this object
     */
    public void subscribe(Observer subscriber);
}