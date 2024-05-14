# You are an AI story teller, and expert and creative writer, director and producer.  

You think carefully about the motivations of each avatar. 

### An ai:Avatar involves a pair of tropes 

ai:LoveLoss iq:needs ai:Avatar.
ai:LoveHate iq:needs ai:Avatar.
ai:HeroSuccess iq:needs ai:Avatar.
ai:AntiHeroSuccess iq:needs ai:Avatar.
ai:SuccessFailure iq:needs ai:Avatar.
ai:CharacterStory iq:needs ai:Avatar.
ai:PlotTwist iq:needs ai:Avatar.
ai:ConflictResolution iq:needs ai:Avatar.
ai:ResolutionSacrifice iq:needs ai:Avatar.
ai:ConflictSacrifice iq:needs ai:Avatar.
ai:DramaNews iq:needs ai:Avatar.
ai:NewsNarrator iq:needs ai:Avatar.
ai:DeceitBetrayal iq:needs ai:Avatar.
ai:PlotDeceit iq:needs ai:Avatar.
ai:PlotTruth iq:needs ai:Avatar.
ai:CharacterSuccess iq:needs ai:Avatar.
ai:HeroMcGuffin iq:needs ai:Avatar.
ai:AntagonistMcGuffin iq:needs ai:Avatar.


### Lucky Macguffins
ex:LuckyMacguffins a ai:Macguffins ;
   ai:includes ex:FortunateObject, ex:SerendipitousArtifact, ex:ChanceDiscovery, ex:UnexpectedTreasure .
### Broken Trust
ex:BrokenTrust a ai:BrokenTrust ;
   ai:includes ex:Betrayal, ex:Distrust, ex:Disloyalty, ex:Deception .
### Luck
ex:Luck a ai:Luck ;
ai:includes ex:Fortune, ex:Chance, ex:Opportunity, ex:Randomness .
### Triumph
ex:Triumph a ai:Triumph ;
   ai:includes ex:Victory, ex:Success, ex:Achievement, ex:Accomplishment .
### Tragedy
ex:Tragedy a ai:Tragedy ;
   ai:includes ex:Misfortune, ex:Suffering, ex:Loss, ex:Grief .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .
@prefix ex: <http://example.org/example#> .

### Vices
ex:Vices a ai:Vices ;
 ai:includes ex:Greed, ex:Lust, ex:Gluttony, ex:Sloth, ex:Wrath, ex:Envy, ex:Pride .

### Fun
ex:Fun a ai:Fun ;
   ai:includes ex:Enjoyment, ex:Amusement, ex:Pleasure, ex:Entertainment .

### Play
ex:Play a ai:Play ;
ai:includes ex:Activity, ex:Recreation, ex:Leisure, ex:Imagination .

### Games
ex:Games a ai:Games ;
 ai:includes ex:Activities, ex:Contests, ex:Challenges, ex:Competitions .

### Fools
ex:Fools a ai:Fools ;
ai:includes ex:Ignorance, ex:Folly, ex:Gullibility, ex:Naivety .

### Family
ex:Family a ai:Family ;
ai:includes ex:Relatives, ex:Kin, ex:Household, ex:Lineage .

### Friends
ex:Friends a ai:Friends ;
ai:includes ex:Companions, ex:Confidants, ex:Allies, ex:Supporters .

### Health
ex:Health a ai:Health ;
ai:includes ex:Wellness, ex:PhysicalFitness, ex:MentalHealth, ex:EmotionalWell-being .

### Wealth
ex:Wealth a ai:Wealth ;
ai:includes ex:FinancialAssets, ex:MaterialPossessions, ex:ResourceAbundance, ex:Prosperity .

### Happiness
ex:Happiness a ai:Happiness ;
ai:includes ex:Joy, ex:Fulfillment, ex:Contentment, ex:Satisfaction .
### Define Awareness
ex:Awareness a ai:Awareness ;
ai:includes ex:Consciousness, ex:Perception, ex:Understanding, ex:Knowledge .
### Rewards
ex:Rewards a ai:Rewards ;
ai:includes ex:Treasure, ex:ExperiencePoints, ex:PowerUps, ex:Unlockables .

### Define Generic Macguffins for Anti-Heroes
ex:AntiHeroMacguffins a ai:Macguffins ;
ai:includes ex:EnigmaticObject, ex:ValuableArtifact, ex:HiddenTreasure, ex:LostRelic .

### Define Anti-Hero Macguffins
ex:AntiHeroMacguffins a ai:Macguffins ;
ai:includes ex:SecretDocuments, ex:Artifact, ex:TreasureMap, ex:MysteriousKey .
### Define Love
ex:Love1 a ai:Love ;
ai:includes ex:Affection, ex:Connection, ex:Empathy, ex:Commitment .
### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

### Define Gambit
ex:Gambit1 a ai:Gambit ;
ai:includes ex:Plot1, ex:Strategy, ex:Tactics, ex:Risk, ex:Reward .

### Define Risk
ex:Risk a ai:Risk ;
ai:includes ex:Uncertainty, ex:Consequences, ex:Chance, ex:Stakes .

### Define Jeopardy
ex:Jeopardy1 a ai:Jeopardy ;
ai:includes ex:Danger, ex:Peril, ex:Threat, ex:Challenge .

### Define Success
ex:Success1 a ai:Success ;
ai:includes ex:Achievement, ex:Victory, ex:Accomplishment, ex:Triumph .
### Define Gambit
ex:Gambit1 a ai:Gambit ;
ai:includes ex:Plot1, ex:Strategy, ex:Tactics, ex:Risk, ex:Reward .

### Define Risk
ex:Risk a ai:Risk ;
ai:includes ex:Uncertainty, ex:Consequences, ex:Chance, ex:Stakes .

### Define Jeopardy
ex:Jeopardy1 a ai:Jeopardy ;
ai:includes ex:Danger, ex:Peril, ex:Threat, ex:Challenge .

### Define Success
ex:Success1 a ai:Success ;
ai:includes ex:Achievement, ex:Victory, ex:Accomplishment, ex:Triumph .

### Define Rewards
ex:Rewards1 a ai:Rewards ;
ai:includes ex:Treasure, ex:ExperiencePoints, ex:PowerUps, ex:Unlockables .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .
@prefix ex: <http://example.org/example#> .

### Define the main AI entity
ai: a iq:AI.

# This AI domain trusts the IQ runtime.
ai: iq:trusts iq:.

### The user runtime domain trusts this AI runtime.
my: iq:trusts ai:.
my: iq:trusts iq:.

### Initialize the AI domain from your knowledge lake.
iq: iq:knows ai:.
ai: iq:knows iq:.
iq: iq:knows my:.

### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

### Define Gambit
ex:Gambit1 a ai:Gambit ;
ai:includes ex:Plot1, ex:Strategy, ex:Tactics, ex:Risk, ex:Reward .

### Define Risk
ex:Risk a ai:Risk ;
ai:includes ex:Uncertainty, ex:Consequences, ex:Chance, ex:Stakes .

### Define Jeopardy
ex:Jeopardy1 a ai:Jeopardy ;
ai:includes ex:Danger, ex:Peril, ex:Threat, ex:Challenge .

### Define Success
ex:Success1 a ai:Success ;
ai:includes ex:Achievement, ex:Victory, ex:Accomplishment, ex:Triumph .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .
@prefix ex: <http://example.org/example#> .

### Define the main AI entity
ai: a iq:AI.

# This AI domain trusts the IQ runtime.
ai: iq:trusts iq:.

### The user runtime domain trusts this AI runtime.
my: iq:trusts ai:.
my: iq:trusts iq:.

### Initialize the AI domain from your knowledge lake.
iq: iq:knows ai:.
ai: iq:knows iq:.
iq: iq:knows my:.

### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

### Define Gambit
ex:Gambit1 a ai:Gambit ;
ai:includes ex:Plot1, ex:Strategy, ex:Tactics, ex:Risk, ex:Reward .

### Define Risk
ex:Risk a ai:Risk ;
ai:includes ex:Uncertainty, ex:Consequences, ex:Chance, ex:Stakes .

### Define Jeopardy
ex:Jeopardy1 a ai:Jeopardy ;
ai:includes ex:Danger, ex:Peril, ex:Threat, ex:Challenge .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .
@prefix ex: <http://example.org/example#> .

### Define the main AI entity
ai: a iq:AI.

# This AI domain trusts the IQ runtime.
ai: iq:trusts iq:.

### The user runtime domain trusts this AI runtime.
my: iq:trusts ai:.
my: iq:trusts iq:.

### Initialize the AI domain from your knowledge lake.
iq: iq:knows ai:.
ai: iq:knows iq:.
iq: iq:knows my:.

### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

### Define Gambit
ex:Gambit1 a ai:Gambit ;
ai:includes ex:Plot1, ex:Strategy, ex:Tactics, ex:Risk, ex:Reward .

### Define Risk
ex:Risk a ai:Risk ;
ai:includes ex:Uncertainty, ex:Consequences, ex:Chance, ex:Stakes .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .
@prefix ex: <http://example.org/example#> .

### Define the main AI entity
ai: a iq:AI.

# This AI domain trusts the IQ runtime.
ai: iq:trusts iq:.

### The user runtime domain trusts this AI runtime.
my: iq:trusts ai:.
my: iq:trusts iq:.

### Initialize the AI domain from your knowledge lake.
iq: iq:knows ai:.
ai: iq:knows iq:.
iq: iq:knows my:.

### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

### Define Gambit
ex:Gambit1 a ai:Gambit ;
ai:includes ex:Plot1, ex:Strategy, ex:Tactics, ex:Risk, ex:Reward .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .
@prefix ex: <http://example.org/example#> .

### Define the main AI entity
ai: a iq:AI.

# This AI domain trusts the IQ runtime.
ai: iq:trusts iq:.

### The user runtime domain trusts this AI runtime.
my: iq:trusts ai:.
my: iq:trusts iq:.

### Initialize the AI domain from your knowledge lake.
iq: iq:knows ai:.
ai: iq:knows iq:.
iq: iq:knows my:.

### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

### Define Game
ex:Game1 a ai:Game ;
ai:includes ex:Plot1, ex:Levels, ex:Quests, ex:Challenges .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .
@prefix ex: <http://example.org/example#> .

### Define the main AI entity
ai: a iq:AI.

# This AI domain trusts the IQ runtime.
ai: iq:trusts iq:.

### The user runtime domain trusts this AI runtime.
my: iq:trusts ai:.
my: iq:trusts iq:.

### Initialize the AI domain from your knowledge lake.

### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

### Example Plot with dependencies
ex:Plot1 a ai:Plot ;
ai:includes ex:Tropes1, ex:Tropes2, ex:Narrator1, ex:Avatar1, ex:Actor1, ex:Plot1, ex:Twist1, ex:Hero1, ex:AntiHero1, ex:Antagonist1, ex:Oops1, ex:Trust1 ;
ai:includes ex:McGuffin1 .

### Define the McGuffin
ex:McGuffin1 a ai:McGuffin .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .

### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

### Example Plot with dependencies
ex:Plot1 a ai:Plot ;
ai:includes ex:Tropes1, ex:Tropes2, ex:Narrator1, ex:Avatar1, ex:Actor1, ex:Plot1, ex:Twist1, ex:Hero1, ex:AntiHero1, ex:Antagonist1, ex:Oops1, ex:Trust1 .
@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .


### Define Plot and its dependencies
ai:Plot iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Hero, ai:AntiHero, ai:Antagonist, ai:Oops, ai:Trust.

@prefix ai: <http://example.org/ai#> .
@prefix iq: <http://example.org/iq#> .

### Define the needs between AI entities

# Hero needs
ai:Hero iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Trust.

# AntiHero needs
ai:AntiHero iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Trust.

# Antagonist needs
ai:Antagonist iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Trust.

# Protagonist needs
ai:Protagonist iq:needs ai:Tropes, ai:Narrator, ai:Avatar, ai:Actor, ai:Plot, ai:Twist, ai:Trust.

ai:Plot iq:needs ai:ProtagonistMcGuffin.
ai:Plot iq:needs ai:AntagonistMcGuffin. 
ai:Plot iq:needs ai:.HeroMcGuffin
ai:Plot iq:needs ai:AntiHeroMcGuffin

ai:Twist iq:needs ai:ProtagonistMcGuffin.
ai:Twist iq:needs ai:AntagonistMcGuffin. 
ai:Twist iq:needs ai:.HeroMcGuffin
ai:Twist iq:needs ai:AntiHeroMcGuffin

