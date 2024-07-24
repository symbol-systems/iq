import jakarta.ws.rs.core.Response
import systems.symbol.agent.IQFacade
import systems.symbol.controller.responses.OopsException

import javax.script.Bindings

iq = (IQFacade)iq;
my = (Bindings)my;

assert my.provider == "discord"

// println("discord.authorize: ${my}")

def api = iq.api("https://discord.com/api/v10/oauth2/token")
api.header('Content-Type', 'application/x-www-form-urlencoded')
api.basic()

def payload = [ 'grant_type': 'authorization_code', 'code': my.code, 'redirect_uri': my.host+"auth/discord" ]
println("discord.payload: ${payload}")

def r = api.post(payload)
def auth = iq.json(r)
println("discord.response: ${auth}")

if (!auth || auth.error) throw new OopsException("discord.oauth.error."+auth.error, Response.Status.FORBIDDEN)

if (auth.access_token) {
def discord_api = iq.api("https://discord.com/api/v10/users/@me")
discord_api.bearer(""+auth.access_token)
def discord_reply = discord_api.get()
my.discord = iq.json(discord_reply)
if (my.discord.global_name && my.discord.id) {
my.name = my.discord.global_name
my.identity = "${my.issuer}${my.discord.id}"
}
}
println("discord.identity: ${my.identity} --> ${my.discord}")
