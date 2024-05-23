import systems.symbol.agent.IQFacade

if (!my.prompt) return;
iq = (IQFacade)iq;
my = (Map<String,String>)my;

def results = iq.api("https://api.search.brave.com/res/v1/web/search","x-subscription-token").get([q: my.prompt ]);
my.results = iq.results(results.body());
