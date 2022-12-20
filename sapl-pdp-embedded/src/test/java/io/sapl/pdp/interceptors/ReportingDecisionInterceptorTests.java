package io.sapl.pdp.interceptors;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.sapl.api.interpreter.Val;
import io.sapl.api.pdp.AuthorizationSubscription;
import io.sapl.api.pip.Attribute;
import io.sapl.api.pip.PolicyInformationPoint;
import io.sapl.grammar.sapl.impl.DenyOverridesCombiningAlgorithmImplCustom;
import io.sapl.interpreter.DefaultSAPLInterpreter;
import io.sapl.interpreter.SAPLInterpreter;
import io.sapl.interpreter.functions.AnnotationFunctionContext;
import io.sapl.interpreter.pip.AnnotationAttributeContext;
import io.sapl.pdp.EmbeddedPolicyDecisionPoint;
import io.sapl.pdp.config.PDPConfiguration;
import io.sapl.pdp.config.PDPConfigurationProvider;
import io.sapl.prp.PolicyRetrievalPoint;
import io.sapl.prp.PolicyRetrievalResult;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;

public class ReportingDecisionInterceptorTests {

	private static class TestingPolicyRetrievalPoint implements PolicyRetrievalPoint {
		public String          permitAllDocument = "policy \"permitAll\" permit";
		public String          errorDocument     = "policy \"errAll\" permit where 1/0;";
		public String          attributeDocument = "policy \"attribute\" permit where \"left\".<test.test> == \"Attribute Result\";";
		public String          setOne            = "set \"set one\" first-applicable policy \"p1 in set one\" permit";
		public SAPLInterpreter interpreter       = new DefaultSAPLInterpreter();

		@Override
		public Flux<PolicyRetrievalResult> retrievePolicies() {
			var permitAll = interpreter.parse(permitAllDocument);
			var error     = interpreter.parse(errorDocument);
			var attribute = interpreter.parse(attributeDocument);
			var one       = interpreter.parse(setOne);
			return Flux.just(new PolicyRetrievalResult().withMatch(permitAll).withMatch(error).withMatch(attribute)
					.withMatch(one));
		}

		@Override
		public void dispose() {
			// NOOP
		}

	}

	@PolicyInformationPoint(name = "test")
	public static class ReportingTestPIP {

		@Attribute(name = "test")
		public Flux<Val> test(Val leftHand) {
			return Flux.just(Val.of("Attribute Result"));
		}
	}

	private static class TestingPDPConfigurationProvider implements PDPConfigurationProvider {

		@Override
		@SneakyThrows
		public Flux<PDPConfiguration> pdpConfiguration() {
			var cAlg         = new DenyOverridesCombiningAlgorithmImplCustom();
			var dInterceptor = new ReportingDecisionInterceptor(new ObjectMapper(), false, true, true, true);
			return Flux
					.just(new PDPConfiguration(new AnnotationAttributeContext(new Object[] { new ReportingTestPIP() }),
							new AnnotationFunctionContext(), Map.of(), cAlg, dInterceptor, x -> x));
		}

		@Override
		public void dispose() {
			// NOOP
		}

	}

	@Test
	public void runReportingTest() {
		var pdp = new EmbeddedPolicyDecisionPoint(new TestingPDPConfigurationProvider(),
				new TestingPolicyRetrievalPoint());
		var sub = AuthorizationSubscription.of("subject", "action", "resource");
		pdp.decide(sub).blockFirst();
	}

}