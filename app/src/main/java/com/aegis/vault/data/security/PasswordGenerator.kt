package com.aegis.vault.data.security

import java.security.SecureRandom

/**
 * The password generator behind L10 (and the inline "Generate" button in the
 * editor). Random mode uses SecureRandom; memorable mode builds passphrases
 * from a bundled wordlist so the result is typeable, without contacting any
 * service.
 */
class PasswordGenerator {

    private val random = SecureRandom()

    private val lowercase = "abcdefghijkmnopqrstuvwxyz"
    private val uppercase = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val numbers = "23456789"
    private val symbols = "!@#$%^&*-_=+?"
    private val lookAlikes = "lI1O0o"

    data class Options(
        val length: Int = 20,
        val uppercase: Boolean = true,
        val lowercase: Boolean = true,
        val numbers: Boolean = true,
        val symbols: Boolean = true,
        val avoidLookAlikes: Boolean = false,
        val memorable: Boolean = false,
        val memorableWords: Int = 4,
    )

    fun generate(options: Options): String =
        if (options.memorable) generateMemorable(options) else generateRandom(options)

    private fun generateRandom(options: Options): String {
        val pools = mutableListOf<String>()
        if (options.lowercase) pools += lowercase
        if (options.uppercase) pools += uppercase
        if (options.numbers) pools += numbers
        if (options.symbols) pools += symbols
        if (pools.isEmpty()) pools += lowercase

        var pool = pools.joinToString("")
        if (options.avoidLookAlikes) {
            pool = pool.filterNot { it in lookAlikes }
            pools.forEachIndexed { index, p -> pools[index] = p.filterNot { it in lookAlikes } }
            if (pools.any { it.isEmpty() }) {
                // Avoid emptying the only pool when everything is a look-alike.
                pools.removeAll { it.isEmpty() }
                pools += lowercase
                pool = pools.joinToString("")
            }
        }

        // Guarantee at least one character from every enabled pool.
        val chars = pools.map { it[random.nextInt(it.length)] }.toMutableList()
        while (chars.size < options.length) {
            chars += pool[random.nextInt(pool.length)]
        }
        chars.shuffle(random)
        return chars.take(options.length).joinToString("")
    }

    private fun generateMemorable(options: Options): String {
        val words = options.memorableWords.coerceIn(3, 6)
        val picked = List(words) {
            WORDS[random.nextInt(WORDS.size)].replaceFirstChar { c -> c.uppercase() }
        }
        val separator = pick(Separators)
        val number = random.nextInt(100)
        val suffix = if (options.symbols || options.numbers) separator else ""
        val digits = if (options.numbers || options.symbols) "$number" else ""
        val tail = if (options.symbols) pick(TrailingSymbols) else ""
        return picked.joinToString(suffix) + digits + tail
    }

    private fun pick(list: List<String>) = list[random.nextInt(list.size)]

    companion object {
        private val Separators = listOf("-", ".", "_", ":")
        private val TrailingSymbols = listOf("!", "#", "%", "&")

        /** ~250 common English words, length 4–8, easy to type and read aloud. */
        val WORDS: List<String> = listOf(
            "amber", "anchor", "apple", "arbor", "arrow", "atlas", "autumn", "avenue",
            "bacon", "badger", "banjo", "bark", "basil", "basin", "beacon", "beagle",
            "belly", "berry", "birch", "bison", "blade", "blanket", "bloom", "bobcat",
            "bonus", "booth", "border", "bottle", "boulder", "bracket", "branch", "brave",
            "breeze", "brick", "bridge", "bright", "bronze", "brook", "brush", "buckle",
            "buffer", "bugle", "bunker", "burrow", "bush", "butter", "cabin", "cable",
            "cactus", "camera", "canal", "candle", "canopy", "canyon", "cargo", "carpet",
            "castle", "cedar", "cellar", "cement", "chalk", "chance", "chapel", "cherry",
            "chest", "chief", "chorus", "chrome", "cider", "circle", "citrus", "clasp",
            "clay", "cliff", "clinic", "clock", "clover", "clutch", "coast", "cobalt",
            "cocoa", "coffee", "column", "comet", "compass", "coral", "corner", "cosmos",
            "cotton", "cougar", "coupe", "credit", "creek", "crescent", "crest", "cricket",
            "crisp", "crater", "crystal", "cupboard", "curtain", "cushion", "custom", "cycle",
            "daisy", "dawn", "decade", "delta", "denim", "depot", "desert", "design",
            "detail", "diagram", "dinner", "disco", "divider", "doctor", "dolphin", "donkey",
            "double", "dragon", "dream", "drift", "drum", "duck", "dune", "dusk",
            "eagle", "early", "earth", "echo", "eclipse", "elbow", "elder", "ember",
            "engine", "escape", "evening", "exact", "fabric", "falcon", "farmer", "fathom",
            "faucet", "feather", "fence", "ferry", "festival", "field", "finch", "fjord",
            "flame", "flash", "flint", "float", "flute", "forest", "forge", "fortune",
            "fossil", "foundry", "fox", "frame", "freight", "fresh", "frog", "frontier",
            "frost", "funnel", "future", "gadget", "galaxy", "gallery", "garden", "garlic",
            "gate", "gauge", "gazelle", "gecko", "geyser", "ginger", "giraffe", "glacier",
            "glass", "glen", "globe", "glove", "goblet", "gold", "gopher", "grace",
            "grain", "granite", "grape", "graphite", "meadow", "marble", "mason", "gravity",
            "grebe", "green", "grille", "grove", "guard", "guest", "guitar", "gulf",
            "habit", "harbor", "harvest", "haven", "hawk", "hazel", "header", "hearth",
            "heather", "helmet", "hemlock", "herald", "heron", "hickory", "hill", "hollow",
            "honey", "hornet", "hostel", "hunter", "hurdle", "hyacinth", "igloo", "impala",
            "index", "indigo", "ink", "insect", "iris", "iron", "island", "ivory",
            "jacket", "jaguar", "jasmine", "javelin", "jelly", "jetty", "jewel", "jockey",
            "jovial", "jungle", "juniper", "kayak", "kelp", "kernel", "kettle", "key",
            "kiln", "king", "kiosk", "kitchen", "kiwi", "knapsack", "knight", "koala",
            "lagoon", "lake", "lamb", "lantern", "larch", "lark", "latch", "laurel",
            "lavender", "lattice", "launch", "leaf", "ledge", "legacy", "lemon", "lever",
            "lichen", "lily", "lime", "linen", "lion", "liquid", "llama", "lobster",
            "locker", "locket", "locust", "lodge", "lotus", "lounge", "lunar", "lynx",
        )
    }
}
