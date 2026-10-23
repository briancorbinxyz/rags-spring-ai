package xyz.briancorbin.rags;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import xyz.briancorbin.rags.eval.GoldenSet;

/// The seams, asserted.
///
/// Every rule here was true when it was written. They exist because the next change will be made by
/// someone who has not read the article explaining why the search does not know what a spec is, and
/// a convention nothing checks is one that lasts until somebody is in a hurry.
///
/// ArchUnit reads bytecode, so a {@code {@link}} in a comment is not a dependency and does not
/// count here — which is the right answer: a mention is not a coupling.
class ArchitectureTest {

  private static final String ROOT = "xyz.briancorbin.rags";
  private static final String SPECTASTIC = ROOT + ".spectastic";

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  /// The point of the whole exercise. Retrieval may name the ports — `Standing`, `Successors`,
  /// `IdentifierGrammar` — but never the archive that answers them.
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

  /// Scoring a run reads its results; it is never what produces them.
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

  /// The document model is the bottom of the stack: it waits to be used, it does not reach up.
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

  /// The two personas read quotations; they have no business knowing how retrieval works.
  @Test
  void theAnswerSideDoesNotKnowHowRetrievalWorks() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".answer")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ROOT + ".search", SPECTASTIC)
        .because(
            "Answerer and Quoter are handed passages; where they came from is not their concern")
        .check(CLASSES);
  }

  /// Two packages that each need the other are one package wearing a disguise.
  @Test
  void thePackagesDoNotDependOnEachOtherInCircles() {
    slices()
        .matching(ROOT + ".(*)..")
        .should()
        .beFreeOfCycles()
        .because("a cycle means the boundary is decorative")
        .check(CLASSES);
  }

  /// At most this many constructor parameters before the call site stops being readable.
  private static final int MANY = 8;

  /// More than a handful of things handed to a constructor is a builder waiting to be written, and
  /// a record's canonical constructor takes every component — which is why this lives here rather
  /// than in Checkstyle, whose `ParameterNumber` refuses `RECORD_DEF` and so cannot see a record at
  /// all. ArchUnit reads bytecode, where a record is a class with a constructor.
  @Test
  void nothingTakesMoreThanAHandfulOfThingsAtOnce() {
    constructors()
        .that()
        // A question's components are the keys of a frozen file on disk, so their number is the
        // golden set's to decide and not ours. Grouping them would change the file.
        .areNotDeclaredIn(GoldenSet.Question.class)
        .should(takeAtMost(MANY))
        .because(
            "a row of positional arguments is a mistake waiting to happen: group them, or give it"
                + " a builder and let the call site say which is which")
        .check(CLASSES);
  }

  private static ArchCondition<JavaConstructor> takeAtMost(int many) {
    return new ArchCondition<>("take at most " + many + " parameters") {
      @Override
      public void check(JavaConstructor constructor, ConditionEvents events) {
        int taken = constructor.getRawParameterTypes().size();
        events.add(
            new SimpleConditionEvent(
                constructor,
                taken <= many,
                "%s takes %d".formatted(constructor.getOwner().getSimpleName(), taken)));
      }
    };
  }

  /// At most this many parameters on anything a caller outside the class can see.
  private static final int FEW = 3;

  /// Three for the visible surface. Checkstyle counts four for everything, because it cannot tell a
  /// private helper from public API — its xpath filter will not suppress by check id — and ArchUnit
  /// reads the modifiers, so the tighter half of the rule lives here.
  @Test
  void whatIsVisibleOutsideAClassTakesFewThings() {
    methods()
        .that()
        .arePublic()
        .and()
        .areNotDeclaredIn(GoldenSet.Question.class)
        .should(takeAtMostArguments(FEW))
        .because("past three, group the arguments or give it a builder")
        .check(CLASSES);
  }

  private static ArchCondition<JavaMethod> takeAtMostArguments(int few) {
    return new ArchCondition<>("take at most " + few + " parameters") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        int taken = method.getRawParameterTypes().size();
        events.add(
            new SimpleConditionEvent(
                method,
                taken <= few,
                "%s.%s takes %d"
                    .formatted(method.getOwner().getSimpleName(), method.getName(), taken)));
      }
    };
  }
}
