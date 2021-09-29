package io.sapl.spring.constraints;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

import org.reactivestreams.Publisher;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@UtilityClass
public class AdviceUtil {

	public static <T> Optional<Consumer<? super T>> advice(Consumer<? super T> consumer) {
		if (consumer == null)
			return Optional.empty();

		return Optional.of(t -> {
			try {
				consumer.accept(t);
			} catch (Throwable e) {
				log.info("Error during advice handling: " + e.getMessage(), e);
			}
		});
	}

	public static Optional<LongConsumer> advice(LongConsumer consumer) {
		if (consumer == null)
			return Optional.empty();

		return Optional.of(t -> {
			try {
				consumer.accept(t);
			} catch (Throwable e) {
				log.info("Error during advice handling: " + e.getMessage(), e);
			}
		});
	}

	public static Optional<Runnable> advice(Runnable runnable) {
		if (runnable == null)
			return Optional.empty();

		return Optional.of(() -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				log.info("Error during advice handling: " + e.getMessage(), e);
			}
		});
	}

	public static <T> Optional<Function<T, T>> advice(Function<T, T> function) {
		if (function == null)
			return Optional.empty();

		return Optional.of(t -> {
			try {
				var result = function.apply(t);
				if (result == null) {
					log.info("Fallback to identity. Handler must not return null.");
					return t;
				}
				return result;
			} catch (Throwable e) {
				log.info("Fallback to identity. Error during advice handling: " + e.getMessage(), e);
				return t;
			}
		});
	}

	public static <T> Optional<Function<T, Publisher<T>>> flatMapAdvice(Function<T, Publisher<T>> function) {
		if (function == null)
			return Optional.empty();

		return Optional.of(t -> {
			var result = function.apply(t);
			if (result == null)
				return Mono.just(t);
			return Flux.from(result)
					.doOnError(
							e -> log.info("Fallback to identity. Error during advice handling: " + e.getMessage(), e))
					.onErrorReturn(t);
		});
	}

}
