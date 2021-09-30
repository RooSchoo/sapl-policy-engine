/*
 * Copyright © 2017-2021 Dominic Heutelbeck (dominic@heutelbeck.com)
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
package io.sapl.spring.method.attributes;

import org.springframework.expression.Expression;

public class PreEnforceAttribute extends AbstractPolicyBasedEnforcementAttribute
		implements PreInvocationEnforcementAttribute {

	private static final long serialVersionUID = 2046032680569217119L;

	public PreEnforceAttribute(String subjectExpression, String actionExpression, String resourceExpression,
			String environmentExpression, Class<?> genericsType) {
		super(subjectExpression, actionExpression, resourceExpression, environmentExpression, genericsType);
	}

	public PreEnforceAttribute(Expression subjectExpression, Expression actionExpression, Expression resourceExpression,
			Expression environmentExpression, Class<?> genericsType) {
		super(subjectExpression, actionExpression, resourceExpression, environmentExpression, genericsType);
	}

}