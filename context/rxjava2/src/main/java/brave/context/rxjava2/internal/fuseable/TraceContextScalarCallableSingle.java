package brave.context.rxjava2.internal.fuseable;

import brave.context.rxjava2.Internal;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;
import brave.propagation.TraceContext;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.internal.fuseable.ScalarCallable;

final class TraceContextScalarCallableSingle<T> extends Single<T> implements ScalarCallable<T> {
  final SingleSource<T> source;
  final CurrentTraceContext contextScoper;
  final TraceContext assembled;

  TraceContextScalarCallableSingle(
      SingleSource<T> source, CurrentTraceContext contextScoper, TraceContext assembled) {
    this.source = source;
    this.contextScoper = contextScoper;
    this.assembled = assembled;
  }

  @Override protected void subscribeActual(SingleObserver<? super T> s) {
    Scope scope = contextScoper.maybeScope(assembled);
    try { // retrolambda can't resolve this try/finally
      source.subscribe(Internal.instance.wrap(s, contextScoper, assembled));
    } finally {
      scope.close();
    }
  }

  /**
   * A scalar value is computed at assembled time. Since call() is at runtime, we shouldn't add
   * overhead of scoping, only to return a constant!
   *
   * <p>See https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0#callable-and-scalarcallable
   */
  @Override @SuppressWarnings("unchecked") public T call() {
    return ((ScalarCallable<T>) source).call();
  }
}