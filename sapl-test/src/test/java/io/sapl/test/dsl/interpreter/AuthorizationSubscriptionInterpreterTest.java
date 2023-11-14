package io.sapl.test.dsl.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.sapl.api.interpreter.Val;
import io.sapl.test.grammar.sAPLTest.AuthorizationSubscription;
import io.sapl.test.grammar.sAPLTest.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationSubscriptionInterpreterTest {
    @Mock
    private ValInterpreter valInterpreterMock;
    @InjectMocks
    private AuthorizationSubscriptionInterpreter authorizationSubscriptionInterpreter;

    private final MockedStatic<io.sapl.api.pdp.AuthorizationSubscription> authorizationSubscriptionMockedStatic = mockStatic(io.sapl.api.pdp.AuthorizationSubscription.class);

    @AfterEach
    void tearDown() {
        authorizationSubscriptionMockedStatic.close();
    }

    @Test
    void getAuthorizationSubscriptionFromDSL_correctlyInterpretsAuthorizationSubscriptionWithMissingEnvironment_returnsSAPLAuthorizationSubscription() {
        final var authorizationSubscriptionMock = mock(AuthorizationSubscription.class);

        final var subjectMock = mock(Value.class);
        final var actionMock = mock(Value.class);
        final var resourceMock = mock(Value.class);

        when(authorizationSubscriptionMock.getSubject()).thenReturn(subjectMock);
        when(authorizationSubscriptionMock.getAction()).thenReturn(actionMock);
        when(authorizationSubscriptionMock.getResource()).thenReturn(resourceMock);
        when(authorizationSubscriptionMock.getEnvironment()).thenReturn(null);

        final var subjectValMock = mock(Val.class);
        final var actionValMock = mock(Val.class);
        final var resourceValMock = mock(Val.class);

        when(valInterpreterMock.getValFromValue(subjectMock)).thenReturn(subjectValMock);
        when(valInterpreterMock.getValFromValue(actionMock)).thenReturn(actionValMock);
        when(valInterpreterMock.getValFromValue(resourceMock)).thenReturn(resourceValMock);

        final var subjectJsonNodeMock = mock(JsonNode.class);
        final var actionJsonNodeMock = mock(JsonNode.class);
        final var resourceJsonNodeMock = mock(JsonNode.class);

        when(subjectValMock.get()).thenReturn(subjectJsonNodeMock);
        when(actionValMock.get()).thenReturn(actionJsonNodeMock);
        when(resourceValMock.get()).thenReturn(resourceJsonNodeMock);

        final var saplAuthorizationSubscriptionMock = mock(io.sapl.api.pdp.AuthorizationSubscription.class);
        authorizationSubscriptionMockedStatic.when(() -> io.sapl.api.pdp.AuthorizationSubscription.of(subjectJsonNodeMock, actionJsonNodeMock, resourceJsonNodeMock)).thenReturn(saplAuthorizationSubscriptionMock);

        final var result = authorizationSubscriptionInterpreter.getAuthorizationSubscriptionFromDSL(authorizationSubscriptionMock);

        assertEquals(saplAuthorizationSubscriptionMock, result);
    }

    @Test
    void getAuthorizationSubscriptionFromDSL_correctlyInterpretsAuthorizationSubscription_returnsSAPLAuthorizationSubscription() {
        final var authorizationSubscriptionMock = mock(AuthorizationSubscription.class);

        final var subjectMock = mock(Value.class);
        final var actionMock = mock(Value.class);
        final var resourceMock = mock(Value.class);
        final var environmentMock = mock(Value.class);

        when(authorizationSubscriptionMock.getSubject()).thenReturn(subjectMock);
        when(authorizationSubscriptionMock.getAction()).thenReturn(actionMock);
        when(authorizationSubscriptionMock.getResource()).thenReturn(resourceMock);
        when(authorizationSubscriptionMock.getEnvironment()).thenReturn(environmentMock);

        final var subjectValMock = mock(Val.class);
        final var actionValMock = mock(Val.class);
        final var resourceValMock = mock(Val.class);
        final var environmentValMock = mock(Val.class);

        when(valInterpreterMock.getValFromValue(subjectMock)).thenReturn(subjectValMock);
        when(valInterpreterMock.getValFromValue(actionMock)).thenReturn(actionValMock);
        when(valInterpreterMock.getValFromValue(resourceMock)).thenReturn(resourceValMock);
        when(valInterpreterMock.getValFromValue(environmentMock)).thenReturn(environmentValMock);

        final var subjectJsonNodeMock = mock(JsonNode.class);
        final var actionJsonNodeMock = mock(JsonNode.class);
        final var resourceJsonNodeMock = mock(JsonNode.class);
        final var environmentJsonNodeMock = mock(JsonNode.class);

        when(subjectValMock.get()).thenReturn(subjectJsonNodeMock);
        when(actionValMock.get()).thenReturn(actionJsonNodeMock);
        when(resourceValMock.get()).thenReturn(resourceJsonNodeMock);
        when(environmentValMock.get()).thenReturn(environmentJsonNodeMock);

        final var saplAuthorizationSubscriptionMock = mock(io.sapl.api.pdp.AuthorizationSubscription.class);
        authorizationSubscriptionMockedStatic.when(() -> io.sapl.api.pdp.AuthorizationSubscription.of(subjectJsonNodeMock, actionJsonNodeMock, resourceJsonNodeMock, environmentJsonNodeMock)).thenReturn(saplAuthorizationSubscriptionMock);

        final var result = authorizationSubscriptionInterpreter.getAuthorizationSubscriptionFromDSL(authorizationSubscriptionMock);

        assertEquals(saplAuthorizationSubscriptionMock, result);
    }
}