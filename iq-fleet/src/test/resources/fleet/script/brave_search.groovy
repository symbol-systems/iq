package fleet.script

import systems.symbol.agent.IQFacade

iq = (IQFacade)iq;
my = (Map<String,String>)my;

println "script.my: ${my}";
if (!my.prompt) return;

def results = iq.api("https://api.search.brave.com/res/v1/web/search","x-subscription-token").get([q: my.prompt]);

my.results = iq.json(results.body());
