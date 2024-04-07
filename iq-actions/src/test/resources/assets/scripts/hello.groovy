assert hello != null

def greetings = "Hello World";
def facts = [ greetings: greetings, hello: hello, now: System.currentTimeMillis() ]

assert facts.hello === "GG"

return [ facts: true, today: new Date() ]
