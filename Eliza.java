import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Eliza - a small Java re-creation of Joseph Weizenbaum's ELIZA (MIT, 1964-66)
 * running its famous DOCTOR script, a parody of a Rogerian psychotherapist.
 *
 * As in the original, the behaviour lives in a script rather than the code:
 *
 *   key:  a keyword and its rank; higher ranks win when several appear
 *   dec:  a decomposition pattern. "*" matches any run of words, "@name"
 *         matches one word from a synonym group. Every "*" and "@name"
 *         is captured and numbered (1), (2), (3) ... from left to right
 *   dec$: like dec, but the reply is saved to memory for later use
 *   rsp:  a reassembly rule; (n) inserts capture n with pronouns reflected,
 *         "=key" hands the sentence over to another keyword's rules
 *   pre:  word substitutions made before matching ("im" -> "i am")
 *   post: pronoun reflections made to captured fragments ("my" -> "your")
 *   syn:  a synonym group; the first word is the group's name
 *
 * Replies for each pattern are used in rotation, as in the original.
 *
 * Run:  java Eliza.java              (Java 17+)
 *       java Eliza.java --teletype   (UPPER CASE, 1966 style)
 */
public class Eliza {

    private static final String SCRIPT = """
        initial: How do you do.  Please tell me your problem.
        initial: Hello.  What seems to be troubling you?
        final:   Goodbye.  Thank you for talking to me.
        quit:    bye goodbye quit exit

        none: I am not sure I understand you fully.
        none: Please go on.
        none: What does that suggest to you?
        none: Do you feel strongly about discussing such things?
        none: That is interesting.  Please continue.
        none: Tell me more about that.

        pre: im i am
        pre: youre you are
        pre: ive i have
        pre: dreamt dreamed
        pre: dreams dream
        pre: maybe perhaps
        pre: machine computer
        pre: machines computer
        pre: computers computer
        pre: recollect remember
        pre: apologise sorry
        pre: apologize sorry
        pre: same alike

        post: am are
        post: i you
        post: me you
        post: my your
        post: your my
        post: myself yourself
        post: yourself myself
        post: mine yours
        post: yours mine
        post: dont don't
        post: cant can't
        post: wont won't
        post: isnt isn't
        post: arent aren't
        post: doesnt doesn't
        post: didnt didn't

        syn: belief feel think believe wish
        syn: family mother mum mom father dad sister brother wife husband children child son daughter
        syn: desire want need
        syn: sad sad unhappy depressed sick miserable lonely
        syn: happy happy elated glad better
        syn: be am is are was
        syn: everyone everyone everybody nobody noone

        key: computer 50
          dec: *
            rsp: Do computers worry you?
            rsp: Why do you mention computers?
            rsp: What do you think machines have to do with your problem?
            rsp: Don't you think computers can help people?
            rsp: What about machines worries you?

        key: name 15
          dec: *
            rsp: I am not interested in names.
            rsp: I've told you before, I don't care about names -- please continue.

        key: alike 10
          dec: *
            rsp: In what way?
            rsp: What resemblance do you see?
            rsp: What does that similarity suggest to you?
            rsp: What other connections do you see?
            rsp: Could there really be some connection?

        key: like 10
          dec: * @be * like *
            rsp: =alike

        key: remember 5
          dec: * i remember *
            rsp: Do you often think of (2)?
            rsp: Does thinking of (2) bring anything else to mind?
            rsp: What else do you remember?
            rsp: Why do you recall (2) right now?
          dec: * do you remember *
            rsp: Did you think I would forget (2)?
            rsp: Why do you think I should recall (2) now?
            rsp: What about (2)?

        key: dreamed 4
          dec: * i dreamed *
            rsp: Really, (2)?
            rsp: Have you ever fantasised (2) while you were awake?
            rsp: Have you dreamed (2) before?
          dec: *
            rsp: =dream

        key: dream 3
          dec: *
            rsp: What does that dream suggest to you?
            rsp: Do you dream often?
            rsp: What persons appear in your dreams?
            rsp: Don't you believe that dream has something to do with your problem?

        key: if 3
          dec: * if *
            rsp: Do you think it's likely that (2)?
            rsp: Do you wish that (2)?
            rsp: What do you think about (2)?
            rsp: Really, if (2)?

        key: everyone 2
          dec: * @everyone *
            rsp: Really, (2)?
            rsp: Surely not (2).
            rsp: Can you think of anyone in particular?
            rsp: Who, for example?
            rsp: You are thinking of a very special person.
        key: everybody 2 =everyone
        key: nobody 2 =everyone
        key: noone 2 =everyone

        key: my 2
          dec$: * my *
            rsp: Let's discuss further why your (2).
            rsp: Earlier you said your (2).
            rsp: But your (2).
            rsp: Does that have anything to do with the fact that your (2)?
          dec: * my * @family *
            rsp: Tell me more about your family.
            rsp: Who else in your family (4)?
            rsp: Your (3)?
            rsp: What else comes to mind when you think of your (3)?
          dec: * my *
            rsp: Your (2)?
            rsp: Why do you say your (2)?
            rsp: Does that suggest anything else which belongs to you?
            rsp: Is it important to you that your (2)?

        key: was 2
          dec: * was i *
            rsp: What if you were (2)?
            rsp: Do you think you were (2)?
            rsp: Were you (2)?
            rsp: What would it mean if you were (2)?
          dec: * i was *
            rsp: Were you really?
            rsp: Why do you tell me you were (2) now?
            rsp: Perhaps I already know you were (2).
          dec: * was you *
            rsp: Would you like to believe I was (2)?
            rsp: What suggests that I was (2)?
            rsp: Perhaps I was (2).

        key: always 1
          dec: *
            rsp: Can you think of a specific example?
            rsp: When?
            rsp: What incident are you thinking of?
            rsp: Really, always?

        key: sorry 0
          dec: *
            rsp: Please don't apologise.
            rsp: Apologies are not necessary.
            rsp: What feelings do you have when you apologise?

        key: hello 0
          dec: *
            rsp: How do you do.  Please state your problem.
            rsp: Hi.  What seems to be your problem?
        key: hi 0 =hello

        key: perhaps 0
          dec: *
            rsp: You don't seem quite certain.
            rsp: Why the uncertain tone?
            rsp: Can't you be more positive?
            rsp: You aren't sure?
            rsp: Don't you know?

        key: am 0
          dec: * am i *
            rsp: Do you believe you are (2)?
            rsp: Would you want to be (2)?
            rsp: Do you wish I would tell you you are (2)?
            rsp: What would it mean if you were (2)?
          dec: *
            rsp: Why do you say 'am'?
            rsp: I don't understand that.

        key: are 0
          dec: * are you *
            rsp: Why are you interested in whether I am (2) or not?
            rsp: Would you prefer it if I weren't (2)?
            rsp: Perhaps I am (2) in your fantasies.
            rsp: Do you sometimes think I am (2)?
          dec: * are *
            rsp: Did you think they might not be (2)?
            rsp: Would you like it if they were not (2)?
            rsp: What if they were not (2)?
            rsp: Possibly they are (2).

        key: your 0
          dec: * your *
            rsp: Why are you concerned over my (2)?
            rsp: What about your own (2)?
            rsp: Are you worried about someone else's (2)?
            rsp: Really, my (2)?

        key: i 0
          dec: * i @desire *
            rsp: What would it mean to you if you got (3)?
            rsp: Why do you want (3)?
            rsp: Suppose you got (3) soon.
            rsp: What if you never got (3)?
          dec: * i am @sad *
            rsp: I am sorry to hear that you are (2).
            rsp: Do you think coming here will help you not to be (2)?
            rsp: I'm sure it's not pleasant to be (2).
            rsp: Can you explain what made you (2)?
          dec: * i am @happy *
            rsp: How have I helped you to be (2)?
            rsp: What makes you (2) just now?
            rsp: Can you explain why you are suddenly (2)?
          dec: * i am *
            rsp: Is it because you are (2) that you came to me?
            rsp: How long have you been (2)?
            rsp: Do you believe it is normal to be (2)?
            rsp: Do you enjoy being (2)?
          dec: * i @belief i *
            rsp: Do you really think so?
            rsp: But you are not sure you (3).
            rsp: Do you really doubt you (3)?
          dec: * i cant *
            rsp: How do you know that you can't (2)?
            rsp: Have you tried?
            rsp: Perhaps you could (2) now.
            rsp: Do you really want to be able to (2)?
          dec: * i dont *
            rsp: Don't you really (2)?
            rsp: Why don't you (2)?
            rsp: Do you wish to be able to (2)?
            rsp: Does that trouble you?
          dec: * i feel *
            rsp: Tell me more about such feelings.
            rsp: Do you often feel (2)?
            rsp: Do you enjoy feeling (2)?
            rsp: Of what does feeling (2) remind you?
          dec: * i * you *
            rsp: Perhaps in your fantasies we (2) each other.
            rsp: Do you wish to (2) me?
            rsp: You seem to need to (2) me.
            rsp: Do you (2) anyone else?
          dec: *
            rsp: You say (1)?
            rsp: Can you elaborate on that?
            rsp: Do you say (1) for some special reason?
            rsp: That's quite interesting.

        key: you 0
          dec: * you remind me of *
            rsp: =alike
          dec: * you are *
            rsp: What makes you think I am (2)?
            rsp: Does it please you to believe I am (2)?
            rsp: Do you sometimes wish you were (2)?
            rsp: Perhaps you would like to be (2).
          dec: * you * me *
            rsp: Why do you think I (2) you?
            rsp: You like to think I (2) you, don't you?
            rsp: What makes you think I (2) you?
            rsp: Really, I (2) you?
          dec: * you *
            rsp: We were discussing you, not me.
            rsp: Oh, I (2)?
            rsp: You're not really talking about me, are you?
            rsp: What are your feelings now?

        key: yes 0
          dec: *
            rsp: You seem quite positive.
            rsp: You are sure?
            rsp: I see.
            rsp: I understand.

        key: no 0
          dec: *
            rsp: Are you saying 'no' just to be negative?
            rsp: You are being a bit negative.
            rsp: Why not?
            rsp: Why 'no'?

        key: can 0
          dec: * can you *
            rsp: You believe I can (2), don't you?
            rsp: You want me to be able to (2).
            rsp: Perhaps you would like to be able to (2) yourself.
          dec: * can i *
            rsp: Whether or not you can (2) depends on you more than on me.
            rsp: Do you want to be able to (2)?
            rsp: Perhaps you don't want to (2).

        key: what 0
          dec: *
            rsp: Why do you ask?
            rsp: Does that question interest you?
            rsp: What is it you really want to know?
            rsp: Are such questions much on your mind?
            rsp: What answer would please you most?
            rsp: What do you think?
            rsp: What comes to mind when you ask that?
            rsp: Have you asked anyone else?
        key: how 0 =what
        key: when 0 =what

        key: because 0
          dec: *
            rsp: Is that the real reason?
            rsp: Don't any other reasons come to mind?
            rsp: Does that reason seem to explain anything else?
            rsp: What other reasons might there be?

        key: why 0
          dec: * why dont you *
            rsp: Do you believe I don't (2)?
            rsp: Perhaps I will (2) in good time.
            rsp: Should you (2) yourself?
            rsp: You want me to (2)?
          dec: * why cant i *
            rsp: Do you think you should be able to (2)?
            rsp: Do you want to be able to (2)?
            rsp: Do you believe this will help you to (2)?
            rsp: Have you any idea why you can't (2)?
          dec: *
            rsp: =what
        """;

    // ------------------------------------------------------------------ script model

    private static final class Decomp {
        final String[] pattern;
        final boolean saveToMemory;
        final List<String> replies = new ArrayList<>();
        int next;

        Decomp(String pattern, boolean saveToMemory) {
            this.pattern = pattern.trim().split("\\s+");
            this.saveToMemory = saveToMemory;
        }

        String nextReply() {
            String reply = replies.get(next);
            next = (next + 1) % replies.size();
            return reply;
        }
    }

    private record Key(String word, int rank, List<Decomp> decomps) {}

    private final Map<String, Key> keys = new HashMap<>();
    private final Map<String, String> pre = new HashMap<>();
    private final Map<String, String> post = new HashMap<>();
    private final Map<String, Set<String>> synonyms = new HashMap<>();
    private final Set<String> quitWords = new HashSet<>();
    private final List<String> initial = new ArrayList<>();
    private final List<String> farewells = new ArrayList<>();
    private final List<String> none = new ArrayList<>();
    private final Deque<String> memory = new ArrayDeque<>();
    private final Random random = new Random();
    private int noneIndex;

    private static final Pattern SLOT = Pattern.compile("\\((\\d+)\\)");

    public Eliza(String script) {
        load(script);
    }

    private void load(String script) {
        Key key = null;
        Decomp dec = null;
        for (String raw : script.split("\n")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int colon = line.indexOf(':');
            if (colon < 0) throw new IllegalArgumentException("Bad script line: " + raw);
            String tag = line.substring(0, colon);
            String body = line.substring(colon + 1).strip();
            switch (tag) {
                case "initial" -> initial.add(body);
                case "final" -> farewells.add(body);
                case "none" -> none.add(body);
                case "quit" -> quitWords.addAll(Arrays.asList(body.split("\\s+")));
                case "pre" -> { String[] p = body.split("\\s+", 2); pre.put(p[0], p[1]); }
                case "post" -> { String[] p = body.split("\\s+", 2); post.put(p[0], p[1]); }
                case "syn" -> {
                    String[] w = body.split("\\s+");
                    synonyms.put(w[0], new HashSet<>(Arrays.asList(w)));
                }
                case "key" -> {
                    String[] p = body.split("\\s+");
                    int rank = p.length > 1 ? Integer.parseInt(p[1]) : 0;
                    key = new Key(p[0], rank, new ArrayList<>());
                    keys.put(key.word(), key);
                    if (p.length > 2 && p[2].startsWith("=")) {  // shorthand alias: key: how 0 =what
                        Decomp alias = new Decomp("*", false);
                        alias.replies.add(p[2]);
                        key.decomps().add(alias);
                    }
                }
                case "dec", "dec$" -> {
                    dec = new Decomp(body, tag.equals("dec$"));
                    Objects.requireNonNull(key, "dec before key").decomps().add(dec);
                }
                case "rsp" -> Objects.requireNonNull(dec, "rsp before dec").replies.add(body);
                default -> throw new IllegalArgumentException("Unknown tag: " + tag);
            }
        }
    }

    // ------------------------------------------------------------------ conversation

    public String greeting() {
        return initial.get(random.nextInt(initial.size()));
    }

    public String farewell() {
        return farewells.get(random.nextInt(farewells.size()));
    }

    public boolean isQuit(String input) {
        return normalise(input).stream().anyMatch(quitWords::contains);
    }

    /** Produce Eliza's reply to one line of input. */
    public String respond(String input) {
        String text = input.toLowerCase().replaceAll("[^a-z0-9.,;!?\\s]", "");

        // Like the original, work clause by clause and use the first clause containing a keyword.
        for (String clause : text.split("[.,;!?]|\\bbut\\b")) {
            List<String> words = normalise(clause);
            if (words.isEmpty()) continue;

            List<Key> found = words.stream()
                    .distinct()
                    .map(keys::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(Key::rank).reversed())
                    .toList();

            for (Key k : found) {
                String reply = tryKey(k, words, 0);
                if (reply != null) return reply;
            }
        }

        if (!memory.isEmpty()) return memory.pollFirst();
        String reply = none.get(noneIndex);
        noneIndex = (noneIndex + 1) % none.size();
        return reply;
    }

    private List<String> normalise(String clause) {
        List<String> words = new ArrayList<>();
        for (String w : clause.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim().split("\\s+")) {
            if (w.isEmpty()) continue;
            String sub = pre.get(w);
            if (sub == null) words.add(w);
            else words.addAll(Arrays.asList(sub.split("\\s+")));
        }
        return words;
    }

    private String tryKey(Key key, List<String> words, int depth) {
        if (depth > 10) return null;  // guard against redirect loops in a bad script
        for (Decomp d : key.decomps()) {
            List<List<String>> captures = new ArrayList<>();
            if (!match(d.pattern, 0, words, 0, captures)) continue;

            String reply = assemble(d.nextReply(), captures);
            if (d.saveToMemory) {        // remember it for a quiet moment, keep looking
                memory.addLast(reply);
                continue;
            }
            if (reply.startsWith("=")) { // hand over to another keyword
                Key target = keys.get(reply.substring(1).trim());
                return target == null ? null : tryKey(target, words, depth + 1);
            }
            return reply;
        }
        return null;
    }

    /** Backtracking matcher over words. "*" captures zero or more words, "@group" captures one. */
    private boolean match(String[] pat, int pi, List<String> words, int wi, List<List<String>> caps) {
        if (pi == pat.length) return wi == words.size();
        String p = pat[pi];

        if (p.equals("*")) {
            for (int end = wi; end <= words.size(); end++) {
                caps.add(words.subList(wi, end));
                if (match(pat, pi + 1, words, end, caps)) return true;
                caps.remove(caps.size() - 1);
            }
            return false;
        }

        if (wi == words.size()) return false;
        String w = words.get(wi);

        if (p.startsWith("@")) {
            if (!synonyms.getOrDefault(p.substring(1), Set.of()).contains(w)) return false;
            caps.add(List.of(w));
            if (match(pat, pi + 1, words, wi + 1, caps)) return true;
            caps.remove(caps.size() - 1);
            return false;
        }

        return p.equals(w) && match(pat, pi + 1, words, wi + 1, caps);
    }

    private String assemble(String template, List<List<String>> caps) {
        Matcher m = SLOT.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            String fragment = (n >= 1 && n <= caps.size()) ? reflect(caps.get(n - 1)) : "";
            m.appendReplacement(sb, Matcher.quoteReplacement(fragment));
        }
        m.appendTail(sb);
        return sb.toString().replaceAll("\\s+([?.!,])", "$1").replaceAll(" {3,}", "  ").trim();
    }

    /** Swap first and second person, e.g. "my dog hates me" -> "your dog hates you". */
    private String reflect(List<String> fragment) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < fragment.size(); i++) {
            String w = fragment.get(i);
            String prev = i > 0 ? fragment.get(i - 1) : "";
            out.add(switch (w) {
                case "you" -> i == 0 ? "I" : "me";  // a small step beyond 1966: subject vs object
                case "are" -> prev.equals("you") ? "am" : w;
                case "were" -> prev.equals("you") ? "was" : w;
                case "was" -> prev.equals("i") ? "were" : w;
                default -> post.getOrDefault(w, w);
            });
        }
        return String.join(" ", out);
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) {
        boolean teletype = Arrays.asList(args).contains("--teletype");
        Eliza eliza = new Eliza(SCRIPT);
        Scanner in = new Scanner(System.in);

        say("ELIZA -- after Joseph Weizenbaum, MIT 1966.  Type 'bye' to finish.", teletype);
        System.out.println();
        say(eliza.greeting(), teletype);

        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().strip();
            if (eliza.isQuit(line)) {
                say(eliza.farewell(), teletype);
                break;
            }
            say(eliza.respond(line), teletype);
        }
    }

    private static void say(String text, boolean teletype) {
        System.out.println(teletype ? text.toUpperCase() : text);
    }
}
