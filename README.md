# Eliza

A small Java re-creation of **ELIZA**, the natural-language program written by Joseph Weizenbaum at MIT between 1964 and 1966. This version runs ELIZA's best-known script, **DOCTOR**, which imitates a Rogerian psychotherapist. The therapist reflects your statements back at you and gently asks you to say more.

```
How do you do.  Please tell me your problem.
> Men are all alike.
In what way?
> They're always bugging us about something or other.
Can you think of a specific example?
> Well, my boyfriend made me come here.
Your boyfriend made you come here?
> He says I'm depressed much of the time.
I am sorry to hear that you are depressed.
> It's true. I am unhappy.
Do you think coming here will help you not to be unhappy?
> I need some help, that much seems certain.
What would it mean to you if you got some help?
> Perhaps I could learn to get along with my mother.
Tell me more about your family.
> My mother takes care of me.
Who else in your family takes care of you?
> hmm
Let's discuss further why your boyfriend made you come here.
```

The opening lines come from the transcript that Weizenbaum published in 1966, and the replies are this program's actual output.

The whole program is one file, `Eliza.java`, and has no dependencies.

---

## Running it

You need Java 17 or later. Java's source launcher runs the file directly, so there is no separate compile step:

```bash
java Eliza.java
```

For the full 1966 effect, use teletype mode, which prints everything in capitals:

```bash
java Eliza.java --teletype
```

To end the session, type `bye`, `goodbye`, `quit` or `exit`.

You can also compile it in the usual way:

```bash
javac Eliza.java
java Eliza
```

---

## The big idea: the engine knows nothing

Weizenbaum's central insight was to keep the program separate from the personality. ELIZA itself is a small, generic engine. It knows nothing about psychotherapy, families or feelings. All of that lives in a **script**, a set of rules that tells the engine:

- which words to look for,
- how to take a sentence apart once one of those words is found,
- how to build a reply from the pieces.

DOCTOR was only one of several scripts that ran on Weizenbaum's ELIZA. It is the one everybody remembers.

This program follows the same design. The Java code is the engine, and the DOCTOR script is a plain-text block stored in the `SCRIPT` constant at the top of the file. To give Eliza a different personality, such as a shopkeeper, a tutor or a grumpy wizard, you rewrite the script and leave the code alone.

---

## How a reply is produced

Every line you type goes through the same pipeline:

```
  your input
      │
      ▼
 1. Normalise ........ lower-case, strip apostrophes, apply "pre" substitutions
      │
      ▼
 2. Split into clauses  at  . , ; ! ?  and the word "but"
      │
      ▼
 3. Find keywords ..... take the first clause containing a keyword;
      │                 sort its keywords by rank, highest first
      ▼
 4. Decompose ......... try each pattern of the best keyword in turn
      │                 until one matches; capture the pieces
      ▼
 5. Reassemble ........ pick that pattern's next reply, reflect the
      │                 pronouns in the captured pieces, slot them in
      ▼
  Eliza's reply

  (no keyword matched? → use a saved memory, or a "none" reply)
```

### 1. Normalisation

The input is lower-cased, and every character other than letters, digits, spaces and clause punctuation is removed. This means apostrophes disappear, so `don't` becomes `dont` and `I'm` becomes `im`. The script is written to match these flattened forms.

Each word is then checked against the script's `pre` table and replaced if a rule applies. For example, `im` becomes `i am`, `dreamt` becomes `dreamed`, and `machines` becomes `computer`. This collapses many surface forms into one, so the rest of the script needs fewer rules.

### 2. Clauses

The original ELIZA only responded to one clause of the input, and this version does the same. The text is split at `.` `,` `;` `!` `?` and at the word *but*. Eliza uses the first clause that contains a keyword. In *"It's true. I am unhappy."*, the first clause has no keyword, so Eliza responds to *"I am unhappy"*.

### 3. Keywords and ranks

Each keyword has a **rank**. When a clause contains several keywords, Eliza tries them from highest rank to lowest. Keywords with equal rank are tried in the order they appear in the sentence. Ranks are how the script says what matters most:

| Keyword  | Rank | Why                                                  |
|----------|------|------------------------------------------------------|
| computer | 50   | The machine talking about machines always takes over |
| name     | 15   | A firm deflection                                    |
| alike, like | 10 | Similarities are a classic therapeutic opening       |
| remember | 5    |                                                      |
| my       | 2    | Possessions and family are worth pursuing            |
| i, you, are … | 0 | Common words, used only when nothing better turns up |

If a keyword's patterns all fail to match, Eliza moves on to the next keyword.

### 4. Decomposition (pattern matching)

Each keyword owns an ordered list of **decomposition patterns**. A pattern is a sequence of words that can contain two kinds of wildcard:

| Token    | Matches                                            |
|----------|----------------------------------------------------|
| `*`      | any run of words, including none                   |
| `@group` | exactly one word from the named synonym group      |
| `word`   | exactly that word                                  |

Every `*` and every `@group` is **captured** and numbered from left to right, starting at 1. Ordinary words are matched but not captured.

Here is how `* my * @family *` matches *"my mother takes care of me"*:

```
pattern:   *     my    *     @family    *
input:          my          mother     takes care of me
capture:  (1)         (2)    (3)        (4)
          ""          ""     "mother"   "takes care of me"
```

The matcher backtracks, and each `*` tries the shortest possible match first, so a match is always found if one exists. Patterns are tried in the order they appear in the script, and the first match wins. That is why specific patterns come before general ones. Under the keyword `i`, the pattern `* i am @sad *` must come before `* i am *`, and the catch-all `*` always comes last.

### 5. Reassembly

Each pattern has a list of **reassembly rules**, the possible replies. They are used **in rotation**: the first time a pattern matches, Eliza uses the first reply, then the second, and so on, wrapping round to the start. Weizenbaum's ELIZA worked the same way, and this rotation is why Eliza rarely repeats itself in a short conversation.

In a reply, `(n)` is replaced by capture *n*, but first the capture is **reflected** from your point of view to Eliza's:

```
"takes care of me"        →  "takes care of you"
"my boyfriend made me"    →  "your boyfriend made you"
"you hate me"             →  "I hate you"
```

The swaps come from the script's `post` table (`my`→`your`, `me`→`you`, `i`→`you`, `am`→`are` and so on). The code adds a few context-sensitive rules that a simple word table cannot express:

- `you` becomes `I` at the start of a fragment and `me` anywhere else. Compare "you hate me" → "I hate you" with "she hates you" → "she hates me".
- `are` after `you` becomes `am`.
- `were` after `you` becomes `was`.
- `was` after `i` becomes `were`.

The original 1966 program did not make the `you` distinction, which is one source of ELIZA's famously odd grammar.

### When nothing matches: memory and "none"

ELIZA's most uncanny trick is its **memory**. Some patterns are marked to *save* their reply instead of using it straight away. In DOCTOR, any clause matching `* my *` quietly stores a reply such as *"Let's discuss further why your boyfriend made you come here"*.

Later, when you say something that contains no keyword at all (*"hmm"*), Eliza brings back the oldest saved memory. To the user, it looks as though Eliza has been thinking about what they said earlier.

If there is no memory either, Eliza falls back on the `none` replies (*"Please go on."*, *"Tell me more about that."*), which are also used in rotation.

---

## The script language in detail

The script is a sequence of lines in the form `tag: content`. Indentation is ignored but makes the structure easier to read. Blank lines are ignored, and lines beginning with `#` are comments.

### Conversation framing

```
initial: How do you do.  Please tell me your problem.
final:   Goodbye.  Thank you for talking to me.
quit:    bye goodbye quit exit
none:    Please go on.
```

| Tag       | Meaning                                                          |
|-----------|------------------------------------------------------------------|
| `initial` | An opening line. If there are several, one is chosen at random.  |
| `final`   | A closing line, chosen at random.                                |
| `quit`    | Words that end the conversation if they appear anywhere in the input. |
| `none`    | A reply for when nothing matches and no memory is saved. Used in rotation. |

### Word tables

```
pre:  im i am
pre:  machines computer
post: my your
post: me you
syn:  family mother mum mom father dad sister brother wife husband
```

| Tag    | Meaning                                                                                  |
|--------|------------------------------------------------------------------------------------------|
| `pre`  | `pre: word replacement…` is applied to the input before matching. The replacement can be several words. |
| `post` | `post: word replacement` is applied to captured fragments before they go into a reply.   |
| `syn`  | `syn: name word word…` defines a synonym group that patterns refer to as `@name`. The first word is the group's name. It is not automatically a member, so DOCTOR lists `sad` twice in `syn: sad sad unhappy …` to make it both. |

### Keywords, patterns and replies

```
key: my 2
  dec$: * my *
    rsp: Let's discuss further why your (2).
    rsp: Earlier you said your (2).
  dec: * my * @family *
    rsp: Tell me more about your family.
    rsp: Who else in your family (4)?
    rsp: Your (3)?
  dec: * my *
    rsp: Your (2)?
    rsp: Why do you say your (2)?
```

| Tag    | Meaning                                                                         |
|--------|---------------------------------------------------------------------------------|
| `key`  | `key: word rank` starts a new keyword. The rank defaults to 0.                  |
| `dec`  | A decomposition pattern belonging to the keyword above it.                      |
| `dec$` | A **memory** pattern. If it matches, its reply is saved for later and Eliza carries on trying the next pattern. |
| `rsp`  | A reply belonging to the pattern above it. `(n)` inserts capture *n*, reflected.|

### Redirects: `=keyword`

A reply of the form `=keyword` does not produce text. Instead, it hands the same sentence to another keyword's patterns. This lets keywords share responses:

```
key: like 10
  dec: * @be * like *        # "you are like my father"
    rsp: =alike              # answer it as a similarity

key: why 0
  dec: * why dont you *
    rsp: ...
  dec: *
    rsp: =what               # any other "why" is just a question
```

There is a shorthand for a keyword that is purely an alias:

```
key: how 0 =what
```

This is equivalent to:

```
key: how 0
  dec: *
    rsp: =what
```

Redirects can be chained. To protect against a script that loops (`a` → `b` → `a` …), the engine gives up after ten hops and moves on.

### Worked example

Here is the full path of *"My mother takes care of me."* through the DOCTOR script:

1. **Normalise:** `my mother takes care of me`. No `pre` rules apply.
2. **Clauses:** there is only one.
3. **Keywords:** only `my` (rank 2) is present.
4. **Decompose:** Eliza tries the patterns of `my` in order.
   - `dec$: * my *` matches with (2) = "mother takes care of me". This is a memory pattern, so the reply *"Earlier you said your mother takes care of you."* is **saved**, and matching continues.
   - `dec: * my * @family *` matches with (3) = "mother" and (4) = "takes care of me".
5. **Reassemble:** Eliza takes the next reply in this pattern's rotation, `Who else in your family (4)?`. It reflects (4) into "takes care of you" and replies:

   > Who else in your family takes care of you?

The saved memory waits in the queue until a sentence with no keyword gives it a chance to appear.

---

## Writing your own script

Edit the `SCRIPT` text block, or replace it entirely. Some tips that Weizenbaum's script writers would recognise:

- **Write patterns in their flattened form.** Write `dont`, `cant` and `im`, not `don't`, `can't` and `I'm`. To put the apostrophe back in replies, add a `post` rule (`post: dont don't`).
- **Order patterns from specific to general,** and end each keyword with a catch-all `dec: *` unless you *want* Eliza to fall through to the next keyword.
- **Use ranks to set priorities.** A topic you want Eliza to seize on, such as a sensitive subject, deserves a high rank.
- **Use `pre` to collapse variants** (`pre: machines computer`) instead of writing duplicate keywords.
- **Give each pattern several replies.** Because replies are used in rotation, three or four per pattern make Eliza seem far less mechanical.
- **Use `dec$` sparingly.** Memory works best when it brings back something the user actually cared about.

A malformed line, such as an unknown tag or a `rsp` before any `dec`, stops the program at start-up with a message that names the offending line.

---

## Code tour

| Part                | Role                                                                 |
|---------------------|----------------------------------------------------------------------|
| `SCRIPT`            | The DOCTOR script as a Java text block.                              |
| `load()`            | Parses the script into keywords, patterns, replies and word tables.  |
| `respond()`         | The pipeline: clauses → keywords by rank → `tryKey()` → memory → `none`. |
| `normalise()`       | Lower-cases the input, strips punctuation, applies `pre` substitutions. |
| `tryKey()`          | Tries a keyword's patterns, handling memory patterns and `=` redirects. |
| `match()`           | Recursive backtracking matcher for `*`, `@group` and literal words.  |
| `assemble()`        | Fills `(n)` slots and tidies spacing around punctuation.             |
| `reflect()`         | Swaps first and second person in captured fragments.                 |
| `main()`            | The read–respond loop, with optional `--teletype` output.            |

---

## Differences from the 1966 original

The original ELIZA was written in MAD-SLIP on MIT's IBM 7094 under CTSS. This program is a faithful *reinterpretation* rather than an emulation:

- Script syntax is a readable `tag: content` format rather than Weizenbaum's nested S-expressions. The concepts carry over directly: keywords, ranks, decomposition, reassembly, `=` redirects, synonym groups, `pre`/`post` substitutions and the `$` memory mechanism.
- Captures are numbered only for wildcards and synonym groups, not for literal words, which makes reassembly rules easier to write.
- Pronoun reflection is a little smarter about `you`/`I`/`me`.
- Output is in mixed case by default. Use `--teletype` for the original look.

---

## Further reading

- Joseph Weizenbaum, "ELIZA — A Computer Program for the Study of Natural Language Communication between Man and Machine", *Communications of the ACM* 9(1), 1966.
- Joseph Weizenbaum, *Computer Power and Human Reason*, 1976. This book records his later unease about how readily people confided in a program he knew to be a simple trick.

---

## Licence

MIT
