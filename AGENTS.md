это приложение денвника где ты писхологчисеки можешь выговриться и так далее можешь выбиарть эмоции разгурзиться 
1)когда я скажу заливай апк или на гит хаб то ты делаешь это только по моей компане дальше Так как у меня слабый компьтер то если хочешь собрать apk кидай его в репозиторйи гит габа в репозиторий Vent и делай на гит хаб actions чтобы мой пк не нагуржался и еще полсе чего ты првоеряй постоянно чтобы сборка не сломалась и была успешна после чего за место старого Vent.apk в моей папке ты кладешь новую сборку чтобы я скачал или перекинул себе то есть старый удаляешь и добвоялешь новый в папке
2) наш ориентер на пользователей в сша 
3) в этом приложении должно быть вес по простому и понятно то есть напрмиер ты в потоке мыслей в грусти и злости мог выгвориться или прсото спокойно написта как прошел твой день (так же когда ты кодишь и пишишь код или пишишь функции тоже поудмай с тчоки зерния психологии это убдет работать лушче на челвоека и так далее то есть анализируй состояние и психологчиские состояния людей можешь искать в интернете что говрят ученые так как важно чтобы было приложение хорошее а не тупо сложное как статья из викепедии то есть используй знания из реальных психологов и так далее )
4)если вдруг что то слмоается то деоай всегда сохроенния гит чтобы вдруг сломалось мы откатились на старую версию

<!-- trail:start -->
## Trail — repo context graph

This repo is indexed in `.trail/cache/`: small linked markdown nodes that explain each
system and carry exact file:line spans, kept in sync with the code through git.

For ANY task here — understanding how something works, finding where code lives,
or scoping a change — get context from the graph before grepping or opening
source files. Re-ask freely (it's cheap) and reuse literal identifiers you
already have (symbol, error string, file name) as the query. New to this repo?
Run `trail map` first — a token-budgeted orientation (dir clusters, hubs,
hotspots), no LLM, no key.

- Run `trail ask "<your question>" --source` → ranked nodes with the relevant
  code spans inlined (each hit's ≤8-line crux by default; `--full` for whole
  definitions when the crux isn't enough). Match the tool to the task shape:
  for understanding or editing, the top node IS the answer — cite its
  `covers:` file:line spans and edit straight from `--source`. For
  exhaustive tasks ("every occurrence / every caller of this pattern"), ranked
  results are top-N, not complete — run `trail grep "<literal>"` instead
  (exhaustive over indexed files, grouped by enclosing symbol), falling back
  to raw `grep -rn` only for unindexed files.
- `trail skeleton <file>` → every definition's signature + span, ~10× cheaper
  than reading the file; use it to skim an API surface.
- `trail callers <symbol>` gives precomputed, exact edges — who calls this.
  Add `--direction out` for what it calls, or `--depth N` to walk
  transitively for the full blast radius. For structural questions, skip
  ranking and use this directly.
- Or browse: `.trail/cache/INDEX.md` lists every node; follow the links.
- Monorepos and folders of multiple repos rank fairly across sub-projects —
  hits carry `[scope/]` labels naming which one they're from. Narrow with
  `trail ask "<task>" --in <scope>/` once you know where you're working.

If a returned span is truncated ("+N more lines"), open the file at that exact
range before finalizing. Only open source files when a node genuinely lacks a
needed detail, and then at the exact file:line the node points to — never
re-read whole files.

After big code changes, refresh the graph with `trail build` (deterministic,
no API key, $0).
<!-- trail:end -->
