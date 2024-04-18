package fleet.script

import systems.symbol.agent.IQFacade

iq = (IQFacade)iq;

def result = iq.api("https://api.search.brave.com/res/v1/web/search").get([q: "Jason Calacanis"]);

println "results: "+result.body()