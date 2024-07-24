import jakarta.ws.rs.core.Response
import systems.symbol.agent.IQFacade
import systems.symbol.controller.responses.OopsException
import systems.symbol.util.IdentityHelper

import javax.script.Bindings

iq = (IQFacade)iq;
my = (Bindings)my;

assert my.provider == "twilio"
def serviceSid = ""

def api = iq.api("https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json")
api.header('Content-Type', 'application/x-www-form-urlencoded')
api.basic()

println("twilio.payload: ${payload}")

def code = IdentityHelper.uuid().substring(0,3)+":"+IdentityHelper.uuid().substring(3,6)
def r = api.post([To: my.mobile, Channel: 'sms', message: code  ])
def verification = iq.json(r)

my.error = verification.error
println("twilio.response: ${verification}")
if (!verification || my.error) throw new OopsException("twilio.verify.error", Response.Status.FORBIDDEN)

println("twilio.verification: ${my.identity}")
