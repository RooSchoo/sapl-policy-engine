/*
 * Copyright © 2017-2022 Dominic Heutelbeck (dominic@heutelbeck.com)
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
package io.sapl.test.verification;

import static io.sapl.hamcrest.Matchers.anyVal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import io.sapl.api.interpreter.Val;
import io.sapl.test.SaplTestException;
import io.sapl.test.mocking.MockCall;

class TimesParameterCalledVerificationTests {

	@Test
	void test() {
		var runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar"), Val.of(1)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));
		runInfo.saveCall(new MockCall(Val.of("yyy"), Val.of(3)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));

		var matcher            = comparesEqualTo(2);
		var expectedParameters = List.of(is(Val.of("xxx")), is(Val.of(2)));
		var verification       = new TimesParameterCalledVerification(new TimesCalledVerification(matcher),
				expectedParameters);

		assertThatNoException().isThrownBy(() -> verification.verify(runInfo));

		assertThat(runInfo.getCalls()).hasSize(4);
		assertThat(runInfo.getCalls().get(0).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(1).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(2).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(3).isUsed()).isTrue();
	}

	@Test
	void test_assertionError() {
		var runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar"), Val.of(1)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));
		runInfo.saveCall(new MockCall(Val.of("yyy"), Val.of(3)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(3)));

		var matcher            = comparesEqualTo(2);
		var expectedParameters = new LinkedList<Matcher<Val>>();
		expectedParameters.add(is(Val.of("xxx")));
		expectedParameters.add(is(Val.of(2)));
		var verification = new TimesParameterCalledVerification(new TimesCalledVerification(matcher),
				expectedParameters);

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> verification.verify(runInfo));

		assertThat(runInfo.getCalls()).hasSize(4);
		assertThat(runInfo.getCalls().get(0).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(1).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(2).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(3).isUsed()).isFalse();
	}

	@Test
	void test_assertionError_tooOftenCalled() {
		var runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar"), Val.of(1)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(3)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(3)));

		var matcher = comparesEqualTo(2);

		var expectedParameters = new LinkedList<Matcher<Val>>();
		expectedParameters.add(is(Val.of("xxx")));
		expectedParameters.add(anyVal());
		var verification = new TimesParameterCalledVerification(new TimesCalledVerification(matcher),
				expectedParameters);

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> verification.verify(runInfo));

		assertThat(runInfo.getCalls()).hasSize(4);
		assertThat(runInfo.getCalls().get(0).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(1).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(2).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(3).isUsed()).isTrue();
	}

	@Test
	void test_MultipleParameterTimesVerifications_WithAnyMatcher_OrderingMatters() {
		var runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar"), Val.of(1)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));
		runInfo.saveCall(new MockCall(Val.of("yyy"), Val.of(3)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(3)));

		var matcher = comparesEqualTo(1);

		var expectedParameters = List.of(is(Val.of("xxx")), is(Val.of(2)));
		var verification       = new TimesParameterCalledVerification(new TimesCalledVerification(matcher),
				expectedParameters);

		assertThatNoException().isThrownBy(() -> verification.verify(runInfo));

		assertThat(runInfo.getCalls()).hasSize(4);
		assertThat(runInfo.getCalls().get(0).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(1).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(2).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(3).isUsed()).isFalse();

		var matcher2            = comparesEqualTo(1);
		var expectedParameters2 = List.of(is(Val.of("xxx")), anyVal());
		var verification2       = new TimesParameterCalledVerification(new TimesCalledVerification(matcher2),
				expectedParameters2);

		assertThatNoException().isThrownBy(() -> verification2.verify(runInfo));

		assertThat(runInfo.getCalls()).hasSize(4);
		assertThat(runInfo.getCalls().get(0).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(1).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(2).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(3).isUsed()).isTrue();
	}

	@Test
	void test_assertionError_verificationMessage() {
		var runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar"), Val.of(1)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));
		runInfo.saveCall(new MockCall(Val.of("yyy"), Val.of(3)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(3)));

		var matcher = comparesEqualTo(2);

		var expectedParameters = new LinkedList<Matcher<Val>>();
		expectedParameters.add(is(Val.of("xxx")));
		expectedParameters.add(is(Val.of(2)));
		var verification = new TimesParameterCalledVerification(new TimesCalledVerification(matcher),
				expectedParameters);

		assertThatThrownBy(() -> verification.verify(runInfo, "VerificationMessage")).isInstanceOf(AssertionError.class)
				.hasMessageContaining("VerificationMessage");

		assertThat(runInfo.getCalls()).hasSize(4);
		assertThat(runInfo.getCalls().get(0).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(1).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(2).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(3).isUsed()).isFalse();
	}

	@Test
	void test_assertionError_VerificationMessage_Empty() {
		var runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar"), Val.of(1)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));
		runInfo.saveCall(new MockCall(Val.of("yyy"), Val.of(3)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(3)));

		var matcher = comparesEqualTo(2);

		var expectedParameters = new LinkedList<Matcher<Val>>();
		expectedParameters.add(is(Val.of("xxx")));
		expectedParameters.add(is(Val.of(2)));
		var verification = new TimesParameterCalledVerification(new TimesCalledVerification(matcher),
				expectedParameters);

		assertThatThrownBy(() -> verification.verify(runInfo, "")).isInstanceOf(AssertionError.class)
				.hasMessageContaining("Error verifying the expected number of calls to the mock");

		assertThat(runInfo.getCalls()).hasSize(4);
		assertThat(runInfo.getCalls().get(0).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(1).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(2).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(3).isUsed()).isFalse();
	}

	@Test
	void test_assertionError_VerificationMessage_Null() {
		var runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar"), Val.of(1)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));
		runInfo.saveCall(new MockCall(Val.of("yyy"), Val.of(3)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(3)));

		var matcher = comparesEqualTo(2);

		var expectedParameters = new LinkedList<Matcher<Val>>();
		expectedParameters.add(is(Val.of("xxx")));
		expectedParameters.add(is(Val.of(2)));
		var verification = new TimesParameterCalledVerification(new TimesCalledVerification(matcher),
				expectedParameters);

		assertThatThrownBy(() -> verification.verify(runInfo, null)).isInstanceOf(AssertionError.class)
				.hasMessageContaining("Error verifying the expected number of calls to the mock");

		assertThat(runInfo.getCalls()).hasSize(4);
		assertThat(runInfo.getCalls().get(0).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(1).isUsed()).isTrue();
		assertThat(runInfo.getCalls().get(2).isUsed()).isFalse();
		assertThat(runInfo.getCalls().get(3).isUsed()).isFalse();
	}

	@Test
	void test_Exception_CountOfExpectedParamterNotEqualsFunctionCallParametersCount() {
		var runInfo = new MockRunInformation("foo");
		runInfo.saveCall(new MockCall(Val.of("bar"), Val.of(1)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(2)));
		runInfo.saveCall(new MockCall(Val.of("yyy"), Val.of(3)));
		runInfo.saveCall(new MockCall(Val.of("xxx"), Val.of(3)));

		var matcher            = comparesEqualTo(2);
		var expectedParameters = new LinkedList<Matcher<Val>>();
		expectedParameters.add(is(Val.of("xxx")));
		var verification = new TimesParameterCalledVerification(new TimesCalledVerification(matcher),
				expectedParameters);

		assertThatExceptionOfType(SaplTestException.class).isThrownBy(() -> verification.verify(runInfo));
	}

}