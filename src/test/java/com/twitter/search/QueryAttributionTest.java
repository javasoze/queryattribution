package com.twitter.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.Version;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryAttributionTest {
  
  static final long[] docset1 = new long[]{1,2,3,4,5,6,7,8,9,10};
  static final long[] docset2 = new long[]{1,3,5,7,9};
  static final long[] docset3 = new long[]{2,4,6,8,10};
  
  static enum QueryTypes {
	  Type0,
	  Type1,
	  Type2,
	  Type3,
	  Type4
  }
  
  static class IDScorerAttributionCollector implements ScorerAttributionCollector<QueryTypes> {

    private HashMap<Integer, Long> collectedMap = new HashMap<Integer, Long>();
    private int docbase = 0;
    
    private QueryTypes[] queryValues = QueryTypes.values();
    
    @Override
    public void newReaderContext(AtomicReaderContext ctx) {
      docbase = ctx.docBase;
      
    }
    
    @Override
    public void collectScorerAttribution(int docid, QueryTypes queryEnum, Scorer sourceScorer) {
      int key = docbase+docid;
      
      int queryid = queryEnum.ordinal();
      Long val = collectedMap.get(key);
      long mask = 0x1 << queryid;
      if (val == null) {
        collectedMap.put(key, mask);
      }
      else {
        long longVal = val.longValue();
        longVal |= mask;
        collectedMap.put(key, longVal);
      }
    }
    
    public EnumSet<QueryTypes> getEnumSet(int docid, int docbase) {
    	int key = docid+docbase;
        Long val = collectedMap.get(key);
        EnumSet<QueryTypes> enumSet = EnumSet.noneOf(QueryTypes.class);
        ArrayList<Integer> idList = new ArrayList<Integer>();
        if (val == null) {
            return enumSet;
        }

    	
        long longVal = val.longValue();
        int i = 0;
        while (longVal != 0) {
          if ((longVal & 1L) != 0L) {
        	enumSet.add(queryValues[i]);
            idList.add(i);
          }
          longVal>>=1;
          i++;
        }
        return enumSet;
    }
    /*
    public List<Integer> getQueryIds(int docid, int docbase) {
      int key = docid+docbase;
      Long val = collectedMap.get(key);
      ArrayList<Integer> idList = new ArrayList<Integer>();
      if (val == null) {
        return idList;
      }
      long longVal = val.longValue();
      int i = 0;
      while (longVal != 0) {
        if ((longVal & 1L) != 0L) {
          idList.add(i);
        }
        longVal>>=1;
        i++;
      }
      return idList;
    }*/
    
    @Override
    public String toString() {
      return collectedMap.toString();
    }
  }
  
  static Directory dir = new RAMDirectory();
  
  static void createFakeIndex() throws Exception{
    IndexWriterConfig writerCfg = new IndexWriterConfig(Version.LUCENE_41, new StandardAnalyzer(Version.LUCENE_41));
    IndexWriter writer = new IndexWriter(dir, writerCfg);
    for (int i=0;i<20;++i) {
      Document doc = new Document();
      Field f = new TextField("number", String.valueOf(i), Store.NO);
      doc.add(f);
      writer.addDocument(doc);
    }
    writer.commit();
    writer.close();
  }
  
  @BeforeClass
  public static void init() throws Exception{
    createFakeIndex();
  }
  
  static int executeQuery(Query query) throws Exception{
    DirectoryReader reader = DirectoryReader.open(dir);
    IndexSearcher searcher = new IndexSearcher(reader);
    
    TopDocs td = searcher.search(query, 100);
    reader.close();
    
    return td.totalHits;
  }
  
  static Query buildQuery(long[] docs) {
    
    final OpenBitSet bs = new OpenBitSet();
    for (long doc : docs) {
      bs.set(doc);
    }
    
    Filter f = new Filter() {
      
      @Override
      public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs)
          throws IOException {
        return bs;
      }
    };
    
    return new ConstantScoreQuery(f);
  }

  @Test
  public void testNullAttrCollector() throws Exception{
    Query q1 = buildQuery(docset1);
    Query q2 = buildQuery(docset2);
    Query q3 = buildQuery(docset3);
    
    Query f2 = new IDQuery<QueryTypes>(q2, QueryTypes.Type0, null);
    Query f3 = new IDQuery<QueryTypes>(q3, QueryTypes.Type1, null);
    Query f1 = new IDQuery<QueryTypes>(q1, QueryTypes.Type2, null);
    
    BooleanQuery bq = new BooleanQuery();
    bq.add(f2, Occur.SHOULD);
    bq.add(f3, Occur.SHOULD);
    bq.add(f1, Occur.MUST);
    
    int numhits = executeQuery(bq); 

    TestCase.assertEquals(10, numhits);
  }
  
  @Test
  public void testSimpleAttrCollection() throws Exception{
    IDScorerAttributionCollector attrCollector = new IDScorerAttributionCollector();
    
    Query q2 = buildQuery(docset2);
    Query q3 = buildQuery(docset3);
    
    Query f2 = new IDQuery<QueryTypes>(q2, QueryTypes.Type0, attrCollector);
    Query f3 = new IDQuery<QueryTypes>(q3, QueryTypes.Type1, attrCollector);
    
    BooleanQuery bq = new BooleanQuery();
    bq.add(f2, Occur.SHOULD);
    bq.add(f3, Occur.SHOULD);
    
    int numhits = executeQuery(bq);
    
    TestCase.assertEquals(10, numhits);
    for (int i=1; i<11;++i) {
      EnumSet<QueryTypes> queryIds = attrCollector.getEnumSet(i, 0);
      TestCase.assertEquals(1, queryIds.size());
            
      if (i%2==0) {
        TestCase.assertTrue(queryIds.contains(QueryTypes.Type1));
      }
      else {
        TestCase.assertTrue(queryIds.contains(QueryTypes.Type0));
      }
    }
  }
  
  @Test
  public void testComplexAttrCollection() throws Exception {
    IDScorerAttributionCollector attrCollector = new IDScorerAttributionCollector();
    
    Query q1 = buildQuery(docset1);
    Query q2 = buildQuery(docset2);
    Query q3 = buildQuery(docset3);
    
    
    Query f2 = new IDQuery<QueryTypes>(q2, QueryTypes.Type0, attrCollector);
    Query f3 = new IDQuery<QueryTypes>(q3, QueryTypes.Type1, attrCollector);
    Query f1 = new IDQuery<QueryTypes>(q1, QueryTypes.Type2, attrCollector);
    
    BooleanQuery bq = new BooleanQuery();
    bq.add(f2, Occur.SHOULD);
    bq.add(f3, Occur.SHOULD);
    
    Query f4 = new IDQuery<QueryTypes>(bq, QueryTypes.Type3, attrCollector);
    
    BooleanQuery bq2 = new BooleanQuery();
    bq2.add(f4, Occur.MUST);
    bq2.add(f1, Occur.MUST);
    
    
    Query f5 = new IDQuery<QueryTypes>(bq2, QueryTypes.Type4, attrCollector);
    
    int numhits = executeQuery(f5);
    
    TestCase.assertEquals(10, numhits);
    
    for (int i=1; i<11;++i) {
      EnumSet<QueryTypes> queryIds = attrCollector.getEnumSet(i, 0);
      TestCase.assertFalse(queryIds.isEmpty());
      TestCase.assertEquals(4, queryIds.size());
      
      TestCase.assertTrue(queryIds.contains(QueryTypes.Type4));
      TestCase.assertTrue(queryIds.contains(QueryTypes.Type2));
      TestCase.assertTrue(queryIds.contains(QueryTypes.Type3));
      
      if (i%2==0) {
        TestCase.assertTrue(queryIds.contains(QueryTypes.Type1));
      }
      else {
        TestCase.assertTrue(queryIds.contains(QueryTypes.Type0));
      }
    }
  }
}
