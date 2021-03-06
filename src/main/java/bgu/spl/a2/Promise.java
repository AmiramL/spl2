package bgu.spl.a2;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * this class represents a deferred result i.e., an object that eventually will
 * be resolved to hold a result of some operation, the class allows for getting
 * the result once it is available and registering a callback that will be
 * called once the result is available.
 *
 * Note for implementors: you may add methods and synchronize any of the
 * existing methods in this class *BUT* you must be able to explain why the
 * synchronization is needed. In addition, the methods you add can only be
 * private, protected or package protected - in other words, no new public
 * methods
 *
 * @param <T>
 *            the result type, <boolean> resolved - initialized ;
 */
public class Promise<T>{
	private boolean resolved;
	private T value;
	private BlockingQueue<callback> callbackQ =new LinkedBlockingDeque<callback>();

	/**
	 *
	 * @return the resolved value if such exists (i.e., if this object has been
	 *         {@link #resolve(java.lang.Object)}ed
	 * @throws IllegalStateException
	 *             in the case where this method is called and this object is
	 *             not yet resolved
	 */
	public T get() {//is two threads gonna aproach the same promise? todo
		if (resolved)
			return value;
		else
			throw new IllegalStateException ("Promise not Resolved yet!");
	}

	/**
	 *
	 * @return true if this object has been resolved - i.e., if the method
	 *         {@link #resolve(java.lang.Object)} has been called on this object
	 *         before.
	 */
	public boolean isResolved() {
		return resolved;
	}//no cuncurrency problem here


	/**
	 * resolve this promise object - from now on, any call to the method
	 * {@link #get()} should return the given value
	 *
	 * Any callbacks that were registered to be notified when this object is
	 * resolved via the {@link #subscribe(callback)} method should
	 * be executed before this method returns
	 *
	 * @throws IllegalStateException
	 * 			in the case where this object is already resolved
	 * @param value
	 *            - the value to resolve this promise object with
	 */
	public synchronized void resolve(T value){
		//System.out.println("resolve + Q size is " +callbackQ.size());
		if (!resolved) {//the problem is what if one thread check and then the other check and change it then we need to make sure we get the right value
			resolved=true;
			callback call;
			this.value = value;
			try{
				while (callbackQ!=null&&!callbackQ.isEmpty()){//do all the callbacks
					call = callbackQ.take();
					if(call !=null)
					call.call();
				}
			} catch (Exception e) {
				System.out.println("Promise.resolve - something went wrong - " + e.getCause().toString());
			}

		}
		else
			throw new IllegalStateException ("Promise has been resolved already!");
	}

	/**
	 * add a callback to be called when this object is resolved. If while
	 * calling this method the object is already resolved - the callback should
	 * be called immediately
	 *
	 * Note that in any case, the given callback should never get called more
	 * than once, in addition, in order to avoid memory leaks - once the
	 * callback got called, this object should not hold its reference any
	 * longer.
	 *
	 * @param callback
	 *            the callback to be called when the promise object is resolved
	 */
	public synchronized void subscribe(callback callback) {//will be called only once since we use block queue no need for syncronization
      // System.out.println("subscribe");
        if(isResolved()){
        	callback.call();
        	return;
		}
		if(callbackQ==null&&!isResolved()) callbackQ = new LinkedBlockingDeque<callback>();
		if (callbackQ.isEmpty()) {
			if (isResolved()) {
			//	System.out.println("make callback null (subscribe)");
				if(callbackQ!=null){callbackQ=null;
				//	System.out.println("make callback null (subscribe)");
				}
				callback.call();
			}
			else try {
				callbackQ.put(callback);
			} catch (Exception e) {
				System.out.println("Promise.subscribe - something went wrong - " + e);
			}

		} else {
			callbackQ.add(callback);
		}
	}
}
