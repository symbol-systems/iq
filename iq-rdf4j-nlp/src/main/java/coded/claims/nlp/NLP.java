package systems.symbol.nlp;

import systems.symbol.io.Fingerprint;
import systems.symbol.string.PrettyString;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class NLP {
    private final Logger log = LoggerFactory.getLogger( getClass() );
    ClassLoader cl;
    ValueFactory vf;

    LemmatizerModel lemas;
    TokenizerModel tokens;
    POSModel pos;
    SentenceModel sentence;

    public IRI nextToken, hasToken, hasSentence;
    IRI lemmaIRI, termIRI;
    double accuracy = 0.9;

    public NLP() throws IOException {
        this(Thread.currentThread().getContextClassLoader(), new ValidatingValueFactory());
    }

    public NLP(ValueFactory vf) throws IOException {
        this(Thread.currentThread().getContextClassLoader(), vf);
    }

    public NLP(ClassLoader cl, ValueFactory vf) throws IOException {
        this.cl = cl;
        this.vf = vf;
        this.lemmaIRI = vf.createIRI("http://wordnet-rdf.princeton.edu/rdf/lemma/");
        this.termIRI = vf.createIRI("http://wordnet-rdf.princeton.edu/ontology#");
        this.hasToken = vf.createIRI("urn:iq:nlp:hasToken");
        this.hasSentence = vf.createIRI("urn:iq:nlp:hasSentence");
        this.nextToken = vf.createIRI("urn:iq:nlp:nextToken");
        load();
    }

    public Model parse(String text) {
        return parse(new LinkedHashModel(), text);
    }

    public Model parse(Model model, String text) {
        IRI tIRI = vf.createIRI( "urn:iq:nlp:text:"+Fingerprint.identify(text) );
        return parse(model, tIRI, text);
    }

    public Model parse(Model model, IRI docIRI, String text) {
        String[] sentences = parseSentences(text);
        int sentence = 0;

        for(String s: sentences) {
            Collection<String> tokens = parseTokens(s);

            String sentencePath = docIRI.stringValue()+"/"+sentence;
            IRI sentenceIRI = vf.createIRI( sentencePath);
            model.add(vf.createStatement(sentenceIRI, RDFS.LABEL, vf.createLiteral(s)));
            // each Sentence is part of a Document
            model.add(vf.createStatement(docIRI, hasSentence, sentenceIRI));

            String[] tokenStrings = tokens.stream().toArray(String[]::new);
            log.trace("iq.nlp.parse.tokens: "+String.join("|", tokenStrings));
            Map<String, String> partOfSpeech = parsePartOfSpeech(tokenStrings);

            IRI lastTokenIRI = null;
            for(int i=0;i<tokenStrings.length;i++) {
                IRI tokenIRI = vf.createIRI(sentencePath+"/"+i);
                // each Token is part of a Sentence
                model.add(vf.createStatement(sentenceIRI, hasToken, tokenIRI));

                String token = tokenStrings[i];
                IRI lemmaIRI = toLemmaIRI(token.toLowerCase(Locale.ROOT));
                // each Token is a kind of Lemma
                model.add(vf.createStatement(tokenIRI, RDFS.SUBCLASSOF, lemmaIRI));

                // each Token is a Part of Speech
                String tokenPoS = partOfSpeech.get(token);
                if (tokenPoS!=null) {
                    IRI kindTokenPoS = toTermIRI(tokenPoS);
                    log.trace("iq.nlp.parse.token: "+token+" -> "+tokenPoS);
                    model.add(vf.createStatement(tokenIRI, RDF.TYPE, kindTokenPoS));
                    if (lastTokenIRI!=null) model.add(vf.createStatement(lastTokenIRI, this.nextToken, tokenIRI));
                    lastTokenIRI = tokenIRI;
                }
            }
            sentence++;
        }
        return model;
    }

    protected Collection parseTokens(String text) {
        TokenizerME tokenizer = new TokenizerME(tokens);
        String[] strings = tokenizer.tokenize(text);
//        log.info("iq.nlp.parse.strings: "+String.join(" | ", strings));
        return relevant(strings, tokenizer.getTokenProbabilities());
    }

    private Collection relevant(String[] strings, double[] probabilities) {
        Collection<String> found = new ArrayList();
        for(int i=0;i<strings.length;i++) {
            if (probabilities[i]>=accuracy) found.add(strings[i]);
        }
        return found;
    }


    public Map<String, String> parsePartOfSpeech(String text) {
        TokenizerME tokenizer = new TokenizerME(tokens);
        return parsePartOfSpeech(tokenizer.tokenize(text));
    }

    protected Map<String, String> parsePartOfSpeech(String[] tokens) {
//        log.info("nlp.pos.parsePartOfSpeech.tokens: "+String.join("|", tokens));

        Map<String, String> map = new HashMap<>();

        POSTaggerME tagger = new POSTaggerME(pos);
        String tags[] = tagger.tag(tokens);
//        log.info("nlp.pos.parsePartOfSpeech.tags: "+String.join("|", tags));

        double[] probabilities = tagger.probs();
//        log.info("nlp.pos.parsePartOfSpeech.probabilities: "+probabilities);

        for(int i=0;i<tags.length;i++) {
            if (!tags[i].equals("PUNCT")) {
//            log.info("nlp.pos.each: "+i +"->"+ tokens[i]+"->"+ tags[i]+"->"+ probabilities[i]);
//                log.info("nlp.pos.each: %s: %s -> %s = %s\n", i, tokens[i], tags[i], probabilities[i]);
                if (probabilities[i]>accuracy)
                    map.put(tokens[i], tags[i]);
                else
                    map.put(tokens[i], tags[i]+"_GUESS");
            }
        }
//log.info("nlp.pos.map: "+map);
        return map;
    }

    protected String[] parseSentences(String text) {
        SentenceDetectorME tagger = new SentenceDetectorME(sentence);
        return tagger.sentDetect(text);
    }

    protected void load() throws IOException {
        loadLanguageDetect(load("nlp/langdetect.bin"));
        loadSentenceDetect(load("nlp/opennlp-en-ud-ewt-sentence.bin"));
        loadPartOfSpeech(load("nlp/opennlp-en-ud-ewt-pos.bin"));
        loadTokens(load("nlp/opennlp-en-ud-ewt-tokens.bin"));
    }

    protected void loadPartOfSpeech(InputStream in) throws IOException {
        pos = new POSModel(in);
        in.close();
    }

    protected void loadSentenceDetect(InputStream in) throws IOException {
        if (in==null || in.available()<1) return;
        sentence = new SentenceModel(in);
        in.close();
    }

    protected void loadTokens(InputStream in) throws IOException {
        if (in==null || in.available()<1) return;
        tokens = new TokenizerModel(in);
        in.close();
    }

    protected void loadLanguageDetect(InputStream in) throws IOException {
        if (in==null || in.available()<1) return;
        in.close();
    }

    protected void loadLemma(InputStream in) throws IOException {
        if (in==null || in.available()<1) return;
        lemas = new LemmatizerModel(in);
        in.close();
    }

    public InputStream load(String path) {
        return cl.getResourceAsStream(path);
    }

    protected Map<String, Double> map(String[] strings, double[] probabilities) {
        Map<String, Double> map = new HashMap();
        for(int i=0;i<strings.length;i++) {
            map.put(strings[i], probabilities[i]);
        }
        return map;
    }

    public IRI toLemmaIRI(String t) {
        return vf.createIRI(this.lemmaIRI.stringValue()+PrettyString.sanitize(t,""));
    }

    public IRI toTermIRI(String t) {
        return vf.createIRI(this.termIRI.stringValue()+t);
    }
}