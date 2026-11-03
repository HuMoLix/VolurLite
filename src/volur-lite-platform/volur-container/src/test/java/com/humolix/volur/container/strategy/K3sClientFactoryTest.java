package com.humolix.volur.container.strategy;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class K3sClientFactoryTest {

    @Test
    void testKubernetesClientInitializationWithJdkHttpClient() {
        Config config = new ConfigBuilder()
                .withMasterUrl("https://127.0.0.1:6443")
                .withTrustCerts(true)
                .build();

        KubernetesClient client = new KubernetesClientBuilder()
                .withConfig(config)
                .build();

        Assertions.assertNotNull(client);
        Assertions.assertDoesNotThrow(client::close);
    }

}
