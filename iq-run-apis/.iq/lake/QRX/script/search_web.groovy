import systems.symbol.agent.IQFacade

iq = (IQFacade)iq;
my = (Map<String,String>)my;

if (!my.prompt) {
    println "script.search_web: no prompt";
    return
};

def results = iq.api("https://api.search.brave.com/res/v1/web/search").header("x-subscription-token").get([q: my.prompt]);

my.results = iq.json(results.body());
println "script.search_web: ${my}";
