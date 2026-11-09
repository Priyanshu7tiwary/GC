"""DILR: the 30 set briefs from the CAT 2026 Original Advanced Question Bank, rewritten as real CAT-style sets.

The file's 150 questions were generic prompts with identical options in every set, so each set now has three
questions written against its own data. Briefs whose numbers were inconsistent or produced fractions were
adjusted (noted per set). Every answer is computed from the data, and logic sets are solved by brute force so
a keyed answer is the only one possible.
"""
import math
from fractions import Fraction as F
from itertools import permutations, product

SETS = []


def S(id, topic, title, passage, questions, note=''):
    SETS.append(dict(id=id, topic=topic, title=title, passage=passage, questions=questions, note=note))


def M(prompt, options, explanation, check):
    return dict(type='MCQ', prompt=prompt, options=options, explanation=explanation, check=check)


def T(prompt, answer, explanation, check):
    return dict(type='TITA', prompt=prompt, answer=str(answer), explanation=explanation, check=check)


p1 = dict(P=30, Q=20, R=40, S=30)
adm1 = dict(P=24, Q=15, R=32, S=24)
S('s01', 'Tables & Caselets', 'Admissions allocation',
  'A university allocates 120 seats across four programmes P, Q, R and S. P has 10 more seats than Q, R has twice as many seats as Q, and S has 10 fewer seats than R. Each programme fills 80% of its seats, except Q, which fills 75%.',
  [T('How many students are admitted in total?', 95, 'Q + 10 + Q + 2Q + 2Q − 10 = 120 gives Q = 20, so P = 30, R = 40, S = 30. Admitted: 24 + 15 + 32 + 24 = 95.',
     lambda: str(sum(adm1.values()))),
   M('Which programme has the most unfilled seats?', ['R', 'P', 'Q', 'S'],
     'Unfilled seats: P 6, Q 5, R 8, S 6.', lambda: max(p1, key=lambda k: p1[k] - adm1[k])),
   M('If R’s capacity is increased by 10 seats and it still fills 80% of its seats, what percentage of all seats is filled?',
     ['79.23%', '79.17%', '80.00%', '73.08%'],
     'Seats = 130 and admitted = 95 + 8 = 103, so 103/130 = 79.23%.', lambda: f'{103 / 130 * 100:.2f}%')],
  note='Fill rates changed from 75%/80% so that every programme admits a whole number of students.')

out2 = dict(A=600, B=900, C=1200)
x2 = dict(A=360, B=450, C=480)
y2 = {k: out2[k] - x2[k] for k in out2}
dx = dict(A=F(5, 100), B=F(2, 100), C=F(5, 100))
dy = dict(A=F(5, 100), B=F(2, 100), C=F(25, 1000))
defx = {k: x2[k] * dx[k] for k in x2}
defy = {k: y2[k] * dy[k] for k in y2}
S('s02', 'Tables & Caselets', 'Factory production',
  'Three factories A, B and C produce components X and Y. Their total output of 2,700 units is split in the ratio 2 : 3 : 4. Component X makes up 60% of A’s output, 50% of B’s and 40% of C’s. The defect rate for X is 5% at A, 2% at B and 5% at C; for Y it is 5% at A, 2% at B and 2.5% at C.',
  [T('How many defective units do the three factories produce in all?', 90, 'Outputs 600, 900, 1200. Defective X: 18 + 9 + 24 = 51; defective Y: 12 + 9 + 18 = 39. Total 90.',
     lambda: str(sum(defx.values()) + sum(defy.values()))),
   M('Which factory has the highest overall defect rate?', ['A', 'C', 'B', 'A and C equally'],
     'A: 30/600 = 5%; B: 18/900 = 2%; C: 42/1200 = 3.5%.', lambda: max(out2, key=lambda k: (defx[k] + defy[k]) / out2[k])),
   M('What share of all defective units are component Y?', ['43.33%', '56.67%', '39.00%', '46.67%'],
     '39 of the 90 defective units are Y: 43.33%.', lambda: f'{float(sum(defy.values()) / 90 * 100):.2f}%')],
  note='Output and defect rates adjusted so defect counts are whole numbers.')

subs = dict(Basic=5000, Plus=3000, Pro=2000)
mob = dict(Basic=F(40, 100), Plus=F(60, 100), Pro=F(80, 100))
mobile = sum(subs[k] * mob[k] for k in subs)
live = mobile * F(3, 10) + (10000 - mobile) * F(2, 10)
S('s03', 'Quant-based LR', 'Streaming subscriptions',
  'A streaming platform has 10,000 subscribers on Basic, Plus and Pro plans in the ratio 5 : 3 : 2. 40% of Basic, 60% of Plus and 80% of Pro subscribers use the mobile app. 30% of mobile-app users watch live content, against 20% of the other subscribers.',
  [T('How many subscribers watch live content?', 2540, 'Mobile users: 2000 + 1800 + 1600 = 5400; others 4600. Live: 1620 + 920 = 2540.', lambda: str(live)),
   M('What percentage of mobile-app users are on the Pro plan?', ['29.63%', '33.33%', '20.00%', '80.00%'],
     '1600 of 5400 mobile users are Pro: 29.63%.', lambda: f'{float(1600 / mobile * 100):.2f}%'),
   T('If 55% of Basic subscribers used the mobile app (other figures unchanged), by how many would the number of live viewers increase?',
     75, '750 more Basic subscribers move from the 20% group to the 30% group: 750 × 10% = 75.',
     lambda: str(5000 * (F(55, 100) - F(40, 100)) * F(1, 10)))],
  note='Absolute subscriber count added so questions have numeric answers.')

res4 = {('A', 'B'): 'A', ('A', 'C'): 'A', ('A', 'D'): 'A', ('A', 'E'): 'E', ('A', 'F'): None, ('B', 'C'): 'B', ('B', 'D'): 'B',
        ('B', 'E'): None, ('B', 'F'): 'F', ('C', 'D'): 'C', ('C', 'E'): 'C', ('C', 'F'): 'F', ('D', 'E'): 'D', ('D', 'F'): 'D', ('E', 'F'): 'E'}


def pts4(results):
    p = dict.fromkeys('ABCDEF', 0)
    for (x, y), w in results.items():
        if w is None:
            p[x] += 1; p[y] += 1
        else:
            p[w] += 3
    return p


S('s04', 'Games & Tournaments', 'Tournament table',
  'Six teams A–F play a round-robin tournament, each pair meeting once. A win earns 3 points and a draw 1 point to each team. A beats B, C and D, loses to E and draws with F. B beats C and D, draws with E and loses to F. C beats D and E and loses to F. D beats E and F. E beats F.',
  [M('How many teams finish with exactly 7 points?', ['3', '2', '1', '4'],
     'Points: A 10, B 7, C 6, D 6, E 7, F 7. B, E and F have 7.', lambda: str(sum(1 for v in pts4(res4).values() if v == 7))),
   T('How many points are awarded in the tournament in total?', 43, '13 decided matches × 3 + 2 draws × 2 = 43.', lambda: str(sum(pts4(res4).values()))),
   M('Had E beaten B instead of drawing, which team would have finished second?', ['E', 'A', 'F', 'B'],
     'E would have 9 points, behind A’s 10 and ahead of everyone else.',
     lambda: sorted(pts4({**res4, ('B', 'E'): 'E'}).items(), key=lambda kv: -kv[1])[1][0])])

dep5 = dict(A=40, B=60, C=50, D=30, E=70)
serve5 = dict(A=(1, 2), B=(2, 3), C=(3, 4), D=(4, 5), E=(5, 1))


def zones5(dep):
    z = dict.fromkeys(range(1, 6), 0)
    for d, (a, b) in serve5.items():
        z[a] += dep[d] / 2; z[b] += dep[d] / 2
    return z


S('s05', 'Networks & Routes', 'Delivery routes',
  'Five depots A–E deliver to zones 1–5. Each depot serves exactly two zones and splits its daily deliveries equally between them: A serves zones 1 and 2, B serves 2 and 3, C serves 3 and 4, D serves 4 and 5, and E serves 5 and 1. Daily deliveries are A 40, B 60, C 50, D 30 and E 70.',
  [M('Which zone receives the fewest deliveries?', ['Zone 4', 'Zone 2', 'Zone 5', 'Zone 1'],
     'Zone totals: 1 → 55, 2 → 50, 3 → 55, 4 → 40, 5 → 50.', lambda: f'Zone {min(zones5(dep5), key=zones5(dep5).get)}'),
   M('If depot E doubled its deliveries, how many zones would receive more than 50 deliveries?', ['3', '2', '4', '1'],
     'E sends 70 to each of zones 1 and 5: zone 1 → 90, zone 5 → 85; zone 3 stays at 55. Three zones exceed 50.',
     lambda: str(sum(1 for v in zones5({**dep5, 'E': 140}).values() if v > 50))),
   T('How many deliveries do zones 1, 3 and 5 receive together?', 160, '55 + 55 + 50 = 160.',
     lambda: str(round(sum(zones5(dep5)[z] for z in (1, 3, 5)))))],
  note='Delivery volumes added so the set has questions to answer.')


def s06(drop_last=False):
    out = []
    for vals in permutations((210, 225, 240, 255)):
        v = dict(zip('PQRS', vals))
        if v['P'] > v['Q'] and v['P'] < v['R'] and v['S'] != min(vals) and v['Q'] != 225 and (drop_last or v['R'] == v['S'] + 15):
            out.append(v)
    return out


S('s06', 'Arrangements (linear/circular)', 'Exam scores',
  'Four candidates P, Q, R and S took an exam. Their total scores were 210, 225, 240 and 255, in some order. P scored more than Q but less than R. S did not have the lowest score. Q did not score 225. R scored exactly 15 more than S.',
  [M('Who scored 240?', ['S', 'P', 'R', 'Q'], 'Q must be lowest (210). With R = S + 15 and P < R, the only order is Q 210, P 225, S 240, R 255.',
     lambda: next(k for k, v in s06()[0].items() if v == 240) if len(s06()) == 1 else None),
   T('By how many marks did S outscore P?', 15, 'S 240 − P 225 = 15.', lambda: str(s06()[0]['S'] - s06()[0]['P'])),
   M('If the last condition (R scored 15 more than S) were removed, how many orders of scores would be possible?', ['3', '1', '2', '4'],
     'Q is still 210; P, R, S can be (225, 240, 255), (225, 255, 240) or (240, 255, 225).', lambda: str(len(s06(drop_last=True))))],
  note='Rewritten: the original test-by-test conditions did not determine anything.')


def s07(drop_f=False):
    out = []
    for shifts in product('MAN', repeat=6):
        a = dict(zip('ABCDEF', shifts))
        if any(list(shifts).count(s) != 2 for s in 'MAN'):
            continue
        if a['C'] == a['D'] and a['A'] != a['B'] and a['E'] == 'M' and a['A'] != 'M' and a['B'] != 'A' and (drop_f or a['F'] != 'N'):
            out.append(a)
    return out


S('s07', 'Scheduling & Calendars', 'Warehouse shifts',
  'Six workers A–F are each assigned to one of three shifts (morning, afternoon, night), two workers per shift. C and D work the same shift. A and B work different shifts. E works the morning shift. A does not work the morning shift, B does not work the afternoon shift, and F does not work the night shift.',
  [M('Who works the same shift as A?', ['F', 'B', 'E', 'C'], 'C and D cannot share the morning with E, and the only consistent assignment is morning E, B; afternoon A, F; night C, D.',
     lambda: next(w for w, s in s07()[0].items() if s == s07()[0]['A'] and w != 'A') if len(s07()) == 1 else None),
   M('Which shift do C and D work?', ['Night', 'Afternoon', 'Morning', 'Cannot be determined'], 'See the unique assignment above.',
     lambda: {'M': 'Morning', 'A': 'Afternoon', 'N': 'Night'}[s07()[0]['C']]),
   T('If the condition on F were removed, how many valid assignments would there be?', 2,
     'C and D could then also take the afternoon shift, with A and F on nights and B in the morning.', lambda: str(len(s07(drop_f=True))))],
  note='Rewritten as a single-day roster: the original two-day conditions did not determine the shifts.')

inv = dict(E=4, D=2, G=3)
y1 = dict(E=inv['E'] * 1.12, D=inv['D'] * 1.07, G=inv['G'] * 1.05)
y2v = dict(E=y1['E'] * 0.9, D=y1['D'] * 1.06, G=y1['G'])
S('s08', 'Tables & Caselets', 'Investment portfolio',
  'An investor splits ₹9 lakh among equity, debt and gold. The equity amount is twice the debt amount, and gold is ₹1 lakh less than equity. In year one the investments return +12%, +7% and +5% respectively. In year two equity falls 10%, debt rises 6% and gold is unchanged.',
  [M('What is the portfolio worth at the end of year one?', ['₹9.77 lakh', '₹9.72 lakh', '₹9.81 lakh', '₹9.45 lakh'],
     'Equity 4, debt 2, gold 3 lakh. After year one: 4.48 + 2.14 + 3.15 = ₹9.77 lakh.', lambda: f'₹{sum(y1.values()):.2f} lakh'),
   M('What is the overall percentage gain after two years?', ['5.00%', '8.56%', '4.50%', '3.20%'],
     'After year two: 4.032 + 2.2684 + 3.15 = ₹9.4504 lakh, a gain of 5.00% on ₹9 lakh.', lambda: f'{(sum(y2v.values()) / 9 - 1) * 100:.2f}%'),
   M('Which investment gained the most in rupees over the two years?', ['Debt', 'Equity', 'Gold', 'Equity and gold equally'],
     'Gains: equity ₹0.032 lakh, debt ₹0.268 lakh, gold ₹0.15 lakh.', lambda: {'E': 'Equity', 'D': 'Debt', 'G': 'Gold'}[max(inv, key=lambda k: y2v[k] - inv[k])])],
  note='Total changed from ₹10 lakh to ₹9 lakh so the amounts are whole lakhs.')

order9 = [o for o in permutations('ABCDE') if o.index('A') > o.index('B') and o.index('A') < o.index('C') and o.index('D') < o.index('B') and o.index('E') > o.index('C')]
days9 = dict(zip(order9[0], (6, 9, 12, 15, 18)))
S('s09', 'Arrangements (linear/circular)', 'Book publishing',
  'Five authors A–E submit manuscripts of different lengths. A’s is longer than B’s but shorter than C’s; D’s is shorter than B’s; E’s is longer than C’s. Editing time is proportional to length: the shortest manuscript takes 6 days and the longest 18 days, and the five editing times form an arithmetic progression.',
  [M('Whose manuscript is the third longest?', ['A', 'B', 'C', 'D'], 'The order from shortest is D, B, A, C, E, so A is third longest.',
     lambda: order9[0][2] if len(order9) == 1 else None),
   T('How many days does C’s manuscript take to edit?', 15, 'The times are 6, 9, 12, 15, 18 days for D, B, A, C, E.', lambda: str(days9['C'])),
   T('If one editor works through the manuscripts from shortest to longest without a break, after how many days does she finish A’s?', 27,
     '6 (D) + 9 (B) + 12 (A) = 27 days.', lambda: str(days9['D'] + days9['B'] + days9['A']))],
  note='Times set in arithmetic progression so every manuscript’s editing time is determined.')

y10 = 6500 // 5
res10 = dict(X=2 * y10, Y=y10, Z=2 * y10 - 500)
loss10 = dict(X=res10['X'] * F(10, 100), Y=res10['Y'] * F(15, 100), Z=res10['Z'] * F(20, 100))
S('s10', 'Tables & Caselets', 'Water reservoirs',
  'Three reservoirs X, Y and Z hold 6,000 litres in total. X holds twice as much as Y, and Z holds 500 litres less than X. Over a week, X loses 10%, Y loses 15% and Z loses 20% of its water to evaporation.',
  [T('How many litres remain in the three reservoirs after the week?', 5125, 'Y = 1300, X = 2600, Z = 2100. Losses: 260 + 195 + 420 = 875, so 5125 litres remain.',
     lambda: str(6000 - sum(loss10.values()))),
   M('Which reservoir loses the most water?', ['Z', 'X', 'Y', 'X and Z equally'], 'Losses: X 260, Y 195, Z 420 litres.', lambda: max(loss10, key=loss10.get)),
   M('What percentage of the total water is lost?', ['14.58%', '15.00%', '13.25%', '16.67%'], '875/6000 = 14.58%.',
     lambda: f'{float(sum(loss10.values()) / 6000 * 100):.2f}%')])

sales11 = dict(A=200, B=250, C=300, D=250)
ph11 = dict(A=F(60, 100), B=F(40, 100), C=F(50, 100), D=F(70, 100))
phones11 = {k: sales11[k] * ph11[k] for k in sales11}
tabs11 = {k: sales11[k] - phones11[k] for k in sales11}
S('s11', 'Tables & Caselets', 'Online marketplace',
  'Four sellers A–D sell 1,000 phones and tablets in all. A accounts for 20% of the units, B for 25%, C for 30% and D for the rest. Phones make up 60% of A’s units, 40% of B’s, 50% of C’s and 70% of D’s.',
  [T('How many tablets are sold in total?', 455, 'Phones: 120 + 100 + 150 + 175 = 545, so tablets = 455.', lambda: str(sum(tabs11.values()))),
   M('Which two sellers sell the same number of tablets?', ['B and C', 'A and D', 'A and B', 'C and D'],
     'Tablets: A 80, B 150, C 150, D 75.', lambda: ' and '.join(k for k in tabs11 if list(tabs11.values()).count(tabs11[k]) == 2)),
   M('What share of all phones does D sell?', ['32.11%', '25.00%', '35.00%', '28.44%'], '175/545 = 32.11%.',
     lambda: f'{float(phones11["D"] / sum(phones11.values()) * 100):.2f}%')],
  note='Total raised from 500 to 1,000 so unit counts are whole numbers.')


def s12(extra=lambda p: True):
    out = []
    for p in permutations('ABCDEFG'):
        pos = {t: i + 1 for i, t in enumerate(p)}
        if pos['A'] < pos['D'] and pos['B'] == pos['E'] + 1 and pos['C'] not in (1, 7) and pos['F'] > pos['D'] and pos['G'] < pos['E'] \
                and abs(pos['A'] - pos['C']) != 1 and extra(pos):
            out.append(pos)
    return out


S('s12', 'Scheduling & Calendars', 'Conference scheduling',
  'Seven talks A–G occupy seven consecutive slots, numbered 1 to 7. A is scheduled before D. B immediately follows E. C is neither first nor last. F is after D. G is before E. A and C are not in adjacent slots.',
  [M('If E is in slot 2, which talk is in slot 4?', ['A', 'D', 'C', 'F'], 'G must be in slot 1 and B in slot 3; the only order that works is G, E, B, A, D, C, F.',
     lambda: next(t for t, s in s12(lambda p: p['E'] == 2)[0].items() if s == 4) if len(s12(lambda p: p['E'] == 2)) == 1 else None),
   T('If C is in slot 2, in which slot is D?', 6, 'The only order is G, C, E, B, A, D, F.',
     lambda: str(s12(lambda p: p['C'] == 2)[0]['D']) if len(s12(lambda p: p['C'] == 2)) == 1 else None),
   M('Which of these talks can be scheduled in slot 7?', ['B', 'D', 'E', 'G'],
     'Only B or F can ever be last; D needs F after it, and G and E need B after them.',
     lambda: next(t for t in 'BDEG' if any(p[t] == 7 for p in s12())) if sum(1 for t in 'BDEG' if any(p[t] == 7 for p in s12())) == 1 else None)])

S('s13', 'Quant-based LR', 'Fuel blend',
  'A fuel station blends petrol grades X (₹96 per litre) and Y (₹108 per litre). The first blend sells at ₹110 per litre at a 10% profit on cost. A second blend mixes the first blend with pure Y in the ratio 3 : 2.',
  [M('In what ratio are X and Y mixed in the first blend?', ['2 : 1', '1 : 2', '3 : 2', '1 : 1'],
     'Cost of the blend = 110/1.1 = ₹100. By alligation, X : Y = (108 − 100) : (100 − 96) = 2 : 1.', lambda: f'{(108 - 100) // 4} : {(100 - 96) // 4}'),
   T('What percentage of the second blend is grade Y?', 60, 'Y = 3 × (1/3) + 2 = 3 parts of 5 = 60%.', lambda: str(int((3 * F(1, 3) + 2) / 5 * 100))),
   M('What is the cost per litre of the second blend?', ['₹103.20', '₹102.40', '₹104.00', '₹100.80'],
     '0.4 × 96 + 0.6 × 108 = ₹103.20.', lambda: f'₹{0.4 * 96 + 0.6 * 108:.2f}')])

loans = dict(Fiction=600, Science=360, History=240)
A14 = dict(Fiction=150, Science=72, History=24)
B14 = dict(Fiction=180, Science=108, History=48)
CD14 = {k: (loans[k] - A14[k] - B14[k]) / 2 for k in loans}
S('s14', 'Tables & Caselets', 'Library circulation',
  'Four library branches A–D made 1,200 loans: 50% fiction, 30% science and 20% history. Branch A made 25% of the fiction loans, 20% of the science loans and 10% of the history loans; branch B made 30%, 30% and 20% respectively. Branches C and D split the remaining loans in each category equally.',
  [T('How many loans did branch B make?', 336, 'B: 180 fiction + 108 science + 48 history = 336.', lambda: str(sum(B14.values()))),
   M('Which branch made the most history loans?', ['C and D equally', 'A', 'B', 'C'],
     'History: A 24, B 48, C 84, D 84.', lambda: 'C and D equally' if CD14['History'] > max(A14['History'], B14['History']) else None),
   M('What percentage of all loans were made by branch A?', ['20.50%', '25.00%', '18.75%', '22.00%'], 'A: 150 + 72 + 24 = 246 of 1200 = 20.5%.',
     lambda: f'{sum(A14.values()) / 1200 * 100:.2f}%')])

users = dict(low=1000, medium=600, high=400)
ret = {k: users[k] * r for k, r in zip(users, (F(2, 10), F(5, 10), F(8, 10)))}
retained = sum(ret.values())
buyers = retained * F(4, 10) + (2000 - retained) * F(5, 100)
S('s15', 'Quant-based LR', 'App engagement',
  'A cohort of 2,000 app users is split into low, medium and high daily use in the ratio 5 : 3 : 2. After 30 days, 20% of low users, 50% of medium users and 80% of high users are still active. 40% of the users still active make a purchase, against 5% of the others.',
  [T('How many users make a purchase?', 387, 'Retained: 200 + 300 + 320 = 820. Buyers: 328 + 5% of 1180 (59) = 387.', lambda: str(buyers)),
   T('What percentage of the cohort is still active after 30 days?', 41, '820/2000 = 41%.', lambda: str(int(retained / 2000 * 100))),
   M('What percentage of buyers are still-active users?', ['84.75%', '80.00%', '41.00%', '15.25%'], '328/387 = 84.75%.',
     lambda: f'{float(retained * F(4, 10) / buyers * 100):.2f}%')])


def s16():
    out = []
    for p in permutations('BCDEFGH'):
        seat = {'A': 0, **{x: i + 1 for i, x in enumerate(p)}}
        adj = lambda x, y: (seat[x] - seat[y]) % 8 in (1, 7)
        if (seat['E'] - seat['A']) % 8 == 4 and seat['B'] == 1 and seat['C'] == (seat['E'] - 1) % 8 and not adj('D', 'A') \
                and not adj('D', 'E') and adj('F', 'G') and adj('F', 'H') and adj('G', 'A'):
            out.append(seat)
    return out


S('s16', 'Arrangements (linear/circular)', 'Circular seating',
  'Eight people A–H sit around a circular table, equally spaced. A sits opposite E. B sits immediately clockwise of A. C sits immediately anticlockwise of E. D is not next to A or E. F sits between G and H, and G is next to A.',
  [M('Who sits opposite B?', ['H', 'F', 'G', 'C'], 'Going clockwise from A: A, B, D, C, E, H, F, G. B is opposite H.',
     lambda: next(x for x, s in s16()[0].items() if s == (s16()[0]['B'] + 4) % 8) if len(s16()) == 1 else None),
   M('Who sits immediately clockwise of E?', ['H', 'C', 'F', 'D'], 'E is followed clockwise by H.',
     lambda: next(x for x, s in s16()[0].items() if s == (s16()[0]['E'] + 1) % 8)),
   T('Going clockwise from D to F, how many people sit between them?', 3, 'C, E and H sit between D and F.',
     lambda: str((s16()[0]['F'] - s16()[0]['D']) % 8 - 1))],
  note='Rewritten: as given (C two places clockwise of E), no seating satisfies every condition.')


def s17_points(win, draw):
    rec = dict(A=(6, 2, 0), B=(4, 2, 2), C=(3, 4, 1), D=(1, 2, 5), E=(0, 2, 6))
    return {k: w * win + d * draw for k, (w, d, l) in rec.items()}


S('s17', 'Games & Tournaments', 'Sports league',
  'Five clubs A–E play each other twice, so each plays 8 matches. Their records (won–drawn–lost) are: A 6–2–0, B 4–2–2, C 3–4–1, D 1–2–5 and E 0–2–6. A win earns 3 points and a draw 1 point.',
  [T('How many points are awarded in the league in total?', 54, '14 decided matches × 3 + 6 drawn matches × 2 = 54.', lambda: str(sum(s17_points(3, 1).values()))),
   M('Which club finishes second?', ['B', 'C', 'D', 'B and C equally'], 'Points: A 20, B 14, C 13, D 5, E 2.',
     lambda: sorted(s17_points(3, 1).items(), key=lambda kv: -kv[1])[1][0]),
   M('Under a system of 2 points per win and 1 per draw, what would happen to second place?', ['B and C would tie', 'C would be second alone', 'B would still be second alone', 'D would move up to second'],
     'B: 8 + 2 = 10; C: 6 + 4 = 10, so they would tie for second.', lambda: 'B and C would tie' if s17_points(2, 1)['B'] == s17_points(2, 1)['C'] else None)],
  note='Rewritten: the original records (20 wins, no losses) are impossible in a 20-match league.')

units18 = dict(P=300, Q=400, R=500)
price18 = dict(P=200, Q=250, R=160)
disc18 = dict(P=F(10, 100), Q=F(20, 100), R=F(15, 100))
rev18 = {k: units18[k] * price18[k] * (1 - disc18[k]) for k in units18}
S('s18', 'Tables & Caselets', 'Pricing strategy',
  'A shop sells 1,200 units of products P, Q and R in the ratio 3 : 4 : 5. Their list prices are ₹200, ₹250 and ₹160, and they are sold at discounts of 10%, 20% and 15% respectively.',
  [T('What is the shop’s total revenue in rupees after discounts?', 202000, 'P: 300 × 180 = 54,000; Q: 400 × 200 = 80,000; R: 500 × 136 = 68,000. Total ₹2,02,000.',
     lambda: str(sum(rev18.values()))),
   M('Which product brings in the most revenue?', ['Q', 'R', 'P', 'Q and R equally'], 'Revenue: P ₹54,000, Q ₹80,000, R ₹68,000.', lambda: max(rev18, key=rev18.get)),
   M('Total discount given is what percentage of the total list value?', ['15.83%', '15.00%', '17.50%', '14.25%'],
     'List value = 60,000 + 1,00,000 + 80,000 = 2,40,000. Discount = 38,000 = 15.83%.',
     lambda: f'{float((240000 - sum(rev18.values())) / 240000 * 100):.2f}%')],
  note='Unit total added so revenue can be computed.')


def s19(drop_c=False):
    out = []
    for p in permutations('ABCD'):
        t = {x: 10 * i for i, x in enumerate(p)}
        if t['A'] < t['C'] and t['B'] == t['D'] + 20 and (drop_c or t['C'] != 30):
            out.append(t)
    return out


S('s19', 'Scheduling & Calendars', 'Train timetable',
  'Four trains A–D leave a station at 8:00, 8:10, 8:20 and 8:30, one at each time. A leaves before C. B leaves exactly 20 minutes after D. C does not leave last.',
  [M('Which train leaves at 8:20?', ['C', 'B', 'A', 'D'], 'D and B must be 8:10 and 8:30 (otherwise C is last), leaving A at 8:00 and C at 8:20.',
     lambda: next(x for x, m in s19()[0].items() if m == 20) if len(s19()) == 1 else None),
   M('Which train leaves last?', ['B', 'C', 'D', 'A'], 'B leaves at 8:30.', lambda: next(x for x, m in s19()[0].items() if m == 30)),
   T('If the condition that C does not leave last were removed, how many departure orders would be possible?', 2,
     'D 8:00, B 8:20 with A 8:10, C 8:30 also becomes possible.', lambda: str(len(s19(drop_c=True))))],
  note='Departure times fixed to four slots; the original allowed any time between 8:00 and 9:00.')

S('s20', 'Venn diagrams', 'Survey analysis',
  'In a survey of 500 people, 280 use service A, 240 use B and 190 use C. 120 use both A and B, 90 use both B and C, 100 use both A and C, and 50 use all three.',
  [T('How many people use none of the three services?', 50, 'At least one = 280 + 240 + 190 − 120 − 90 − 100 + 50 = 450, so 50 use none.',
     lambda: str(500 - (280 + 240 + 190 - 120 - 90 - 100 + 50))),
   T('How many people use exactly one service?', 240, 'Only A = 110, only B = 80, only C = 50.',
     lambda: str((280 - 120 - 100 + 50) + (240 - 120 - 90 + 50) + (190 - 90 - 100 + 50))),
   M('How many people use exactly two services?', ['160', '310', '210', '110'], '(120 − 50) + (90 − 50) + (100 − 50) = 160.',
     lambda: str((120 - 50) + (90 - 50) + (100 - 50)))])


def s21(keep_c=True):
    out = []
    for p in permutations(range(2, 7)):
        v = dict(zip('ABCDE', p))
        if v['A'] > v['B'] and v['C'] < v['D'] and v['E'] == v['A'] + 1 and v['D'] > v['E'] and (not keep_c or v['C'] == 3):
            out.append(v)
    return out


S('s21', 'Arrangements (linear/circular)', 'Coding contest',
  'Five contestants A–E each solve a different number of problems from 2 to 6. A solves more than B. C solves fewer than D. E solves exactly one more than A. D solves more than E. C solves 3 problems.',
  [M('Who solves the most problems?', ['D', 'E', 'A', 'C'], 'C = 3, so A and E take two consecutive values from {2, 4, 5, 6} with D above E: A 4, E 5, D 6, B 2.',
     lambda: max(s21()[0], key=s21()[0].get) if len(s21()) == 1 else None),
   T('How many problems does E solve?', 5, 'See the unique arrangement above.', lambda: str(s21()[0]['E'])),
   M('If the condition that C solves 3 problems were removed, how many arrangements would be possible?', ['3', '1', '2', '6'],
     'A 3, B 2, C 5, D 6, E 4; A 4, B 2, C 3, D 6, E 5; and A 4, B 3, C 2, D 6, E 5.', lambda: str(len(s21(keep_c=False))))],
  note='Two conditions added: as given, six arrangements were possible.')

base22 = dict(Solar=200, Wind=300, Thermal=500)
peak22 = dict(Solar=300, Wind=240, Thermal=500)
S('s22', 'Tables & Caselets', 'Energy mix',
  'A city generates 1,000 MW from solar, wind and thermal sources in the ratio 2 : 3 : 5. During peak hours solar output rises 50%, wind output falls 20% and thermal output is unchanged.',
  [T('What is the total output in MW during peak hours?', 1040, 'Solar 300 + wind 240 + thermal 500 = 1040 MW.', lambda: str(sum(peak22.values()))),
   M('What share of peak output comes from solar?', ['28.85%', '30.00%', '20.00%', '23.08%'], '300/1040 = 28.85%.', lambda: f'{300 / 1040 * 100:.2f}%'),
   M('By what percentage does total output change at peak?', ['+4%', '+10%', '−4%', '+30%'], '1040 versus 1000 is a 4% rise.', lambda: f'+{round((1040 / 1000 - 1) * 100)}%')])

stock23 = dict(Bats=400, Balls=1000, Shoes=600)
ret23 = dict(Bats=60, Balls=80, Shoes=72)
S('s23', 'Tables & Caselets', 'Sports equipment',
  'A retailer stocks 2,000 items: bats, balls and shoes in the ratio 2 : 5 : 3. 15% of bats, 8% of balls and 12% of shoes are returned.',
  [T('How many items are returned in total?', 212, 'Bats 60, balls 80, shoes 72.', lambda: str(sum(stock23[k] * r // 100 for k, r in zip(stock23, (15, 8, 12))))),
   M('Which category has the most returns?', ['Balls', 'Shoes', 'Bats', 'Shoes and bats equally'], 'Balls: 80 returns.', lambda: max(ret23, key=ret23.get)),
   M('What is the overall return rate?', ['10.60%', '11.67%', '12.00%', '8.00%'], '212/2000 = 10.6%.', lambda: f'{212 / 2000 * 100:.2f}%')])

fleet24 = dict(Sedan=40, SUV=30, EV=30)
rev24 = dict(Sedan=40 * 8 * 250, SUV=30 * 6 * 400, EV=30 * 10 * 300)
S('s24', 'Quant-based LR', 'Taxi fleet',
  'A taxi company has 100 vehicles: sedans, SUVs and EVs in the ratio 4 : 3 : 3. A sedan makes 8 trips a day, an SUV 6 and an EV 10. Each trip earns ₹250 for a sedan, ₹400 for an SUV and ₹300 for an EV.',
  [T('What is the fleet’s total daily revenue in rupees?', 242000, 'Sedans ₹80,000 + SUVs ₹72,000 + EVs ₹90,000.', lambda: str(sum(rev24.values()))),
   M('Which vehicle type earns the most in total each day?', ['EV', 'Sedan', 'SUV', 'Sedan and EV equally'], 'EVs earn ₹90,000.', lambda: max(rev24, key=rev24.get)),
   T('What is the daily revenue per SUV in rupees?', 2400, '6 trips × ₹400 = ₹2,400.', lambda: str(rev24['SUV'] // 30))],
  note='Fleet size added so revenue can be computed.')


def s25(total):
    b = F(total - 10, 7)
    return dict(A=2 * b, B=b, C=b + 5, D=(3 * b + 5) / 2, E=(3 * b + 5) / 2)


S('s25', 'Quant-based LR', 'Research grants',
  'Five labs A–E receive research grants totalling ₹59 lakh. A receives twice as much as B. C receives ₹5 lakh more than B. D and E receive equal amounts, and A and C together receive as much as D and E together.',
  [M('Which lab receives the smallest grant?', ['B', 'C', 'D', 'A'], 'B = 7, A = 14, C = 12, D = E = 13 (₹ lakh).', lambda: min(s25(59), key=s25(59).get)),
   T('How much does D receive, in ₹ lakh?', 13, '7B + 10 = 59 gives B = 7, so D = (A + C)/2 = 13.', lambda: str(s25(59)['D'])),
   T('If the total were ₹66 lakh with the same conditions, how much would A receive, in ₹ lakh?', 16, '7B + 10 = 66 gives B = 8, so A = 16.', lambda: str(s25(66)['A']))],
  note='Total changed from ₹50 lakh to ₹59 lakh so the grants are whole lakhs.')

A26 = 100000
paint = dict(A=A26, B=A26 * 6 // 5, C=A26 * 6 // 5 * 9 // 10, D=A26 + 12000)
S('s26', 'Quant-based LR', 'Art auction',
  'Four paintings A–D are sold at an auction. B sells for 20% more than A, C for 10% less than B, and D for ₹12,000 more than A. The average sale price of the four paintings is ₹1,10,000.',
  [T('At what price, in rupees, does C sell?', 108000, '4.28A + 12,000 = 4,40,000 gives A = ₹1,00,000, so B = 1,20,000 and C = 1,08,000.', lambda: str(paint['C'])),
   M('Which painting fetches the highest price?', ['B', 'D', 'C', 'A'], 'B ₹1,20,000; D ₹1,12,000; C ₹1,08,000; A ₹1,00,000.', lambda: max(paint, key=paint.get)),
   T('By how many rupees does B’s price exceed D’s?', 8000, '1,20,000 − 1,12,000 = 8,000.', lambda: str(paint['B'] - paint['D']))],
  note='Average changed from ₹1,08,000 to ₹1,10,000 so prices are whole rupees.')

farm = dict(A=dict(rice=50, wheat=50, pulses=20), B=dict(rice=20, wheat=30, pulses=40), C=dict(rice=45, wheat=35, pulses=20))
farm['D'] = dict(rice=160 - 50 - 20 - 45, wheat=140 - 50 - 30 - 35, pulses=100 - 20 - 40 - 20)
S('s27', 'Tables & Caselets', 'Crop rotation',
  'Four farms A–D grow rice, wheat and pulses on 400 hectares in all: rice on 40% of the land, wheat on 35% and pulses on the rest. Farm A covers 120 hectares, with equal areas of rice and wheat and 20 hectares of pulses. Farm B grows 20 hectares of rice, 30 of wheat and twice as much pulses as rice. Farm C grows 45 hectares of rice and 35 of wheat. Farms C and D grow equal areas of pulses.',
  [M('Which farm is the largest?', ['A', 'C', 'B', 'D'], 'Areas: A 120, B 90, C 100, D 90 hectares.', lambda: max(farm, key=lambda k: sum(farm[k].values()))),
   T('What is farm D’s total area in hectares?', 90, 'D grows rice 160 − 115 = 45, wheat 140 − 115 = 25 and pulses (100 − 60)/2 = 20.', lambda: str(sum(farm['D'].values()))),
   M('Which farm devotes the largest share of its land to pulses?', ['B', 'D', 'C', 'A'], 'Pulses share: A 17%, B 44%, C 20%, D 22%.',
     lambda: max(farm, key=lambda k: farm[k]['pulses'] / sum(farm[k].values())))],
  note='Farm-level figures added: the original gave too little to answer anything.')

items28 = dict(A=200, B=300, C=500)
def28 = dict(A=2, B=6, C=20)
S('s28', 'Quant-based LR', 'Quality inspection',
  'A shipment of 1,000 items comes from production lines A, B and C in the ratio 2 : 3 : 5. Their defect rates are 1%, 2% and 4% respectively. A defective item is picked at random from the shipment.',
  [T('How many items in the shipment are defective?', 28, 'A 2, B 6, C 20.', lambda: str(sum(def28.values()))),
   M('What is the probability that the defective item came from line C?', ['5/7', '1/2', '4/7', '2/7'], '20 of the 28 defective items are from C.', lambda: str(F(def28['C'], 28))),
   M('What is the probability that it came from line A or B?', ['2/7', '1/2', '5/7', '1/14'], '8 of 28 = 2/7.', lambda: str(F(def28['A'] + def28['B'], 28)))])

m29 = 1000
week = dict(Mon=m29, Tue=m29 * 6 // 5, Wed=m29 * 6 // 5 * 9 // 10)
week.update(Thu=week['Mon'] + week['Wed'], Fri=2 * m29)
S('s29', 'Tables & Caselets', 'Museum visitors',
  'A museum had 7,360 visitors from Monday to Friday. Tuesday had 20% more visitors than Monday, Wednesday 10% fewer than Tuesday, Thursday as many as Monday and Wednesday together, and Friday twice as many as Monday.',
  [M('Which day had the most visitors?', ['Thursday', 'Friday', 'Tuesday', 'Wednesday'], 'Monday 1000, Tuesday 1200, Wednesday 1080, Thursday 2080, Friday 2000.',
     lambda: {'Mon': 'Monday', 'Tue': 'Tuesday', 'Wed': 'Wednesday', 'Thu': 'Thursday', 'Fri': 'Friday'}[max(week, key=week.get)] if sum(week.values()) == 7360 else None),
   T('How many visitors came on Wednesday?', 1080, 'Monday = 7360/7.36 = 1000; Wednesday = 1000 × 1.2 × 0.9 = 1080.', lambda: str(week['Wed'])),
   M('What percentage of the week’s visitors came from Monday to Wednesday?', ['44.57%', '40.76%', '50.00%', '42.39%'], '3280/7360 = 44.57%.',
     lambda: f'{(week["Mon"] + week["Tue"] + week["Wed"]) / 7360 * 100:.2f}%')],
  note='Total changed from 5,000 to 7,360 so daily counts are whole numbers.')

edges30 = {frozenset(e) for e in ('AB', 'AC', 'BC', 'BD', 'CD', 'CE', 'DE', 'DF', 'EF')}
nbr = lambda x: {y for y in 'ABCDEF' if frozenset(x + y) in edges30}


def paths30(a, b, n):
    out = [[a]]
    for _ in range(n):
        out = [p + [y] for p in out for y in nbr(p[-1]) if y not in p]
    return [p for p in out if p[-1] == b]


S('s30', 'Networks & Routes', 'Flight connections',
  'Six cities A–F are linked by direct two-way flights: A–B, A–C, B–C, B–D, C–D, C–E, D–E, D–F and E–F. There are no other direct flights.',
  [M('What is the minimum number of flights needed to travel from A to F?', ['3', '2', '4', '5'], 'A’s neighbours (B, C) and F’s neighbours (D, E) do not overlap, but A–B–D–F works.',
     lambda: str(next(n for n in range(1, 6) if paths30('A', 'F', n)))),
   M('How many cities have direct flights to exactly four other cities?', ['2', '1', '3', '4'], 'C (A, B, D, E) and D (B, C, E, F).',
     lambda: str(sum(1 for c in 'ABCDEF' if len(nbr(c)) == 4))),
   T('How many different routes from A to F use exactly three flights without revisiting a city?', 3, 'A–B–D–F, A–C–D–F and A–C–E–F.', lambda: str(len(paths30('A', 'F', 3))))])


def sets():
    out = []
    for s in SETS:
        qs = []
        for q in s['questions']:
            got = q['check']()
            want = q['options'][0] if q['type'] == 'MCQ' else q['answer']
            ok = got is not None and (str(got) == want if q['type'] == 'MCQ' else float(got) == float(want))
            assert ok, f"{s['id']} {q['prompt'][:50]!r}: key {want!r} but the check computes {got!r}"
            qs.append({k: v for k, v in q.items() if k != 'check'})
        out.append({**s, 'questions': qs})
    return out
