package io.sapl.test.dsl.interpreter;

import static io.sapl.test.Imports.arguments;
import static io.sapl.test.Imports.parentValue;
import static io.sapl.test.Imports.whenAttributeParams;
import static io.sapl.test.Imports.whenParentValue;

import io.sapl.api.interpreter.Val;
import io.sapl.test.dsl.interpreter.matcher.ValMatcherInterpreter;
import io.sapl.test.grammar.sAPLTest.Attribute;
import io.sapl.test.grammar.sAPLTest.AttributeWithParameters;
import io.sapl.test.steps.GivenOrWhenStep;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Matcher;

@RequiredArgsConstructor
public class AttributeInterpreter {

    private final ValInterpreter valInterpreter;
    private final ValMatcherInterpreter matcherInterpreter;
    private final DurationInterpreter durationInterpreter;

    GivenOrWhenStep interpretAttribute(final GivenOrWhenStep initial, final Attribute attribute) {
        final var importName = attribute.getImportName();

        if (attribute.getReturn() == null || attribute.getReturn().isEmpty()) {
            return initial.givenAttribute(importName);
        } else {
            final var values = attribute.getReturn().stream().map(valInterpreter::getValFromValue).toArray(Val[]::new);

            final var duration = durationInterpreter.getJavaDurationFromDuration(attribute.getDuration());

            return duration == null ? initial.givenAttribute(importName, values) : initial.givenAttribute(importName, duration, values);
        }
    }

    GivenOrWhenStep interpretAttributeWithParameters(final GivenOrWhenStep initial, final AttributeWithParameters attributeWithParameters) {
        final var importName = attributeWithParameters.getImportName();

        final var parentValueMatcher = matcherInterpreter.getHamcrestValMatcher(attributeWithParameters.getParentMatcher());
        final var returnValue = valInterpreter.getValFromValue(attributeWithParameters.getReturn());

        final var arguments = attributeWithParameters.getParameters();

        if (arguments == null || arguments.isEmpty()) {
            return initial.givenAttribute(importName, whenParentValue(parentValueMatcher), returnValue);
        }
        final var args = arguments.stream().map(matcherInterpreter::getHamcrestValMatcher).<Matcher<Val>>toArray(Matcher[]::new);
        return initial.givenAttribute(importName, whenAttributeParams(parentValue(parentValueMatcher), arguments(args)), returnValue);
    }
}