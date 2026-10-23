package xyz.briancorbin.rags.answer;

import xyz.briancorbin.rags.units.Address;

/// A quotation the program has traced to a retrieved chunk.
///
/// Only quotations that appear, word for word, in something the retriever brought back become a
/// `Quote`. The address comes from that chunk's metadata, never from the model.
///
/// @param label how Answerer cites it, e.g. `Q2`
/// @param text the quotation as Quoter wrote it
/// @param address where it came from
/// @param rank the retrieved chunk's position in the search results, counting from 1
public record Quote(String label, String text, Address address, int rank) {

  /// How the quotation is shown to Answerer: label, address, then the words.
  public String toPrompt() {
    return "%s (%s)%n\"%s\"".formatted(label, address, text);
  }
}
