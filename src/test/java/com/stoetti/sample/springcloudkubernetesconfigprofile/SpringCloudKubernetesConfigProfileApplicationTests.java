package com.stoetti.sample.springcloudkubernetesconfigprofile;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableKubernetesMockClient(crud = true)
@ActiveProfiles("test")
class SpringCloudKubernetesConfigProfileApplicationTests {

    private static KubernetesClient mockClient;

    private final static ConfigMapBuilder applicationConfigMapBuilder = new ConfigMapBuilder()
            .withNewMetadata()
            .withName("test-profiled-properties")
            .endMetadata()
            .addToData("test-profiled-properties.properties", "key=defaultValue");

    @BeforeAll
    public static void setUpBeforeClass() {
        mockClient.configMaps()
                .inNamespace("default")
                .resource(applicationConfigMapBuilder.build())
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

    @Autowired
    private ContextRefresher contextRefresher;

    @Test
    void contextLoads() {
        // if the configMap contains only the default properties everything is fine
        final var defaultKey = context.getEnvironment().getProperty("key");
        assertThat(defaultKey).isEqualTo("defaultValue");

        // now a second entry with a profile-specific override is added to the ConfigMap
        mockClient.configMaps()
                .inNamespace("default")
                .resource(
                        applicationConfigMapBuilder
                                .addToData("test-profiled-properties-test.properties", "key=profileValue")
                                .build())
                .update();
        // while refreshing the environment an exception is logged that there is a duplicate key is recognized
        contextRefresher.refreshEnvironment();
        final var testKey = context.getEnvironment().getProperty("key");
        assertThat(testKey).isEqualTo("profileValue");
    }

}
