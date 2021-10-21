package io.sapl.spring.constraints;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

import org.reactivestreams.Subscription;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ConstraintHandlerBundle<T> {

	List<Runnable> onDecisionHandlers = new LinkedList<>();
	List<Runnable> onCancelHandlers = new LinkedList<>();
	List<Runnable> onCompleteHandlers = new LinkedList<>();
	List<Runnable> onTerminateHandlers = new LinkedList<>();
	List<Runnable> afterTerminateHandlers = new LinkedList<>();
	List<Consumer<Subscription>> onSubscribeHandlers = new LinkedList<>();
	List<LongConsumer> onRequestHandlers = new LinkedList<>();
	List<Consumer<T>> doOnNextHandlers = new LinkedList<>();
	List<Function<T, T>> onNextMapHandlers = new LinkedList<>();
	List<Consumer<Throwable>> doOnErrorHandlers = new LinkedList<>();
	List<Function<Throwable, Throwable>> onErrorMapHandlers = new LinkedList<>();

	public void handleOnSubscribeConstraints(Subscription s) {
		consumeAll(onSubscribeHandlers).accept(s);
	}

	public T handleAllOnNextConstraints(T value) {
		handleOnNextConstraints(value);
		return handleOnNextMapConstraints(value);
	}

	private T handleOnNextMapConstraints(T value) {
		return mapAll(onNextMapHandlers).apply(value);
	}

	public void handleOnNextConstraints(T value) {
		consumeAll(doOnNextHandlers).accept(value);
	}

	public void handleOnRequestConstraints(Long value) {
		consumeAllLong(onRequestHandlers).accept(value);
	}

	public void handleOnCompleteConstraints() {
		runAll(onCompleteHandlers).run();
	}

	public void handleOnTerminateConstraints() {
		runAll(onTerminateHandlers).run();
	}

	public void handleOnDecisionConstraints() {
		runAll(onDecisionHandlers).run();
	}

	public void handleAfterTerminateConstraints() {
		runAll(afterTerminateHandlers).run();
	}

	public void handleOnCancelConstraints() {
		runAll(onCancelHandlers).run();
	}

	public Throwable handleAllOnErrorConstraints(Throwable error) {
		handleOnErrorConstraints(error);
		return handleOnErrorMapConstraints(error);
	}

	private Throwable handleOnErrorMapConstraints(Throwable error) {
		return mapAll(onErrorMapHandlers).apply(error);
	}

	private void handleOnErrorConstraints(Throwable error) {
		consumeAll(doOnErrorHandlers).accept(error);
	}

	public Flux<T> wrap(Flux<T> resourceAccesspoint) {
		var wrapped = resourceAccesspoint;

		if (!onRequestHandlers.isEmpty())
			wrapped = wrapped.doOnRequest(this::handleOnRequestConstraints);

		if (!onSubscribeHandlers.isEmpty())
			wrapped = wrapped.doOnSubscribe(this::handleOnSubscribeConstraints);

		if (!onErrorMapHandlers.isEmpty())
			wrapped = wrapped.onErrorMap(this::handleOnErrorMapConstraints);

		if (!doOnErrorHandlers.isEmpty())
			wrapped = wrapped.doOnError(this::handleOnErrorConstraints);

		if (!onNextMapHandlers.isEmpty())
			wrapped = wrapped.map(this::handleOnNextMapConstraints);

		if (!doOnNextHandlers.isEmpty())
			wrapped = wrapped.doOnNext(this::handleOnNextConstraints);

		if (!onCancelHandlers.isEmpty())
			wrapped = wrapped.doOnCancel(this::handleOnCancelConstraints);

		if (!onCompleteHandlers.isEmpty())
			wrapped = wrapped.doOnComplete(this::handleOnCompleteConstraints);

		if (!onTerminateHandlers.isEmpty())
			wrapped = wrapped.doOnTerminate(this::handleOnTerminateConstraints);

		if (!afterTerminateHandlers.isEmpty())
			wrapped = wrapped.doAfterTerminate(this::handleAfterTerminateConstraints);

		if (!onDecisionHandlers.isEmpty())
			wrapped = onDecision(onDecisionHandlers).thenMany(wrapped);

		return wrapped;
	}

	private LongConsumer consumeAllLong(List<LongConsumer> handlers) {
		return value -> handlers.stream().forEach(handler -> handler.accept(value));
	}

	private <V> Function<V, V> mapAll(List<Function<V, V>> handlers) {
		return value -> handlers.stream()
				.reduce(Function.identity(), (merged, newFunction) -> x -> newFunction.apply(merged.apply(x)))
				.apply(value);
	}

	private <V> Consumer<V> consumeAll(List<Consumer<V>> handlers) {
		return value -> handlers.stream().forEach(handler -> handler.accept(value));
	}

	private Mono<Void> onDecision(List<Runnable> handlers) {
		return Mono.fromRunnable(runAll(handlers));
	}

	private Runnable runAll(List<Runnable> handlers) {
		return () -> handlers.stream().forEach(Runnable::run);
	}

}