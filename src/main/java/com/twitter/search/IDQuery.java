package com.twitter.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;

public class IDQuery extends Query {

  protected final Query inner;
  private final int id;
  private final ScorerAttributionCollector attrCollector;
  
  public IDQuery(Query inner, int id, ScorerAttributionCollector attrCollector) {
    this.inner = inner;
    this.id = id;
    this.attrCollector = attrCollector;
  }
  
  @Override
  public Weight createWeight(IndexSearcher searcher) throws IOException {
    Weight innerWeight = inner.createWeight(searcher);
    return new IDWeight(this, innerWeight, id, attrCollector);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    Query rewritten = inner.rewrite(reader);
    if (rewritten != inner) {
      return new IDQuery(rewritten, id, attrCollector);
    }
    return this;
  }
  
  @Override
  public String toString(String field) {
    return inner.toString(field);
  }
}
