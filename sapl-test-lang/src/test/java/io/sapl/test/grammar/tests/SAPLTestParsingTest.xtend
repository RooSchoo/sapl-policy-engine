/*
 * SAPLTest generated by Xtext
 */
package io.sapl.test.grammar.tests

import com.google.inject.Inject
import io.sapl.test.grammar.SAPLTest.SAPLTest
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

@ExtendWith(InjectionExtension)
@InjectWith(SAPLTestInjectorProvider)
class SAPLTestParsingTest {
	@Inject
	ParseHelper<SAPLTest> parseHelper

}