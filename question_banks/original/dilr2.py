"""DILR, batch 2: nineteen new CAT-style sets written for 99ile, four questions each.

They fill the syllabus topics batch 1 had no sets for (bar and line graphs, pie charts, data sufficiency, cubes and
dice, truth-teller/liar puzzles, selections and distributions) and add harder sets to the logic topics, the kind
that dominate recent CAT DILR sections.

Every answer is computed from the set's data. Logic sets are solved by brute force over every possible case, and
the build asserts that each puzzle has exactly the solutions the questions rely on, so a keyed answer is the only
one possible. Chart sets also write their figure (see charts.py); the passage embeds it with an [[img:…]] marker.
"""
import heapq
from collections import Counter
from fractions import Fraction as F
from itertools import combinations, permutations, product

import charts

SETS = []
FIGURES = {}  # file name -> SVG text, written by the builder to frontend/public/pyq/img/


def S(id, topic, title, passage, questions, note=''):
    SETS.append(dict(id=id, topic=topic, title=title, passage=passage, questions=questions, note=note))


def M(prompt, options, explanation, check, difficulty='MODERATE'):
    return dict(type='MCQ', prompt=prompt, options=options, explanation=explanation, check=check, difficulty=difficulty)


def T(prompt, answer, explanation, check, difficulty='MODERATE'):
    return dict(type='TITA', prompt=prompt, answer=str(answer), explanation=explanation, check=check, difficulty=difficulty)


def figure(name, svg):
    FIGURES[name] = svg
    return f'[[img:/pyq/img/{name}]]'


def pct(x):
    return f'{float(x) * 100:.2f}%'


# ============================================================ Bar & Line graphs

years = ['2021', '2022', '2023', '2024']
rev = dict(P=[120, 150, 180, 210], Q=[200, 190, 230, 250], R=[80, 120, 150, 160], S=[160, 160, 200, 240])
S('b01', 'Bar & Line graphs', 'Revenue of four companies',
  'The bar chart shows the annual revenue (₹ crore) of four companies P, Q, R and S from 2021 to 2024.\n\n'
  + figure('bank2-b01-revenue.svg', charts.bar('Annual revenue (₹ crore)', years, rev, '₹ crore', 300, 50)),
  [M('Which company’s revenue grew by the largest percentage from 2021 to 2024?', ['R', 'P', 'S', 'Q'],
     'Growth 2021→2024: P 75%, Q 25%, R 100%, S 50%.',
     lambda: max(rev, key=lambda k: F(rev[k][3], rev[k][0]))),
   T('Considering each company in each of 2022, 2023 and 2024 (12 cases), in how many cases did revenue grow by at least 20% over the previous year?', 7,
     'P: 25%, 20%, 16.7% (2 cases); Q: −5%, 21.1%, 8.7% (1); R: 50%, 25%, 6.7% (2); S: 0%, 25%, 20% (2). Total 7.',
     lambda: str(sum(1 for v in rev.values() for a, b in zip(v, v[1:]) if F(b, a) >= F(6, 5)))),
   M('By what percentage was the combined revenue of the four companies in 2024 higher than in 2021?', ['53.57%', '34.88%', '50.00%', '58.33%'],
     'Combined revenue was 560 in 2021 and 860 in 2024: 300/560 = 53.57%.',
     lambda: pct(F(sum(v[3] for v in rev.values()) - sum(v[0] for v in rev.values()), sum(v[0] for v in rev.values())))),
   M('In which year was S’s share of the combined revenue of the four companies the highest?', ['2024', '2023', '2021', '2022'],
     'S’s share: 2021 160/560 = 28.6%; 2022 160/620 = 25.8%; 2023 200/760 = 26.3%; 2024 240/860 = 27.9%. Highest in 2021.',
     lambda: None, difficulty='HARD')])
# The last question's key is 2021, not 2024: rebuild it with the correct option first.
SETS[-1]['questions'][3] = M('In which year was S’s share of the combined revenue of the four companies the highest?', ['2021', '2024', '2023', '2022'],
                             'S’s share: 2021 160/560 = 28.6%; 2022 160/620 = 25.8%; 2023 200/760 = 26.3%; 2024 240/860 = 27.9%. Highest in 2021.',
                             lambda: years[max(range(4), key=lambda i: F(rev['S'][i], sum(v[i] for v in rev.values())))], difficulty='HARD')

months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun']
units = {'Store X': [40, 45, 38, 50, 55, 60], 'Store Y': [35, 42, 46, 44, 58, 52]}
X, Y = units['Store X'], units['Store Y']
S('b02', 'Bar & Line graphs', 'Monthly sales of two stores',
  'The line graph shows the number of units (in hundreds) of a product sold by two stores, X and Y, in each month from January to June. '
  'Store X sells the product at ₹250 a unit and Store Y at ₹280 a unit throughout.\n\n'
  + figure('bank2-b02-sales.svg', charts.line('Units sold (hundreds)', months, units, 'Units (hundreds)', 30, 65, 5)),
  [T('In how many months did Store Y sell more units than Store X?', 2, 'March (46 vs 38) and May (58 vs 55).',
     lambda: str(sum(y > x for x, y in zip(X, Y)))),
   M('In which month did Store X’s sales change the most compared with the previous month?', ['April', 'March', 'May', 'February'],
     'Changes for X: Feb +5, Mar −7, Apr +12, May +5, Jun +5. The largest change is April’s.',
     lambda: months[max(range(1, 6), key=lambda i: abs(X[i] - X[i - 1]))].replace('Apr', 'April').replace('Mar', 'March').replace('May', 'May').replace('Feb', 'February')),
   T('How many units did the two stores sell in all from January to June? (Give the number of units, not hundreds.)', 56500,
     'X sold 288 hundred and Y 277 hundred units: 565 hundred = 56,500 units.', lambda: str((sum(X) + sum(Y)) * 100)),
   M('In which month was the difference between the two stores’ sales revenues the largest?', ['March', 'May', 'June', 'February'],
     'Revenue (₹ lakh, per hundred units × price): Jan 10.00 vs 9.80; Feb 11.25 vs 11.76; Mar 9.50 vs 12.88; Apr 12.50 vs 12.32; May 13.75 vs 16.24; Jun 15.00 vs 14.56. The gap is largest in March (₹3.38 lakh).',
     lambda: {'Mar': 'March', 'May': 'May', 'Jun': 'June', 'Feb': 'February', 'Jan': 'January', 'Apr': 'April'}[months[max(range(6), key=lambda i: abs(250 * X[i] - 280 * Y[i]))]],
     difficulty='HARD')])

depts = ['Sales', 'Tech', 'HR', 'Finance', 'Ops']
staff = {'Men': [60, 90, 15, 30, 45], 'Women': [40, 30, 35, 20, 15]}
men, women = staff['Men'], staff['Women']


def hires_needed(m, w, share=F(2, 5)):
    h = 0
    while F(w + h, m + w + h) < share:
        h += 1
    return h


S('b03', 'Bar & Line graphs', 'Employees by department',
  'The stacked bar chart shows the number of men and women employed in each of the five departments of a company.\n\n'
  + figure('bank2-b03-staff.svg', charts.stacked('Employees by department', depts, staff, 'Employees', 140, 20)),
  [M('What percentage of the company’s employees are women?', ['36.84%', '35.00%', '38.89%', '63.16%'],
     '140 of the 380 employees are women: 36.84%.', lambda: pct(F(sum(women), sum(men) + sum(women)))),
   M('Which department has the highest proportion of women?', ['HR', 'Sales', 'Finance', 'Ops'],
     'Women’s shares: Sales 40%, Tech 25%, HR 70%, Finance 40%, Ops 25%.',
     lambda: depts[max(range(5), key=lambda i: F(women[i], men[i] + women[i]))]),
   T('The company wants women to make up at least 40% of every department. If it only hires women and nobody leaves, what is the minimum number of women it must hire?', 45,
     'Sales, HR and Finance already meet 40%. Tech needs (30 + h)/(120 + h) ≥ 0.4, so h ≥ 30; Ops needs (15 + h)/(60 + h) ≥ 0.4, so h ≥ 15. Total 45.',
     lambda: str(sum(hires_needed(m, w) for m, w in zip(men, women))), difficulty='HARD'),
   M('What is the ratio of men in Tech and Ops together to women in Sales and HR together?', ['9 : 5', '5 : 9', '3 : 2', '27 : 16'],
     'Men in Tech and Ops: 90 + 45 = 135. Women in Sales and HR: 40 + 35 = 75. 135 : 75 = 9 : 5.',
     lambda: (lambda r: f'{r.numerator} : {r.denominator}')(F(men[1] + men[4], women[0] + women[2])))])

# ============================================================ Pie charts

budget = {'Rent': 30, 'Food': 22.5, 'Transport': 10, 'Education': 15, 'Savings': 12.5, 'Others': 10}
INCOME = 80000
amt = {k: F(str(v)) / 100 * INCOME for k, v in budget.items()}
S('b04', 'Pie charts', 'A household budget',
  'The pie chart shows how a household divides its monthly income of ₹80,000.\n\n'
  + figure('bank2-b04-budget.svg', charts.pies('Monthly budget (share of ₹80,000)', [('Share of income', budget)])),
  [M('What is the central angle of the Education sector of the pie chart?', ['54°', '45°', '36°', '60°'],
     '15% of 360° = 54°.', lambda: f'{F(str(budget["Education"])) / 100 * 360}°'),
   T('How many rupees a month does the household spend on Food and Transport together?', 26000,
     '22.5% + 10% = 32.5% of ₹80,000 = ₹26,000.', lambda: str(amt['Food'] + amt['Transport'])),
   M('By what percentage is the rent more than Education and Transport spending combined?', ['20%', '5%', '16.67%', '25%'],
     'Rent is 30% of income and Education + Transport 25%: 30/25 = 1.2, i.e. 20% more.',
     lambda: f'{(amt["Rent"] / (amt["Education"] + amt["Transport"]) - 1) * 100:.0f}%'),
   M('Next month the income rises to ₹88,000. Every category keeps the same rupee amount except Savings, which takes all of the extra money. What share of the new income goes to Savings?',
     ['20.45%', '15.91%', '12.50%', '22.73%'],
     'Savings become 10,000 + 8,000 = ₹18,000 out of ₹88,000: 20.45%.',
     lambda: pct((amt['Savings'] + 88000 - INCOME) / 88000), difficulty='HARD')])

streams = ['Engineering', 'Commerce', 'Science', 'Arts', 'Law']
y24 = dict(zip(streams, [35, 25, 20, 15, 5]))
y25 = dict(zip(streams, [30, 28, 22, 12, 8]))
N24, N25 = 2400, 3000
c24 = {k: N24 * v // 100 for k, v in y24.items()}
c25 = {k: N25 * v // 100 for k, v in y25.items()}
S('b05', 'Pie charts', 'Students by stream',
  'The pie charts show how the students of a university were spread across five streams in 2024 (2,400 students) and in 2025 (3,000 students).\n\n'
  + figure('bank2-b05-streams.svg', charts.pies('Students by stream', [('2024 — 2,400 students', y24), ('2025 — 3,000 students', y25)])),
  [M('Which stream’s number of students grew by the largest percentage from 2024 to 2025?', ['Law', 'Science', 'Commerce', 'Engineering'],
     'Students 2024 → 2025: Engineering 840 → 900 (+7.1%), Commerce 600 → 840 (+40%), Science 480 → 660 (+37.5%), Arts 360 → 360 (0%), Law 120 → 240 (+100%).',
     lambda: max(streams, key=lambda k: F(c25[k], c24[k]))),
   T('In how many streams did the number of students not increase from 2024 to 2025?', 1,
     'Only Arts: 15% of 2,400 = 360 and 12% of 3,000 = 360.', lambda: str(sum(c25[k] <= c24[k] for k in streams))),
   T('By how many students did Commerce grow from 2024 to 2025?', 240, '28% of 3,000 − 25% of 2,400 = 840 − 600 = 240.',
     lambda: str(c25['Commerce'] - c24['Commerce'])),
   M('In 2026 the university expects 3,300 students, with Engineering and Arts keeping their 2025 numbers and the other three streams sharing the rest in their 2025 ratio. How many Law students does it expect?',
     ['293', '264', '240', '330'],
     'Engineering + Arts = 1,260, leaving 2,040 for Commerce, Science and Law in the ratio 840 : 660 : 240 = 14 : 11 : 4. Law gets 2,040 × 4/29 ≈ 281 … ',
     lambda: None, difficulty='HARD')])
# 2,040 × 4/29 is not a whole number, so the last question uses a total that keeps every stream whole.
SETS[-1]['questions'][3] = M(
    'In 2026 the university expects 3,710 students, with Engineering and Arts keeping their 2025 numbers and the other three streams sharing the rest in their 2025 ratio. How many Law students does it expect?',
    ['320', '297', '240', '371'],
    'Engineering + Arts = 900 + 360 = 1,260, leaving 2,450 for Commerce, Science and Law in the ratio 840 : 660 : 240 = 14 : 11 : 4. Law gets 2,450 × 4/29 … which must be whole; the figures give 2,320 for the three, so check: ',
    lambda: None, difficulty='HARD')


def _law_2026(total):
    rest = total - c25['Engineering'] - c25['Arts']
    share = F(rest * c25['Law'], c25['Commerce'] + c25['Science'] + c25['Law'])
    return share


# choose the smallest total above 3,300 that keeps every stream whole (the three streams' ratio sums to 29)
T26 = next(t for t in range(3300, 4000) if all(_law_2026(t) * 0 + F((t - 1260) * c25[k], 1740).denominator == 1 for k in ('Commerce', 'Science', 'Law')))
SETS[-1]['questions'][3] = M(
    f'In 2026 the university expects {T26:,} students, with Engineering and Arts keeping their 2025 numbers and the other three streams sharing the rest in their 2025 ratio. How many Law students does it expect?',
    [str(_law_2026(T26)), str(round(T26 * F(8, 100))), str(c25['Law']), str(round(_law_2026(T26) * F(11, 10)))],
    f'Engineering + Arts = 900 + 360 = 1,260, leaving {T26 - 1260:,} for Commerce, Science and Law in the 2025 ratio 840 : 660 : 240 = 14 : 11 : 4. Law gets {T26 - 1260:,} × 4/29 = {_law_2026(T26)}.',
    lambda: str(_law_2026(T26)), difficulty='HARD')

# ============================================================ Data sufficiency

DS_DIR = ('Each question below is followed by two statements, I and II. Decide whether the data in the statements is sufficient '
          'to answer the question. Treat each question on its own: the statements of one question do not apply to another.')
DS_OPTS = {'I': 'Statement I alone is sufficient, but statement II alone is not.',
           'II': 'Statement II alone is sufficient, but statement I alone is not.',
           'both': 'Both statements together are sufficient, but neither alone is.',
           'neither': 'The two statements together are not sufficient.'}
DS_ORDER = ['I', 'II', 'both', 'neither']


def ds_verdict(worlds, ask, s1, s2):
    """Classify sufficiency by brute force: a statement set is sufficient when every world it allows gives one answer."""
    def suff(pred):
        answers = {ask(w) for w in worlds if pred(w)}
        assert answers, 'a statement contradicts the setup'
        return len(answers) == 1
    one, two = suff(s1), suff(s2)
    if one and two:
        return 'either'
    if one:
        return 'I'
    if two:
        return 'II'
    return 'both' if suff(lambda w: s1(w) and s2(w)) else 'neither'


def DSQ(question, st1, st2, worlds, ask, p1, p2, key, explanation):
    opts = [DS_OPTS[key]] + [DS_OPTS[k] for k in DS_ORDER if k != key]
    return M(f'{question}\nI. {st1}\nII. {st2}', opts, explanation, lambda: DS_OPTS.get(ds_verdict(worlds, ask, p1, p2)))


ages = [dict(zip('ABCDE', p)) for p in permutations(range(20, 25))]
S('b06', 'Data sufficiency', 'Five friends’ ages',
  f'Five friends A, B, C, D and E are of different ages: one each of 20, 21, 22, 23 and 24 years.\n\n{DS_DIR}',
  [DSQ('Who is the oldest of the five?', 'A is older than B, C and D.', 'E is younger than C.', ages,
       lambda w: max(w, key=w.get), lambda w: w['A'] > max(w['B'], w['C'], w['D']), lambda w: w['E'] < w['C'], 'both',
       'I leaves A or E as the oldest; II alone says nothing about A. Together, E < C < A and A is older than B and D, so A is the oldest.'),
   DSQ('What is C’s age?', 'C is older than exactly three of the others.', 'C’s age is the average of B’s and D’s ages.', ages,
       lambda w: w['C'], lambda w: sum(w['C'] > w[x] for x in 'ABDE') == 3, lambda w: 2 * w['C'] == w['B'] + w['D'], 'I',
       'I puts C second oldest: 23. II allows C to be 21, 22 or 23.'),
   DSQ('Is D older than B?', 'D is 22 years old.', 'B is the youngest of the five.', ages,
       lambda w: w['D'] > w['B'], lambda w: w['D'] == 22, lambda w: w['B'] == 20, 'II',
       'II makes B 20, so D is certainly older. I fixes only D’s age; B could be younger or older.'),
   DSQ('What is A’s age?', 'The ages of A and E add up to 45.', 'A is one year older than E.', ages,
       lambda w: w['A'], lambda w: w['A'] + w['E'] == 45, lambda w: w['A'] - w['E'] == 1, 'both',
       'I allows {A, E} = {21, 24} or {22, 23}; II allows four pairs. Together, A = 23 and E = 22.')])

balls = [dict(r=r, b=b, g=20 - r - b) for r in range(1, 19) for b in range(1, 19) if 20 - r - b >= 1]
S('b07', 'Data sufficiency', 'A bag of coloured balls',
  f'A bag holds 20 balls, each red, blue or green, with at least one ball of each colour.\n\n{DS_DIR}',
  [DSQ('How many red balls are in the bag?', 'There are twice as many red balls as blue balls.', 'There are 5 green balls.', balls,
       lambda w: w['r'], lambda w: w['r'] == 2 * w['b'], lambda w: w['g'] == 5, 'both',
       'I alone allows (r, b) = (2, 1), (4, 2), … ; II alone leaves r + b = 15 open. Together 3b = 15, so b = 5 and r = 10.'),
   DSQ('Are there more green balls than blue balls?', 'There are more than 10 green balls.', 'There are fewer blue balls than red balls.', balls,
       lambda w: w['g'] > w['b'], lambda w: w['g'] > 10, lambda w: w['b'] < w['r'], 'I',
       'With g ≥ 11, b ≤ 20 − 11 − 1 = 8 < g, so I answers “yes”. II allows both answers.'),
   DSQ('How many blue balls are in the bag?', 'The number of blue balls is a multiple of 6.', 'The number of blue balls equals the number of red balls multiplied by the number of green balls.', balls,
       lambda w: w['b'], lambda w: w['b'] % 6 == 0, lambda w: w['b'] == w['r'] * w['g'], 'II',
       'II: r + g + rg = 20, i.e. (r + 1)(g + 1) = 21, so {r, g} = {2, 6} and b = 12 either way. I allows b = 6, 12 or 18.'),
   DSQ('How many blue balls are in the bag?', 'There are as many red balls as green balls.', 'The number of blue balls is even.', balls,
       lambda w: w['b'], lambda w: w['r'] == w['g'], lambda w: w['b'] % 2 == 0, 'neither',
       'I gives b = 20 − 2r, which is always even, so II adds nothing: b can still be 2, 4, …, 18.')],
  note='Statements are checked by brute force over every possible (red, blue, green) split.')

# ============================================================ Cubes & Dice

L_, B_, H_ = 6, 4, 3
cells = list(product(range(L_), range(B_), range(H_)))


def colours(c):
    x, y, z = c
    out = set()
    if z in (0, H_ - 1):
        out.add('red')      # top and bottom: the 6 × 4 faces
    if y in (0, B_ - 1):
        out.add('blue')     # front and back: the 6 × 3 faces
    if x in (0, L_ - 1):
        out.add('green')    # the two ends: the 4 × 3 faces
    return out


S('b08', 'Cubes & Dice', 'A painted block',
  'A solid wooden block measures 6 cm × 4 cm × 3 cm. Its top and bottom faces (6 × 4) are painted red, its front and back faces (6 × 3) blue, '
  'and its two end faces (4 × 3) green. The block is then cut into 72 cubes of side 1 cm.',
  [T('How many of the small cubes have no paint on them?', 8, 'The unpainted core is (6 − 2) × (4 − 2) × (3 − 2) = 8 cubes.',
     lambda: str(sum(1 for c in cells if not colours(c)))),
   T('How many of the small cubes show exactly two colours?', 28,
     'These are the edge cubes that are not corners. The 4 edges of length 6 give 4 each, the 4 of length 4 give 2 each, the 4 of length 3 give 1 each: 16 + 8 + 4 = 28.',
     lambda: str(sum(1 for c in cells if len(colours(c)) == 2))),
   T('How many of the small cubes have both red and blue paint but no green paint?', 16,
     'They lie on the four edges where a red face meets a blue face (the 6 cm edges), excluding the corner cubes: 4 × 4 = 16.',
     lambda: str(sum(1 for c in cells if colours(c) == {'red', 'blue'}))),
   M('How many of the small cubes have at least one face painted green?', ['24', '12', '20', '16'],
     'Each 4 × 3 end face holds 12 cubes, and the two ends do not share any: 24.',
     lambda: str(sum(1 for c in cells if 'green' in colours(c))), difficulty='HARD')])


def roll(state, d):
    top, north, east = state
    bottom, south, west = 7 - top, 7 - north, 7 - east
    return {'N': (south, top, east), 'S': (north, bottom, east), 'E': (west, north, top), 'W': (east, north, bottom)}[d]


def after(moves, state=(1, 2, 3)):
    for d in moves:
        state = roll(state, d)
    return state


def min_rolls(goal, start=(1, 2, 3)):
    seen, frontier, n = {start}, [start], 0
    while frontier:
        if any(goal(s) for s in frontier):
            return n
        nxt = []
        for s in frontier:
            for d in 'NSEW':
                t = roll(s, d)
                if t not in seen:
                    seen.add(t)
                    nxt.append(t)
        frontier, n = nxt, n + 1


S('b09', 'Cubes & Dice', 'Rolling a die',
  'A standard die, whose opposite faces add up to 7, stands on a grid with 1 on top, 2 facing north and 3 facing east. '
  'A “roll” tips the die over one of its bottom edges onto the next square: rolling north moves the top face to the north side, the north face to the bottom, '
  'the bottom face to the south side and the south face to the top, and similarly for the other directions.',
  [M('After one roll to the north, which number is on top?', ['5', '2', '6', '4'],
     'The south face (7 − 2 = 5) comes up to the top.', lambda: str(after('N')[0])),
   M('The die is rolled north and then east. Which number is now on top?', ['4', '3', '5', '2'],
     'After N: top 5, north 1, east 3, so west = 4. Rolling east brings the west face up: 4.', lambda: str(after('NE')[0])),
   T('The die is rolled N, N, E, E, S in that order. What is the sum of the numbers on top after each of the five rolls?', 18,
     'Tops: after N 5, after N 6, after E 4, after E 3, after S 0 … track (top, north, east): (5,1,3), (6,5,3), (4,5,1), (3,5,6), (2,3,6). Sum 5 + 6 + 4 + 3 + 2 = 20.',
     lambda: None),
   T('What is the least number of rolls needed to bring 6 to the top while 2 still faces north?', 2,
     'Rolling east twice turns the die upside down about the north–south axis: 6 on top, 2 still north. One roll can never put 6 on top.',
     lambda: str(min_rolls(lambda s: s[0] == 6 and s[1] == 2)), difficulty='HARD')])
_tops = []
_s = (1, 2, 3)
for _d in 'NNEES':
    _s = roll(_s, _d)
    _tops.append(_s)
SETS[-1]['questions'][2] = T(
    'The die is rolled N, N, E, E, S in that order. What is the sum of the numbers on top after each of the five rolls?', sum(t[0] for t in _tops),
    'Tracking (top, north, east): ' + ', '.join(f'({a}, {b}, {c})' for a, b, c in _tops) + '. The tops add up to '
    + ' + '.join(str(t[0]) for t in _tops) + f' = {sum(t[0] for t in _tops)}.',
    lambda: str(sum(after('NNEES'[:k])[0] for k in range(1, 6))))

# ============================================================ Binary logic (truth/lie)

KN = 'ABCDE'
kn_claims = {'A': lambda t: t['C'], 'B': lambda t: t['A'] == t['D'], 'C': lambda t: not t['A'] and not t['E'],
             'D': lambda t: sum(t.values()) == 2, 'E': lambda t: t['D']}


def kn_solutions(claims):
    out = []
    for v in product([True, False], repeat=5):
        t = dict(zip(KN, v))
        if all(claims[n](t) == t[n] for n in KN):
            out.append(t)
    return out


kn = kn_solutions(kn_claims)
assert len(kn) == 1
kn = kn[0]
S('b10', 'Binary logic (truth/lie)', 'Five islanders',
  'On an island every inhabitant is either a truth-teller, who always tells the truth, or a liar, who always lies. Five inhabitants A, B, C, D and E say:\n'
  'A: “C is a truth-teller.”\nB: “A and D are of the same type.”\nC: “A and E are both liars.”\nD: “Exactly two of us five are truth-tellers.”\nE: “D is a truth-teller.”',
  [T('How many of the five are truth-tellers?', 2,
     'A and C are of the same type (A vouches for C). If C told the truth, A would be truthful, contradicting C’s own claim — so A and C are liars. '
     'C’s claim is then false with A a liar, so E is truthful, and so is D. B’s claim (A and D alike) is false. Truth-tellers: D and E.',
     lambda: str(sum(kn.values()))),
   M('Which two of them are both liars?', ['A and B', 'B and D', 'C and E', 'A and E'],
     'The liars are A, B and C.', lambda: next(o for o in ['A and B', 'B and D', 'C and E', 'A and E'] if not kn[o[0]] and not kn[o[-1]])),
   M('Whose statement is true?', ['E’s', 'A’s', 'B’s', 'C’s'], 'Only D and E tell the truth; of the options, E.',
     lambda: next(o for o in ['E’s', 'A’s', 'B’s', 'C’s'] if kn[o[0]])),
   T('Suppose B had instead said “A and D are of different types”, and the other statements were unchanged. How many different assignments of types to the five would then be consistent with all the statements?', 0,
     'The argument for A, C, D and E does not involve B, so D and E are still truth-tellers and A and C liars. B’s new claim would be true, making three truth-tellers — but D says exactly two. No assignment works.',
     lambda: str(len(kn_solutions({**kn_claims, 'B': lambda t: t['A'] != t['D']}))), difficulty='HARD')])


def pod_truths(c):
    s = {'A': c in 'BC', 'B': c not in 'BD', 'D': c == 'A', 'E': c != 'C'}
    s['C'] = s['E']
    return s


pods = {c: pod_truths(c) for c in KN}
by_true = {k: [c for c in KN if sum(pods[c].values()) == k] for k in range(6)}
assert by_true[4] == ['A']
S('b11', 'Binary logic (truth/lie)', 'The missing coffee pod',
  'One of five colleagues A, B, C, D and E took the last coffee pod. Questioned, they said:\n'
  'A: “It was B or C.”\nB: “It was neither D nor me.”\nC: “E is telling the truth.”\nD: “It was A.”\nE: “It wasn’t C.”\n'
  'Exactly one of the five statements is false.',
  [M('Who took the coffee pod?', ['A', 'B', 'C', 'E'],
     'C and E stand or fall together, so with one false statement both are true: not C. D and A cannot both be true. If D is true (A took it) only A’s statement is false — consistent. '
     'If D is false, A must be true and it was B — but then B’s statement is false too.',
     lambda: by_true[4][0]),
   M('Whose statement is the false one?', ['A’s', 'B’s', 'D’s', 'E’s'], 'A took the pod, so A’s claim that it was B or C is false; the rest are true.',
     lambda: next(f'{n}’s' for n in KN if not pods[by_true[4][0]][n])),
   T('If exactly three of the statements were true instead, how many of the five could have taken the pod?', 2,
     'Checking each culprit: A makes 4 statements true, B 3, C 2, D 2 and E 3. So B or E.',
     lambda: str(len(by_true[3]))),
   M('If exactly two of the statements were true, which of these could have taken the pod?', ['Either C or D', 'Only C', 'Only D', 'Either B or E'],
     'Exactly two statements are true when C took it (A and D true … ) or when D did. Both C and D give two true statements.',
     lambda: 'Either C or D' if by_true[2] == ['C', 'D'] else None, difficulty='HARD')])
# fix the wording of the last explanation to list the true statements precisely
SETS[-1]['questions'][3]['explanation'] = (
    'If C took it, only A’s and B’s statements are true; if D took it, only A’s and … ' if False else
    'Count true statements for each culprit: A → 4, B → 3, C → 2 (A’s and B’s), D → 2 (C’s and E’s), E → 3. So exactly two are true only if C or D took it.')
assert [n for n in KN if pods['C'][n]] == ['A', 'B'] and [n for n in KN if pods['D'][n]] == ['C', 'E']

# ============================================================ Selections & Distributions

CAND = ['E1', 'E2', 'E3', 'D1', 'D2', 'D3', 'L1', 'L2']


def valid_committee(s):
    e, d, l = (sum(x[0] == k for x in s) for k in 'EDL')
    return (e >= 1 and d >= 1 and l >= 1 and e <= 2 and not ('E1' in s and 'D1' in s)
            and ('L1' not in s or 'E2' in s) and (('D2' in s) == ('D3' in s)))


comms = [set(c) for c in combinations(CAND, 5) if valid_committee(set(c))]
seat_count = Counter(x for c in comms for x in c)
S('b12', 'Selections & Distributions', 'Forming a committee',
  'A five-member committee is to be chosen from three engineers (E1, E2, E3), three doctors (D1, D2, D3) and two lawyers (L1, L2), subject to these rules:\n'
  '1. The committee has at least one engineer, one doctor and one lawyer.\n2. It has at most two engineers.\n3. E1 and D1 cannot both be on it.\n'
  '4. If L1 is on it, E2 must be on it too.\n5. D2 and D3 are either both on it or both off it.',
  [T('How many different committees can be formed?', 10,
     'Listing by doctors: with D2 and D3 (and not D1) the other three come from engineers and lawyers — 6 valid choices; with D1 alone, 1; with D1, D2 and D3, 3. Total 10.',
     lambda: str(len(comms))),
   T('In how many of the possible committees is E1 a member?', 3,
     'E1 excludes D1, so D2 and D3 are in; the remaining two are one more engineer or lawyer combination: {E2, L1}, {E2, L2} or {E3, L2} (E3 with L1 would break rule 4).',
     lambda: str(sum('E1' in c for c in comms))),
   M('If both lawyers are on the committee, who else must be on it?', ['E2', 'E3', 'D2', 'D1'],
     'Both-lawyer committees are {D1, E2, E3, L1, L2} and {D2, D3, E2, L1, L2}; L1 forces E2 in both.',
     lambda: next(x for x in ['E2', 'E3', 'D2', 'D1'] if all(x in c for c in comms if {'L1', 'L2'} <= c))),
   M('Which candidate is a member of the largest number of possible committees?', ['D2 (and equally D3)', 'E2', 'L2', 'E3'],
     'Counts: D2 and D3 9 each, E2 8, L2 7, L1 5, E3 5, D1 4, E1 3.',
     lambda: 'D2 (and equally D3)' if seat_count.most_common(1)[0][1] == seat_count['D2'] == seat_count['D3'] > seat_count['E2'] else None, difficulty='HARD')])

shares = [t for t in product(range(2, 17), repeat=4) if sum(t) == 16 and t[0] > max(t[1:]) and t[1] == t[2] and t[3] % 2 == 0]
S('b13', 'Selections & Distributions', 'Sharing chocolates',
  'Sixteen identical chocolates are shared among four children A, B, C and D, with every chocolate given out. Each child gets at least two. '
  'A gets more than each of the other three, B and C get the same number, and D gets an even number.',
  [T('In how many different ways can the chocolates be shared?', 5,
     'With B = C = b and D = d: A = 16 − 2b − d must exceed b and d. The options (A, B, C, D) are (6, 3, 3, 4), (6, 4, 4, 2), (8, 2, 2, 4), (8, 3, 3, 2) and (10, 2, 2, 2).',
     lambda: str(len(shares))),
   T('What is the largest number of chocolates D can get?', 4, 'D = 6 would need A ≥ 7 and B = C ≥ 2, totalling at least 17.',
     lambda: str(max(t[3] for t in shares))),
   T('If D gets as many chocolates as B, how many does A get?', 10, 'Only (10, 2, 2, 2) has D = B.',
     lambda: (lambda xs: str(xs[0]) if len(xs) == 1 else None)([t[0] for t in shares if t[3] == t[1]])),
   M('Which of the following is true in every possible sharing?', ['A gets at least 6 chocolates.', 'D gets 2 chocolates.', 'B gets 2 chocolates.', 'A gets exactly twice as many as D.'],
     'A gets 6, 8 or 10 in the five sharings. D, B and the A : D ratio all vary.',
     lambda: next(o for o, f in [('A gets at least 6 chocolates.', lambda t: t[0] >= 6), ('D gets 2 chocolates.', lambda t: t[3] == 2),
                                 ('B gets 2 chocolates.', lambda t: t[1] == 2), ('A gets exactly twice as many as D.', lambda t: t[0] == 2 * t[3])]
                  if all(f(t) for t in shares)), difficulty='HARD')])

# ============================================================ Venn diagrams

total_v, nx, ny, nz, two, three = 300, 150, 130, 120, 105, 30
one = nx + ny + nz - 2 * two - 3 * three
none_v = total_v - one - two - three
xy_only, xz_only = 40, 35
yz_only = two - xy_only - xz_only
x_only = nx - xy_only - xz_only - three
S('b14', 'Venn diagrams', 'Magazine readers',
  'In a survey of 300 people about three magazines X, Y and Z, 150 read X, 130 read Y and 120 read Z. 105 people read exactly two of the magazines and 30 read all three.',
  [T('How many of those surveyed read none of the three magazines?', 65,
     'Adding the readers of X, Y and Z counts “exactly one” readers once, “exactly two” twice and “all three” three times: 400 = e₁ + 2 × 105 + 3 × 30, so e₁ = 100. Readers = 235, so 65 read none.',
     lambda: str(none_v)),
   T('How many people read exactly one magazine?', 100, 'From the count above, e₁ = 400 − 210 − 90 = 100.', lambda: str(one)),
   T('If 40 people read X and Y but not Z, and 35 read X and Z but not Y, how many read Y and Z but not X?', 30,
     'The three “exactly two” groups add up to 105: 105 − 40 − 35 = 30.', lambda: str(yz_only)),
   T('With the figures in the previous question, how many people read only X?', 45,
     'X = only X + 40 + 35 + 30 (all three): only X = 150 − 105 = 45.', lambda: str(x_only), difficulty='HARD')])

# ============================================================ Arrangements (linear/circular)

ROUND = 'PQRSTU'
DRINKS = ['tea', 'coffee', 'juice', 'milk', 'soda', 'water']


def round_solutions(extra=lambda at, cw, d, who: True, fix_drinks=True):
    out = []
    for perm in permutations(ROUND[1:]):
        at = ('P',) + perm
        seat = {x: i for i, x in enumerate(at)}
        cw = lambda x, k=1: at[(seat[x] + k) % 6]
        if not (cw('P', 3) != 'Q' and cw('R') == 'P' and cw('S', 3) == 'T' and 'U' not in (cw('P'), cw('P', -1))
                and 'S' in (cw('Q'), cw('Q', -1))):
            continue
        for dp in permutations(DRINKS):
            d = dict(zip(at, dp))
            who = {v: k for k, v in d.items()}
            if fix_drinks and not (d['Q'] == 'soda' and cw('U') == who['tea'] and cw(who['coffee'], 3) == who['tea']
                                   and d['R'] in ('juice', 'milk') and cw(who['milk'], 3) == who['soda'] and d['P'] != 'water'):
                continue
            if extra(at, cw, d, who):
                out.append((at, d))
    return out


rs = round_solutions()
assert len(rs) == 1
r_at, r_d = rs[0]
r_seat = {x: i for i, x in enumerate(r_at)}
r_cw = lambda x, k=1: r_at[(r_seat[x] + k) % 6]
S('b15', 'Arrangements (linear/circular)', 'Six friends at a round table',
  'Six friends P, Q, R, S, T and U sit at equal intervals around a round table, and each drinks a different one of tea, coffee, juice, milk, soda and water. '
  '“Clockwise” is as seen from above the table.\n'
  '1. P is the next person clockwise from R.\n2. S sits opposite T, and P does not sit opposite Q.\n3. U is not next to P.\n4. Q sits next to S.\n'
  '5. Q drinks soda, and the milk drinker sits opposite Q.\n6. The tea drinker is the next person clockwise from U, and the coffee drinker sits opposite the tea drinker.\n'
  '7. R drinks juice or milk, and P does not drink water.',
  [M('Who sits opposite R?', ['Q', 'S', 'U', 'T'], 'Going clockwise from P the order is P, S, Q, U, T, R, so R is opposite Q.',
     lambda: r_cw('R', 3)),
   M('What does U drink?', ['Water', 'Juice', 'Tea', 'Milk'],
     'T (clockwise from U) drinks tea and S, opposite T, coffee. R sits opposite Q, so R drinks milk. P cannot drink water, so P drinks juice and U water.',
     lambda: r_d['U'].capitalize()),
   T('Going clockwise from P, how many people sit between P and the tea drinker?', 3,
     'The order is P, S, Q, U, T: S, Q and U sit between P and T.', lambda: str((r_seat[{v: k for k, v in r_d.items()}['tea']] - r_seat['P']) % 6 - 1)),
   T('If clue 7 were dropped, in how many different ways could the drinks be assigned (the seating staying as it is)?', 3,
     'Seating, tea (T), coffee (S), soda (Q) and milk (R) are fixed without clue 7; juice and water can then go to P and U either way … but R’s milk comes from clue 5, so only P and U vary: 2 ways. ',
     lambda: None, difficulty='HARD')])
_n7 = len({tuple(sorted(d.items())) for at, d in round_solutions(
    extra=lambda at, cw, d, who: d['Q'] == 'soda' and cw('U') == who['tea'] and cw(who['coffee'], 3) == who['tea'] and cw(who['milk'], 3) == who['soda'],
    fix_drinks=False)})
SETS[-1]['questions'][3] = T(
    'If clue 7 were dropped, in how many different ways could the drinks be assigned (the seating staying as it is)?', _n7,
    'Without clue 7, tea (T), coffee (S), soda (Q) and milk (R, opposite Q) are still forced. Juice and water can go to P and U either way: 2 assignments.',
    lambda: str(_n7) if _n7 == 2 else None, difficulty='HARD')

FLOOR_CLUES = [
    ('A lives on an odd-numbered floor, above D.', lambda f: f['A'] % 2 == 1 and f['A'] > f['D']),
    ('Exactly two people live between A and B.', lambda f: abs(f['A'] - f['B']) == 3),
    ('C lives on the floor immediately below E.', lambda f: f['E'] == f['C'] + 1),
    ('G lives above F, but not on the floor immediately above F.', lambda f: f['G'] - f['F'] >= 2),
    ('D does not live on floor 1, and G does not live on floor 7.', lambda f: f['D'] != 1 and f['G'] != 7),
    ('B lives above C.', lambda f: f['B'] > f['C']),
    ('D and E do not live on adjacent floors, and neither do F and C.', lambda f: abs(f['D'] - f['E']) > 1 and abs(f['F'] - f['C']) > 1),
    ('D lives on an even-numbered floor.', lambda f: f['D'] % 2 == 0),
]


def floor_solutions(clues):
    return [f for f in (dict(zip('ABCDEFG', p)) for p in permutations(range(1, 8))) if all(c(f) for _, c in clues)]


fl = floor_solutions(FLOOR_CLUES)
assert len(fl) == 1
fl = fl[0]
on = {v: k for k, v in fl.items()}
S('b16', 'Arrangements (linear/circular)', 'Seven floors',
  'Seven people A, B, C, D, E, F and G live in a seven-storey building, one on each floor; floor 1 is the lowest and floor 7 the highest.\n'
  + '\n'.join(f'{i}. {t}' for i, (t, _) in enumerate(FLOOR_CLUES, 1)),
  [M('Who lives on floor 4?', ['B', 'F', 'G', 'D'], 'From the bottom: C, E, F, B, G, D, A.', lambda: on[4]),
   T('How many people live between C and G?', 3, 'C is on floor 1 and G on floor 5: E, F and B are in between.', lambda: str(abs(fl['C'] - fl['G']) - 1)),
   M('Who lives immediately above F?', ['B', 'G', 'E', 'D'], 'F is on floor 3 and B on floor 4.', lambda: on[fl['F'] + 1]),
   T('If clue 8 were dropped, how many arrangements would satisfy the remaining clues?', 2,
     'Clues 1–7 allow C, E, F, B on floors 1–4 and A on 7, with D and G on floors 5 and 6 either way. Clue 8 puts D on 6.',
     lambda: str(len(floor_solutions(FLOOR_CLUES[:7]))), difficulty='HARD')])

# ============================================================ Scheduling & Calendars

WS = ['Design', 'Finance', 'Law', 'Marketing', 'Strategy']
TR = ['Iyer', 'Joshi', 'Khan', 'Lal', 'Mehta']
DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']


def schedules(with_lal=True):
    out = []
    for wp in permutations(range(5)):
        d = dict(zip(WS, wp))
        if not (d['Finance'] == d['Law'] + 1 and d['Strategy'] in (0, 4) and d['Design'] != 1):
            continue
        for tp in permutations(WS):
            t = dict(zip(TR, tp))
            day = {k: d[v] for k, v in t.items()}
            if (t['Khan'] == 'Marketing' and day['Joshi'] == 2 and day['Iyer'] == day['Khan'] - 1
                    and t['Mehta'] not in ('Design', 'Strategy') and t['Lal'] != 'Strategy'
                    and (not with_lal or day['Lal'] > d['Finance'])):
                out.append((d, t))
    return out


sch = schedules()
assert len(sch) == 1
sd, st = sch[0]
on_day = {v: k for k, v in sd.items()}
led = {v: k for k, v in st.items()}
S('b17', 'Scheduling & Calendars', 'A week of workshops',
  'Five workshops — Design, Finance, Law, Marketing and Strategy — are held from Monday to Friday, one a day, each led by a different trainer: Iyer, Joshi, Khan, Lal and Mehta.\n'
  '1. The Finance workshop is held on the day after the Law workshop.\n2. Strategy is held on Monday or Friday, and Design is not held on Tuesday.\n'
  '3. Khan leads Marketing, and Iyer’s workshop is on the day before Khan’s.\n4. Joshi’s workshop is on Wednesday.\n'
  '5. Mehta leads neither Design nor Strategy, and Lal does not lead Strategy.\n6. Lal’s workshop is later in the week than the Finance workshop.',
  [M('Which workshop is held on Wednesday?', ['Law', 'Design', 'Finance', 'Marketing'],
     'The schedule is Mon Strategy (Iyer), Tue Marketing (Khan), Wed Law (Joshi), Thu Finance (Mehta), Fri Design (Lal).', lambda: on_day[2]),
   M('Who leads the Finance workshop?', ['Mehta', 'Lal', 'Joshi', 'Iyer'],
     'Lal must come after Finance, so Finance is not on Friday; that forces Law–Finance on Wednesday–Thursday and Mehta, the only trainer left who can, leads Finance.',
     lambda: led['Finance']),
   T('How many workshops are held between Marketing and Design?', 2, 'Marketing is on Tuesday and Design on Friday: Law and Finance fall in between.',
     lambda: str(abs(sd['Marketing'] - sd['Design']) - 1)),
   T('If clue 6 were dropped, how many different schedules (workshop and trainer for each day) would be possible?', 3,
     'Without clue 6 there are three: the one above, and two with Design on Wednesday and Law–Finance on Thursday–Friday (Mehta and Lal leading those in either order).',
     lambda: str(len(schedules(with_lal=False))), difficulty='HARD')])

# ============================================================ Games & Tournaments

TEAMS = 'ABCDE'
MATCHES = list(combinations(TEAMS, 2))
POINTS = dict(A=10, B=7, C=6, D=4, E=1)


def table(res):
    p = dict.fromkeys(TEAMS, 0)
    for (x, y), r in zip(MATCHES, res):
        if r == 0:
            p[x] += 1
            p[y] += 1
        else:
            p[x if r == 1 else y] += 3
    return p


grids = [dict(zip(MATCHES, res)) for res in product(range(3), repeat=10) if table(res) == POINTS]
assert len(grids) == 4
beat = lambda g, x, y: g[(x, y)] == 1 if (x, y) in g else g[(y, x)] == 2
drew = lambda g, x, y: g[tuple(sorted((x, y)))] == 0
S('b18', 'Games & Tournaments', 'A five-team league',
  'Five teams A, B, C, D and E play a league in which every team plays every other team once. A win earns 3 points, a draw 1 point to each side and a loss nothing. '
  'At the end, the teams have A 10, B 7, C 6, D 4 and E 1 point.',
  [T('How many matches ended in a draw?', 2,
     'Ten matches were played. Each decided match gives out 3 points and each draw 2; the total is 28, so 30 − 28 = 2 draws.',
     lambda: (lambda s: str(s.pop()) if len(s) == 1 else None)({sum(v == 0 for v in g.values()) for g in grids})),
   M('Which of these results is certain?', ['A beat C', 'A beat B', 'B beat D', 'D beat E'],
     'A’s 10 points are 3 wins and a draw, and C (6 points, no draws possible with 6 = 3 + 3 … ) — checking every consistent results table, A beat C in all of them; the other three results vary.',
     lambda: next(o for o in ['A beat C', 'A beat B', 'B beat D', 'D beat E'] if all(beat(g, o[0], o[-1]) for g in grids))),
   M('If the match between A and B was drawn, which of the following must be true?', ['D and E drew', 'B beat D', 'C beat D', 'B beat C'],
     'With A–B drawn, the only draw left must involve E (E has 1 point), and only D–E fits; B–D and B–C can go either way.',
     lambda: next(o for o, f in [('D and E drew', lambda g: drew(g, 'D', 'E')), ('B beat D', lambda g: beat(g, 'B', 'D')),
                                 ('C beat D', lambda g: beat(g, 'C', 'D')), ('B beat C', lambda g: beat(g, 'B', 'C'))]
                  if all(f(g) for g in grids if drew(g, 'A', 'B')))),
   M('If E’s only point came from a draw with B, what was the result of the match between A and D?', ['A and D drew', 'A beat D', 'D beat A', 'It cannot be determined'],
     'Then A’s draw cannot be with B or E; the consistent table has A drawing with D.',
     lambda: (lambda gs: 'A and D drew' if gs and all(drew(g, 'A', 'D') for g in gs) else None)([g for g in grids if drew(g, 'B', 'E')]),
     difficulty='HARD')])
SETS[-1]['questions'][1]['explanation'] = ('Of the four results tables consistent with the points, A beat C in every one: C’s 6 points must be two wins and two losses, and one of those losses is always to A. '
                                           'A–B, B–D and D–E each differ between tables.')

# ============================================================ Networks & Routes

ROADS = {('W', 'A'): 12, ('W', 'B'): 20, ('A', 'B'): 6, ('A', 'C'): 15, ('B', 'C'): 7, ('B', 'D'): 18,
         ('C', 'D'): 9, ('C', 'E'): 20, ('D', 'E'): 8, ('D', 'F'): 14, ('E', 'F'): 5}


def dist(a, b, closed=()):
    g = {}
    for (x, y), w in ROADS.items():
        if (x, y) in closed:
            continue
        g.setdefault(x, []).append((y, w))
        g.setdefault(y, []).append((x, w))
    best, heap = {a: 0}, [(0, a)]
    while heap:
        d, u = heapq.heappop(heap)
        if u == b:
            return d
        for v, w in g.get(u, []):
            if d + w < best.get(v, 1e9):
                best[v] = d + w
                heapq.heappush(heap, (d + w, v))


def on_all_shortest(a, b):
    return sorted(v for v in 'ABCDEF' if v not in (a, b) and dist(a, v) + dist(v, b) == dist(a, b)
                  and dist(a, b, closed=tuple(k for k in ROADS if v in k)) != dist(a, b))


road_rows = list(ROADS.items())
road_table = '\n'.join('    '.join(f'{x}–{y}  {w:>2} min' for (x, y), w in road_rows[i:i + 4]) for i in range(0, len(road_rows), 4))
S('b19', 'Networks & Routes', 'Delivery roads',
  'A courier firm’s warehouse W serves six towns A–F. The only roads are the two-way roads below, with their travel times:\n\n'
  + road_table + '\n\nVans always take the quickest available route.',
  [T('What is the shortest travel time, in minutes, from W to F?', 47, 'W–A–B–C–D–E–F: 12 + 6 + 7 + 9 + 8 + 5 = 47. Going D–F directly takes 48.',
     lambda: str(dist('W', 'F'))),
   T('The road between C and D is closed. What is now the shortest travel time from W to F, in minutes?', 49,
     'Best routes avoiding C–D: W–A–B–D–E–F = 12 + 6 + 18 + 8 + 5 = 49 (W–A–B–C–E–F and W–A–B–D–F take 50).',
     lambda: str(dist('W', 'F', closed=(('C', 'D'),)))),
   T('How many of the towns A–D lie on the quickest route from W to E?', 4,
     'The quickest route is W–A–B–C–D–E (42 minutes); W–A–B–C–E takes 45. All four of A, B, C and D are on it.',
     lambda: str(len([v for v in on_all_shortest('W', 'E') if v in 'ABCD']))),
   T('A van leaves W, must deliver at both D and F, and then return to W. What is the least total travel time, in minutes?', 94,
     'W to D is 34 and D to F is 13 (via E, quicker than the direct road). F back to W is 47. 34 + 13 + 47 = 94; doing F first gives the same total.',
     lambda: str(min(dist('W', a) + dist(a, b) + dist(b, 'W') for a, b in (('D', 'F'), ('F', 'D')))), difficulty='HARD')])


def sets():
    out = []
    for s in SETS:
        qs = []
        for q in s['questions']:
            got = q['check']()
            want = q['options'][0] if q['type'] == 'MCQ' else q['answer']
            ok = got is not None and (str(got) == want if q['type'] == 'MCQ' else float(got) == float(want))
            assert ok, f"{s['id']} {q['prompt'][:50]!r}: key {want!r} but the check computes {got!r}"
            if q['type'] == 'MCQ':
                assert len(set(q['options'])) == 4, f"{s['id']} {q['prompt'][:50]!r}: options are not four distinct values"
            qs.append({k: v for k, v in q.items() if k != 'check'})
        out.append({**s, 'questions': qs})
    return out
