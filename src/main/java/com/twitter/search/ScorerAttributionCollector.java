package com.twitter.search;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Scorer;

public interface ScorerAttributionCollector<E extends Enum<?>> {
  void newReaderContext(AtomicReaderContext ctx);
  void collectScorerAttribution(int docid, E queryEnum, Scorer sourceScorer);
}
