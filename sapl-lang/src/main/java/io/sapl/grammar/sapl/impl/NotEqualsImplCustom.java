/**
 * Copyright © 2020 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.grammar.sapl.impl;

import static io.sapl.grammar.sapl.impl.OperatorUtil.operator;

import io.sapl.api.interpreter.Val;
import io.sapl.interpreter.EvaluationContext;
import lombok.NonNull;
import reactor.core.publisher.Flux;

/**
 * Checks for non equality of two values.
 *
 * Grammar: Comparison returns Expression: Prefixed (({NotEquals.left=current}
 * '!=') right=Prefixed)? ;
 */
public class NotEqualsImplCustom extends NotEqualsImpl {

	@Override
	public Flux<Val> evaluate(@NonNull EvaluationContext ctx, @NonNull Val relativeNode) {
		return operator(this, this::notEqual, ctx, relativeNode);
	}

	private Val notEqual(Val left, Val right) {
		if (left.isUndefined() && right.isUndefined()) {
			return Val.FALSE;
		}
		if (left.isUndefined() || right.isUndefined()) {
			return Val.TRUE;
		}
		if (left.isNumber() && right.isNumber()) {
			return Val.of(left.decimalValue().compareTo(right.decimalValue()) != 0);
		} else {
			return Val.of(!left.get().equals(right.get()));
		}
	}

}
