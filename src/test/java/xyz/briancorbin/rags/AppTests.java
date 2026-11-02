package xyz.briancorbin.rags;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/// Loads the context without running anything, so it needs no Ollama and no checkout. Building the
/// element search reads the archive, so this asks for Episode 3's chunk store instead; which search
/// Beta gets by default is asserted in {@link ArchiveConfigurationTest}, which needs neither.
@SpringBootTest(properties = {"archive.run=none", "archive.search=chunks"})
class AppTests {

  @Test
  void contextLoads() {}
}
