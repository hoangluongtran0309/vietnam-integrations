package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.DockerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;

class DockerAvailabilityInCiTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    void dockerIsAvailableOnCi() {
        assertThat(DockerClientFactory.instance().isDockerAvailable())
                .as("Docker must be available when CI=true; PostgreSQL tests must not be skipped on CI")
                .isTrue();
    }
}
