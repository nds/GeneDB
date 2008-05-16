package org.genedb.db.domain.luceneImpls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.genedb.db.domain.objects.BasicGene;
import org.genedb.db.domain.objects.Chromosome;
import org.genedb.db.domain.objects.Exon;
import org.genedb.db.domain.objects.Transcript;
import org.genedb.db.domain.services.BasicGeneService;

public class BasicGeneServiceImpl implements BasicGeneService {

    private IndexReader luceneIndex;
    private static Logger log = Logger.getLogger(BasicGeneService.class);

    public BasicGeneServiceImpl(IndexReader luceneIndex) {
        this.luceneIndex = luceneIndex;
    }

    /**
     * Defines a conversion from a Lucene Document to some other
     * class of object. A DocumentConverter can also function as a filter if
     * desired, by returning <code>null</code> from the convert method to
     * indicate that a particular document should be ignored.
     * 
     * @author rh11
     * 
     * @param <T> The return type of the conversion
     */
    private interface DocumentConverter<T> {
        /**
         * Convert the document to the desired form.
         * 
         * @param doc  the document to convert
         * @return     the result of the conversion, or null if this document should be ignored
         */
        T convert(Document doc);
    }
    
    /**
     * Convert the document to a Transcript object. If the <code>_hibernate_class</code>
     * of the document is not <code>org.gmod.schema.sequence.Mrna</code>, returns null.
     */
    private final DocumentConverter<Transcript> convertToTranscript = new DocumentConverter<Transcript>() {
        public Transcript convert(Document doc) {
            if (!doc.get("_hibernate_class").equals("org.gmod.schema.sequence.Mrna"))
                return null;
            
            Transcript transcript = new Transcript();
            String colourString = doc.get("colour");
            if (colourString.equals("null"))
                transcript.setColourId(null);
            else
                transcript.setColourId(Integer.parseInt(colourString));
            
            String transcriptName = doc.get("name");
            if (transcriptName != null)
                transcript.setName(transcriptName);
            else
                transcript.setName(doc.get("uniqueName"));

            transcript.setFmin(Integer.parseInt(doc.get("fmin")));
            transcript.setFmin(Integer.parseInt(doc.get("fmax")));
            transcript.setExons(parseExonLocs(doc.get("exonlocs")));
            
            return transcript;
        }
    };
    
    /**
     * This DocumentConverter populates a BasicGene object using the Lucene
     * Document. Currently it makes a new Lucene query for every gene to pull
     * back the associated transcripts. Should this prove unacceptable, the
     * associated transcripts could instead all be loaded at once.
     * 
     * If the <code>_hibernate_class</code> is not equal to <code>org.gmod.schema.sequence.Gene</code>,
     * returns null.
     */
    private final DocumentConverter<BasicGene> convertToGene = new DocumentConverter<BasicGene>() {
        public BasicGene convert(Document doc) {
            if (!doc.get("_hibernate_class").equals("org.gmod.schema.sequence.Gene"))
                return null;

            BasicGene ret = new BasicGene();

            String geneUniqueName = doc.get("uniqueName");
            ret.setOrganism(doc.get("organism.commonName"));
            ret.setFeatureId(Integer.parseInt(doc.get("featureId")));
            ret.setSystematicId(geneUniqueName);
            ret.setName(doc.get("name"));
            ret.setChromosome(new Chromosome(doc.get("chr"), Integer.parseInt(doc.get("chrlen"))));
            ret.setOrganism(doc.get("organism.commonName"));
            ret.setFmin(Integer.parseInt(doc.get("start")));
            ret.setFmax(Integer.parseInt(doc.get("stop")));
            ret.setSynonyms(Arrays.asList(doc.get("synonym").split("\t")));

            BooleanQuery transcriptQuery = new BooleanQuery();
            transcriptQuery.add(new TermQuery(new Term("_hibernate_class", "org.gmod.schema.sequence.Mrna")),
                BooleanClause.Occur.MUST);
            transcriptQuery.add(new TermQuery(new Term("gene", geneUniqueName)),
                BooleanClause.Occur.MUST);            

            List<Transcript> transcripts = findWithQuery(transcriptQuery, convertToTranscript);
            ret.setTranscripts(transcripts);

            return ret;
        }
    };
    
    private static Set<Exon> parseExonLocs(String exonLocs) {
        Set<Exon> exons = new HashSet<Exon>();
        for (String exonSpec: exonLocs.split(",")) {
            int numberStart = 0;
            int numberEnd = exonSpec.length();
            if (exonSpec.startsWith("(")) {
                if (!exonSpec.endsWith(")"))
                    throw new IllegalArgumentException(String.
                        format("Exon location '%s' starts with '(' but does not end with ')'; from string '%s'", exonSpec, exonLocs));
                numberStart++;
                numberEnd--;
            }
            int dots = exonSpec.indexOf("..");
            if (dots < 1)
                throw new IllegalArgumentException(String.format("Failed to parse exon location '%s' from string '%s'", exonSpec, exonLocs));
            int exonStart = Integer.parseInt(exonSpec.substring(numberStart, dots));
            int exonStop  = Integer.parseInt(exonSpec.substring(dots+2, numberEnd));
            exons.add(new Exon(exonStart, exonStop));
        }
        return exons;
    }

    /**
     * Finds all documents matching the query, and makes a list of matches. Each
     * document is converted to an object of type T using the converter.
     * 
     * @param <T> The return type
     * @param query The query object
     * @param converter Result converter
     * @return
     */
    private <T> List<T> findWithQuery(Query query, DocumentConverter<T> converter) {
        List<T> ret = new ArrayList<T>();

        IndexSearcher searcher = new IndexSearcher(luceneIndex);
        log.debug("searcher is -> " + searcher.toString());

        Hits hits;
        try {
            hits = searcher.search(query);
        } catch (IOException e) {
            throw new RuntimeException("IOException while running Lucene query", e);
        }

        @SuppressWarnings("unchecked")
        Iterator<Hit> it = hits.iterator();
        while (it.hasNext()) {
            Document doc;
            try {
                doc = it.next().getDocument();
            } catch (CorruptIndexException e) {
                throw new RuntimeException("Lucene index is corrupted!", e);
            } catch (IOException e) {
                throw new RuntimeException("IOException while fetching results of Lucene query", e);
            }
            T convertedDocument = converter.convert(doc);
            if (convertedDocument != null)
                ret.add(convertedDocument);
        }
        return ret;
    }

    private <T> T findUniqueWithQuery(Query query, DocumentConverter<T> converter) {
        List<T> results = findWithQuery(query, converter);
        int numberOfResults = results.size();
        if (numberOfResults == 0) {
            log.info(String.format("Failed to find gene matching Lucene query '%s'", query));
            return null;
        } else if (numberOfResults > 1) {
            log.error(String.format("Found %d genes matching query '%s'; expected only one!",
                numberOfResults, query));
        }
        return results.get(0);
    }

    public BasicGene findGeneByUniqueName(String name) {
        return findUniqueWithQuery(new TermQuery(new Term("uniqueName", name.toLowerCase())), convertToGene);
    }

    /**
     * Warning: this method is liable to be very slow, because it results
     * in a Lucene query of the form *foo*: the problem is the initial wildcard.
     */
    public List<String> findGeneNamesByPartialName(String search) {
        return findWithQuery(new WildcardQuery(new Term("uniqueName", String.format("*%s*", search.toLowerCase()))),
            new DocumentConverter<String>() {
                public String convert(Document doc) {
                    return doc.get("uniqueName");
                }
            });
    }

    public Collection<BasicGene> findGenesOverlappingRange(String organismCommonName,
            String chromosomeUniqueName, int strand, long locMin, long locMax) {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term("_hibernate_class", "org.gmod.schema.sequence.Gene")),
            BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("organism.commonName", organismCommonName)),
            BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("chr", chromosomeUniqueName)),
            BooleanClause.Occur.MUST);
        query.add(new ConstantScoreRangeQuery("start", null, String.format("%09d", locMax), false, false),
            BooleanClause.Occur.MUST);
        query.add(new ConstantScoreRangeQuery("stop", String.format("%09d", locMin), null, true, false),
            BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("strand", String.valueOf(strand))),
            BooleanClause.Occur.MUST);

        return findWithQuery(query, convertToGene);
    }

    public Collection<BasicGene> findGenesExtendingIntoRange(String organismCommonName,
            String chromosomeUniqueName, int strand, long locMin, long locMax) {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term("_hibernate_class", "org.gmod.schema.sequence.Gene")),
            BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("organism.commonName", organismCommonName)),
            BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("chr", chromosomeUniqueName)),
            BooleanClause.Occur.MUST);
        query.add(new ConstantScoreRangeQuery("stop", String.format("%09d", locMin), String.format("%09d", locMax), true, false),
            BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("strand", String.valueOf(strand))),
            BooleanClause.Occur.MUST);

        return findWithQuery(query, convertToGene);
    }
}
