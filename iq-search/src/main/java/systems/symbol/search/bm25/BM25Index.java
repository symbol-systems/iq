package systems.symbol.search.bm25;

import systems.symbol.search.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * BM25 full-text search using Apache Lucene.
 * Phase 1: Foundation implementation. Phase 2: Advanced query syntax.
 */
public class BM25Index implements I_Index {
    private static final Logger log = LoggerFactory.getLogger(BM25Index.class);
    
    private final Directory directory = new ByteBuffersDirectory();
    private final Analyzer analyzer = new StandardAnalyzer();
    private IndexWriter writer;
    private IndexSearcher searcher;
    private final Map<String, IRI> iriBydocId = new HashMap<>();
    private int docId = 0;

    public BM25Index() {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setSimilarity(new org.apache.lucene.search.similarities.BM25Similarity(1.2f, 0.75f));
            this.writer = new IndexWriter(directory, config);
            refresh();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BM25Index", e);
        }
    }

    @Override
    public void index(IRI entity, String text, IRI concept) {
        if (text == null || text.isEmpty()) return;

        try {
            Document doc = new Document();
            String docIdStr = "doc-" + (docId++);
            
            doc.add(new StringField("id", docIdStr, Field.Store.YES));
            doc.add(new TextField("content", text, Field.Store.YES));
            doc.add(new StringField("entity", entity.stringValue(), Field.Store.YES));
            doc.add(new StringField("concept", concept.stringValue(), Field.Store.YES));
            
            writer.addDocument(doc);
            iriBydocId.put(docIdStr, entity);
            
            log.debug("bm25.index: {} (concept: {})", entity, concept);
        } catch (Exception e) {
            log.error("bm25.index.error", e);
        }
    }

    @Override
    public SearchResult search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            refresh();
            
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return new SearchResult(new ArrayList<>(), 
                    new SearchStats("bm25", writer.numDocs(), 0, 0, 0.0));
            }

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            
            // Main text query
            QueryParser parser = new QueryParser("content", analyzer);
            Query contentQuery = parser.parse(request.getQuery());
            queryBuilder.add(contentQuery, BooleanClause.Occur.SHOULD);
            
            // Concept filter
            if (request.getConcept() != null) {
                Query conceptQuery = new TermQuery(
                    new Term("concept", request.getConcept().stringValue()));
                queryBuilder.add(conceptQuery, BooleanClause.Occur.MUST);
            }

            TopDocs topDocs = searcher.search(queryBuilder.build(), request.getMaxResults());
            
            List<SearchHit> hits = new ArrayList<>();
            double totalScore = 0.0;
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                if (scoreDoc.score >= request.getMinScore()) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    String docStr = doc.get("id");
                    IRI entity = iriBydocId.get(docStr);
                    
                    if (entity != null) {
                        double normalizedScore = Math.min(1.0, scoreDoc.score / 10.0);
                        hits.add(new SearchHit(entity, normalizedScore, doc.get("content")));
                        totalScore += normalizedScore;
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double avgScore = hits.isEmpty() ? 0.0 : totalScore / hits.size();
            
            return new SearchResult(hits, 
                new SearchStats("bm25", writer.numDocs(), hits.size(), elapsed, avgScore));
        } catch (Exception e) {
            log.error("bm25.search.error", e);
            return new SearchResult(new ArrayList<>(), 
                new SearchStats("bm25", writer.numDocs(), 0, 
                    System.currentTimeMillis() - startTime, 0.0));
        }
    }

    @Override
    public void clear() {
        try {
            writer.deleteAll();
            iriBydocId.clear();
            docId = 0;
            log.info("bm25.cleared");
        } catch (Exception e) {
            log.error("bm25.clear.error", e);
        }
    }

    @Override
    public String getType() {
        return "bm25";
    }

    private void refresh() throws Exception {
        if (writer.hasUncommittedChanges()) {
            writer.commit();
        }
        IndexReader reader = DirectoryReader.open(directory);
        this.searcher = new IndexSearcher(reader);
    }

    public void close() throws Exception {
        if (writer != null) {
            writer.close();
        }
        if (searcher != null && searcher.getIndexReader() != null) {
            searcher.getIndexReader().close();
        }
        analyzer.close();
        directory.close();
    }
}
