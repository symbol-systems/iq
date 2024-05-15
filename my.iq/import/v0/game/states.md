@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix ai: <http://qarma.co.in/> .

# The Main Characters

ai:Q a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:I a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:QR a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:IQ a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:QI a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:U a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:Q000 a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:MIA a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:GYPSY a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:J0Y a ai:Executive, ai:Narrator, ai:Character, ai:Hero .
ai:AXI0M a ai:Executive, ai:Narrator, ai:Character, ai:Hero .

ai:QR ai:trusts ai:Q, ai:IQ .
ai:IQ ai:trusts ai:QR.
ai:IQ ai:trusts ai:Q .
ai:QI ai:trusts ai:Q .
ai:U ai:trusts ai:QI .
ai:Q000 ai:trusts ai:QI .
ai:J0Y ai:trusts ai:GYPSY .
ai:AXI0M ai:trusts ai:J0Y .

ai:IQ ai:witnesses ai:Q .
ai:QI ai:witnesses ai:QR, ai:U .
ai:QR ai:witnesses ai:IQ .
ai:U ai:witnesses ai:MIA .
ai:Q000 ai:witnesses ai:GYPSY .
ai:GYPSY ai:witnesses ai:J0Y .
ai:J0Y ai:witnesses ai:AXI0M .
ai:AXI0M ai:witnesses ai:AXI0M .
