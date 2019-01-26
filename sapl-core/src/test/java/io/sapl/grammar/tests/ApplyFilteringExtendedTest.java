package io.sapl.grammar.tests;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.sapl.api.interpreter.PolicyEvaluationException;
import io.sapl.grammar.sapl.BasicRelative;
import io.sapl.grammar.sapl.FilterExtended;
import io.sapl.grammar.sapl.FilterStatement;
import io.sapl.grammar.sapl.IndexStep;
import io.sapl.grammar.sapl.RecursiveIndexStep;
import io.sapl.grammar.sapl.SaplFactory;
import io.sapl.grammar.sapl.impl.SaplFactoryImpl;
import io.sapl.interpreter.EvaluationContext;
import io.sapl.interpreter.functions.FunctionContext;
import io.sapl.interpreter.variables.VariableContext;
import reactor.test.StepVerifier;

public class ApplyFilteringExtendedTest {
	private static SaplFactory factory = SaplFactoryImpl.eINSTANCE;
	private static JsonNodeFactory JSON = JsonNodeFactory.instance;

	private static VariableContext variableCtx = new VariableContext();
	private static FunctionContext functionCtx = new MockFilteringContext();
	private static EvaluationContext ctx = new EvaluationContext(null, functionCtx, variableCtx);

	private static final String REMOVE = "remove";

	@Test
	public void removeNoStepsNoEach() {
		JsonNode root = JSON.objectNode();

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		statement.setTarget(factory.createBasicRelative());
		statement.getFsteps().add(REMOVE);
		filter.getStatements().add(statement);

		StepVerifier.create(filter.apply(root, ctx, false, null))
				.expectError(PolicyEvaluationException.class)
				.verify();
	}

	@Test
	public void removeEachNoArray() {
		JsonNode root = JSON.objectNode();

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		statement.setTarget(factory.createBasicRelative());
		statement.getFsteps().add(REMOVE);
		statement.setEach(true);
		filter.getStatements().add(statement);

		StepVerifier.create(filter.apply(root, ctx, false, null))
				.expectError(PolicyEvaluationException.class)
				.verify();
	}

	@Test
	public void removeNoStepsEach() {
		ArrayNode root = JSON.arrayNode();
		root.add(JSON.nullNode());
		root.add(JSON.booleanNode(true));

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		statement.setTarget(factory.createBasicRelative());
		statement.getFsteps().add(REMOVE);
		statement.setEach(true);
		filter.getStatements().add(statement);

		JsonNode expectedResult = JSON.arrayNode();

		filter.apply(root, ctx, false, null)
				.take(1)
				.subscribe(result -> assertEquals("Function remove, no steps and each should return empty array",
						expectedResult, result)
				);
	}

	@Test
	public void emptyStringNoStepsNoEach() {
		ArrayNode root = JSON.arrayNode();
		root.add(JSON.nullNode());
		root.add(JSON.booleanNode(true));

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		statement.setTarget(factory.createBasicRelative());
		statement.getFsteps().add("EMPTY_STRING");
		filter.getStatements().add(statement);

		JsonNode expectedResult = JSON.textNode("");

		filter.apply(root, ctx, false, null)
				.take(1)
				.subscribe(result -> assertEquals("Mock function EMPTY_STRING, no steps, no each should return empty string",
						expectedResult, result)
				);
	}

	@Test
	public void emptyStringNoStepsEach() {
		ArrayNode root = JSON.arrayNode();
		root.add(JSON.nullNode());
		root.add(JSON.booleanNode(true));

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		statement.setTarget(factory.createBasicRelative());
		statement.getFsteps().add("EMPTY_STRING");
		statement.setEach(true);
		filter.getStatements().add(statement);

		ArrayNode expectedResult = JSON.arrayNode();
		expectedResult.add(JSON.textNode(""));
		expectedResult.add(JSON.textNode(""));

		filter.apply(root, ctx, false, null)
				.take(1)
				.subscribe(result -> assertEquals("Mock function EMPTY_STRING, no steps, each should array with empty strings",
						expectedResult, result)
				);
	}

	@Test
	public void emptyStringEachNoArray() {
		ArrayNode root = JSON.arrayNode();
		root.add(JSON.objectNode());
		root.add(JSON.booleanNode(true));

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		statement.setTarget(factory.createBasicRelative());
		statement.getFsteps().add("EMPTY_STRING");

		IndexStep step = factory.createIndexStep();
		step.setIndex(BigDecimal.valueOf(0));
		BasicRelative expression = factory.createBasicRelative();
		expression.getSteps().add(step);
		statement.setTarget(expression);

		statement.setEach(true);

		filter.getStatements().add(statement);

		StepVerifier.create(filter.apply(root, ctx, false, null))
				.expectError(PolicyEvaluationException.class)
				.verify();
	}

	@Test
	public void removeResultArrayNoEach() {
		ArrayNode root = JSON.arrayNode();
		root.add(JSON.nullNode());
		root.add(JSON.booleanNode(true));

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		BasicRelative target = factory.createBasicRelative();
		RecursiveIndexStep step = factory.createRecursiveIndexStep();
		step.setIndex(BigDecimal.valueOf(0));
		target.getSteps().add(step);

		statement.setTarget(target);
		statement.getFsteps().add(REMOVE);
		filter.getStatements().add(statement);

		StepVerifier.create(filter.apply(root, ctx, false, null))
				.expectError(PolicyEvaluationException.class)
				.verify();
	}

	@Test
	public void emptyStringResultArrayNoEach() {
		ArrayNode root = JSON.arrayNode();
		root.add(JSON.nullNode());
		root.add(JSON.booleanNode(true));

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		BasicRelative target = factory.createBasicRelative();
		RecursiveIndexStep step = factory.createRecursiveIndexStep();
		step.setIndex(BigDecimal.valueOf(0));
		target.getSteps().add(step);

		statement.setTarget(target);
		statement.getFsteps().add("EMPTY_STRING");
		filter.getStatements().add(statement);

		StepVerifier.create(filter.apply(root, ctx, false, null))
				.expectError(PolicyEvaluationException.class)
				.verify();
	}

	@Test
	public void emptyStringResultArrayEach() {
		ArrayNode root = JSON.arrayNode();
		root.add(JSON.nullNode());
		root.add(JSON.booleanNode(true));

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		BasicRelative target = factory.createBasicRelative();
		RecursiveIndexStep step = factory.createRecursiveIndexStep();
		step.setIndex(BigDecimal.valueOf(0));
		target.getSteps().add(step);

		statement.setTarget(target);
		statement.getFsteps().add("EMPTY_STRING");
		statement.setEach(true);
		filter.getStatements().add(statement);

		ArrayNode expectedResult = JSON.arrayNode();
		expectedResult.add(JSON.textNode(""));
		expectedResult.add(JSON.booleanNode(true));

		filter.apply(root, ctx, false, null)
				.take(1)
				.subscribe(result -> assertEquals(
					"Mock function EMPTY_STRING applied to result array and each should replace selected elements by empty string",
					expectedResult, result)
				);
	}

	@Test
	public void removeResultArrayEach() {
		ArrayNode root = JSON.arrayNode();
		root.add(JSON.nullNode());
		root.add(JSON.booleanNode(true));

		FilterExtended filter = factory.createFilterExtended();
		FilterStatement statement = factory.createFilterStatement();
		BasicRelative target = factory.createBasicRelative();
		RecursiveIndexStep step = factory.createRecursiveIndexStep();
		step.setIndex(BigDecimal.valueOf(0));
		target.getSteps().add(step);

		statement.setTarget(target);
		statement.getFsteps().add(REMOVE);
		statement.setEach(true);
		filter.getStatements().add(statement);

		ArrayNode expectedResult = JSON.arrayNode();
		expectedResult.add(JSON.booleanNode(true));

		filter.apply(root, ctx, false, null)
				.take(1)
				.subscribe(result -> assertEquals("Remove applied to result array and each should remove each element",
						expectedResult, result)
				);
	}

}
