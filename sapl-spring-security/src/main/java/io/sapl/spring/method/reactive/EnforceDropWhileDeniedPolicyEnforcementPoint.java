package io.sapl.spring.method.reactive;

import static java.util.function.Predicate.not;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;
import org.springframework.security.access.AccessDeniedException;

import io.sapl.api.pdp.AuthorizationDecision;
import io.sapl.api.pdp.Decision;
import io.sapl.spring.constraints.ReactiveConstraintEnforcementService;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

/**
 * The EnforceDropWhileDeniedPolicyEnforcementPoint implements continuous policy
 * enforcement on a Flux resource access point.
 * 
 * After an initial PERMIT, the PEP subscribes to the resource access point and
 * forwards events downstream until a non-PERMIT decision from the PDP is
 * received. Then, all events are dropped until a new PERMIT signal arrives.
 * 
 * Whenever a decision is received, the handling of obligations and advice are
 * updated accordingly.
 * 
 * The PEP does not permit onErrorContinue() downstream.
 */
public class EnforceDropWhileDeniedPolicyEnforcementPoint extends Flux<Object> {

	private Flux<AuthorizationDecision> decisions;
	private Flux<?> resourceAccessPoint;
	private ReactiveConstraintEnforcementService constraintsService;
	EnforcementSink sink;

	AtomicReference<Disposable> decisionsSubscription = new AtomicReference<Disposable>();
	AtomicReference<Disposable> dataSubscription = new AtomicReference<Disposable>();
	AtomicReference<AuthorizationDecision> latestDecision = new AtomicReference<AuthorizationDecision>();
	AtomicBoolean stopped = new AtomicBoolean(false);

	private EnforceDropWhileDeniedPolicyEnforcementPoint(Flux<AuthorizationDecision> decisions,
			Flux<?> resourceAccessPoint, ReactiveConstraintEnforcementService constraintsService) {
		this.decisions = decisions;
		this.resourceAccessPoint = resourceAccessPoint;
		this.constraintsService = constraintsService;
	}

	public static Flux<Object> of(Flux<AuthorizationDecision> decisions, Flux<?> resourceAccessPoint,
			ReactiveConstraintEnforcementService constraintsService) {
		var pep = new EnforceDropWhileDeniedPolicyEnforcementPoint(decisions, resourceAccessPoint, constraintsService);
		return pep.doOnTerminate(pep::handleOnTerminate).doAfterTerminate(pep::handleAfterTerminate)
				.onErrorMap(AccessDeniedException.class, pep::handleAccessDenied).doOnCancel(pep::handleCancel)
				.onErrorStop();
	}

	@Override
	public void subscribe(CoreSubscriber<? super Object> actual) {
		if (sink != null)
			throw new IllegalStateException("Operator may only be subscribed once.");
		ContextView context = actual.currentContext();
		sink = new EnforcementSink();
		resourceAccessPoint = resourceAccessPoint.contextWrite(context);
		Flux.create(sink).subscribe(actual);
		decisionsSubscription.set(decisions.doOnNext(this::handleNextDecision).contextWrite(context).subscribe());
	}

	private void handleNextDecision(AuthorizationDecision decision) {
		var previousDecision = latestDecision.getAndSet(decision);

		if (previousDecision == null)
			dataSubscription.set(wrapResourceAccessPointAndSubcribe());
	}

	private Disposable wrapResourceAccessPointAndSubcribe() {
		return resourceAccessPoint.doOnError(this::handleError).doOnRequest(this::handleRequest)
				.doOnSubscribe(this::handleSubscribe).doOnNext(this::handleNext).doOnComplete(this::handleComplete)
				.subscribe();
	}

	private void handleSubscribe(Subscription s) {
		try {
			constraintsService.handleOnSubscribeConstraints(latestDecision.get(), s);
		} catch (Throwable t) {
			handleNextDecision(AuthorizationDecision.DENY);
		}
	}

	private void handleNext(Object value) {
		// the following guard clause makes sure that the constraint handlers do not get
		// called after downstream consumers cancelled. If the RAP is not consisting of
		// delayed elements, but something like Flux.just(1,2,3) the handler would be
		// called for 2 and 3, even if there was a take(1) applied downstream.
		if (stopped.get())
			return;

		var decision = latestDecision.get();

		if (decision.getDecision() != Decision.PERMIT)
			return;

		try {
			var transformedValue = constraintsService.handleOnNextConstraints(decision, value);
			if (transformedValue != null)
				sink.next(transformedValue);
		} catch (Throwable t) {
			handleNextDecision(AuthorizationDecision.DENY);
		}
	}

	private void handleRequest(Long value) {
		try {
			constraintsService.handleOnRequestConstraints(latestDecision.get(), value);
		} catch (Throwable t) {
			handleNextDecision(AuthorizationDecision.DENY);
		}
	}

	private void handleOnTerminate() {
		constraintsService.handleOnTerminateConstraints(latestDecision.get());
	}

	private void handleAfterTerminate() {
		constraintsService.handleAfterTerminateConstraints(latestDecision.get());
	}

	private void handleComplete() {
		if (stopped.get())
			return;
		try {
			constraintsService.handleOnCompleteConstraints(latestDecision.get());
			sink.complete();
		} catch (Throwable t) {
			sink.complete();
		}
		disposeDecisionsAndResourceAccessPoint();
	}

	private void handleCancel() {
		try {
			constraintsService.handleOnCancelConstraints(latestDecision.get());
		} catch (Throwable t) {
			// NOOP
		}
		disposeDecisionsAndResourceAccessPoint();
	}

	private void handleError(Throwable error) {
		try {
			sink.error(constraintsService.handleOnErrorConstraints(latestDecision.get(), error));
		} catch (Throwable t) {
			sink.error(t);
			handleNextDecision(AuthorizationDecision.DENY);
			disposeDecisionsAndResourceAccessPoint();
		}
	}

	private Throwable handleAccessDenied(Throwable error) {
		try {
			return constraintsService.handleOnErrorConstraints(latestDecision.get(), error);
		} catch (Throwable t) {
			return error;
		}
	}

	private void disposeDecisionsAndResourceAccessPoint() {
		stopped.set(true);
		disposeUndisposedIfPresent(decisionsSubscription);
		disposeUndisposedIfPresent(dataSubscription);
	}

	private void disposeUndisposedIfPresent(AtomicReference<Disposable> atomicDisposable) {
		Optional.ofNullable(atomicDisposable.get()).filter(not(Disposable::isDisposed)).ifPresent(Disposable::dispose);
	}

}