package io.sapl.spring.marshall.action;

import io.sapl.spring.marshall.Action;
import lombok.Value;

@Value
@Deprecated
public class SimpleAction implements Action {

	String method;

}
