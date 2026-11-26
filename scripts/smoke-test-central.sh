#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <version> [--repo <url>]" >&2
}

if [[ $# -lt 1 ]]; then
  usage
  exit 2
fi

version=$1
shift
repository_url=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)
      if [[ $# -lt 2 ]]; then
        usage
        exit 2
      fi
      repository_url=$2
      if [[ -z "$repository_url" ]]; then
        echo "Repository URL must be non-empty" >&2
        exit 2
      fi
      shift 2
      ;;
    *)
      usage
      exit 2
      ;;
  esac
done

if [[ -z "$version" || "$version" == *['<>&']* || "$repository_url" == *['<>&']* ]]; then
  echo "Version must be non-empty and values must be XML-safe" >&2
  exit 2
fi

started_at=$(date +%s)
repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
temporary_dir=$(mktemp -d)
trap 'rm -rf -- "$temporary_dir"' EXIT

mkdir -p "$temporary_dir/local-repository" "$temporary_dir/project/src/main/java/smoke"

repositories_block=""
if [[ -n "$repository_url" ]]; then
  repositories_block=$(cat <<EOF
  <repositories>
    <repository>
      <id>smoke-input</id>
      <url>$repository_url</url>
      <releases><enabled>true</enabled></releases>
      <snapshots><enabled>true</enabled></snapshots>
    </repository>
  </repositories>
EOF
)
fi

cat > "$temporary_dir/settings.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <localRepository>$temporary_dir/local-repository</localRepository>
</settings>
EOF

cat > "$temporary_dir/project/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>smoke</groupId>
  <artifactId>vietnam-integrations-smoke-test</artifactId>
  <version>1.0.0</version>
  <properties>
    <maven.compiler.release>17</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
$repositories_block
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.github.hoangluongtran0309</groupId>
        <artifactId>vietnam-bom</artifactId>
        <version>$version</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.hoangluongtran0309</groupId>
      <artifactId>vietnam-vietqr-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>io.github.hoangluongtran0309</groupId>
      <artifactId>vietnam-zalo-oa-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>io.github.hoangluongtran0309</groupId>
      <artifactId>vietnam-einvoice-spring-boot-starter</artifactId>
    </dependency>
  </dependencies>
</project>
EOF

cat > "$temporary_dir/project/src/main/java/smoke/SmokeTest.java" <<'EOF'
package smoke;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.github.hoangluongtran0309.vietnam.einvoice.inspection.InvoiceInspector;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrDecoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrEncoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrPayload;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrRequest;
import io.github.hoangluongtran0309.vietnam.vietqr.reference.PaymentReferenceFormat;
import io.github.hoangluongtran0309.vietnam.zalo.InMemoryZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloAccountConfig;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaRegistry;

public final class SmokeTest {
    private SmokeTest() {
    }

    public static void main(String[] args) {
        VietQrEncoder encoder = new VietQrEncoder();
        String payload = encoder.encode(VietQrRequest.builder()
                .bankBin("970436")
                .accountNumber("0123456789")
                .amount(150_000)
                .purpose("DH123455")
                .build());
        VietQrPayload decoded = new VietQrDecoder().decode(payload);
        require("970436".equals(decoded.bankBin()), "VietQR bank BIN did not round-trip");
        require(Long.valueOf(150_000).equals(decoded.amount()), "VietQR amount did not round-trip");

        PaymentReferenceFormat references = PaymentReferenceFormat.of("DH");
        String reference = references.encode("12345");
        require(references.isValid(reference), "Payment reference is invalid");

        ZaloAccountConfig account = new ZaloAccountConfig(
                "default", "fake-app", "fake-secret", "fake-oa", null);
        ZaloOaRegistry registry = ZaloOaRegistry.builder(
                List.of(account), new InMemoryZaloTokenStore()).build();
        require("fake-oa".equals(registry.defaultOa().orElseThrow().oaId()),
                "Zalo registry did not select its only account");

        byte[] invoiceXml = """
                <HDon>
                  <DLHDon>
                    <TTChung>
                      <PBan>2.1.0</PBan><THDon>Synthetic sales invoice</THDon><KHMSHDon>2</KHMSHDon>
                      <KHHDon>K20TST</KHHDon><SHDon>1</SHDon><NLap>2020-01-02</NLap>
                      <HDDCKPTQuan>0</HDDCKPTQuan><DVTTe>VND</DVTTe>
                      <HTTToan>Cash</HTTToan><MSTTCGP>0000000000</MSTTCGP>
                    </TTChung>
                    <NDHDon>
                      <NBan><Ten>Sample seller</Ten><MST>0000000000</MST><DChi>Sample address</DChi></NBan>
                      <DSHHDVu><HHDVu><TChat>1</TChat><THHDVu>Sample item</THHDVu><ThTien>90</ThTien></HHDVu></DSHHDVu>
                      <TToan><TgTTTBSo>90</TgTTTBSo><TgTTTBChu>Ninety dong</TgTTTBChu></TToan>
                    </NDHDon>
                  </DLHDon>
                </HDon>
                """.getBytes(StandardCharsets.UTF_8);
        boolean isValid = InvoiceInspector.defaults().inspect(invoiceXml).report().isValid();
        System.out.println("E-invoice isValid: " + isValid);
        require(isValid, "Synthetic e-invoice failed validation");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
EOF

"$repo_root/mvnw" -B -s "$temporary_dir/settings.xml" -f "$temporary_dir/project/pom.xml" \
  package dependency:copy-dependencies -DincludeScope=runtime

java -cp "$temporary_dir/project/target/classes:$temporary_dir/project/target/dependency/*" smoke.SmokeTest

elapsed_seconds=$(( $(date +%s) - started_at ))
echo "SMOKE TEST OK $version (${elapsed_seconds}s)"
