package fleet.tools

import systems.symbol.persona.Persona
my = (Map<String,String>)my;

my.results = [[ self: my.self, label: "healthy"]]

println "self.my: ${my}"

def persona = new Persona()

if (my.answer) persona.speak(my.answer)
else persona.speak("I'm here")
