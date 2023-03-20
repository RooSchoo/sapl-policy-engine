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
package io.sapl.mavenplugin.test.coverage.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.sapl.mavenplugin.test.coverage.TestFileHelper;
import io.sapl.test.coverage.api.CoverageAPIFactory;
import io.sapl.test.coverage.api.model.PolicySetHit;

class CoverageAPIHelperTest {

	private Path baseDir;

	@BeforeEach
	void setup() {
		baseDir = Paths.get("target/sapl-coverage");
		TestFileHelper.deleteDirectory(baseDir.toFile());
	}

	@AfterEach
	void cleanup() {
		TestFileHelper.deleteDirectory(baseDir.toFile());
	}

	@Test
	void test() {
		var helper = new CoverageAPIHelper();
		var writer = CoverageAPIFactory.constructCoverageHitRecorder(baseDir);

		var hits1 = helper.readHits(baseDir);
		assertEquals(0, hits1.getPolicySets().size());

		writer.recordPolicySetHit(new PolicySetHit("testSet"));
		var hits2 = helper.readHits(baseDir);
		assertEquals(1, hits2.getPolicySets().size());

	}

}