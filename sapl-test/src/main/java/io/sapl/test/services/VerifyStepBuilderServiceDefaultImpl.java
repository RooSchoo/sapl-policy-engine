package io.sapl.test.services;

import static io.sapl.hamcrest.Matchers.hasObligation;
import static io.sapl.hamcrest.Matchers.isDeny;
import static io.sapl.hamcrest.Matchers.isIndeterminate;
import static io.sapl.hamcrest.Matchers.isNotApplicable;
import static io.sapl.hamcrest.Matchers.isPermit;
import static org.hamcrest.Matchers.allOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sapl.api.interpreter.Val;
import io.sapl.api.pdp.AuthorizationDecision;
import io.sapl.test.grammar.sAPLTest.*;
import io.sapl.test.interfaces.VerifyStepBuilder;
import io.sapl.test.steps.ExpectStep;
import io.sapl.test.steps.VerifyStep;
import java.time.Duration;
import org.hamcrest.Matcher;

public final class VerifyStepBuilderServiceDefaultImpl implements VerifyStepBuilder {
    @Override
    public VerifyStep constructVerifyStep(TestCase testCase, ExpectStep expectOrVerifyStep) {
        final var expect = testCase.getExpect();
        if (expect instanceof SingleExpect singleExpect) {
            return interpretSingleExpect(expectOrVerifyStep, singleExpect);
        } else if (expect instanceof RepeatedExpect repeatedExpect) {
            for (var expectOrAdjustment : repeatedExpect.getExpectSteps()) {
                if (expectOrAdjustment instanceof Next nextExpect) {
                    final var actualAmount = nextExpect.getAmount() instanceof Multiple multiple ? multiple.getAmount() : 1;

                    expectOrVerifyStep = switch (nextExpect.getExpectedDecision()) {
                        case "permit" -> expectOrVerifyStep.expectNextPermit(actualAmount);
                        case "deny" -> expectOrVerifyStep.expectNextDeny(actualAmount);
                        case "indeterminate" -> expectOrVerifyStep.expectNextIndeterminate(actualAmount);
                        default -> expectOrVerifyStep.expectNextNotApplicable(actualAmount);
                    };
                } else if (expectOrAdjustment instanceof NextWithMatcher nextWithMatcher) {
                    expectOrVerifyStep = constructNextWithMatcher(expectOrVerifyStep, nextWithMatcher);
                } else if (expectOrAdjustment instanceof AttributeAdjustment attributeAdjustment) {
                    expectOrVerifyStep = expectOrVerifyStep.thenAttribute(attributeAdjustment.getAttribute(), getValFromReturnValue(attributeAdjustment.getReturnValue()));
                } else if (expectOrAdjustment instanceof Await await) {
                    expectOrVerifyStep = expectOrVerifyStep.thenAwait(Duration.ofSeconds(await.getAmount().getSeconds()));
                } else if (expectOrAdjustment instanceof NoEvent noEvent) {
                    expectOrVerifyStep = expectOrVerifyStep.expectNoEvent(Duration.ofSeconds(noEvent.getDuration()));
                }
            }
        }
        return (VerifyStep) expectOrVerifyStep;
    }

    private ExpectStep constructNextWithMatcher(ExpectStep expectStep, NextWithMatcher nextWithMatcher) {
        final var matchers = nextWithMatcher.getMatcher();
        if(matchers.isEmpty()) {
            return expectStep;
        }

        final var actualMatchers = matchers.stream().map(matcher -> {
            if(matcher instanceof AuthorizationDecisionMatcher authorizationDecisionMatcher) {
                return switch (authorizationDecisionMatcher.getDecision()) {
                    case "permit" -> isPermit();
                    case "deny" -> isDeny();
                    case "indeterminate" -> isIndeterminate();
                    default -> isNotApplicable();
                };
            } else if(matcher instanceof ObligationMatcher obligationMatcher) {
                return hasObligation(obligationMatcher.getValue());
            }
            return null;
        }).toArray(Matcher[]::new);

        return expectStep.expectNext(actualMatchers.length > 1 ? allOf(actualMatchers) : actualMatchers[0]);
    }

    private VerifyStep interpretSingleExpect(ExpectStep givenOrWhenStep, SingleExpect singleExpect) {
        var authorizationDecision = getAuthorizationDecisionFromDSL(singleExpect.getDecision());
        final var obligationElements = singleExpect.getObligationElements();
        final var resourceElements = singleExpect.getResourceElements();

        final var mapper = new ObjectMapper();
        if(obligationElements != null && !obligationElements.isEmpty()) {
            ObjectNode obligation = mapper.createObjectNode();
            obligationElements.forEach(obligationElement -> obligation.put(obligationElement.getKey(), obligationElement.getValue()));
            ArrayNode obligations = mapper.createArrayNode();
            obligations.add(obligation);
            authorizationDecision = authorizationDecision.withObligations(obligations);
        }
        if(resourceElements != null && !resourceElements.isEmpty()) {
            ObjectNode resource = mapper.createObjectNode();
            resourceElements.forEach(obligationElement -> resource.put(obligationElement.getKey(), obligationElement.getValue()));
            authorizationDecision = authorizationDecision.withResource(resource);
        }
        return givenOrWhenStep.expect(authorizationDecision);
    }

    private AuthorizationDecision getAuthorizationDecisionFromDSL(String decision) {
        return switch (decision) {
            case "permit" -> AuthorizationDecision.PERMIT;
            case "deny" -> AuthorizationDecision.DENY;
            case "indeterminate" -> AuthorizationDecision.INDETERMINATE;
            default -> AuthorizationDecision.NOT_APPLICABLE;
        };
    }

    private Val getValFromReturnValue(io.sapl.test.grammar.sAPLTest.Val value) {
        if (value instanceof IntVal intVal) {
            return Val.of(intVal.getValue());
        } else if (value instanceof StringVal stringVal) {
            return Val.of(stringVal.getValue());
        } else if (value instanceof BoolVal boolVal) {
            return Val.of(boolVal.isIsTrue());
        }
        return null;
    }
}
