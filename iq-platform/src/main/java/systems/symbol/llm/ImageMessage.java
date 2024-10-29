package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * {
 * "type": "image_url",
 * "image_url": {
 * "url": f"data:image/jpeg;base64,{base64_image}",
 * },
 * },
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class ImageMessage extends AbstractMessage<String> {
JsonNode image_url;

public ImageMessage() {
}

@JsonCreator
public ImageMessage(
@JsonProperty("role") String role,
@JsonProperty("image_url") JsonNode image_url) {
this.type = MessageType.image_url;
this.role = RoleType.valueOf(role);
this.image_url = image_url;
}

public String getContent() {
return image_url.get("url").asText();
}
}
