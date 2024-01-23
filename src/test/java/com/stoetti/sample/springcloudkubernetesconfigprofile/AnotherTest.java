package com.stoetti.sample.springcloudkubernetesconfigprofile;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author wind57
 */
@SpringBootTest
@EnableKubernetesMockClient(crud = true)
@ActiveProfiles("test")
class AnotherTest {

    private static KubernetesClient mockClient;

    private final static ConfigMapBuilder applicationConfigMapBuilder = new ConfigMapBuilder()
            .withNewMetadata()
            .withName("my-source")
            .endMetadata()
            .addToData("my-source.properties", "key=defaultValue");

    private final static ConfigMapBuilder applicationConfigMapBuilderTest = new ConfigMapBuilder()
            .withNewMetadata()
            .withName("my-source-test")
            .endMetadata()
            .addToData("my-source-test.properties", "key=non-default-value");

    @BeforeAll
    static void setUpBeforeClass() {
        mockClient.configMaps()
                .inNamespace("default")
                .resource(applicationConfigMapBuilder.build())
                .create();

        mockClient.configMaps()
                .inNamespace("default")
                .resource(applicationConfigMapBuilderTest.build())
                .create();

        // Configure the kubernetes master url to point to the mock server
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl());
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");
        System.setProperty(Config.KUBERNETES_HTTP2_DISABLE, "true");
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void testSpecificSupport() {
        String defaultKey = context.getEnvironment().getProperty("key");
        assertThat(defaultKey).isEqualTo("defaultValue");
    }

}
