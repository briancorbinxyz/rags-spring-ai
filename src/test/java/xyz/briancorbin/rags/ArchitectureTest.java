package xyz.briancorbin.rags;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * The seams, asserted.
 *
 * <p>Every rule here was true when it was written. They exist because the next change will be made
 * by someone who has not read the article explaining why the search does not know what a spec is,
 * and a convention nothing checks is one that lasts until somebody is in a hurry.
 *
 * <p>ArchUnit reads bytecode, so a {@code {@link}} in a comment is not a dependency and does not
 * count here — which is the right answer: a mention is not a coupling.
 */
class ArchitectureTest {

  private static final String ROOT = "xyz.briancorbin.rags";
  private static final String SPECTASTIC = ROOT + ".spectastic";

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  /**
   * The point of the whole exercise. Retrieval may name the ports — {@code Standing}, {@code
   * Successors}, {@code IdentifierGrammar} — but never the archive that answers them.
   */
  @Test
  void theSearchDoesNotKnowWhoseArchiveItIs() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".search")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(SPECTASTIC)
        .because(
            "a second corpus should need a new adapter, not edits to the search: that is the whole"
                + " claim this separation makes")
        .check(CLASSES);
  }

  /** Scoring a run reads its results; it is never what produces them. */
  @Test
  void theSearchDoesNotDependOnItsOwnScoring() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".search")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".eval")
        .because("a search that knows how it is marked can be tuned to the mark")
        .check(CLASSES);
  }

  /** The document model is the bottom of the stack: it waits to be used, it does not reach up. */
  @Test
  void theUnitModelDoesNotReachUpwards() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".units")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ROOT + ".search", ROOT + ".eval", ROOT + ".answer")
        .because("a unit is what the search is given, so it cannot be built out of the search")
        .check(CLASSES);
  }

  /** The two personas read quotations; they have no business knowing how retrieval works. */
  @Test
  void theAnswerSideDoesNotKnowHowRetrievalWorks() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".answer")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ROOT + ".search", SPECTASTIC)
        .because("Alpha and Beta are handed passages; where they came from is not their concern")
        .check(CLASSES);
  }

  /** Two packages that each need the other are one package wearing a disguise. */
  @Test
  void thePackagesDoNotDependOnEachOtherInCircles() {
    slices()
        .matching(ROOT + ".(*)..")
        .should()
        .beFreeOfCycles()
        .because("a cycle means the boundary is decorative")
        .check(CLASSES);
  }
}
