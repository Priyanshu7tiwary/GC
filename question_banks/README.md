# Original (non-PYQ) question bank

`CAT_2026_Original_Advanced_Question_Bank(1).xlsx` is the source file. As delivered it had 450 rows, but it couldn't be used as-is:

- **QA:** every answer was A, and 21 rows were exact duplicates. Most rows were near-identical variants of about 25 templates, and many variants had wrong keys, impossible numbers, nonsensical premises ("8 and 14 are distinct primes"), duplicate options or unfilled placeholders.
- **DILR:** the 30 set descriptions were real, but the 150 questions used only 15 generic texts with the same options in every set, all keyed A.
- **VARC:** each of the 15 passages had the same 6 generic questions and options. Each of the 4 verbal-ability items appeared 15 times, and one of them didn't match its own prompt.

The cleaned bank keeps what is usable and adds new questions:

| Section | Questions | What they are |
|---|---|---|
| QA | 68 | 26 from the file (one per template, every key recomputed, wrong keys and broken premises fixed) and 42 new ones, covering topics the file missed |
| DILR | 90 | the 30 sets, made consistent where needed (each change is noted in the set), with 3 questions each computed from the set's data; logic sets are solved by brute force so each answer is the only one possible |
| VARC | 69 | the 15 passages with 3 specific questions each (45), and 24 new verbal-ability items (summaries, completions, para jumbles, odd sentence out) |

Every question is labelled with its syllabus topic (`QA › Number System › HCF & LCM`). MCQ options are shuffled per question, so answers are spread across A–D.

## Files

- `original/qa.py`, `original/dilr.py`, `original/varc.py`: the questions. QA and DILR entries carry a check that recomputes the answer, and the build stops if a key is wrong.
- `build_original_bank.py`: validates everything and writes the outputs below.
- `catalyst_original_bank.json`: the built bank, in the admin import format.
- `backend/src/main/resources/db/migration/V12__original_question_bank.sql`: inserts the bank into `questions` (origin `BANK`), linked to topics.

## Adding questions

Add entries to the module for the section, then run `python3 -B question_banks/build_original_bank.py`. Once V12 has been applied anywhere, put new questions in a new migration (or import them through the admin Question bank page) rather than editing V12.
