package com.twitter.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;

public class IDQuery<E extends Enum<E>> extends Query {

  protected final Query inner;
  private final E queryEnum;
  private final ScorerAttributionCollector<E> attrCollector;
  
  public IDQuery(Query inner,E queryEnum, ScorerAttributionCollector<E> attrCollector) {
    this.inner = inner;
    this.queryEnum = queryEnum;
    this.attrCollector = attrCollector;
  }
  
  @Override
  public Weight createWeight(IndexSearcher searcher) throws IOException {
    Weight innerWeight = inner.createWeight(searcher);
    return new IDWeight<E>(this, innerWeight, queryEnum, attrCollector);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    Query rewritten = inner.rewrite(reader);
    if (rewritten != inner) {
      return new IDQuery<E>(rewritten, queryEnum, attrCollector);
    }
    return this;
  }
  
  @Override
  public String toString(String field) {
    return inner.toString(field);
  }
}
