package com.twitter.search;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;

public class IDWeight<E extends Enum<?>> extends Weight {

  protected final Weight inner;
  private final E queryEnum;
  private final ScorerAttributionCollector<E> attrCollector;
  private final IDQuery<E> query;
  
  public IDWeight(IDQuery<E> query, Weight inner, E queryEnum, ScorerAttributionCollector<E> attrCollector) {
    this.query = query;
    this.inner = inner;
    this.queryEnum = queryEnum;
    this.attrCollector = attrCollector; 
  }
  
  @Override
  public Explanation explain(AtomicReaderContext context, int doc)
      throws IOException {
    return inner.explain(context, doc);
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public float getValueForNormalization() throws IOException {
    return inner.getValueForNormalization();
  }

  @Override
  public void normalize(float norm, float topLevelBoost) {
    inner.normalize(norm, topLevelBoost);
  }

  @Override
  public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder,
      boolean topScorer, Bits acceptDocs) throws IOException {
    if (attrCollector != null) {
      attrCollector.newReaderContext(context);
    }
    Scorer innerScorer = inner.scorer(context, scoreDocsInOrder, topScorer, acceptDocs);
    return new IDFilteredScorer<E>(this, innerScorer, queryEnum, attrCollector);
  }

}
