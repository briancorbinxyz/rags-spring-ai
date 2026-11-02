package xyz.briancorbin.rags.answer;

import java.util.List;
import org.springframework.ai.document.Document;

/// Where an answer's passages come from, so {@link Archive} needs to know only that it can ask for
/// some: Episode 3's chunk store behind one adapter, the element search behind another.
///
/// Only used when a chat call fails before its advisor can report what it found, so that a model
/// failure is not counted as a retrieval miss. The advisor does the searching on every other call.
@FunctionalInterface
public interface Passages {

  /// The passages for a question, best first.
  List<Document> forQuestion(String question);
}
