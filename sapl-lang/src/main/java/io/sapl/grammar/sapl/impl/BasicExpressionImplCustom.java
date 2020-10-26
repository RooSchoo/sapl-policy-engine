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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.common.util.EList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.sapl.api.interpreter.PolicyEvaluationException;
import io.sapl.api.interpreter.Val;
import io.sapl.grammar.sapl.Step;
import io.sapl.interpreter.EvaluationContext;
import io.sapl.interpreter.selection.JsonNodeWithoutParent;
import io.sapl.interpreter.selection.ResultNode;
import reactor.core.publisher.Flux;

/**
 * Superclass of basic expressions providing a method to evaluate the steps,
 * filter and subtemplate possibly being part of the basic expression.
 *
 * Grammar: BasicExpression returns Expression: Basic (FILTER
 * filter=FilterComponent | SUBTEMPLATE subtemplate=BasicExpression)? ;
 *
 * Basic returns BasicExpression: {BasicGroup} '(' expression=Expression ')'
 * steps+=Step* | {BasicValue} value=Value steps+=Step* | {BasicFunction}
 * fsteps+=ID ('.' fsteps+=ID)* arguments=Arguments steps+=Step* |
 * {BasicIdentifier} identifier=ID steps+=Step* | {BasicRelative} '@'
 * steps+=Step* ;
 */
public class BasicExpressionImplCustom extends BasicExpressionImpl {

	/**
	 * Method which is supposed to be inherited by the various subclasses of
	 * BasicExpression and used in their {@code evaluate} method.
	 *
	 * The method takes the JsonNode from evaluating the first part of the
	 * BasicExpression and applies the selection steps as well as a filter or
	 * sub-template specified in the BasicExpression.
	 * 
	 * @param resultBeforeSteps the result before evaluating selection steps, filter
	 *                          or sub-template
	 * @param steps             the selection steps
	 * @param ctx               the evaluation context
	 * @param isBody            true if the expression occurs within the policy body
	 *                          (attribute finder steps are only allowed if set to
	 *                          true)
	 * @param relativeNode      the node a relative expression would point to
	 * @return a Flux of JsonNodes that are the result after evaluating the steps,
	 *         filter and sub-template
	 */
	protected Flux<Val> evaluateStepsFilterSubtemplate(Val resultBeforeSteps, EList<Step> steps, EvaluationContext ctx,
			boolean isBody, Val relativeNode) {
		Flux<ResultNode> result = StepResolver.resolveSteps(resultBeforeSteps, steps, ctx, isBody, relativeNode);
		if (filter != null) {
			result = result.switchMap(resultNode -> {
				final Val jsonNode = resultNode.asJsonWithoutAnnotations();
				return filter.apply(jsonNode, ctx, isBody, relativeNode).map(JsonNodeWithoutParent::new);
			});
		} else if (subtemplate != null) {
			result = result.switchMap(resultNode -> {
				final Val jsonNode = resultNode.asJsonWithoutAnnotations();
				return evaluateSubtemplate(jsonNode, ctx, isBody).map(JsonNodeWithoutParent::new);
			});
		}
		return result.map(ResultNode::asJsonWithoutAnnotations);
	}

	/**
	 * The function applies a subtemplate to an array. I.e., it evaluates an
	 * expression for each of the items and replaces each items with the result.
	 * 
	 * @param preliminaryResult the array
	 * @param ctx               the evaluation context
	 * @param isBody            true if the expression is evaluated in the policy
	 *                          body
	 * @return a Flux of altered array nodes
	 */
	private Flux<Val> evaluateSubtemplate(Val preliminaryResult, EvaluationContext ctx, boolean isBody) {
		if (preliminaryResult.isUndefined() || !preliminaryResult.get().isArray()) {
			return Flux.error(new PolicyEvaluationException("Type mismatch. Expected an array."));
		}

		final ArrayNode arrayNode = (ArrayNode) preliminaryResult.get();
		final List<Flux<Val>> fluxes = new ArrayList<>(arrayNode.size());
		for (int i = 0; i < arrayNode.size(); i++) {
			final JsonNode childNode = arrayNode.get(i);
			fluxes.add(subtemplate.evaluate(ctx, isBody, Val.of(childNode)));
		}
		// handle the empty array
		if (fluxes.isEmpty()) {
			return Flux.just(Val.of(arrayNode));
		}
		return Flux.combineLatest(fluxes, Function.identity())
				.flatMap(replacements -> replace(arrayNode, replacements));
	}

	private Flux<Val> replace(ArrayNode arrayNode, Object[] replacements) {
		for (int i = 0; i < arrayNode.size(); i++) {
			Val value = ((Val) replacements[i]);
			if (value.isUndefined()) {
				return Flux.error(new PolicyEvaluationException("undefined cannot be added to JSON array"));
			}
			arrayNode.set(i, value.get());
		}
		return Flux.just(Val.of(arrayNode));
	}

}