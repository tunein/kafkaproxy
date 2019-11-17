/*
 * Copyright 2019 Alex Stockinger
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
 *
 */

package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.broker.BrokerConfig;
import com.dajudge.kafkaproxy.config.broker.BrokerConfigSource;
import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BrokerConfigTest extends BaseConfigTest<BrokerConfig> {

    private static final String BROKER1 = "broker1";
    private static final String BROKER2 = "broker2";
    private static final String BROKER1_ENDPOINT = "broker1.kafka.local";
    private static final String BROKER2_ENDPOINT = "broker2.kafka.local";
    private static final int BROKER_PORT = 9092;
    private static final int BROKER2_PROXY_PORT = 39093;
    private static final int BROKER1_PROXY_PORT = 39092;
    private static final String PROXY_ENDPOINT = "kafka.example.com";

    @Test
    public void asterisk_proxies_all() {
        assertProxiedBrokers("*", BROKER1, BROKER2);
    }

    @Test
    public void can_set_proxied_brokers() {
        assertProxiedBrokers(BROKER1, BROKER1);
    }

    @Test
    public void defaults_to_all_brokers_proxied() {
        assertProxiedBrokers(null, BROKER1, BROKER2);
    }

    private void assertProxiedBrokers(final String value, final String... brokers) {
        final TestEnvironment env = fullEnvironment()
                .withEnv("KAFKAPROXY_PROXIED_BROKERS", value);
        final BrokerConfig config = parse(env);
        final Set<String> proxiedBrokers = config.getBrokersToProxy().stream()
                .map(BrokerMapping::getName)
                .collect(toSet());
        assertEquals(new HashSet<>(asList(brokers)), proxiedBrokers);
    }

    @Test
    public void uses_default_brokermap_location_when_not_set() {
        final TestEnvironment env = fullEnvironment()
                .withEnv("KAFKAPROXY_BROKERMAP_LOCATION", null);
        final BrokerConfig brokerMap = parse(env);
        assertEquals(1, brokerMap.getBrokerMap().getAll().size());
    }

    @Test
    public void test_getByBrokerEndpoint() {
        final BrokerMap brokerMap = parse(fullEnvironment()).getBrokerMap();
        assertMapping1(brokerMap.getByBrokerEndpoint(BROKER1_ENDPOINT, BROKER_PORT));
        assertMapping2(brokerMap.getByBrokerEndpoint(BROKER2_ENDPOINT, BROKER_PORT));
    }

    @Test
    public void test_getByProxyName() {
        final BrokerMap brokerMap = parse(fullEnvironment()).getBrokerMap();
        assertMapping1(brokerMap.getByProxyName(BROKER1));
        assertMapping2(brokerMap.getByProxyName(BROKER2));
    }

    private void assertMapping1(final BrokerMapping endpoint) {
        assertEndpoint(endpoint, BROKER1, BROKER1_ENDPOINT, BROKER1_PROXY_PORT);
    }

    private void assertMapping2(final BrokerMapping endpoint) {
        assertEndpoint(endpoint, BROKER2, BROKER2_ENDPOINT, BROKER2_PROXY_PORT);
    }

    private void assertEndpoint(
            final BrokerMapping endpoint,
            final String name,
            final String hostname,
            final int proxyPort
    ) {
        assertNotNull(endpoint);
        assertEquals(name, endpoint.getName());
        assertEquals(BROKER_PORT, endpoint.getBroker().getPort());
        assertEquals(proxyPort, endpoint.getProxy().getPort());
        assertEquals(PROXY_ENDPOINT, endpoint.getProxy().getHost());
        assertEquals(hostname, endpoint.getBroker().getHost());
    }

    @Override
    TestEnvironment fullEnvironment() {
        return new TestEnvironment()
                .withEnv("KAFKAPROXY_BROKERMAP_LOCATION", "brokermap.yml")
                .withEnv("KAFKAPROXY_PROXIED_BROKERS", "*")
                .withFile("brokermap.yml", "configs/config1.yml")
                .withFile("/etc/kafkaproxy/brokermap.yml", "configs/config2.yml");
    }

    @Override
    BrokerConfig parse(final Environment e) {
        return new BrokerConfigSource().parse(e);
    }
}
