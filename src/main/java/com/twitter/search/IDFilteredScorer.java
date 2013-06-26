package com.twitter.search;

import java.io.IOException;

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class IDFilteredScorer extends FilteredScorer {

  private final int id;
  private final ScorerAttributionCollector attrCollector;
  private final int docbase;
  
  public IDFilteredScorer(Weight weight, Scorer inner, int docbase, int id, ScorerAttributionCollector attrCollector) {
    super(weight, inner);
    this.docbase = docbase;
    this.id = id;
    this.attrCollector = attrCollector;
  }

  public int getId() {
    return id;
  }

  @Override
  public int nextDoc() throws IOException {
    int docid = super.nextDoc();
    if (attrCollector != null) {
      if (docid != NO_MORE_DOCS) {
        attrCollector.collectScorerAttribution(docbase, docid, id, this);
      }
    }
    return docid;
  }

  @Override
  public int advance(int target) throws IOException {
    int docid = super.advance(target);
    if (attrCollector != null) {
      if (docid != NO_MORE_DOCS) {
        attrCollector.collectScorerAttribution(docbase, docid, id, this);
      }
    }
    return docid;
  }
}
