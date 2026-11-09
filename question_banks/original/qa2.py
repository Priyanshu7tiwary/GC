"""Quantitative Aptitude, batch 2: new CAT-style questions written for 99ile, at least three per syllabus topic.

Modelled on the question patterns of recent CAT papers (arithmetic- and algebra-heavy, about a third TITA).
As in qa.py, each MCQ lists the correct option first (the builder shuffles) and every question carries a `check`
that recomputes the key independently, by brute force wherever possible; a wrong key fails the build.
"""
import math
from fractions import Fraction as F
from itertools import combinations, permutations, product

Q = []


def mcq(id, topic, prompt, options, explanation, difficulty='MODERATE', check=None):
    Q.append(dict(id=id, section='QA', topic=topic, type='MCQ', prompt=prompt, options=options, explanation=explanation,
                  difficulty=difficulty, check=check, source='new'))


def tita(id, topic, prompt, answer, explanation, difficulty='MODERATE', check=None):
    Q.append(dict(id=id, section='QA', topic=topic, type='TITA', prompt=prompt, answer=str(answer), explanation=explanation,
                  difficulty=difficulty, check=check, source='new'))


def frac(x):
    x = F(x)
    return str(x.numerator) if x.denominator == 1 else f'{x.numerator}/{x.denominator}'


# ================================================================== ARITHMETIC

# ---- Percentages
mcq('qa2-001', 'Percentages',
    'The price of sugar rises by 25%. By what percentage must a household cut its sugar consumption so that its spending on sugar does not change?',
    ['20%', '25%', '18%', '22.5%'],
    'Spending = price × quantity. With the price at 1.25 times, the quantity must be 1/1.25 = 0.8 times, a cut of 20%.',
    difficulty='EASY', check=lambda: f'{(1 - F(4, 5)) * 100:.0f}%' if F(1) / F(5, 4) == F(4, 5) else None)

tita('qa2-002', 'Percentages',
     'A town’s population of 50,000 grows by 10% in the first year, falls by 10% in the second and grows by 20% in the third. What is the population at the end of the third year?',
     59400, '50,000 × 1.1 × 0.9 × 1.2 = 59,400.', difficulty='EASY',
     check=lambda: str(50000 * F(11, 10) * F(9, 10) * F(6, 5)))

tita('qa2-003', 'Percentages',
     'In a two-candidate election, 10% of the voters on the list did not vote and 10% of the votes cast were invalid. The winner got 54% of the valid votes and won by 1,620 votes. How many voters were on the list?',
     25000, 'Valid votes = 0.9 × 0.9 × V = 0.81V. The margin is 54% − 46% = 8% of valid votes, so 0.08 × 0.81V = 0.0648V = 1,620 and V = 25,000.',
     check=lambda: str(next(v for v in range(100, 100000, 100) if F(8, 100) * F(81, 100) * v == 1620)))

mcq('qa2-004', 'Percentages',
    'A earns 40% more than B. A spends 70% of his salary and B spends 60% of hers. If A saves ₹1,200 a month more than B, what is A’s monthly salary?',
    ['₹84,000', '₹60,000', '₹72,000', '₹90,000'],
    'If B earns b, A saves 0.3 × 1.4b = 0.42b and B saves 0.4b. The difference 0.02b = 1,200 gives b = ₹60,000, so A earns ₹84,000.',
    check=lambda: f'₹{next(b for b in range(1000, 200000, 1000) if F(42, 100) * b - F(40, 100) * b == 1200) * 7 // 5:,}')

mcq('qa2-005', 'Percentages',
    'In a class, 60% of the students are boys. 30% of the boys and 45% of the girls passed a test. What percentage of the class failed?',
    ['64%', '36%', '62.5%', '58%'],
    'Passed = 0.6 × 0.3 + 0.4 × 0.45 = 0.18 + 0.18 = 0.36 of the class, so 64% failed.',
    difficulty='EASY', check=lambda: f'{(1 - (F(6, 10) * F(3, 10) + F(4, 10) * F(45, 100))) * 100:.0f}%')

# ---- Profit, Loss & Discount
mcq('qa2-006', 'Profit, Loss & Discount',
    'A shopkeeper marks goods 40% above cost and then allows a discount of 15% on the marked price. What is his profit percentage?',
    ['19%', '25%', '21%', '17%'],
    'Selling price = 1.40 × 0.85 = 1.19 times the cost: a profit of 19%.',
    difficulty='EASY', check=lambda: f'{(F(14, 10) * F(85, 100) - 1) * 100:.0f}%')

mcq('qa2-007', 'Profit, Loss & Discount',
    'A dishonest dealer uses a 900 g weight in place of a 1 kg weight and also charges 5% above his cost price. What is his actual profit percentage?',
    ['16.67%', '15.00%', '16.00%', '14.29%'],
    'For goods costing him 0.9 (per kg of claimed weight) he collects 1.05. Profit = 0.15/0.9 = 16.67%.',
    check=lambda: f'{float((F(105, 100) / F(9, 10) - 1) * 100):.2f}%')

tita('qa2-008', 'Profit, Loss & Discount',
     'A trader sells two phones for ₹9,900 each. He gains 10% on one and loses 10% on the other. What is his overall loss in rupees?',
     200, 'Cost prices are 9,900/1.1 = ₹9,000 and 9,900/0.9 = ₹11,000. Total cost ₹20,000, total sale ₹19,800: a loss of ₹200.',
     check=lambda: str(9900 / F(11, 10) + 9900 / F(9, 10) - 2 * 9900))

tita('qa2-009', 'Profit, Loss & Discount',
     'Selling a watch for ₹1,140 causes a loss of 5%. At what price (in rupees) should it be sold to make a profit of 15%?',
     1380, 'Cost = 1,140/0.95 = ₹1,200, so the price for a 15% profit is 1,200 × 1.15 = ₹1,380.', difficulty='EASY',
     check=lambda: str(F(1140) / F(95, 100) * F(115, 100)))

mcq('qa2-010', 'Profit, Loss & Discount',
    'After allowing a 20% discount on the marked price, a trader still makes a 20% profit. By what percentage above the cost price had he marked the goods?',
    ['50%', '40%', '44%', '25%'],
    'Marked price × 0.8 = cost × 1.2, so the marked price is 1.5 times the cost: 50% above.',
    check=lambda: f'{(F(12, 10) / F(8, 10) - 1) * 100:.0f}%')

# ---- Simple & Compound Interest
mcq('qa2-011', 'Simple & Compound Interest',
    'A sum of money doubles in 5 years at compound interest. In how many years will it become 8 times itself at the same rate?',
    ['15 years', '20 years', '40 years', '10 years'],
    '8 = 2³, so it takes three doubling periods: 15 years.', difficulty='EASY',
    check=lambda: f'{5 * round(math.log2(8))} years')

tita('qa2-012', 'Simple & Compound Interest',
     'The difference between the compound interest and the simple interest on a sum for 2 years at 10% per annum is ₹150. What is the sum in rupees?',
     15000, 'For 2 years, CI − SI = P(r/100)² = P × 0.01 = 150, so P = ₹15,000.',
     check=lambda: str(next(p for p in range(100, 100000, 100) if p * F(11, 10) ** 2 - p - p * F(2, 10) == 150)))

tita('qa2-013', 'Simple & Compound Interest',
     'What is the compound interest in rupees on ₹16,000 for 1 year at 10% per annum, compounded half-yearly?',
     1640, 'Two half-years at 5%: 16,000 × (1.05² − 1) = 16,000 × 0.1025 = ₹1,640.', difficulty='EASY',
     check=lambda: str(16000 * (F(105, 100) ** 2 - 1)))

mcq('qa2-014', 'Simple & Compound Interest',
    'A sum lent at simple interest amounts to ₹8,400 in 3 years and to ₹9,000 in 5 years. What is the rate of interest?',
    ['4%', '5%', '3.5%', '4.5%'],
    'Two years’ interest = ₹600, so ₹300 a year. Principal = 8,400 − 900 = ₹7,500 and the rate = 300/7,500 = 4%.',
    check=lambda: f'{F(9000 - 8400, 2) / (8400 - 3 * F(9000 - 8400, 2)) * 100:.0f}%')

tita('qa2-015', 'Simple & Compound Interest',
     'A loan of ₹6,620 is repaid in three equal annual instalments, the first one year after the loan, at 10% per annum compound interest. What is each instalment in rupees?',
     2662, 'The instalments’ present values add up to the loan: x(1/1.1 + 1/1.21 + 1/1.331) = 6,620. That is x × 3.31/1.331 = 6,620, so x = ₹2,662.',
     difficulty='HARD', check=lambda: str(F(6620) / sum(F(10, 11) ** k for k in (1, 2, 3))))

# ---- Ratio & Proportion
tita('qa2-016', 'Ratio & Proportion',
     'The incomes of A and B are in the ratio 5 : 4 and their expenses in the ratio 3 : 2. If each of them saves ₹1,600, what is A’s income in rupees?',
     4000, 'Let incomes be 5x, 4x and expenses 3y, 2y. Then 5x − 3y = 1,600 and 4x − 2y = 1,600, giving x = 800 and A’s income = ₹4,000.',
     check=lambda: str(next(5 * x for x in range(1, 5000) if (5 * x - 1600) % 3 == 0 and 4 * x - 2 * ((5 * x - 1600) // 3) == 1600)))

tita('qa2-017', 'Ratio & Proportion',
     'A bag contains ₹1, 50-paise and 25-paise coins in the ratio 2 : 3 : 4. The total value of the coins is ₹90. How many 25-paise coins are in the bag?',
     80, 'One “set” of 2 + 3 + 4 coins is worth 2 + 1.5 + 1 = ₹4.50, so there are 90/4.5 = 20 sets and 80 coins of 25 paise.', difficulty='EASY',
     check=lambda: str(4 * F(90) / (2 + F(3, 2) + 1)))

mcq('qa2-018', 'Ratio & Proportion',
    'If a : b = 2 : 3, b : c = 4 : 5 and c : d = 6 : 7, what is a : d?',
    ['16 : 35', '8 : 15', '12 : 35', '4 : 7'],
    'a/d = (2/3)(4/5)(6/7) = 48/105 = 16/35.',
    check=lambda: (lambda r: f'{r.numerator} : {r.denominator}')(F(2, 3) * F(4, 5) * F(6, 7)))

mcq('qa2-019', 'Ratio & Proportion',
    'x varies directly as the square of y and inversely as z. If x = 6 when y = 3 and z = 4, what is x when y = 6 and z = 8?',
    ['12', '24', '6', '18'],
    'x = ky²/z with k = 6 × 4/9 = 8/3. Then x = (8/3) × 36/8 = 12.',
    check=lambda: frac(F(6 * 4, 9) * 36 / 8))

tita('qa2-020', 'Ratio & Proportion',
     'A starts a business with ₹40,000. Three months later B joins with ₹60,000. Out of a year-end profit of ₹34,000, how much (in rupees) does A get?',
     16000, 'Capital × months: A 40,000 × 12 = 4.8 lakh; B 60,000 × 9 = 5.4 lakh. A’s share = 34,000 × 4.8/10.2 = ₹16,000.',
     check=lambda: str(F(34000 * 40000 * 12, 40000 * 12 + 60000 * 9)))

# ---- Mixtures & Alligation
tita('qa2-021', 'Mixtures & Alligation',
     'A vessel holds 80 litres of pure milk. 8 litres are taken out and replaced with water, and this is done three times in all. How many litres of milk remain?',
     58.32, 'Each operation keeps 72/80 = 0.9 of the milk: 80 × 0.9³ = 58.32 litres.',
     check=lambda: str(float(80 * F(9, 10) ** 3)))

mcq('qa2-022', 'Mixtures & Alligation',
    'Alloy X has gold and copper in the ratio 2 : 3, and alloy Y has them in the ratio 3 : 7. In what ratio should X and Y be melted together to get an alloy with gold and copper in the ratio 1 : 2?',
    ['1 : 2', '2 : 1', '1 : 3', '3 : 2'],
    'Gold fractions are 2/5 and 3/10; the target is 1/3. By alligation, X : Y = (1/3 − 3/10) : (2/5 − 1/3) = 1/30 : 1/15 = 1 : 2.',
    check=lambda: (lambda r: f'{r.numerator} : {r.denominator}')((F(1, 3) - F(3, 10)) / (F(2, 5) - F(1, 3))))

mcq('qa2-023', 'Mixtures & Alligation',
    'A merchant blends tea costing ₹160/kg with tea costing ₹220/kg and sells the blend at ₹230.40/kg, making a 20% profit. In what ratio did he blend them?',
    ['7 : 8', '8 : 7', '3 : 2', '2 : 3'],
    'Cost of the blend = 230.40/1.2 = ₹192/kg. Alligation: (220 − 192) : (192 − 160) = 28 : 32 = 7 : 8.',
    check=lambda: (lambda c: (lambda r: f'{r.numerator} : {r.denominator}')(F(220 - c, 1) / (c - 160)))(F(23040, 100) / F(12, 10)))

# ---- Time & Work
tita('qa2-024', 'Time & Work',
     'A can finish a job in 12 days and B in 18 days. With C’s help, the three finish it in 6 days. In how many days can C alone finish it?',
     36, 'C’s daily rate = 1/6 − 1/12 − 1/18 = (6 − 3 − 2)/36 = 1/36, so 36 days.', difficulty='EASY',
     check=lambda: str(1 / (F(1, 6) - F(1, 12) - F(1, 18))))

mcq('qa2-025', 'Time & Work',
    'A is twice as efficient as B, and together they finish a job in 14 days. How long would A take alone?',
    ['21 days', '28 days', '42 days', '18 days'],
    'If B does b per day, A does 2b and 3b = 1/14, so b = 1/42 and A takes 21 days.', difficulty='EASY',
    check=lambda: f'{1 / (2 * F(1, 14) / 3)} days')

tita('qa2-026', 'Time & Work',
     'Pipe A fills a tank in 20 minutes and pipe B in 30 minutes, while pipe C empties the full tank in 15 minutes. All three are opened on the empty tank and C is closed after 10 minutes. How many minutes after the start is the tank full?',
     20, 'Together: 1/20 + 1/30 − 1/15 = 1/60 per minute, so after 10 minutes 1/6 is full. A and B then fill the remaining 5/6 at 1/12 per minute in 10 minutes. Total 20 minutes.',
     check=lambda: str(10 + (1 - 10 * (F(1, 20) + F(1, 30) - F(1, 15))) / (F(1, 20) + F(1, 30))))

mcq('qa2-027', 'Time & Work',
    '12 men or 18 women can do a job in 14 days. How long will 8 men and 16 women take to do it together?',
    ['9 days', '8 days', '10 days', '12 days'],
    '12 men = 18 women, so 1 man = 1.5 women and 8 men + 16 women = 28 women. 18 × 14 = 252 woman-days, and 252/28 = 9 days.',
    check=lambda: f'{F(18 * 14) / (8 * F(3, 2) + 16)} days')

mcq('qa2-028', 'Time & Work',
    'A can do a job in 6 days and B in 9 days. With C’s help they finish it in 3 days and are paid ₹3,000 in all. If pay is shared by work done, what is C’s share?',
    ['₹500', '₹1,000', '₹600', '₹750'],
    'In 3 days A does 1/2 and B 1/3 of the job, so C does 1/6 and gets ₹500.',
    check=lambda: f'₹{int(3000 * (1 - 3 * F(1, 6) - 3 * F(1, 9))):,}')

# ---- Time, Speed & Distance
tita('qa2-029', 'Time, Speed & Distance',
     'Trains 150 m and 250 m long run on parallel tracks in opposite directions at 54 km/h and 36 km/h. How many seconds do they take to cross each other completely?',
     16, 'Relative speed = 90 km/h = 25 m/s; distance = 400 m; time = 16 s.', difficulty='EASY',
     check=lambda: str(F(150 + 250) / ((54 + 36) * F(5, 18))))

mcq('qa2-030', 'Time, Speed & Distance',
    'A boat goes 30 km downstream in 2 hours and 18 km upstream in 2 hours. What is the speed of the stream?',
    ['3 km/h', '6 km/h', '12 km/h', '4 km/h'],
    'Downstream 15 km/h, upstream 9 km/h. Stream speed = (15 − 9)/2 = 3 km/h.', difficulty='EASY',
    check=lambda: f'{(F(30, 2) - F(18, 2)) / 2} km/h')

mcq('qa2-031', 'Time, Speed & Distance',
    'P and Q are 300 km apart. A leaves P for Q at 8:00 a.m. at 40 km/h, and B leaves Q for P at 9:00 a.m. at 60 km/h. When do they meet?',
    ['11:36 a.m.', '11:00 a.m.', '11:12 a.m.', '12:00 noon'],
    'By 9:00 A has covered 40 km. The remaining 260 km close at 100 km/h in 2.6 h = 2 h 36 min, so they meet at 11:36 a.m.',
    check=lambda: (lambda m: f'{9 + m // 60}:{m % 60:02d} a.m.')(int((300 - 40) * 60 / 100)))

tita('qa2-032', 'Time, Speed & Distance',
     'Walking to the office at 4 km/h, a man arrives 10 minutes late; walking at 5 km/h he arrives 5 minutes early. How far is the office, in km?',
     5, 'The two times differ by 15 minutes: d/4 − d/5 = 1/4, so d/20 = 1/4 and d = 5 km.', difficulty='EASY',
     check=lambda: str(F(1, 4) / (F(1, 4) - F(1, 5))))

tita('qa2-033', 'Time, Speed & Distance',
     'A and B start together from the same point of a 600 m circular track and run in the same direction at 5 m/s and 7 m/s. After how many seconds are they first together again at the starting point?',
     600, 'A completes a lap every 120 s and B every 600/7 s. Both are at the start when t is a multiple of each: 120a = 600b/7, i.e. 7a = 5b, first at a = 5, t = 600 s.',
     difficulty='HARD', check=lambda: str(next(t for t in range(1, 10000) if (5 * t) % 600 == 0 and (7 * t) % 600 == 0)))

# ---- Averages
mcq('qa2-034', 'Averages',
    'The average of five consecutive odd numbers is 27. What is the largest of them?',
    ['31', '29', '33', '35'],
    'The middle number is the average, 27, so the numbers are 23, 25, 27, 29, 31.', difficulty='EASY',
    check=lambda: str(next(a + 8 for a in range(1, 100, 2) if sum(range(a, a + 10, 2)) == 135)))

tita('qa2-035', 'Averages',
     'The average age of the four members of a family is 25 years. A child is born today. What will be the average age of the five members five years from now?',
     25, 'Today the four total 100 years and the baby 0. In five years they total 100 + 4 × 5 + 5 = 125, an average of 25.',
     check=lambda: str(F(4 * 25 + 4 * 5 + 5, 5)))

tita('qa2-036', 'Averages',
     'The average weight of 40 students is 50 kg. When the teacher is included, the average rises by 0.5 kg. What is the teacher’s weight in kg?',
     70.5, 'New total = 41 × 50.5 = 2,070.5 kg; old total = 2,000 kg. Teacher = 70.5 kg.',
     check=lambda: str(float(41 * F(101, 2) - 40 * 50)))

mcq('qa2-037', 'Averages',
    'Section A has 30 students with an average score of 72 and section B has 20 students with an average of 67. What is the average of all 50 students?',
    ['70', '69.5', '70.5', '69'],
    '(30 × 72 + 20 × 67)/50 = (2,160 + 1,340)/50 = 70.', difficulty='EASY',
    check=lambda: frac(F(30 * 72 + 20 * 67, 50)))

# ================================================================== ALGEBRA

# ---- Linear equations
tita('qa2-038', 'Linear equations',
     'Adult tickets for a show cost ₹200 and child tickets ₹120. 150 tickets were sold for ₹25,200 in all. How many child tickets were sold?',
     60, 'With a adult tickets: 200a + 120(150 − a) = 25,200, so 80a = 7,200, a = 90 and 60 child tickets.', difficulty='EASY',
     check=lambda: str(next(150 - a for a in range(151) if 200 * a + 120 * (150 - a) == 25200)))

mcq('qa2-039', 'Linear equations',
    'For what value of k does the system 3x + 2y = 12, kx + 4y = 24 have infinitely many solutions?',
    ['6', '3', '12', 'No value of k'],
    'The second equation must be a multiple of the first: 4/2 = 24/12 = 2, so k = 2 × 3 = 6.',
    check=lambda: str(next(k for k in range(-20, 21) if F(k, 3) == F(4, 2) == F(24, 12))))

tita('qa2-040', 'Linear equations',
     'Five years ago a father was seven times as old as his son. Five years from now he will be three times as old as his son. What is the sum of their present ages?',
     50, 'F − 5 = 7(S − 5) and F + 5 = 3(S + 5) give S = 10 and F = 40. Sum = 50.',
     check=lambda: str(next(f + s for s in range(6, 60) for f in range(6, 120) if f - 5 == 7 * (s - 5) and f + 5 == 3 * (s + 5))))

tita('qa2-041', 'Linear equations',
     'How many ordered pairs of non-negative integers (x, y) satisfy 3x + 5y = 100?',
     7, '100 − 5y must be a multiple of 3, which needs y ≡ 2 (mod 3): y = 2, 5, 8, 11, 14, 17, 20. That is 7 pairs.',
     check=lambda: str(sum(1 for x in range(40) for y in range(25) if 3 * x + 5 * y == 100)))

# ---- Quadratic equations
tita('qa2-042', 'Quadratic equations',
     'If α and β are the roots of x² − 6x + 4 = 0, what is α² + β²?',
     28, 'α + β = 6 and αβ = 4, so α² + β² = 36 − 8 = 28.', difficulty='EASY',
     check=lambda: str(round(sum(r ** 2 for r in (3 + math.sqrt(5), 3 - math.sqrt(5))))))

mcq('qa2-043', 'Quadratic equations',
    'For which values of k does x² + kx + 9 = 0 have two equal real roots?',
    ['k = ±6', 'k = ±3', 'k = 6 only', 'k = ±9'],
    'Equal roots need k² − 36 = 0, so k = 6 or k = −6.', difficulty='EASY',
    check=lambda: 'k = ±6' if [k for k in range(-20, 21) if k * k - 36 == 0] == [-6, 6] else None)

mcq('qa2-044', 'Quadratic equations',
    'The roots of x² − px + q = 0 are consecutive integers. What is p² − 4q?',
    ['1', '0', '4', 'It depends on p'],
    'p² − 4q = (sum)² − 4(product) = (difference of roots)² = 1.',
    check=lambda: str({(2 * n + 1) ** 2 - 4 * n * (n + 1) for n in range(-10, 10)}.pop()))

tita('qa2-045', 'Quadratic equations',
     'For how many integer values of k does x² − 2kx + 3k + 4 = 0 have no real roots?',
     4, 'No real roots: 4k² − 4(3k + 4) < 0, i.e. (k − 4)(k + 1) < 0, so −1 < k < 4: k = 0, 1, 2, 3.',
     check=lambda: str(sum(1 for k in range(-50, 50) if 4 * k * k - 4 * (3 * k + 4) < 0)))

tita('qa2-046', 'Quadratic equations',
     'What is the product of all real roots of x² − 5|x| + 6 = 0?',
     36, 'With t = |x|, t² − 5t + 6 = 0 gives t = 2 or 3, so x = ±2, ±3. Product = (2)(−2)(3)(−3) = 36.', difficulty='HARD',
     check=lambda: str(math.prod(x for x in range(-20, 21) if x * x - 5 * abs(x) + 6 == 0)))

# ---- Inequalities & Modulus
tita('qa2-047', 'Inequalities & Modulus',
     'How many integers x satisfy |x − 3| + |x + 2| ≤ 9?',
     10, 'Between −2 and 3 the sum is 5. For x > 3 it is 2x − 1 ≤ 9, so x ≤ 5; for x < −2 it is 1 − 2x ≤ 9, so x ≥ −4. Integers −4 to 5: 10.',
     check=lambda: str(sum(1 for x in range(-100, 100) if abs(x - 3) + abs(x + 2) <= 9)))

mcq('qa2-048', 'Inequalities & Modulus',
    'Which values of x satisfy (x − 2)/(x + 3) > 1?',
    ['x < −3', 'x > −3', 'x > 2', '−3 < x < 2'],
    '(x − 2)/(x + 3) − 1 = −5/(x + 3) > 0, which holds exactly when x + 3 < 0.',
    check=lambda: 'x < −3' if all(((x - 2) / (x + 3) > 1) == (x < -3) for x in [i / 4 for i in range(-80, 80)] if x != -3) else None)

mcq('qa2-049', 'Inequalities & Modulus',
    'What is the minimum value of |x − 1| + |x − 4| + |x − 9| over all real x?',
    ['8', '5', '9', '12'],
    'A sum of distances is least at the median point, x = 4: 3 + 0 + 5 = 8.',
    check=lambda: str(min(abs(x - 1) + abs(x - 4) + abs(x - 9) for x in [i / 10 for i in range(-100, 200)])).rstrip('0').rstrip('.'))

mcq('qa2-050', 'Inequalities & Modulus',
    'What is the least value of 4x + 9/x for x > 0?',
    ['12', '13', '6', '36'],
    'By AM ≥ GM, 4x + 9/x ≥ 2√36 = 12, with equality at x = 3/2.',
    check=lambda: str(round(min(4 * x + 9 / x for x in [i / 1000 for i in range(1, 10000)]))))

# ---- Functions & Graphs
tita('qa2-051', 'Functions & Graphs',
     'If f(x) = 2x + 3 and g(x) = x² − 1, what is f(g(2)) − g(f(1))?',
     -15, 'g(2) = 3 and f(3) = 9; f(1) = 5 and g(5) = 24. 9 − 24 = −15.', difficulty='EASY',
     check=lambda: (lambda f, g: str(f(g(2)) - g(f(1))))(lambda x: 2 * x + 3, lambda x: x * x - 1))

mcq('qa2-052', 'Functions & Graphs',
    'A function satisfies f(x) + 2f(1/x) = 3x for every x ≠ 0. What is f(2)?',
    ['−1', '1', '2', '−2'],
    'Put x = 2 and x = 1/2: f(2) + 2f(1/2) = 6 and f(1/2) + 2f(2) = 3/2. Eliminating f(1/2): 3f(2) = 3 − 6, so f(2) = −1.',
    check=lambda: (lambda a: str(int(a)).replace('-', '−'))((2 * F(3, 2) - 6) / 3))

mcq('qa2-053', 'Functions & Graphs',
    'If f(x) = (x − 1)/(x + 1), what is f(f(x)) for x ≠ 0, −1?',
    ['−1/x', 'x', '1/x', '−x'],
    'f(f(x)) = [(x − 1) − (x + 1)]/[(x − 1) + (x + 1)] = −2/(2x) = −1/x.',
    check=lambda: '−1/x' if all((lambda f: f(f(F(x))) == F(-1, x))(lambda t: (t - 1) / (t + 1)) for x in (2, 3, 5, 7, -3)) else None)

tita('qa2-054', 'Functions & Graphs',
     'What is the maximum value of min(2x + 1, 7 − x) over all real x?',
     5, 'The minimum of a rising and a falling line is largest where they meet: 2x + 1 = 7 − x at x = 2, value 5.',
     check=lambda: str(round(max(min(2 * x + 1, 7 - x) for x in [i / 100 for i in range(-1000, 1000)]))))

# ---- Logarithms
tita('qa2-055', 'Logarithms',
     'If log₂(log₃(log₄ x)) = 0, what is x?',
     64, 'log₃(log₄ x) = 1, so log₄ x = 3 and x = 4³ = 64.', difficulty='EASY',
     check=lambda: str(next(x for x in range(5, 10000) if abs(math.log2(math.log(math.log(x, 4), 3))) < 1e-9)))

tita('qa2-056', 'Logarithms',
     'Given log₁₀ 2 = 0.3010, how many digits does 2⁵⁰ have?',
     16, 'log₁₀ 2⁵⁰ = 50 × 0.3010 = 15.05, so 2⁵⁰ lies between 10¹⁵ and 10¹⁶ and has 16 digits.',
     check=lambda: str(len(str(2 ** 50))))

mcq('qa2-057', 'Logarithms',
    'Solve log₁₀ x + log₁₀(x − 3) = 1.',
    ['x = 5', 'x = −2', 'x = 5 or x = −2', 'x = 2'],
    'x(x − 3) = 10 gives x = 5 or −2, but logarithms need x > 3, so only x = 5.',
    check=lambda: 'x = ' + ' or '.join(str(x) for x in range(-10, 20) if x > 3 and x * (x - 3) == 10))

tita('qa2-058', 'Logarithms',
     'What is the value of 1/log₃ 60 + 1/log₄ 60 + 1/log₅ 60?',
     1, '1/logₐ 60 = log₆₀ a, so the sum is log₆₀(3 × 4 × 5) = log₆₀ 60 = 1.',
     check=lambda: str(round(sum(1 / math.log(60, a) for a in (3, 4, 5)), 9)))

# ---- Progressions (AP/GP/HP)
tita('qa2-059', 'Progressions (AP/GP/HP)',
     'The 4th term of an arithmetic progression is 14 and the 10th term is 38. What is the sum of its first 20 terms?',
     800, '6d = 24, so d = 4 and a = 2. S₂₀ = 10 × (2 × 2 + 19 × 4) = 800.', difficulty='EASY',
     check=lambda: str(sum(2 + 4 * k for k in range(20)) if 2 + 3 * 4 == 14 and 2 + 9 * 4 == 38 else None))

mcq('qa2-060', 'Progressions (AP/GP/HP)',
    'An infinite geometric progression has first term 5 and sum 20. What is its second term?',
    ['15/4', '5/4', '4', '10/3'],
    '5/(1 − r) = 20 gives r = 3/4, so the second term is 15/4.',
    check=lambda: frac(5 * (1 - F(5, 20))))

tita('qa2-061', 'Progressions (AP/GP/HP)',
     'Three numbers in arithmetic progression add up to 27 and their product is 693. What is the largest of them?',
     11, 'Write them as 9 − d, 9, 9 + d. Then 9(81 − d²) = 693, so d² = 4 and the numbers are 7, 9, 11.',
     check=lambda: str(max(c for a in range(-50, 50) for b in range(-50, 50) for c in [2 * b - a] if a + b + c == 27 and a * b * c == 693)))

tita('qa2-062', 'Progressions (AP/GP/HP)',
     'How many terms of the progression 3, 7, 11, 15, … must be added for the sum to first exceed 1,000?',
     23, 'Sₙ = n(2n + 1). S₂₂ = 990 and S₂₃ = 1,081, so 23 terms.',
     check=lambda: str(next(n for n in range(1, 100) if sum(3 + 4 * k for k in range(n)) > 1000)))

mcq('qa2-063', 'Progressions (AP/GP/HP)',
    'If 3, b and 6 are in harmonic progression, what is b?',
    ['4', '4.5', '3√2', '5'],
    'The middle term of an HP is 2ac/(a + c) = 36/9 = 4 (equivalently 1/3, 1/4, 1/6 are in AP).',
    difficulty='EASY', check=lambda: frac(F(2 * 3 * 6, 3 + 6)))

# ---- Surds & Indices
mcq('qa2-064', 'Surds & Indices',
    'What is √(7 + 4√3)?',
    ['2 + √3', '1 + √3', '4 + √3', '1 + 2√3'],
    '(2 + √3)² = 4 + 3 + 4√3 = 7 + 4√3.', difficulty='EASY',
    check=lambda: '2 + √3' if abs(math.sqrt(7 + 4 * math.sqrt(3)) - (2 + math.sqrt(3))) < 1e-12 else None)

tita('qa2-065', 'Surds & Indices',
     'If 3ˣ⁺² − 3ˣ = 216, what is x?',
     3, '3ˣ(9 − 1) = 216, so 3ˣ = 27 and x = 3.', difficulty='EASY',
     check=lambda: str(next(x for x in range(10) if 3 ** (x + 2) - 3 ** x == 216)))

mcq('qa2-066', 'Surds & Indices',
    'Which is the largest of 2^(1/2), 3^(1/3), 4^(1/4) and 6^(1/6)?',
    ['3^(1/3)', '2^(1/2)', '4^(1/4)', '6^(1/6)'],
    'Raise all to the 12th power: 2⁶ = 64, 3⁴ = 81, 4³ = 64, 6² = 36. 3^(1/3) is largest.',
    check=lambda: max(['2^(1/2)', '3^(1/3)', '4^(1/4)', '6^(1/6)'], key=lambda s: int(s[0]) ** (1 / int(s[5]))))

mcq('qa2-067', 'Surds & Indices',
    'Simplify (√5 + √3)/(√5 − √3).',
    ['4 + √15', '8 + √15', '4 + 2√15', '(8 + √15)/2'],
    'Multiply above and below by √5 + √3: (8 + 2√15)/2 = 4 + √15.',
    check=lambda: '4 + √15' if abs((math.sqrt(5) + math.sqrt(3)) / (math.sqrt(5) - math.sqrt(3)) - (4 + math.sqrt(15))) < 1e-12 else None)

# ================================================================== NUMBER SYSTEM

# ---- Divisibility & Remainders
tita('qa2-068', 'Divisibility & Remainders',
     'What is the remainder when 3¹⁰⁰ is divided by 7?',
     4, '3⁶ ≡ 1 (mod 7) and 100 = 6 × 16 + 4, so 3¹⁰⁰ ≡ 3⁴ = 81 ≡ 4.',
     check=lambda: str(pow(3, 100, 7)))

tita('qa2-069', 'Divisibility & Remainders',
     'What is the smallest positive integer that leaves remainders 2, 3 and 4 when divided by 3, 4 and 5 respectively?',
     59, 'In each case the remainder is one less than the divisor, so N + 1 is a multiple of LCM(3, 4, 5) = 60. N = 59.', difficulty='EASY',
     check=lambda: str(next(n for n in range(1, 1000) if n % 3 == 2 and n % 4 == 3 and n % 5 == 4)))

tita('qa2-070', 'Divisibility & Remainders',
     'The four-digit number 7A5B is divisible by 36, where A and B are digits. How many such numbers are there?',
     3, 'Divisible by 4: 5B is a multiple of 4, so B = 2 or 6. Divisible by 9: 7 + A + 5 + B is a multiple of 9. B = 2 needs 14 + A to be a multiple of 9, so A = 4; B = 6 needs 18 + A, so A = 0 or 9. Numbers: 7452, 7056, 7956.',
     check=lambda: str(sum(1 for a in range(10) for b in range(10) if int(f'7{a}5{b}') % 36 == 0)))

tita('qa2-071', 'Divisibility & Remainders',
     'What is the remainder when 1! + 2! + 3! + … + 100! is divided by 12?',
     9, 'From 4! onwards every factorial is a multiple of 12, so the remainder comes from 1 + 2 + 6 = 9.',
     check=lambda: str(sum(math.factorial(k) for k in range(1, 101)) % 12))

# ---- HCF & LCM
tita('qa2-072', 'HCF & LCM',
     'The product of two positive integers is 2,160 and their HCF is 12. How many such (unordered) pairs are there?',
     2, 'Write them as 12a and 12b with a, b coprime and ab = 15: (1, 15) and (3, 5). The pairs are (12, 180) and (36, 60).',
     check=lambda: str(sum(1 for a in range(1, 2161) if 2160 % a == 0 and a <= 2160 // a and math.gcd(a, 2160 // a) == 12)))

mcq('qa2-073', 'HCF & LCM',
    'Three bells ring at intervals of 12, 18 and 30 minutes. They ring together at 9:00 a.m. When do they next ring together?',
    ['12:00 noon', '10:30 a.m.', '11:00 a.m.', '12:30 p.m.'],
    'LCM(12, 18, 30) = 180 minutes = 3 hours after 9:00 a.m.', difficulty='EASY',
    check=lambda: {180: '12:00 noon'}.get(math.lcm(12, 18, 30)))

tita('qa2-074', 'HCF & LCM',
     'What is the largest number that divides 1,305, 4,665 and 6,905 leaving the same remainder each time?',
     1120, 'It must divide the differences 3,360, 2,240 and 5,600. HCF of these = 1,120.',
     check=lambda: str(max(d for d in range(1, 7000) if 1305 % d == 4665 % d == 6905 % d)))

tita('qa2-075', 'HCF & LCM',
     'Two numbers have HCF 5 and LCM 495, and their sum is 100. What is their positive difference?',
     10, 'Write them as 5a and 5b with a, b coprime: ab = 99 and a + b = 20, so 9 and 11. The numbers are 45 and 55.',
     check=lambda: str(next(b - a for a in range(1, 100) for b in range(a, 100) if a + b == 100 and math.gcd(a, b) == 5 and math.lcm(a, b) == 495)))

# ---- Factors
tita('qa2-076', 'Factors',
     'How many positive factors does 720 have?',
     30, '720 = 2⁴ × 3² × 5, so it has (4 + 1)(2 + 1)(1 + 1) = 30 factors.', difficulty='EASY',
     check=lambda: str(sum(1 for d in range(1, 721) if 720 % d == 0)))

tita('qa2-077', 'Factors',
     'How many even factors does 1,440 have?',
     30, '1,440 = 2⁵ × 3² × 5. An even factor uses 2¹ to 2⁵: 5 × 3 × 2 = 30.',
     check=lambda: str(sum(1 for d in range(2, 1441, 2) if 1440 % d == 0)))

tita('qa2-078', 'Factors',
     'In how many ways can 360 be written as a product of two coprime positive integers, if order does not matter?',
     4, '360 = 2³ × 3² × 5. Each prime power goes wholly to one factor: 2³ = 8 ordered ways, so 4 unordered (1 × 360, 8 × 45, 9 × 40, 5 × 72).',
     check=lambda: str(sum(1 for a in range(1, 361) if 360 % a == 0 and a <= 360 // a and math.gcd(a, 360 // a) == 1)))

mcq('qa2-079', 'Factors',
    'What is the sum of all positive factors of 196?',
    ['399', '392', '343', '400'],
    '196 = 2² × 7², so the sum is (1 + 2 + 4)(1 + 7 + 49) = 7 × 57 = 399.',
    check=lambda: str(sum(d for d in range(1, 197) if 196 % d == 0)))

# ---- Base systems
tita('qa2-080', 'Base systems',
     'What is the binary number 1101101 in decimal?',
     109, '64 + 32 + 8 + 4 + 1 = 109.', difficulty='EASY',
     check=lambda: str(int('1101101', 2)))

mcq('qa2-081', 'Base systems',
    'In which base is it true that 3 × 4 = 13?',
    ['9', '8', '11', '7'],
    '12 (decimal) must equal 1 × b + 3, so b = 9.',
    check=lambda: str(next(b for b in range(5, 17) if b + 3 == 12)))

mcq('qa2-082', 'Base systems',
    'What is (235)₇ + (164)₇, written in base 7?',
    ['432', '422', '402', '431'],
    '(235)₇ = 124 and (164)₇ = 95; the sum 219 = 4 × 49 + 3 × 7 + 2 = (432)₇.',
    check=lambda: (lambda n: ''.join(str(n // 7 ** k % 7) for k in (2, 1, 0)))(int('235', 7) + int('164', 7)))

tita('qa2-083', 'Base systems',
     'When 50! is written in base 12, how many zeros does it end with?',
     22, '12 = 2² × 3. 50! contains 2⁴⁷ (23 pairs of 2s) and 3²². The 3s run out first, so 22 trailing zeros.', difficulty='HARD',
     check=lambda: str(next(k for k in range(100) if math.factorial(50) % 12 ** (k + 1) != 0)))

# ---- Unit & last digits
tita('qa2-084', 'Unit & last digits',
     'What is the units digit of 7⁹⁵ − 3⁵⁸?',
     4, '7⁹⁵ ends in 3 (95 ≡ 3 mod 4) and 3⁵⁸ ends in 9 (58 ≡ 2 mod 4). As 7⁹⁵ is larger, the difference ends in 13 − 9 = 4.',
     check=lambda: str((7 ** 95 - 3 ** 58) % 10))

tita('qa2-085', 'Unit & last digits',
     'What are the last two digits of 7²⁰²⁶? (Enter the two-digit number.)',
     49, '7⁴ = 2401 ends in 01, so 7²⁰²⁴ ends in 01 and 7²⁰²⁶ ends in 49.',
     check=lambda: str(pow(7, 2026, 100)))

tita('qa2-086', 'Unit & last digits',
     'What is the units digit of 1¹ + 2² + 3³ + … + 10¹⁰?',
     7, 'Units digits: 1, 4, 7, 6, 5, 6, 3, 6, 9, 0. Their sum is 47, so the units digit is 7.',
     check=lambda: str(sum(k ** k for k in range(1, 11)) % 10))

tita('qa2-087', 'Unit & last digits',
     'How many zeros does 125! end with?',
     31, '⌊125/5⌋ + ⌊125/25⌋ + ⌊125/125⌋ = 25 + 5 + 1 = 31.', difficulty='EASY',
     check=lambda: str(len(str(math.factorial(125))) - len(str(math.factorial(125)).rstrip('0'))))

# ================================================================== GEOMETRY & MENSURATION

# ---- Triangles
mcq('qa2-088', 'Triangles',
    'In triangle ABC, D lies on AB and E on AC with DE parallel to BC and AD : DB = 2 : 3. What is the ratio of the area of triangle ADE to that of trapezium DBCE?',
    ['4 : 21', '4 : 25', '2 : 3', '4 : 9'],
    'ADE ~ ABC with ratio 2 : 5, so the areas are 4 : 25 and the trapezium is 25 − 4 = 21.',
    check=lambda: (lambda r: f'{r.numerator} : {r.denominator}')(F(2, 5) ** 2 / (1 - F(2, 5) ** 2)))

mcq('qa2-089', 'Triangles',
    'A triangle has sides 7, 24 and 25. What is the radius of its circumcircle?',
    ['12.5', '12', '13', '10'],
    '7² + 24² = 25², so it is right-angled and the hypotenuse is a diameter: R = 12.5.', difficulty='EASY',
    check=lambda: str(7 * 24 * 25 / (4 * (7 * 24 / 2))))

tita('qa2-090', 'Triangles',
     'How many non-congruent triangles with integer sides have a perimeter of 15?',
     7, 'With a ≤ b ≤ c and c < 7.5: (1,7,7), (2,6,7), (3,5,7), (3,6,6), (4,4,7), (4,5,6), (5,5,5).',
     check=lambda: str(sum(1 for a in range(1, 15) for b in range(a, 15) for c in range(b, 15) if a + b + c == 15 and a + b > c)))

tita('qa2-091', 'Triangles',
     'In triangle ABC, ∠B = 90°, AB = 6 and BC = 8. BD is the altitude from B to AC. What is the length of BD?',
     4.8, 'AC = 10. Twice the area is AB × BC = AC × BD, so BD = 48/10 = 4.8.', difficulty='EASY',
     check=lambda: str(6 * 8 / math.hypot(6, 8)))

tita('qa2-092', 'Triangles',
     'In triangle ABC, AB = 12, AC = 18 and BC = 20. The bisector of angle A meets BC at D. What is BD?',
     8, 'By the angle-bisector theorem, BD : DC = AB : AC = 2 : 3, so BD = 20 × 2/5 = 8.',
     check=lambda: str(F(20 * 12, 12 + 18)))

# ---- Circles
tita('qa2-093', 'Circles',
     'Two circles of radii 5 cm and 3 cm have centres 10 cm apart. What is the length (in cm) of a transverse common tangent?',
     6, 'Transverse tangent = √(d² − (r₁ + r₂)²) = √(100 − 64) = 6 cm.',
     check=lambda: str(math.sqrt(10 ** 2 - (5 + 3) ** 2)))

mcq('qa2-094', 'Circles',
    'A chord of a circle of radius 6 cm subtends an angle of 120° at the centre. How long is the chord?',
    ['6√3 cm', '6 cm', '3√3 cm', '12 cm'],
    'Chord = 2r sin(60°) = 12 × √3/2 = 6√3 cm.',
    check=lambda: '6√3 cm' if abs(2 * 6 * math.sin(math.radians(60)) - 6 * math.sqrt(3)) < 1e-12 else None)

mcq('qa2-095', 'Circles',
    'A square is inscribed in a circle of radius 10 cm. What area of the circle (in cm²) lies outside the square?',
    ['100π − 200', '100π − 100', '50π − 100', '100π − 400'],
    'The square’s diagonal is the diameter, 20, so its area is 20²/2 = 200. The circle’s area is 100π.', difficulty='EASY',
    check=lambda: f'100π − {round(20 ** 2 / 2)}')

mcq('qa2-096', 'Circles',
    'ABCD is a cyclic quadrilateral with ∠A = 70° and ∠B = 95°. What is ∠D?',
    ['85°', '95°', '110°', '70°'],
    'Opposite angles of a cyclic quadrilateral add up to 180°, so ∠D = 180° − ∠B = 85°.', difficulty='EASY',
    check=lambda: f'{180 - 95}°')

# ---- Polygons & Quadrilaterals
tita('qa2-097', 'Polygons & Quadrilaterals',
     'The interior angles of a convex polygon add up to 1,440°. How many diagonals does it have?',
     35, '(n − 2) × 180 = 1,440 gives n = 10, and 10 × 7/2 = 35 diagonals.',
     check=lambda: str(next(n * (n - 3) // 2 for n in range(3, 50) if (n - 2) * 180 == 1440)))

tita('qa2-098', 'Polygons & Quadrilaterals',
     'A rectangular lawn is 24 m by 18 m. A path 3 m wide runs inside it along all four edges. What is the area of the path in m²?',
     216, 'Lawn 24 × 18 = 432 m²; inner part 18 × 12 = 216 m². Path = 216 m².', difficulty='EASY',
     check=lambda: str(24 * 18 - (24 - 6) * (18 - 6)))

mcq('qa2-099', 'Polygons & Quadrilaterals',
    'What is the area of a regular hexagon with side 4 cm?',
    ['24√3 cm²', '16√3 cm²', '48√3 cm²', '12√3 cm²'],
    'It is six equilateral triangles of side 4: 6 × (√3/4) × 16 = 24√3 cm².',
    check=lambda: f'{round(6 * 16 / 4)}√3 cm²')

tita('qa2-100', 'Polygons & Quadrilaterals',
     'An isosceles trapezium has parallel sides 10 cm and 22 cm, and each slant side is 10 cm. What is its area in cm²?',
     128, 'Each overhang is (22 − 10)/2 = 6, so the height is √(100 − 36) = 8 and the area is (10 + 22)/2 × 8 = 128.',
     check=lambda: str(round((10 + 22) / 2 * math.sqrt(10 ** 2 - ((22 - 10) / 2) ** 2))))

# ---- Coordinate geometry
tita('qa2-101', 'Coordinate geometry',
     'What is the distance of the point (3, 4) from the line 3x + 4y − 5 = 0?',
     4, '|3·3 + 4·4 − 5|/√(9 + 16) = 20/5 = 4.', difficulty='EASY',
     check=lambda: str(abs(3 * 3 + 4 * 4 - 5) / math.hypot(3, 4)))

tita('qa2-102', 'Coordinate geometry',
     'What is the area of the triangle with vertices (1, 2), (4, 6) and (7, 2)?',
     12, 'Base from (1, 2) to (7, 2) is 6 and the height to (4, 6) is 4, so the area is 12.', difficulty='EASY',
     check=lambda: str(abs(1 * (6 - 2) + 4 * (2 - 2) + 7 * (2 - 6)) / 2))

mcq('qa2-103', 'Coordinate geometry',
    'Which line passes through (2, 3) and is perpendicular to 2x − y + 5 = 0?',
    ['x + 2y = 8', '2x + y = 7', 'x − 2y = −4', 'x + 2y = 6'],
    'The given line has slope 2, so the required slope is −1/2: y − 3 = −(x − 2)/2, i.e. x + 2y = 8.',
    check=lambda: f'x + 2y = {2 + 2 * 3}' if 1 * 2 + 2 * (-1) == 0 else None)

tita('qa2-104', 'Coordinate geometry',
     'How many points with integer coordinates lie strictly inside the triangle with vertices (0, 0), (10, 0) and (0, 10)?',
     36, 'Interior points have x, y ≥ 1 and x + y ≤ 9: 8 + 7 + … + 1 = 36.',
     check=lambda: str(sum(1 for x in range(11) for y in range(11) if x > 0 and y > 0 and x + y < 10)))

# ---- 3D Mensuration
tita('qa2-105', '3D Mensuration',
     'The diagonal of a cube is 6√3 cm. What is its volume in cm³?',
     216, 'Diagonal = a√3, so a = 6 and the volume is 216 cm³.', difficulty='EASY',
     check=lambda: str(round((6 * math.sqrt(3) / math.sqrt(3)) ** 3)))

tita('qa2-106', '3D Mensuration',
     'A cone has base radius 7 cm and height 24 cm. What is its curved surface area in cm²? (Take π = 22/7.)',
     550, 'Slant height = √(49 + 576) = 25, so the curved surface is πrl = (22/7) × 7 × 25 = 550 cm².',
     check=lambda: str(F(22, 7) * 7 * math.isqrt(7 ** 2 + 24 ** 2)))

tita('qa2-107', '3D Mensuration',
     'A solid sphere of radius 6 cm is melted and recast into solid cones of base radius 3 cm and height 4 cm. How many cones are made?',
     24, 'Sphere: (4/3)π × 216 = 288π. Cone: (1/3)π × 9 × 4 = 12π. 288/12 = 24.',
     check=lambda: str(F(4, 3) * 216 / (F(1, 3) * 9 * 4)))

mcq('qa2-108', '3D Mensuration',
    'A cube of side 10 cm is dropped into a cylindrical tank of radius 10 cm that holds enough water to submerge it. By about how much does the water level rise?',
    ['3.18 cm', '10 cm', '1 cm', '3.33 cm'],
    'Rise = 1,000/(π × 100) = 10/π ≈ 3.18 cm.',
    check=lambda: f'{1000 / (math.pi * 100):.2f} cm')

# ---- Trigonometry basics
tita('qa2-109', 'Trigonometry basics',
     'From a point 40 m from the foot of a tower, the angle of elevation of its top is 60°. How many metres further away (in a straight line from the tower) must one move for the angle of elevation to be 30°?',
     80, 'Height = 40√3. At 30° the distance is 40√3 × √3 = 120 m, so move 80 m further.',
     check=lambda: str(round(40 * math.tan(math.radians(60)) / math.tan(math.radians(30)) - 40)))

mcq('qa2-110', 'Trigonometry basics',
    'If tan θ = 3/4 and θ is acute, what is sin θ + cos θ?',
    ['7/5', '5/4', '1', '12/25'],
    'A 3-4-5 triangle gives sin θ = 3/5 and cos θ = 4/5.', difficulty='EASY',
    check=lambda: frac(F(3, 5) + F(4, 5)) if F(3, 5) / F(4, 5) == F(3, 4) else None)

tita('qa2-111', 'Trigonometry basics',
     'What is sin²1° + sin²2° + sin²3° + … + sin²89°?',
     44.5, 'Pair sin²k° with sin²(90 − k)° = cos²k°: 44 pairs give 44, and sin²45° adds 1/2. Total 44.5.',
     check=lambda: str(round(sum(math.sin(math.radians(k)) ** 2 for k in range(1, 90)), 9)))

mcq('qa2-112', 'Trigonometry basics',
    'If sec θ + tan θ = 3, what is cos θ?',
    ['3/5', '4/5', '1/3', '5/3'],
    'sec² − tan² = 1, so sec θ − tan θ = 1/3. Adding, 2 sec θ = 10/3, so sec θ = 5/3 and cos θ = 3/5.',
    check=lambda: frac(1 / ((3 + F(1, 3)) / 2)))

# ================================================================== MODERN MATHS

# ---- Permutations & Combinations
tita('qa2-113', 'Permutations & Combinations',
     'In how many distinct ways can the letters of BANANA be arranged?',
     60, '6!/(3! × 2!) = 720/12 = 60.', difficulty='EASY',
     check=lambda: str(len(set(permutations('BANANA')))))

tita('qa2-114', 'Permutations & Combinations',
     'A team of 3 men and 2 women is to be chosen from 6 men and 5 women. In how many ways can this be done?',
     200, '⁶C₃ × ⁵C₂ = 20 × 10 = 200.', difficulty='EASY',
     check=lambda: str(math.comb(6, 3) * math.comb(5, 2)))

tita('qa2-115', 'Permutations & Combinations',
     'How many four-digit numbers with all digits different are divisible by 5?',
     952, 'Ending in 0: 9 × 8 × 7 = 504. Ending in 5: the first digit can’t be 0 or 5, so 8 × 8 × 7 = 448. Total 952.',
     check=lambda: str(sum(1 for n in range(1000, 10000) if n % 5 == 0 and len(set(str(n))) == 4)))

tita('qa2-116', 'Permutations & Combinations',
     'In how many ways can 10 identical balls be put into 4 distinct boxes so that no box is empty?',
     84, 'Stars and bars: ⁹C₃ = 84.',
     check=lambda: str(sum(1 for t in product(range(1, 11), repeat=4) if sum(t) == 10)))

tita('qa2-117', 'Permutations & Combinations',
     'In how many ways can 6 people sit around a round table if two particular people must not sit next to each other?',
     72, 'All arrangements: 5! = 120. With the two together: 4! × 2 = 48. Answer 120 − 48 = 72.', difficulty='HARD',
     check=lambda: str(sum(1 for p in permutations(range(1, 6)) if p.index(1) not in (0, 4))))

# ---- Probability
mcq('qa2-118', 'Probability',
    'Two fair dice are thrown. What is the probability that the sum is a prime number?',
    ['5/12', '1/3', '7/18', '1/2'],
    'Prime sums 2, 3, 5, 7, 11 occur in 1 + 2 + 4 + 6 + 2 = 15 of the 36 outcomes: 15/36 = 5/12.', difficulty='EASY',
    check=lambda: frac(F(sum(1 for a in range(1, 7) for b in range(1, 7) if a + b in (2, 3, 5, 7, 11)), 36)))

mcq('qa2-119', 'Probability',
    'A bag has 4 red and 6 blue balls. Three balls are drawn at random without replacement. What is the probability that exactly two are red?',
    ['3/10', '1/5', '1/2', '9/25'],
    '⁴C₂ × ⁶C₁/¹⁰C₃ = 36/120 = 3/10.',
    check=lambda: frac(F(sum(1 for c in combinations(range(10), 3) if sum(1 for i in c if i < 4) == 2), math.comb(10, 3))))

mcq('qa2-120', 'Probability',
    'A and B take turns tossing a fair coin, A first. Whoever tosses the first head wins. What is the probability that A wins?',
    ['2/3', '1/2', '3/4', '1/3'],
    'P(A) = 1/2 + (1/4)(1/2) + … = (1/2)/(1 − 1/4) = 2/3.',
    check=lambda: frac(F(1, 2) / (1 - F(1, 4))))

mcq('qa2-121', 'Probability',
    'A two-digit number is chosen at random. What is the probability that it is divisible by 3 or by 7?',
    ['13/30', '43/90', '2/5', '7/15'],
    'Of the 90 two-digit numbers, 30 are multiples of 3, 13 of 7 and 4 of 21: 30 + 13 − 4 = 39, and 39/90 = 13/30.',
    check=lambda: frac(F(sum(1 for n in range(10, 100) if n % 3 == 0 or n % 7 == 0), 90)))

# ---- Set theory
tita('qa2-122', 'Set theory',
     'Of 200 people surveyed, 110 drink tea, 90 coffee and 80 juice; 40 drink tea and coffee, 30 coffee and juice, 35 tea and juice, and 15 all three. How many drink none of the three?',
     10, 'At least one = 110 + 90 + 80 − 40 − 30 − 35 + 15 = 190, so 10 drink none.', difficulty='EASY',
     check=lambda: str(200 - (110 + 90 + 80 - 40 - 30 - 35 + 15)))

tita('qa2-123', 'Set theory',
     'In a group of 50 people, 35 like cricket and 30 like football. What is the least possible number who like both?',
     15, 'n(C ∩ F) = 35 + 30 − n(C ∪ F) ≥ 65 − 50 = 15.', difficulty='EASY',
     check=lambda: str(min(b for b in range(31) if 35 + 30 - b <= 50)))

tita('qa2-124', 'Set theory',
     'Each of 120 students takes at least one of the subjects A, B and C. 70 take A, 60 take B and 50 take C, and 20 take all three. How many take exactly two of the subjects?',
     20, 'Let e₁, e₂, e₃ be the numbers taking exactly one, two, three. e₁ + e₂ + e₃ = 120 and e₁ + 2e₂ + 3e₃ = 180. Subtracting, e₂ + 2e₃ = 60, so e₂ = 20.',
     difficulty='HARD', check=lambda: str(next(e2 for e2 in range(121) if (120 - e2 - 20) + 2 * e2 + 3 * 20 == 180)))

mcq('qa2-125', 'Set theory',
    'In a class of 60, 40 students play chess and 35 play carrom. What is the largest possible number of students who play neither?',
    ['20', '25', '15', '5'],
    'Neither is largest when the union is smallest, i.e. when all carrom players also play chess: union = 40, neither = 20.',
    check=lambda: str(max(60 - (40 + 35 - b) for b in range(36))))

# ---- Sequences & Series
mcq('qa2-126', 'Sequences & Series',
    'What is 1/(1·2) + 1/(2·3) + 1/(3·4) + … + 1/(99·100)?',
    ['99/100', '1', '100/101', '98/99'],
    'Each term is 1/n − 1/(n + 1), so the sum telescopes to 1 − 1/100 = 99/100.', difficulty='EASY',
    check=lambda: frac(sum(F(1, n * (n + 1)) for n in range(1, 100))))

tita('qa2-127', 'Sequences & Series',
     'A sequence has a₁ = 1 and aₙ₊₁ = aₙ + 2n for n ≥ 1. What is a₁₀?',
     91, 'a₁₀ = 1 + 2(1 + 2 + … + 9) = 1 + 90 = 91.',
     check=lambda: str((lambda a: [a.append(a[-1] + 2 * n) for n in range(1, 10)] and a[-1])([1])))

tita('qa2-128', 'Sequences & Series',
     'What is 1² + 2² + 3² + … + 20²?',
     2870, 'n(n + 1)(2n + 1)/6 = 20 × 21 × 41/6 = 2,870.', difficulty='EASY',
     check=lambda: str(sum(k * k for k in range(1, 21))))

tita('qa2-129', 'Sequences & Series',
     'What is 1·2 + 2·3 + 3·4 + … + 10·11?',
     440, 'Σ k(k + 1) = n(n + 1)(n + 2)/3 = 10 × 11 × 12/3 = 440.',
     check=lambda: str(sum(k * (k + 1) for k in range(1, 11))))

tita('qa2-130', 'Sequences & Series',
     'The sequence 2, 5, 10, 17, 26, … has nth term n² + 1. What is the sum of its first 10 terms?',
     395, 'Σ(n² + 1) = 385 + 10 = 395.',
     check=lambda: str(sum(n * n + 1 for n in range(1, 11))))

# ================================================================== harder, CAT-level items across topics

mcq('qa2-131', 'Time, Speed & Distance',
    'A and B set out at the same time from X and Y towards each other. After they meet, A takes 9 more hours to reach Y and B takes 4 more hours to reach X. What is the ratio of A’s speed to B’s?',
    ['2 : 3', '3 : 2', '4 : 9', '9 : 4'],
    'For two travellers meeting and then taking t₁ and t₂ more hours, speed ratio = √(t₂/t₁) = √(4/9) = 2 : 3.', difficulty='HARD',
    check=lambda: next(f'{a} : {b}' for a in range(1, 10) for b in range(1, 10) if math.gcd(a, b) == 1
                       # meet after t hours: a·t = b·4 (A covers what B still has to do) and b·t = a·9
                       for t in [F(4 * b, a)] if b * t == 9 * a))

tita('qa2-132', 'Time & Work',
     'A and B together can do a job in 12 days, B and C in 15 days, and C and A in 20 days. How many days would A take alone?',
     30, 'Adding the three pairs counts everyone twice: 2(a + b + c) = 1/12 + 1/15 + 1/20 = 1/5, so a + b + c = 1/10 and a = 1/10 − 1/15 = 1/30.', difficulty='HARD',
     check=lambda: str(1 / ((F(1, 12) + F(1, 15) + F(1, 20)) / 2 - F(1, 15))))

tita('qa2-133', 'Divisibility & Remainders',
     'For how many integers n from 1 to 100 is n² + 1 divisible by 5?',
     40, 'n² ≡ 4 (mod 5) exactly when n ≡ 2 or 3 (mod 5): 20 + 20 = 40 values.', difficulty='HARD',
     check=lambda: str(sum(1 for n in range(1, 101) if (n * n + 1) % 5 == 0)))

tita('qa2-134', 'Divisibility & Remainders',
     'What is the remainder when 2²⁰²⁶ is divided by 9?',
     7, '2⁶ = 64 ≡ 1 (mod 9) and 2026 = 6 × 337 + 4, so 2²⁰²⁶ ≡ 2⁴ = 16 ≡ 7.',
     check=lambda: str(pow(2, 2026, 9)))

tita('qa2-135', 'Factors',
     'What is the highest power of 6 that divides 40!?',
     18, '40! contains 2 to the power 20 + 10 + 5 + 2 + 1 = 38 and 3 to the power 13 + 4 + 1 = 18. The 3s limit it: 6¹⁸.', difficulty='HARD',
     check=lambda: str(next(k for k in range(100) if math.factorial(40) % 6 ** (k + 1) != 0)))

tita('qa2-136', 'Surds & Indices',
     'If x + 1/x = 3, what is x⁵ + 1/x⁵?',
     123, 'Let tₙ = xⁿ + 1/xⁿ. t₂ = 3² − 2 = 7, t₃ = 3³ − 3 × 3 = 18, and t₅ = t₂t₃ − t₁ = 126 − 3 = 123.', difficulty='HARD',
     check=lambda: str(round(((3 + math.sqrt(5)) / 2) ** 5 + ((3 - math.sqrt(5)) / 2) ** 5)))

tita('qa2-137', 'Factors',
     'How many pairs of positive integers (x, y) satisfy x² − y² = 96?',
     4, '(x − y)(x + y) = 96 with both factors even and x − y < x + y: (2, 48), (4, 24), (6, 16), (8, 12).', difficulty='HARD',
     check=lambda: str(sum(1 for x in range(1, 100) for y in range(1, x) if x * x - y * y == 96)))

tita('qa2-138', 'Functions & Graphs',
     'f(x) = ax + b with a > 0, and f(f(x)) = 9x + 8 for all x. What is f(5)?',
     17, 'f(f(x)) = a²x + ab + b, so a² = 9 (a = 3) and 4b = 8 (b = 2). f(5) = 17.',
     check=lambda: str(next(a * 5 + b for a in range(1, 10) for b in range(-20, 21) if a * a == 9 and a * b + b == 8)))

tita('qa2-139', 'Quadratic equations',
     'For how many integers n is (n² + 7)/(n + 1) an integer?',
     8, 'n² + 7 = (n + 1)(n − 1) + 8, so n + 1 must divide 8: n + 1 ∈ {±1, ±2, ±4, ±8}, eight values.', difficulty='HARD',
     check=lambda: str(sum(1 for n in range(-1000, 1000) if n != -1 and (n * n + 7) % (n + 1) == 0)))

tita('qa2-140', 'Inequalities & Modulus',
     'How many ordered pairs of positive integers (x, y) satisfy 1/x + 1/y = 1/6?',
     9, 'Clearing fractions, xy − 6x − 6y = 0, i.e. (x − 6)(y − 6) = 36. Both factors must be positive (a negative one would make x or y ≤ 0), and 36 has 9 divisors.', difficulty='HARD',
     check=lambda: str(sum(1 for x in range(7, 100) for y in range(7, 100) if F(1, x) + F(1, y) == F(1, 6))))

tita('qa2-141', 'Progressions (AP/GP/HP)',
     'What is the sum of all three-digit numbers that leave a remainder of 2 when divided by 7?',
     70692, 'They run 100, 107, …, 996: (996 − 100)/7 + 1 = 129 terms, summing to 129 × (100 + 996)/2 = 70,692.',
     check=lambda: str(sum(n for n in range(100, 1000) if n % 7 == 2)))

mcq('qa2-142', 'Circles',
    'Three circles of radius 2 cm touch one another externally. What is the area (in cm²) of the region enclosed between them?',
    ['4√3 − 2π', '4√3 − π', '2√3 − π', '8√3 − 2π'],
    'The centres form an equilateral triangle of side 4 (area 4√3). It contains three 60° sectors, together half a circle: 2π. The gap is 4√3 − 2π.', difficulty='HARD',
    check=lambda: '4√3 − 2π' if abs(math.sqrt(3) / 4 * 16 - 3 * (60 / 360) * math.pi * 4 - (4 * math.sqrt(3) - 2 * math.pi)) < 1e-12 else None)

tita('qa2-143', 'Coordinate geometry',
     'What is the area of the region enclosed by |x − 1| + |y + 2| = 3?',
     18, 'It is a square (a rhombus with equal diagonals) centred at (1, −2) with diagonals 6 and 6: area 6 × 6/2 = 18.',
     check=lambda: str(round(sum(1 for i in range(-400, 400) for j in range(-400, 400) if abs(i / 50 + 0.01 - 1) + abs(j / 50 + 0.01 + 2) <= 3) / 2500)))

mcq('qa2-144', '3D Mensuration',
    'A right circular cone is cut by a plane parallel to its base, halfway up its height. What is the ratio of the volume of the small cone to that of the remaining frustum?',
    ['1 : 7', '1 : 8', '1 : 3', '1 : 4'],
    'The small cone is similar with ratio 1/2, so its volume is 1/8 of the whole and the frustum is 7/8.',
    check=lambda: (lambda r: f'{r.numerator} : {r.denominator}')(F(1, 8) / (1 - F(1, 8))))

tita('qa2-145', 'Permutations & Combinations',
     'In how many ways can three different numbers be chosen from 1, 2, …, 20 so that their sum is even?',
     570, 'Either all three are even (¹⁰C₃ = 120) or exactly one is even (¹⁰C₂ × 10 = 450). Total 570.', difficulty='HARD',
     check=lambda: str(sum(1 for c in combinations(range(1, 21), 3) if sum(c) % 2 == 0)))

mcq('qa2-146', 'Probability',
    'Three different numbers are chosen at random from 1 to 10. What is the probability that they form an arithmetic progression (in some order)?',
    ['1/6', '1/5', '1/8', '1/12'],
    'Choose the outer pair: they must have an even difference, and the middle term is then fixed. Common difference 1, 2, 3, 4 gives 8, 6, 4, 2 progressions: 20 out of ¹⁰C₃ = 120.', difficulty='HARD',
    check=lambda: frac(F(sum(1 for a, b, c in combinations(range(1, 11), 3) if b - a == c - b), math.comb(10, 3))))

tita('qa2-147', 'Mixtures & Alligation',
     'Vessel A holds milk and water in the ratio 3 : 1 and vessel B in the ratio 5 : 3. How many litres from B must be mixed with 12 litres from A to get milk and water in the ratio 2 : 1?',
     24, 'Milk fractions are 3/4 and 5/8, target 2/3. A : B = (2/3 − 5/8) : (3/4 − 2/3) = 1/24 : 1/12 = 1 : 2, so 24 litres of B.',
     check=lambda: str(12 * (F(3, 4) - F(2, 3)) / (F(2, 3) - F(5, 8))))

tita('qa2-148', 'Averages',
     'The integers 1, 2, 3, …, n are written on a board and one of them is erased. The average of the rest is 35 7/17. Which number was erased?',
     7, 'The average of the rest is close to n/2, so n is about 69 or 70, and n − 1 must be a multiple of 17: n = 69. Sum 1…69 = 2,415, and 68 × 602/17 = 2,408, so 7 was erased.', difficulty='HARD',
     check=lambda: str(next(n * (n + 1) // 2 - (n - 1) * F(602, 17) for n in range(2, 500) if (n - 1) * F(602, 17) == int((n - 1) * F(602, 17))
                            and 1 <= n * (n + 1) // 2 - (n - 1) * F(602, 17) <= n)))

tita('qa2-149', 'Simple & Compound Interest',
     'A sum invested at compound interest amounts to ₹17,640 in 2 years and ₹18,522 in 3 years. What was the sum invested, in rupees?',
     16000, 'The third year’s growth 18,522/17,640 = 1.05 gives a 5% rate, so the principal is 17,640/1.05² = ₹16,000.',
     check=lambda: str(F(17640) / (F(18522, 17640)) ** 2))

tita('qa2-150', 'Ratio & Proportion',
     'Two numbers are in the ratio 3 : 5. If 9 is subtracted from each, the ratio becomes 12 : 23. What is the smaller number?',
     33, '(3k − 9)/(5k − 9) = 12/23 gives 69k − 207 = 60k − 108, so k = 11 and the numbers are 33 and 55.',
     check=lambda: str(next(3 * k for k in range(4, 100) if F(3 * k - 9, 5 * k - 9) == F(12, 23))))

mcq('qa2-151', 'Logarithms',
    'If log₁₂ 27 = a, what is log₆ 16 in terms of a?',
    ['4(3 − a)/(3 + a)', '4(3 + a)/(3 − a)', '(3 − a)/(3 + a)', '2(3 − a)/(3 + a)'],
    'With x = log₂ 3: a = 3x/(2 + x), so x = 2a/(3 − a). log₆ 16 = 4/(1 + x) = 4(3 − a)/(3 + a).', difficulty='HARD',
    check=lambda: (lambda a: '4(3 − a)/(3 + a)' if abs(math.log(16, 6) - 4 * (3 - a) / (3 + a)) < 1e-12 else None)(math.log(27, 12)))

mcq('qa2-152', 'Trigonometry basics',
    'What is the greatest value of 3 sin x + 4 cos x?',
    ['5', '7', '4', '√7'],
    'a sin x + b cos x ranges between −√(a² + b²) and √(a² + b²) = 5.',
    check=lambda: str(round(max(3 * math.sin(t / 1000) + 4 * math.cos(t / 1000) for t in range(6284)), 3)).rstrip('0').rstrip('.'))


def questions():
    out = []
    for q in Q:
        got = q['check']()
        want = q['options'][0] if q['type'] == 'MCQ' else q['answer']
        if q['type'] == 'TITA':
            ok = got is not None and abs(float(got) - float(want)) < 1e-9
        else:
            ok = got == want
            assert len(set(q['options'])) == 4, f"{q['id']}: options are not four distinct values"
        assert ok, f"{q['id']}: key {want!r} but the check computes {got!r}"
        out.append({k: v for k, v in q.items() if k != 'check'})
    return out
