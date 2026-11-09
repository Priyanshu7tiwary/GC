# CAT 2017–2025 answer keys and text repairs

The official question papers carry no answer key, and converting them to text lost maths and figures. Both gaps are filled from 2IIM's public previous-year paper pages (`https://online.2iim.com/CAT-question-paper/CAT-<year>-Question-Paper-Slot-<n>-<VARC|DILR|Quant>/`, one per slot and section). Every 2IIM question is matched to an official question **by content, not position**: 2IIM often orders questions, and MCQ options, differently from the official paper.

## `answer_keys.csv`

There is one row per official question (`papers/extracted/cat_pyq_questions.jsonl`). MCQ keys are zero-based indices into the official option order.

| status | count | meaning |
| --- | --- | --- |
| `verified_match` | 1,713 | Same question, and the key was carried across safely. MCQ options are paired one-to-one, by exact text where possible, otherwise by similarity: every pair at least 0.8 similar, a clear margin over any other pairing, and no near-duplicate options. `option_map` records the pairing (official index → source index). For a TITA, the question wording matches. |
| `ambiguous_options` | 46 | With the maths stripped, two options read the same (e.g. "√x, √y, √z in AP" vs "x, y, z in AP"), so the pairing isn't trusted. |
| `option_set_mismatch` / `option_text_mismatch` | 25 | The option texts differ too much to pair, usually because the extracted maths is garbled. |
| `unmatched` / `type_mismatch` | 46 | The key matcher couldn't confirm the question. |

An earlier version of the matcher accepted lists whose options were only similar *on average* ("12" vs "13"). That assigned about 146 MCQ keys to the wrong option. The pairing rules above replace it, and V9 corrects those keys in the database.

## `source_overlay.jsonl`

These are repairs applied on top of the extracted text by `generate_pyq_bank.py`:

- **QA:** the prompt and options are taken from 2IIM's TeX. Options are placed through `option_map`, so they stay in official order. Where there is no safe map (the ambiguous or garbled cases above), 2IIM's options are used in 2IIM's order **together with 2IIM's key** (`options_from_source`). Most questions the key matcher couldn't confirm are paired by their non-maths words, or by identical option numbers; these take 2IIM's text and key too.
- **DILR:** set figures and tables that were images in the PDFs are downloaded to `frontend/public/pyq/img/` and attached to the set text.
- Every formula is rendered with KaTeX at build time (`check_tex.mjs`). Anything that fails keeps the extracted text.

After all repairs, 1,812 of 1,830 questions have a key. The other 18 are shown but not scored.

## `question_topics.csv`

Each official-paper question's syllabus topic, written by `classify_topics.py`:

- **QA:** 2IIM's chapter heading for the question (on about 57% of them) narrows the candidate topics, then weighted keywords in the repaired question text choose one.
- **DILR:** the whole set is classified from its text, and every question in it shares that topic.
- **VARC:** verbal-ability questions are classified by their instruction (odd one out, jumble, summary, placement). Reading-comprehension questions are classified by what they ask (inference, main idea, tone, strengthen/weaken, specific detail), falling back to the passage's genre.

These are heuristics. A hand check of a random sample put them at about 85–90% right. Fix any misfiled question from the admin Question bank page ("Move to"); the database keeps the correction.

## Rebuild

```bash
python3 backend/scripts/pyq/fetch_2iim.py /tmp/2iim                       # 72 pages, sequential and throttled
python3 backend/scripts/pyq/parse_2iim.py /tmp/2iim.jsonl /tmp/2iim/*.html
python3 backend/scripts/pyq/match_answer_keys.py papers/extracted/cat_pyq_questions.jsonl /tmp/2iim.jsonl \
        resources/cat_pyq_answer_keys/answer_keys.csv /tmp/keys_report.json
python3 backend/scripts/pyq/build_source_overlay.py /tmp/2iim.jsonl frontend/public/pyq/img   # needs frontend/node_modules
python3 backend/scripts/pyq/classify_topics.py /tmp/2iim.jsonl             # topic per question (run after generate once, as it reads the paper JSON)
python3 backend/scripts/pyq/generate_pyq_bank.py                          # paper JSON; V7, V9 and V11 only when missing
```

V9 is idempotent and leaves admin-corrected keys alone. Once V9 has been applied anywhere, save regenerated SQL as a new migration version rather than editing V9.
