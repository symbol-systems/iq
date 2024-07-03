import jakarta.ws.rs.core.Response
import systems.symbol.agent.IQFacade
import systems.symbol.controller.responses.OopsException

import javax.script.Bindings

iq = (IQFacade)iq;
my = (Bindings)my;

assert my.provider == "twilio"
def serviceSid = ""

def api = iq.api("https://verify.twilio.com/v2/Services/${serviceSid}/Verifications")
api.header('Content-Type', 'application/x-www-form-urlencoded')
api.basic()

println("twilio.payload: ${payload}")

def r = api.post([To: my.mobile, Channel: 'sms'  ])
def verification = iq.json(r)

my.error = verification.error
println("twilio.response: ${verification}")
if (!verification || my.error) throw new OopsException("twilio.verify.error", Response.Status.FORBIDDEN)

println("twilio.verification: ${my.identity}")
