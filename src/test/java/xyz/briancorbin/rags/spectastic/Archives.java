package xyz.briancorbin.rags.spectastic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// A three-page archive in spectastic's shape, small enough to reason about by hand. FR-002 is
/// superseded by its status attribute; D-001 only by a status element inside it, as spec 004's
/// D-005 is, so the lifecycle filter can't see it and the redirect has to.
public final class Archives {

  private Archives() {}

  public static Path tiny(Path dir) throws IOException {
    demoSpec(dir);
    aChangeProposalThatApplied(dir);
    anotherSpecWithTheSameId(dir);
    return dir;
  }

  /// FR-002 is superseded by an attribute the filter can read; D-001 only by a status element
  /// inside it, which the filter cannot see and the redirect has to find.
  private static void demoSpec(Path dir) throws IOException {
    write(dir, "specs/001-demo/spec.html", page("001 Demo", REQUIREMENTS + DECISIONS));
  }

  private static final String REQUIREMENTS =
      """
      <spec-meta><b>Status</b> <span><spec-status value="accepted">Accepted</spec-status></span></spec-meta>
      <section id="requirements"><h2>Requirements</h2>
      <p>The requirements for the demo service follow.</p>
      <ul>
      <li id="FR-001">The service MUST record every request.</li>
      <li id="FR-002" data-status="superseded">The service MUST log to a file. Superseded by <a href="#FR-003">FR-003</a>.</li>
      <li id="FR-003">The service MUST log to standard output.</li>
      <li id="FR-004">Too short</li>
      </ul>
      </section>
      """;

  private static final String DECISIONS =
      """
      <section id="decisions"><h2>Decisions</h2>
      <div id="D-001"><h4>D-001 · Rotate logs daily</h4>
        <dl><dt>Status</dt><dd><spec-status value="superseded">Superseded</spec-status> by <a href="#D-002">D-002</a></dd></dl>
        <p>Logs rotate once a day.</p></div>
      <div id="D-002"><h4>D-002 · Rotate logs by size</h4>
        <p>Logs rotate when they reach ten megabytes.</p></div>
      </section>
      """;

  /// One page in spectastic's shape, so a fixture says what it is testing rather than boilerplate.
  private static String page(String title, String body) {
    return """
        <!doctype html><html><head><title>%s</title></head><body><main>
        %s</main></body></html>
        """
        .formatted(title, body);
  }

  private static void aChangeProposalThatApplied(Path dir) throws IOException {
    write(
        dir,
        "specs/001-demo/changes/archive/2026-01-01-log-to-stdout/proposal.html",
        """
        <!doctype html><html><head><title>Change: log to standard output</title></head><body>
        <spec-delta target="FR-003"><div id="FR-003">The service MUST log to standard output.</div></spec-delta>
        </body></html>
        """);
  }

  private static void anotherSpecWithTheSameId(Path dir) throws IOException {
    write(
        dir,
        "specs/002-other/spec.html",
        """
        <!doctype html><html><head><title>002 Other</title></head><body>
        <section id="requirements"><h2>Requirements</h2>
        <ul><li id="FR-001">The other service MUST answer within a second.</li></ul>
        </section>
        </body></html>
        """);
  }

  /// Two specs numbered past 999, each holding the same id. The archive is three digits wide today,
  /// so nothing here is read from spectastic; it exists to keep the lookup honest if that changes.
  public static Path pastNineNinetyNine(Path dir) throws IOException {
    write(
        dir,
        "specs/1000-first/spec.html",
        """
        <!doctype html><html><head><title>1000 First</title></head><body><main>
        <section id="requirements"><h2>Requirements</h2>
        <ul><li id="FR-009">The first service MUST retry a failed upload twice.</li></ul>
        </section>
        </main></body></html>
        """);
    write(
        dir,
        "specs/1001-second/spec.html",
        """
        <!doctype html><html><head><title>1001 Second</title></head><body><main>
        <section id="requirements"><h2>Requirements</h2>
        <ul><li id="FR-009">The second service MUST reject an upload over one gigabyte.</li></ul>
        </section>
        </main></body></html>
        """);
    return dir;
  }

  private static void write(Path dir, String file, String html) throws IOException {
    Path p = dir.resolve(file);
    Files.createDirectories(p.getParent());
    Files.writeString(p, html);
  }
}
