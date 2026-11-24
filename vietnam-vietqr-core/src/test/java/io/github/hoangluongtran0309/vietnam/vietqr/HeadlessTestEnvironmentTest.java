package io.github.hoangluongtran0309.vietnam.vietqr;

import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/** Ensures rendering tests do not depend on an available display. */
class HeadlessTestEnvironmentTest {

    @Test
    void awtRunsInHeadlessMode() {
        assertThat(System.getProperty("java.awt.headless")).isEqualTo("true");
        assertThat(GraphicsEnvironment.isHeadless()).isTrue();
    }
}
