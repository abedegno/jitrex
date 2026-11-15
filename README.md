# Humio JITrex - Fast JVM Bytecode Regex Engine

> **About this repository.** Humio archived [humio/jitrex](https://github.com/humio/jitrex) at
> version 0.1.17, but kept releasing newer versions as part of LogScale. This repository continues
> it with **version 0.1.21**, the version shipped in LogScale 1.258.3. The 0.1.21 changes were
> recovered from the published Apache-2.0 JAR (see [Version 0.1.21](#version-0121)). Building this
> source with JDK 11 (`-source 1.8 -target 1.8`, from the pom) gives class files identical, byte
> for byte, to the ones in that JAR.

This project is based on work done in 2000 by Peter Sorotokin as part 
of the now defunct [Jint Programming Language](http://jint.sourceforge.net).
Peter kindly agreed to relicense the original code under Apache License.
See [background](#background).

A regex engine that has the following properties:

- Compiling a regex with `com.humio.jitrex.Pattern.compile(...)` produces a custom Java class.
  This step is not as fast as the other regex engines. 
- The resulting regex however is fast, especially once HotSpot has jit'ed the code, so you
  should cache the compiled `Pattern` instance.
- Matching (generally) only does allocation on the first invocation, and the execution
  behavior of the matcher has very good locality.
- The regex engine does use backtracking, but inside the buffer allocated in the first application
  of a `Matcher` and without overrunning the stack as can happen in pathological cases 
  with `java.lang.util.Regex`.
- As another consequence of using backtracking, capture groups work faster than e.g. RE2J.
- Very large regular expressions can fail to compile if the resuling byte code does not
  fit the JVM's 64k limit per method.
- At present, it does not understand code points beyond `0xffff`, so `.` does not match 💩,
  which is represented as two 16-bit `char`s.  If however your input regex contains a 💩, then
  it will match just fine.  So in most cases this is not an issue.
  
This is not a regex engine for everyone.  It is (relatively) slow to compile,
but fast at matching if you use the same `Matcher` instance many times.

## Performance

For our use case, the Mark Twain samples provide a good indication of how `jitrex` performs.
Here is a sample run comparing it with `java.util.regex` and `com.google.re2j`.
It runs the following regexes over the 302,278 lines of text (15MB) in the collective Mark 
Twain texts from the Gutenberg project:


regex | #found | `jitrex` | `com.google.re2j` | `java.util.regex`
---   | ---:    | ---:     | ---:              | ---: 
regex | matches | `jitrex` | `com.google.re2j` | `java.util.regex`
---   | ---     | ---      | ---               | ---  
 `/Twain/` | 811 | 50ms | 19ms  (38%) | 38ms (76%)
 `/(?i)Twain/` | 965 | 81ms | 1129ms  (1393%) | 115ms (141%)
 `/[a-z]shing/` | 1540 | 155ms | 920ms  (593%) | 183ms (118%)
 `/Huck[a-zA-Z]+\|Saw[a-zA-Z]+/` | 262 | 140ms | 1281ms  (915%) | 241ms (172%)
 `/\b\w+nn\b/` | 262 | 180ms | 1322ms  (734%) | 326ms (181%)
 `/[a-q][^u-z]{13}x/` | 3021 | 426ms | 2435ms  (571%) | 454ms (106%)
 `/Tom\|Sawyer\|Huckleberry\|Finn/` | 2598 | 181ms | 2407ms  (1329%) | 448ms (247%)
 `/(?i)(Tom\|Sawyer\|Huckleberry\|Finn)/` | 4152 | 286ms | 4853ms  (1696%) | 572ms (200%)
 `/.{0,2}(?:Tom\|Sawyer\|Huckleberry\|Finn)/` | 2598 | 594ms | 3547ms  (597%) | 1821ms (306%)
 `/.{2,4}(Tom\|Sawyer\|Huckleberry\|Finn)/` | 1976 | 995ms | 4151ms  (417%) | 1784ms (179%)
 `/Tom.{10,25}river\|river.{10,25}Tom/` | 2 | 116ms | 1308ms  (1127%) | 201ms (173%)
 `/[a-zA-Z]+ing/` | 78424 | 165ms | 1157ms  (701%) | 435ms (263%)
 `/\s[a-zA-Z]{0,12}ing\s/` | 45393 | 271ms | 1580ms  (583%) | 432ms (159%)
 `/([A-Za-z]awyer\|[A-Za-z]inn)\s/` | 194 | 278ms | 1643ms  (591%) | 585ms (210%)
 `/["'][^"']{0,30}[?!\.]["']/` | 8072 | 148ms | 913ms  (616%) | 174ms (117%)
 `/∞\|✓/` | 2 | 111ms | 759ms  (683%) | 399ms (359%)
 `/\p{Sm}/` | 69 | 113ms | 732ms  (647%) | 140ms (123%)

Just adding up these numbers provides an overall ~2x speedup relative to java.util.regex,
and 7x speedup relative to com.google.re2j.

## Background

This project is based on work done in 2000 by Peter Sorotokin as part 
of the now defunct [Jint Programming Language](http://jint.sourceforge.net).
Peter kindly agreed to relicense the original code under Apache License.

This project also includes code developed by Google as part of the
[re2j](https://github.com/google/re2j) project, made available under the
Go License.

Based on Peter Sotorokin's work, we have 

- Added a Java-like API (`com.humio.jitrex.Pattern` and `com.humio.jitrex.Matcher`),
- Removed some Perl-style regex features like `<` and `>`, 
- Added many RE2-style regex features such as `(?i:...)` group local flags, `\p{Greek}` unicode classes,
- Made the code pass all relevant parts of the [re2j](https://github.com/google/re2j) junit tests.
- Fixed numerous bugs in the process passing the tests.

## Version 0.1.21

Humio's public repository stops at 0.1.17. The 0.1.21 source in this repository was rebuilt from
the `jitrex-0.1.21.jar` that ships, under the Apache License 2.0 (`META-INF/LICENSE.md` and
`META-INF/NOTICE` in the JAR), with LogScale 1.258.3. The 0.1.17 files were the starting point.
They were changed only where the 0.1.21 class files differ, and every changed file was checked by
compiling it and comparing the result with the shipped class files. Comments in recovered code were
taken from the 0.1.17 source where they fit. Others were written new, so the comments in changed
files are not necessarily Humio's own.

Changes from 0.1.17 to 0.1.21:

- Matching state moved out of the generated regex classes into a new `jvm.MatcherState`, so a
  compiled regex no longer holds per-match state. `Regex.init`, `search` and `matches` now take a
  `MatcherState`, and `Matcher` keeps one per matcher.
- Matching can be cancelled through a flag (`Matcher.setCancelledFlag`), which throws the new
  `RegexCancelledException`. `RegexRuntimeLimitException` now extends the new
  `JitrexRuntimeException`.
- Input refilling was removed (`util.RegexRefiller`, `jvm.RJavaClassCustomizer`).
- `RNode.processFlags` was split into `processFlagsGlobal` and `processFlagsLocal`. Runs of atomic
  nodes are prepared iteratively, and repeat bounds use saturating arithmetic.

Files changed or added for 0.1.21 (Apache License 2.0, section 4(b)):
`Matcher`, `Pattern`, `JitrexRuntimeException` (new), `RegexCancelledException` (new),
`RegexRuntimeLimitException`, `compiler/RCompiler`, `compiler/RDebugMachine`, `compiler/RMachine`,
`jvm/JavaClassRegexStub`, `jvm/MatcherState` (new), `jvm/RJavaClassMachine`, `parser/RParser`,
`tree/RAltNode`, `tree/RCharClassNode`, `tree/RConstNode`, `tree/RLookAheadNode`, `tree/RNode`,
`tree/RPickNode`, `tree/RRepeatNode`, `util/CaseInsensitiveRegex`, `util/ProxyRegex`, `util/Regex`,
`util/Replacer` (under `com.humio.jitrex`) and `com.humio.util.jint.lang.CharString`.

The 0.1.21 tests were not published. The 0.1.17 tests were updated for the new API: the failure
count is read from `MatcherState`. Two tests that used hand-decompiled copies of 0.1.17 generated
classes (`jvm/Sample`, `jvm/Sample2`) were removed. All 499 remaining tests pass.

