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
package io.sapl.pdp.config.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sapl.grammar.sapl.CombiningAlgorithm;
import io.sapl.interpreter.combinators.CombiningAlgorithmFactory;
import io.sapl.pdp.config.PolicyDecisionPointConfiguration;
import io.sapl.pdp.config.VariablesAndCombinatorSource;
import io.sapl.util.JarPathUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
public class ResourcesVariablesAndCombinatorSource implements VariablesAndCombinatorSource {

    private static final String DEFAULT_CONFIG_PATH = "/policies";
    private static final String CONFIG_FILE_GLOB_PATTERN = "pdp.json";

    private final ObjectMapper mapper;
    private final PolicyDecisionPointConfiguration config;

    public ResourcesVariablesAndCombinatorSource() {
        this(DEFAULT_CONFIG_PATH);
    }

    public ResourcesVariablesAndCombinatorSource(String configPath) {
        this(configPath, new ObjectMapper());
    }

    public ResourcesVariablesAndCombinatorSource(@NonNull String configPath, @NonNull ObjectMapper mapper) {
        this(ResourcesVariablesAndCombinatorSource.class, configPath, mapper);
    }

    public ResourcesVariablesAndCombinatorSource(@NonNull Class<?> clazz, @NonNull String configPath,
                                                 @NonNull ObjectMapper mapper) {
        log.info("Loading the PDP configuration from bundled resources: '{}'", configPath);
        this.mapper = mapper;
        URL configFolderUrl = clazz.getResource(configPath);
        if (configFolderUrl == null) {
            throw new RuntimeException("Config folder not found. Path:" + configPath + " - URL: null");
        }

        if ("jar".equals(configFolderUrl.getProtocol())) {
            config = readConfigFromJar(configFolderUrl);
        } else {
            config = readConfigFromDirectory(configFolderUrl);
        }
    }

    PolicyDecisionPointConfiguration readConfigFromJar(URL configFolderUrl) {
        log.debug("reading config from jar {}", configFolderUrl);
        val jarPathElements = configFolderUrl.toString().split("!");
        val jarFilePath = JarPathUtil.getJarFilePath(jarPathElements);
        val dirPath = new StringBuilder();
        for (var i = 1; i < jarPathElements.length; i++) {
            dirPath.append(jarPathElements[i]);
        }
        if (dirPath.charAt(0) == '/') {
            dirPath.deleteCharAt(0);
        }
        final String configFilePath = dirPath.append('/').append(CONFIG_FILE_GLOB_PATTERN).toString();

        try (ZipFile zipFile = new ZipFile(jarFilePath)) {
            Enumeration<? extends ZipEntry> e = zipFile.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory() && entry.getName().equals(configFilePath)) {
                    log.info("loading PDP configuration: {}", entry.getName());
                    BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                    String fileContentsStr = IOUtils.toString(bis, StandardCharsets.UTF_8);
                    bis.close();
                    return mapper.readValue(fileContentsStr, PolicyDecisionPointConfiguration.class);
                }
            }
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
        log.info("No PDP configuration found in resources. Using defaults.");
        return new PolicyDecisionPointConfiguration();
    }

    PolicyDecisionPointConfiguration readConfigFromDirectory(URL configFolderUrl) {
        log.debug("reading config from directory {}", configFolderUrl);
        Path configDirectoryPath;
        try {
            configDirectoryPath = Paths.get(configFolderUrl.toURI());
        } catch (URISyntaxException e) {
            throw Exceptions.propagate(e);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDirectoryPath, CONFIG_FILE_GLOB_PATTERN)) {
            for (Path filePath : stream) {
                log.info("loading PDP configuration: {}", filePath.toAbsolutePath());
                return mapper.readValue(filePath.toFile(), PolicyDecisionPointConfiguration.class);
            }
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
        log.info("No PDP configuration found in resources. Using defaults.");
        return new PolicyDecisionPointConfiguration();
    }

    @Override
    public Flux<Optional<CombiningAlgorithm>> getCombiningAlgorithm() {
        return Flux.just(config.getAlgorithm()).map(CombiningAlgorithmFactory::getCombiningAlgorithm).map(Optional::of);
    }

    @Override
    public Flux<Optional<Map<String, JsonNode>>> getVariables() {
        return Flux.just(config.getVariables()).map(HashMap::new).map(Optional::of);
    }

    @Override
    public void dispose() {
        // NOP nothing to dispose
    }
}
