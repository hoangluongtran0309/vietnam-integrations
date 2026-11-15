package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CoreDependencyRulesTest {

    @Test
    void coreDoesNotImportSpringOrJackson() throws IOException {
        Path sourceRoot = Path.of(System.getProperty("basedir"), "src/main/java");
        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertThat(source)
                        .as("framework-neutral source %s", file)
                        .doesNotContain("import org.springframework.")
                        .doesNotContain("import com.fasterxml.jackson.");
            }
        }
    }
}
