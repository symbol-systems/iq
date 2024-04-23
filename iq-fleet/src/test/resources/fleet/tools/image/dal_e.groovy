package fleet.tools.brave

import groovy.json.JsonSlurper
import systems.symbol.agent.IQFacade

iq = (IQFacade)iq;
my = (Map<String,String>)my;

println "image.my: ${my}";
if (!my.prompt) return;

def query = [
model: "dall-e-2",
prompt: my.prompt,
"n": 1,
"response_format": "url",
"size": "256x256"
] as Map;
println "image.query: ${query}";

def url = "https://api.openai.com/v1/images/generations" // "https://jsonplaceholder.typicode.com/posts"
def results = iq.api(url).post(query)

def found = new JsonSlurper().parse(results.body().bytes()) as Map
println "image.found: ${found}"

if (found && found.data) {
found.data.each(item -> {
println "download: ${item}"
def downloaded = iq.download(item.url)
println "downloaded: ${downloaded}"
})
}
println "image.done"
