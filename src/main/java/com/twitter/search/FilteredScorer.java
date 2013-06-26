package com.twitter.search;

import java.io.IOException;

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class FilteredScorer extends Scorer {

  protected final Scorer inner;
  
  public FilteredScorer(Weight weight, Scorer inner) {
    super(weight);
    this.inner = inner;
  }
  
  @Override
  public float score() throws IOException {
    return inner.score();
  }

  @Override
  public int freq() throws IOException {
    return inner.freq();
  }

  @Override
  public int docID() {
    return inner.docID();
  }

  @Override
  public int nextDoc() throws IOException {
    return inner.nextDoc();
  }

  @Override
  public int advance(int target) throws IOException {
    return inner.advance(target);
  }
}
