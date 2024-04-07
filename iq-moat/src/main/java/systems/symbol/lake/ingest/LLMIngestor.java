package systems.symbol.lake.ingest;

import systems.symbol.agent.apis.APIException;
import systems.symbol.lake.ContentEntity;
import systems.symbol.llm.*;

import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;

public class LLMIngestor extends AbstractIngestor<ContentEntity<String>> {
protected static final Logger log = LoggerFactory.getLogger(LLMIngestor.class);
I_LLM<String> llm;
private String systemPrompt = "";

public LLMIngestor(I_LLM<String> llm, String systemPrompt, Consumer<ContentEntity<String>> next) throws FileSystemException {
super(next);
this.llm = llm;
this.systemPrompt = systemPrompt;
}

protected ContentEntity<String> transform (ContentEntity<?> content) throws IOException, APIException {
ChatThread thread = new ChatThread();
thread.system(systemPrompt+"\n: Your base URI is:"+content.getIdentity());
thread.user(content.getContent().toString());
I_Thread<String> answer = this.llm.generate(thread);
I_LLMessage<?> latest = answer.latest();
String reply = latest.getContent().toString();
log.debug("llm.reply: {} -> {}", latest.getType(), reply);
return new ContentEntity<String>(content.getIdentity(), reply);
}

@Override
public void accept(ContentEntity content) {
try {
log.debug("accept: {} => {}", content.getIdentity(), content.getContent().toString());
next(transform(content));
} catch (IOException e) {
log.error("llm.io: {}", content.getIdentity(), e);
throw new RuntimeException(e);
} catch (APIException e) {
log.error("llm.api: {}", content.getIdentity(), e);
throw new RuntimeException(e);
}
}
}
