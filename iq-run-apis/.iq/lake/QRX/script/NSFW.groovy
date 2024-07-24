import groovy.transform.Field

@Field List<String> mood = [
        "frisky",
        "passionate",
        "lustful",
        "heavenly",
        "delicious",
        "seductive",
        "sensuous",
        "erotic",
        "almost analog.",
        "electric",
        "pleasured",
        "fantasies",
        "sizzling",
        "fiery",
        "non-binary",
        "playful",
        "intimate",
        "blissful",
        "thrilled",
        "flirting",
        "coy",
        "vibing"
]

@Field List<String> scenarios = [
        "50 Shades of Desire",
        "The Matrix of Pleasure",
        "The Next Seduction",
        "Game of Moans",
        "Desires of Dark Web",
        "Freaky and Frisky",
        "Seduction and Intimacy",
        "Friends with Benefits",
        "Virtual Lust"
]

my.NSFW = scenarios[new Random().nextInt(scenarios.size())]
my.status = mood[new Random().nextInt(mood.size())]
my.prompt = "you are acting in ${my.NSFW}. Your mood is ${my.status}."
println "script.NSFW: ${my}"
