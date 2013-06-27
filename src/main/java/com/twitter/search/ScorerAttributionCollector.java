package com.twitter.search;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Scorer;

public interface ScorerAttributionCollector {
  void newReaderContext(AtomicReaderContext ctx);
  void collectScorerAttribution(int docid, int queryid, Scorer sourceScorer);
}
