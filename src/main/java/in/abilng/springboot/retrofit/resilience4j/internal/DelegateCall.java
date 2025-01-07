package in.abilng.springboot.retrofit.resilience4j.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Simple decorator class that implements Call&lt;T&gt; and delegates all calls the the Call
 * instance provided in the constructor.  Methods can be overridden as required.
 *
 * @param <T> Call parameter type
 */
public abstract class DelegateCall<T> implements Call<T> {

    /**
     * The Delegate.
     */
    protected final Call<T> delegate;

    /**
     * Instantiates a new Delegate call.
     *
     * @param delegate the delegate
     */
    public DelegateCall(Call<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response<T> execute() throws IOException {
        return this.delegate.execute();
    }

    @Override
    public void enqueue(Callback<T> callback) {
        delegate.enqueue(callback);
    }

    @Override
    public boolean isExecuted() {
        return delegate.isExecuted();
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }

    @Override
    public boolean isCanceled() {
        return delegate.isCanceled();
    }

    @SuppressWarnings({
        "PMD.ProperCloneImplementation",
        "PMD.CloneMethodReturnTypeMustMatchClassName"
    })
    @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
    @Override
    public abstract Call<T> clone();

    @Override
    public Request request() {
        return delegate.request();
    }

    @Override
    public Timeout timeout() {
        return this.delegate.timeout();
    }
}
