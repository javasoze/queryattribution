package com.twitter.search;

import org.apache.lucene.search.Scorer;

public interface ScorerAttributionCollector {
  void collectScorerAttribution(int docbase, int docid, int queryid, Scorer sourceScorer);
}
