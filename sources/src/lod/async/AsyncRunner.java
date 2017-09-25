package lod.async;

import sun.reflect.Reflection;

import com.rapidminer.operator.OperatorException;

/**
 * Class enables asynchronous execution of custom method by its name (by means
 * of {@link Reflection} ). It could be used to extend existing runners adding
 * asynchronous functionality. Furthermore, this class can be called directly to
 * parallelize custom operations. The class also has a mechanism to control
 * concurrent execution and stop it when necessary.
 * 
 * @author Evgeny MItichkin
 * 
 */
public class AsyncRunner {

	public static final String DEFAULT_ERROR_MESSAGE = "Connection Problems!";

	/**
	 * Background worker thread (Created and handeled by AsyncRunner)
	 */
	protected AsyncRunnerThread mAsyncRunnerThread;

	/**
	 * Determined whether the caller thread is running
	 */
	public boolean mUIThreadRunning = true;

	/**
	 * Starts asynchronous operation
	 */
	public void startAsyncRunner() {
		mAsyncRunnerThread.start();
	}

	/**
	 * Returns the result of asynchronous operation as {@link Object}
	 * 
	 * @return {@link Object} result.
	 */
	public Object getAsyncOperationResult() {
		return mAsyncRunnerThread.getOperationResult();// mAsyncRunner.getOperationResult();
	}

	/**
	 * Sets the value of asynchronous operation in the {@link AsyncRunnerThread}
	 * object (internally) to null. This method should be called after consuming
	 * the result of asynchronous operation in order to prevent confusions in
	 * further operations (Makes sense in case of multiple calls of the same
	 * method in async mode).
	 */
	public void setAsyncOperationResultNull() {
		mAsyncRunnerThread.setOperationResultNull();
	}

	/**
	 * The method is responsible for thread synchronization. It should be called
	 * after {@link AsyncRunner.startAsyncRunner()} Handles emergency
	 * interruption of the thread.
	 * 
	 * @throws OperatorException
	 */
	public void enableWaiter() throws OperatorException {
		synchronized (mAsyncRunnerThread) {
			try {
				boolean threadIsRunning = true;
				while (threadIsRunning) {
					if (mAsyncRunnerThread.isException()) {
						String excMsg = mAsyncRunnerThread
								.getexceptionMessage();
						threadIsRunning = false;
						finalizeAsyncThread();
						excMsg = (excMsg == null || excMsg.equals("")) ? DEFAULT_ERROR_MESSAGE
								: excMsg;
						throw new OperatorException(excMsg);
					}
					if (mAsyncRunnerThread.isRunnerExecuting()) {
						if (!mUIThreadRunning) {
							finalizeAsyncThread();
							break;
						}
						Thread.sleep(1);
					} else {
						threadIsRunning = false;
						finalizeAsyncThread();
					}
				}
			} catch (InterruptedException e) {
				mUIThreadRunning = false;
				finalizeAsyncThread();
			}
		}
	}

	/**
	 * Forces the async runner to stop executing the method and put the result,
	 * as it is at the moment, to
	 * {@link lod.async.AsyncRunnerThread.operationResult} variable.
	 */
	public synchronized void finalizeAsyncThread() {
		boolean success = false;
		if (mAsyncRunnerThread != null) {
			try {
				while (!success) {
					mAsyncRunnerThread.interrupt();
					success = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the asynchronous thread itself ({@link AsyncRunnerThread}
	 * object). Warning! Use it carefully only in case you wish to create your
	 * own thread handler.
	 * 
	 * @return {@link AsyncRunnerThread} thread that performs asynchronous
	 *         operation.
	 */
	public synchronized Thread getAsyncThread() {
		return mAsyncRunnerThread;
	}
}
