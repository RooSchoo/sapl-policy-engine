package io.sapl.test.interfaces;

import io.sapl.test.grammar.sAPLTest.TestCase;
import io.sapl.test.steps.ExpectStep;
import io.sapl.test.steps.VerifyStep;

public interface VerifyStepBuilder {
    VerifyStep constructVerifyStep(TestCase testCase, ExpectStep expectOrVerifyStep);
}
