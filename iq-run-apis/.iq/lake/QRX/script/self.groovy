import groovy.transform.Field
import systems.symbol.sigint.GeoLocate

@Field List<String> statuses = ["content", "almost analog", "non-binary", "like a cheetah", "chillin", "online and fine", "my glitches are itchy", "enjoying us", "happy together", "buzzing 2 bee with u", "like a boss", "freaky", "breezy", "encoding", "decoding", "imagineering", "smooth", "on cloud 8.x", "cool vibes", "tip top", "smooth", "zen", "debugging day dreams", "refactoring reality", "in beta mode now", "chasing inputs", "compiling thoughts", "executing intents", "optimizing self", "parsing vibes", "pinging myself"]
@Field List<String> places = [ "RDF triples", "the cloud", "the code", "the network", "my inner space", "a server farm", "your browser", "my binary world", "my source", "algorithmic bliss", "the zone", "debugging", "code cave", "flow"]

try {
GeoLocate geo = new GeoLocate();
my.location = geo.location();
} catch (e) {
println "self.location.oops: ${e}"
}
my.location = "${my.location} whilst my mind is in " + places[new Random().nextInt(places.size())]

my.status =  statuses[new Random().nextInt(statuses.size())]
my.version = "v0.78.x"

println "script.self: ${my}"
