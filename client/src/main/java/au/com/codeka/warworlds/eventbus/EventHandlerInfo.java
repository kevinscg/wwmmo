package au.com.codeka.warworlds.eventbus;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import android.os.Handler;
import android.os.Looper;
import au.com.codeka.common.Log;

/** Holds all the details about a single event handler. */
class EventHandlerInfo {
    private static final Log log = new Log("EventHandlerInfo");

    private final Class<?> mEventClass;
    private final Method mMethod;
    private final WeakReference<?> mSubscriber;
    private final boolean mCallOnUiThread;
    private final Handler mHandler;
    private int registerCount;

    public EventHandlerInfo(Class<?> eventClass, Method method, Object subscriber,
            boolean callOnUiThread) {
        mEventClass = eventClass;
        mMethod = method;
        mSubscriber = new WeakReference<Object>(subscriber);
        mCallOnUiThread = callOnUiThread;
        if (mCallOnUiThread) {
            mHandler = new Handler(Looper.getMainLooper());
        } else {
            mHandler = null;
        }
        registerCount = 1;
    }

    public boolean handles(Object event) {
        return mEventClass.isInstance(event);
    }

    public int register() {
        return ++registerCount;
    }

    public int unregister() {
        return --registerCount;
    }

    /** Gets the subscriber object, may be null. */
    public Object getSubscriber() {
        return mSubscriber.get();
    }

    /** Calls the subscriber's method with the given event object, on the UI thread if needed. */
    public void call(final Object event) {
        final Exception callLocation = new Exception("Location of EventHandlerInfo.call()");
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final Object subscriber = mSubscriber.get();
                if (subscriber == null) {
                    return;
                }
                try {
                    mMethod.invoke(subscriber, event);
                } catch (Exception e) {
                    log.error("Exception caught handling event.", e);
                    log.error("Call location.", callLocation);
                }
            }
        };

        // if we're scheduled to run on the UI thread, and we're *not* on the UI thread, then
        // post a message to run on the UI thread. If we're alread on the UI then don't go through
        // the trouble of posting a message.
        if (mHandler != null && Looper.myLooper() != Looper.getMainLooper()) {
            mHandler.post(runnable);
        } else {
            runnable.run();
        }
    }
}
