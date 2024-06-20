package systems.symbol.lake.ingest;

import systems.symbol.agent.tools.APIException;
import systems.symbol.lake.ContentEntity;
import systems.symbol.llm.*;

import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class LLMIngestor extends AbstractIngestor<ContentEntity<String>> {
    protected static final Logger log = LoggerFactory.getLogger(LLMIngestor.class);
    I_LLM<String> llm;
    private String systemPrompt = "";
    private int max_tokens = 512;

    public LLMIngestor(I_LLM<String> llm, String systemPrompt, Consumer<ContentEntity<String>> next) throws FileSystemException {
        super(next);
        this.llm = llm;
        this.systemPrompt = systemPrompt;
        this.max_tokens = llm.getConfig().getMaxTokens();
    }

    protected ContentEntity<String> transform (ContentEntity<?> content) throws IOException, APIException {
        Conversation thread = new Conversation();
        thread.system(systemPrompt+"\n: Your base URI is:"+content.getSelf());
        thread.user(content.getContent().toString());
        this.llm.complete(thread);
        I_LLMessage<?> latest = thread.latest();
        String reply = latest.getContent().toString();
        log.debug("llm.reply: {} -> {}", latest.getType(), reply);
        return new ContentEntity<String>(content.getSelf(), reply, "text/plain");
    }

    @Override
    public void accept(ContentEntity content) {
        try {
            log.debug("accept: {} => {}", content.getSelf(), content.getContent().toString());
            next(transform(content));
        } catch (IOException e) {
            log.error("llm.io: {}", content.getSelf(), e);
            throw new RuntimeException(e);
        } catch (APIException e) {
            log.error("llm.api: {}", content.getSelf(), e);
            throw new RuntimeException(e);
        }
    }
}
