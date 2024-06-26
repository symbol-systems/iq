package systems.symbol.finder;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

/**
 * The {@code I_Finder} interface defines a contract for text embedding and retrieval operations.
 * Implementations of this interface provide methods for embedding, storing, and finding text segments.
 */
public interface I_Finder {

    /**
     * Embeds the given text into a vector representation.
     *
     * @param text The text to be embedded.
     * @return An {@link Embedding} representing the vectorized form of the input text.
     */
    Embedding embed(String text);

    /**
     * Stores the provided text along with an associated identifier, facilitating future retrieval.
     *
     * @param id   The unique identifier for the text.
     * @param text The text to be stored.
     * @return An {@link Embedding} representing the vectorized form of the stored text.
     */
    Embedding store(String id, String text);

    /**
     * Finds text segments similar to the given embedding, considering a maximum number of results
     * and a minimum similarity score.
     *
     * @param embedding  The embedding to find similar text segments for.
     * @param maxResults The maximum number of results to retrieve.
     * @param minScore   The minimum similarity score for a result to be considered.
     * @return A list of {@link EmbeddingMatch} objects representing matching text segments.
     */
    List<EmbeddingMatch<TextSegment>> search(Embedding embedding, int maxResults, double minScore);

    /**
     * Finds text segments similar to the given text, considering the default maximum number of results
     * and the default minimum similarity score.
     *
     * @param text The text to find similar segments for.
     * @return A list of {@link EmbeddingMatch} objects representing matching text segments.
     */
    List<EmbeddingMatch<TextSegment>> search(String text);
}
