package com.twitter.search;

import java.io.IOException;

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class IDFilteredScorer<E extends Enum<E>> extends FilteredScorer {

  private final E queryEnum;
  private final ScorerAttributionCollector<E> attrCollector;
  
  public IDFilteredScorer(Weight weight, Scorer inner, E queryEnum, ScorerAttributionCollector<E> attrCollector) {
    super(weight, inner);
    this.queryEnum = queryEnum;
    this.attrCollector = attrCollector;
  }

  public E getEnum() {
    return queryEnum;
  }

  @Override
  public int nextDoc() throws IOException {
    int docid = super.nextDoc();
    if (attrCollector != null) {
      if (docid != NO_MORE_DOCS) {
        attrCollector.collectScorerAttribution(docid, queryEnum, this);
      }
    }
    return docid;
  }

  @Override
  public int advance(int target) throws IOException {
    int docid = super.advance(target);
    if (attrCollector != null) {
      if (docid != NO_MORE_DOCS) {
        attrCollector.collectScorerAttribution(docid, queryEnum, this);
      }
    }
    return docid;
  }
}
