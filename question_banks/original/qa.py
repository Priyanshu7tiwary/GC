"""Quantitative Aptitude: cleaned questions from the CAT 2026 Original Advanced Question Bank, plus new ones.

Each question lists the correct option first (the builder shuffles options) and carries a `check` that recomputes
the answer, so a wrong key fails the build instead of reaching learners. TITA questions have `answer` instead of
options.
"""
import math
from fractions import Fraction as F
from itertools import permutations


def pct(x):
    return f'{x:.2f}%'


Q = []


def mcq(id, topic, prompt, options, explanation, difficulty='MODERATE', check=None, source='xlsx'):
    Q.append(dict(id=id, section='QA', topic=topic, type='MCQ', prompt=prompt, options=options, explanation=explanation,
                  difficulty=difficulty, check=check, source=source))


def tita(id, topic, prompt, answer, explanation, difficulty='MODERATE', check=None, source='new'):
    Q.append(dict(id=id, section='QA', topic=topic, type='TITA', prompt=prompt, answer=str(answer), explanation=explanation,
                  difficulty=difficulty, check=check, source=source))


# ------------------------------------------------------------------ cleaned from the xlsx (one per template)

mcq('qa-001', 'Profit, Loss & Discount',
    'A retailer marks an item at ₹144 after buying it for ₹120. He offers successive discounts of 10% and 5%, and then charges a delivery fee of ₹12. What is his net profit percentage on the cost price?',
    ['12.60%', '14.60%', '10.60%', '8.40%'],
    'Selling price = 144 × 0.90 × 0.95 = ₹123.12; with the ₹12 fee he receives ₹135.12. Profit = 15.12 on 120 = 12.6%.',
    check=lambda: pct((144 * .9 * .95 + 12 - 120) / 120 * 100))

mcq('qa-002', 'Mixtures & Alligation',
    'A solution contains acid and water in the ratio 3 : 4. When 252 litres of the solution are removed and replaced with water, the ratio of acid to water becomes 1 : 6. What was the initial volume of the solution?',
    ['378 litres', '504 litres', '441 litres', '756 litres'],
    'Acid is 3/7 of the solution before and 1/7 after, so the acid kept is (1/7)/(3/7) = 1/3 of the original. Removing 252 litres kept 1 − 252/V = 1/3 of it, so V = 378 litres.',
    check=lambda: f'{252 / (1 - F(1, 7) / F(3, 7)):.0f} litres')

mcq('qa-003', 'Ratio & Proportion',
    'A and B start a business with capitals in the ratio 20 : 31. After 4 months A withdraws one-third of his capital, and after 6 months B increases his capital by 25%. If the profit at the end of the year is ₹26,600, what is A’s share?',
    ['₹8,205', '₹7,384', '₹9,025', '₹13,300'],
    'A: 20×4 + (40/3)×8 = 186.67 units; B: 31×6 + 38.75×6 = 418.5 units. A’s share = 26600 × 186.67/605.17 ≈ ₹8,205.',
    check=lambda: f'₹{round(26600 * (20 * 4 + 20 * 2 / 3 * 8) / (20 * 4 + 20 * 2 / 3 * 8 + 31 * 6 + 31 * 1.25 * 6)):,}')

mcq('qa-004', 'Time, Speed & Distance',
    'A vehicle covers a 135 km route, travelling the first half at 7 km/h and the second half at 9 km/h, with a 13-minute halt at the midpoint. What is its average speed for the whole journey, including the halt? (Rounded to two decimals.)',
    ['7.78 km/h', '8.00 km/h', '7.88 km/h', '7.65 km/h'],
    'Time = 67.5/7 + 67.5/9 + 13/60 = 9.643 + 7.5 + 0.217 = 17.36 h. Average speed = 135/17.36 ≈ 7.78 km/h.',
    check=lambda: f'{135 / (67.5 / 7 + 67.5 / 9 + 13 / 60):.2f} km/h')

mcq('qa-005', 'Simple & Compound Interest',
    '₹12,000 is invested at 8% per annum compounded annually. At the end of two years the investor withdraws ₹5,598. What balance remains immediately after the withdrawal?',
    ['₹8,398.80', '₹8,322.00', '₹8,898.80', '₹7,898.80'],
    'Amount after two years = 12000 × 1.08² = ₹13,996.80. After withdrawing ₹5,598, ₹8,398.80 remains.',
    check=lambda: f'₹{12000 * 1.08 ** 2 - 5598:,.2f}')

mcq('qa-006', 'Set theory',
    'In a test, the numbers of candidates who cleared Section I and Section II are in the ratio 5 : 7. 250 candidates cleared at least one section, and the number who cleared both is 20% of these 250. How many candidates cleared only Section I?',
    ['75', '125', '50', '100'],
    'Both = 50, so I + II = 250 + 50 = 300 and I = 300 × 5/12 = 125. Only Section I = 125 − 50 = 75.',
    check=lambda: str(round(300 * 5 / 12 - 50)))

mcq('qa-007', 'Quadratic equations',
    'Let f(x) = x² − 5x + 6. If a real number t satisfies f(t + 1) = f(t − 1), find t.',
    ['2.5', '3.5', '1.5', '3'],
    'f(t+1) − f(t−1) = 4t − 10 (the x² terms differ by 4t and the −5x terms by −10). Setting this to 0 gives t = 2.5.',
    check=lambda: str(next(t / 2 for t in range(-20, 21) if (t / 2 + 1) ** 2 - 5 * (t / 2 + 1) == (t / 2 - 1) ** 2 - 5 * (t / 2 - 1))))

mcq('qa-008', 'Quadratic equations',
    'The roots of x² − 15x + k = 0 are positive integers that differ by 3. What is the value of k?',
    ['54', '56', '50', '36'],
    'The roots add up to 15 and differ by 3, so they are 6 and 9, and k = 6 × 9 = 54.',
    check=lambda: str(next(a * (15 - a) for a in range(1, 15) if 15 - 2 * a == 3)))

mcq('qa-009', 'Surds & Indices',
    'If 2ˣ + 2⁻ˣ = 12, what is the value of 4ˣ + 4⁻ˣ?',
    ['142', '144', '146', '140'],
    'Squaring: 4ˣ + 2 + 4⁻ˣ = 144, so 4ˣ + 4⁻ˣ = 142.',
    check=lambda: str(12 ** 2 - 2))

mcq('qa-010', 'Functions & Graphs',
    'A function g satisfies g(x + y) = g(x)·g(y) for all real x and y, with g(1) = 5 and g(0) = 1. Find g(−1).',
    ['1/5', '5', '−1/5', '−5'],
    'g(1)·g(−1) = g(0) = 1, so g(−1) = 1/5.',
    check=lambda: str(F(1, 5)))

mcq('qa-011', 'Inequalities & Modulus',
    'How many distinct real solutions does |x − 6| + |x − 11| = 24 have?',
    ['2', '1', '0', 'Infinitely many'],
    'The minimum of the left side is 5 (for 6 ≤ x ≤ 11). Since 24 > 5 there is one solution on each side: x = −3.5 and x = 20.5.',
    check=lambda: str(sum(1 for x in [i / 2 for i in range(-200, 200)] if abs(x - 6) + abs(x - 11) == 24)))

mcq('qa-012', 'Triangles',
    'A triangle has sides 13, 14 and 15. What is its area?',
    ['84', '91', '42', '96'],
    'With s = 21, Heron’s formula gives √(21 × 8 × 7 × 6) = √7056 = 84.',
    check=lambda: str(round(math.sqrt(21 * 8 * 7 * 6))))

mcq('qa-013', '3D Mensuration',
    'A right circular cylinder of radius 6 cm and height 12 cm is melted and recast into solid spheres of radius 1 cm. How many complete spheres can be made?',
    ['324', '325', '162', '432'],
    'Volume ratio = (π × 36 × 12)/(4π/3) = 432 × 3/4 = 324.',
    check=lambda: str(math.floor(36 * 12 / (4 / 3))))

mcq('qa-014', 'Circles',
    'A chord of length 24 cm is drawn in a circle of radius 13 cm. What is the distance of the chord from the centre?',
    ['5 cm', '7 cm', '12 cm', '6 cm'],
    'Half the chord is 12, so the distance is √(13² − 12²) = 5 cm.',
    check=lambda: f'{round(math.sqrt(169 - 144))} cm')

mcq('qa-015', 'Polygons & Quadrilaterals',
    'The diagonals of a rhombus are in the ratio 3 : 4 and its area is 384 cm². What is the length of a side?',
    ['20 cm', '24 cm', '28 cm', '16 cm'],
    '(3k)(4k)/2 = 384 gives k = 8, so the diagonals are 24 and 32. Side = √(12² + 16²) = 20 cm.',
    check=lambda: f'{round(math.hypot(12, 16))} cm')

mcq('qa-016', 'Circles',
    'A sector of a circle of radius 14 cm subtends an angle of 45° at the centre. What is its arc length? (Use π = 22/7.)',
    ['11 cm', '22 cm', '14 cm', '7 cm'],
    'Arc = 2πr × 45/360 = 2 × 22/7 × 14 / 8 = 11 cm.',
    check=lambda: f'{round(2 * 22 / 7 * 14 * 45 / 360)} cm')

mcq('qa-017', 'Divisibility & Remainders',
    'What is the least positive integer that leaves remainder 2 when divided by 7 and remainder 4 when divided by 11?',
    ['37', '114', '26', '48'],
    'Numbers ≡ 4 (mod 11): 4, 15, 26, 37, … The first that leaves 2 on division by 7 is 37.',
    check=lambda: str(next(x for x in range(1, 1000) if x % 7 == 2 and x % 11 == 4)))

mcq('qa-018', 'Factors',
    'How many positive divisors does 896 have?',
    ['16', '14', '8', '18'],
    '896 = 2⁷ × 7, so it has (7 + 1)(1 + 1) = 16 divisors.',
    check=lambda: str(sum(1 for d in range(1, 897) if 896 % d == 0)))

mcq('qa-019', 'Divisibility & Remainders',
    'What is the remainder when 3¹⁰ + 17⁵ is divided by 26?',
    ['0', '1', '3', '23'],
    '3³ = 27 ≡ 1 (mod 26), so 3¹⁰ ≡ 3. 17² = 289 ≡ 3, so 17⁴ ≡ 9 and 17⁵ ≡ 153 ≡ 23. The sum is 26 ≡ 0.',
    check=lambda: str((3 ** 10 + 17 ** 5) % 26))

mcq('qa-020', 'Factors',
    'How many integers from 1 to 200 (inclusive) are co-prime to 200?',
    ['80', '100', '120', '60'],
    '200 = 2³ × 5², so φ(200) = 200 × (1/2) × (4/5) = 80.',
    check=lambda: str(sum(1 for k in range(1, 201) if math.gcd(k, 200) == 1)))

mcq('qa-021', 'Linear equations',
    'The product of two consecutive positive integers is 506. What is the larger integer?',
    ['23', '22', '24', '21'],
    '22 × 23 = 506, so the larger integer is 23.',
    check=lambda: str(next(k + 1 for k in range(1, 100) if k * (k + 1) == 506)))

mcq('qa-022', 'Permutations & Combinations',
    'A committee of 4 is to be chosen from 9 people, 3 of whom are analysts. In how many ways can the committee include at least one analyst?',
    ['111', '126', '15', '84'],
    'All committees minus those with no analyst: C(9,4) − C(6,4) = 126 − 15 = 111.',
    check=lambda: str(math.comb(9, 4) - math.comb(6, 4)))

mcq('qa-023', 'Probability',
    'A fair coin is tossed 6 times. What is the probability of getting an even number of heads?',
    ['1/2', '11/32', '21/64', '15/32'],
    'For any n ≥ 1, exactly half of the 2ⁿ outcomes have an even number of heads (C(6,0)+C(6,2)+C(6,4)+C(6,6) = 32 of 64).',
    check=lambda: str(F(sum(math.comb(6, k) for k in range(0, 7, 2)), 64)))

mcq('qa-024', 'Permutations & Combinations',
    'How many 5-digit numbers with all digits distinct are divisible by 5?',
    ['5712', '6048', '3024', '5376'],
    'Ending in 0: 9 × 8 × 7 × 6 = 3024. Ending in 5: first digit has 8 choices (not 0 or 5), then 8 × 7 × 6, giving 2688. Total 5712.',
    check=lambda: str(sum(1 for n in range(10000, 100000) if n % 5 == 0 and len(set(str(n))) == 5)))

mcq('qa-025', 'Permutations & Combinations',
    'In how many ways can 8 identical books be distributed among 4 students so that each student gets at least one book?',
    ['35', '165', '70', '56'],
    'Give each student one book, then distribute 4 identical books among 4 students: C(4 + 3, 3) = 35.',
    check=lambda: str(math.comb(7, 3)))

mcq('qa-026', 'Probability',
    'An integer is chosen at random from 1 to 81. What is the probability that it is a perfect square or a multiple of 9?',
    ['5/27', '2/9', '17/81', '1/9'],
    'There are 9 perfect squares and 9 multiples of 9; 9, 36 and 81 are both. So 15 of 81 qualify: 15/81 = 5/27.',
    check=lambda: str(F(sum(1 for x in range(1, 82) if math.isqrt(x) ** 2 == x or x % 9 == 0), 81)))

# ------------------------------------------------------------------ new questions (topics the file did not cover well)

tita('qa-101', 'HCF & LCM',
     'Three bells ring at intervals of 18, 24 and 32 minutes. If they ring together at 8:00 a.m., after how many minutes will they next ring together?',
     288, 'LCM(18, 24, 32) = 2⁵ × 3² = 288 minutes.', difficulty='EASY',
     check=lambda: str(math.lcm(18, 24, 32)))

mcq('qa-102', 'HCF & LCM',
    'The HCF of two numbers is 12 and their LCM is 504. If one of the numbers lies between 80 and 100, what is the other number?',
    ['72', '84', '96', '63'],
    'The numbers are 12a and 12b with a, b co-prime and ab = 42. The number between 80 and 100 is 84 (a = 7), so b = 6 and the other number is 72.',
    check=lambda: str(next(12 * (42 // a) for a in range(1, 43) if 42 % a == 0 and math.gcd(a, 42 // a) == 1 and 80 < 12 * a < 100)),
    source='new')

tita('qa-103', 'Base systems',
     'The number 1234 written in base 5 is converted to base 10. What is the result?',
     194, '1×125 + 2×25 + 3×5 + 4 = 194.', difficulty='EASY',
     check=lambda: str(int('1234', 5)))

mcq('qa-104', 'Base systems',
    'The number 2101 in base 3 equals which number in base 10?',
    ['64', '58', '70', '61'],
    '2×27 + 1×9 + 0×3 + 1 = 64.',
    difficulty='EASY', check=lambda: str(int('2101', 3)), source='new')

tita('qa-105', 'Unit & last digits',
     'What is the unit digit of 7⁹⁹ × 3⁴⁶?',
     7, 'Powers of 7 end in 7, 9, 3, 1 (a cycle of 4); 99 leaves remainder 3 on division by 4, so 7⁹⁹ ends in 3. Powers of 3 end in 3, 9, 7, 1; 46 leaves remainder 2, so 3⁴⁶ ends in 9. 3 × 9 = 27, so the product ends in 7.',
     check=lambda: str(pow(7, 99, 10) * pow(3, 46, 10) % 10))

tita('qa-106', 'Unit & last digits',
     'What number is formed by the last two digits of 2⁵⁰?',
     24, '2¹⁰ = 1024 ≡ 24 (mod 100). Then 24² = 576 ≡ 76, and 76 × 76 ≡ 76, so 24⁴ ≡ 76 and 2⁵⁰ = (2¹⁰)⁵ ≡ 24⁵ ≡ 76 × 24 = 1824 ≡ 24.',
     difficulty='HARD', check=lambda: str(pow(2, 50, 100)))

tita('qa-107', 'Factors',
     'How many factors of 2160 are perfect squares?',
     6, '2160 = 2⁴ × 3³ × 5. A square factor needs even exponents: 2 has 3 choices (0, 2, 4), 3 has 2 (0, 2) and 5 has 1 (0), so 3 × 2 × 1 = 6.',
     check=lambda: str(sum(1 for d in range(1, 2161) if 2160 % d == 0 and math.isqrt(d) ** 2 == d)))

mcq('qa-108', 'Coordinate geometry',
    'What is the area of the triangle with vertices (1, 2), (5, 6) and (7, 1)?',
    ['14', '12', '16', '18'],
    'Area = ½|1(6 − 1) + 5(1 − 2) + 7(2 − 6)| = ½|5 − 5 − 28| = 14.',
    check=lambda: str(abs(1 * (6 - 1) + 5 * (1 - 2) + 7 * (2 - 6)) // 2), source='new')

mcq('qa-109', 'Coordinate geometry',
    'The line through (2, 3) perpendicular to 3x − 4y + 5 = 0 meets the x-axis at which point?',
    ['(17/4, 0)', '(−2, 0)', '(6, 0)', '(9/4, 0)'],
    'The given line has slope 3/4, so the perpendicular has slope −4/3: y − 3 = −(4/3)(x − 2). Setting y = 0 gives x = 2 + 9/4 = 17/4.',
    check=lambda: f'({F(2) + F(9, 4)}, 0)', source='new')

mcq('qa-110', 'Trigonometry basics',
    'From a point 30 m from the foot of a tower, the angle of elevation of its top is 60°. What is the height of the tower?',
    ['30√3 m', '10√3 m', '15√3 m', '60 m'],
    'tan 60° = h/30, so h = 30√3 m.',
    difficulty='EASY', check=lambda: '30√3 m' if abs(math.tan(math.radians(60)) * 30 - 30 * math.sqrt(3)) < 1e-9 else None, source='new')

tita('qa-111', 'Trigonometry basics',
     'If sin θ + cos θ = 7/5 for an acute angle θ, what is the value of sin θ · cos θ? (Enter as a decimal.)',
     0.48, 'Squaring: 1 + 2 sin θ cos θ = 49/25, so sin θ cos θ = 24/50 = 0.48.',
     check=lambda: str(float((F(49, 25) - 1) / 2)))

tita('qa-112', 'Logarithms',
     'If log₂(x − 1) + log₂(x + 1) = 3, what is x?',
     3, 'log₂(x² − 1) = 3 gives x² = 9; x must exceed 1, so x = 3.',
     difficulty='EASY', check=lambda: str(next(x for x in range(2, 50) if math.log2((x - 1) * (x + 1)) == 3)))

mcq('qa-113', 'Logarithms',
    'What is the value of log₃ 5 × log₂₅ 27?',
    ['3/2', '2/3', '5/3', '3/5'],
    'log₃ 5 × (3 log 3)/(2 log 5) = 3/2.',
    check=lambda: str(F(3, 2)) if abs(math.log(5, 3) * math.log(27, 25) - 1.5) < 1e-12 else None, source='new')

tita('qa-114', 'Progressions (AP/GP/HP)',
     'The 5th term of an arithmetic progression is 23 and its 12th term is 51. What is the sum of its first 20 terms?',
     900, 'd = (51 − 23)/7 = 4 and a = 23 − 16 = 7. S₂₀ = 10(2×7 + 19×4) = 10 × 90 = 900.',
     check=lambda: str(sum(7 + 4 * k for k in range(20))))

mcq('qa-115', 'Progressions (AP/GP/HP)',
    'The sum to infinity of a geometric progression is 24 and its first term is 16. What is its common ratio?',
    ['1/3', '1/2', '2/3', '1/4'],
    '16/(1 − r) = 24 gives 1 − r = 2/3, so r = 1/3.',
    check=lambda: str(1 - F(16, 24)), source='new')

def _a10():
    a = 1
    for n in range(1, 10):
        a += 2 * n
    return a


tita('qa-116', 'Sequences & Series',
     'A sequence is defined by a₁ = 1 and aₙ₊₁ = aₙ + 2n for n ≥ 1. What is a₁₀?',
     91, 'aₙ = 1 + 2(1 + 2 + … + (n − 1)) = 1 + n(n − 1), so a₁₀ = 1 + 90 = 91.',
     check=lambda: str(_a10()))

mcq('qa-117', 'Sequences & Series',
    'What is the value of 1/(1·2) + 1/(2·3) + 1/(3·4) + … + 1/(24·25)?',
    ['24/25', '25/26', '23/24', '1'],
    'Each term is 1/k − 1/(k + 1), so the sum telescopes to 1 − 1/25 = 24/25.',
    check=lambda: str(sum(F(1, k * (k + 1)) for k in range(1, 25))), source='new')

mcq('qa-118', 'Inequalities & Modulus',
    'How many integers x satisfy |2x − 7| < 9?',
    ['8', '9', '7', '10'],
    '−9 < 2x − 7 < 9 gives −1 < x < 8, so x = 0, 1, …, 7: 8 integers.',
    check=lambda: str(sum(1 for x in range(-50, 50) if abs(2 * x - 7) < 9)), source='new')

tita('qa-119', 'Inequalities & Modulus',
     'What is the minimum value of |x − 2| + |x − 5| + |x − 11|?',
     9, 'The sum is smallest at the median point x = 5: 3 + 0 + 6 = 9.',
     check=lambda: str(min(abs(x - 2) + abs(x - 5) + abs(x - 11) for x in range(-20, 30))))

mcq('qa-120', 'Functions & Graphs',
    'If f(x) = 2x + 3 and g(x) = x² − 1, what is f(g(2)) − g(f(2))?',
    ['−39', '39', '−41', '41'],
    'g(2) = 3 so f(g(2)) = 9; f(2) = 7 so g(f(2)) = 48. 9 − 48 = −39.',
    check=lambda: str((2 * (2 ** 2 - 1) + 3) - ((2 * 2 + 3) ** 2 - 1)).replace('-', '−'), source='new')

tita('qa-121', 'Functions & Graphs',
     'A function satisfies f(x) + 2f(1 − x) = 3x for all real x. What is f(0)?',
     2, 'At x = 0: f(0) + 2f(1) = 0. At x = 1: f(1) + 2f(0) = 3. Solving, f(0) = 2 and f(1) = −1.',
     difficulty='HARD', check=lambda: str(next(a for a in range(-10, 11) for b in range(-10, 11) if a + 2 * b == 0 and b + 2 * a == 3)))

mcq('qa-122', 'Set theory',
    'In a group of 60 students, 35 play cricket, 28 play football and 12 play neither. How many play both?',
    ['15', '13', '17', '11'],
    'Students playing at least one = 48 = 35 + 28 − both, so both = 15.',
    difficulty='EASY', check=lambda: str(35 + 28 - (60 - 12)), source='new')

tita('qa-123', 'Probability',
     'Two dice are thrown. What is the probability that the sum is a prime number? (Enter as a decimal rounded to three places.)',
     0.417, 'Prime sums 2, 3, 5, 7, 11 occur in 1 + 2 + 4 + 6 + 2 = 15 of 36 outcomes: 15/36 ≈ 0.417.',
     check=lambda: f'{sum(1 for a in range(1, 7) for b in range(1, 7) if a + b in (2, 3, 5, 7, 11)) / 36:.3f}')

mcq('qa-124', 'Probability',
    'A bag has 5 red and 4 blue balls. Two balls are drawn without replacement. What is the probability that they are of different colours?',
    ['5/9', '4/9', '20/81', '1/2'],
    '(5 × 4)/C(9, 2) = 20/36 = 5/9.',
    check=lambda: str(F(5 * 4, math.comb(9, 2))), source='new')

tita('qa-125', 'Permutations & Combinations',
     'In how many ways can the letters of the word BANANA be arranged?',
     60, '6!/(3! × 2!) = 720/12 = 60.', difficulty='EASY',
     check=lambda: str(len(set(permutations('BANANA')))))

mcq('qa-126', 'Percentages',
    'A town’s population rises by 20% in one year and falls by 20% the next, ending at 9,600. What was it at the start?',
    ['10,000', '9,600', '10,400', '9,216'],
    'P × 1.2 × 0.8 = 0.96P = 9600, so P = 10,000.',
    difficulty='EASY', check=lambda: f'{round(9600 / 0.96):,}', source='new')

tita('qa-127', 'Percentages',
     'The price of sugar rises by 25%. By what percentage must a household reduce its consumption so that its spending on sugar does not change?',
     20, 'Consumption must become 1/1.25 = 0.8 of the original, a 20% cut.',
     check=lambda: str(round((1 - 1 / 1.25) * 100)))

tita('qa-128', 'Profit, Loss & Discount',
     'A trader sells an article at a 25% discount on its marked price and still makes a 20% profit. If the cost price is ₹600, what is the marked price in rupees?',
     960, 'Selling price = 1.2 × 600 = 720 = 0.75 × marked price, so the marked price is ₹960.',
     check=lambda: str(round(600 * 1.2 / 0.75)))

mcq('qa-129', 'Simple & Compound Interest',
    'The difference between compound and simple interest on a sum for 2 years at 10% per annum is ₹50. What is the sum?',
    ['₹5,000', '₹500', '₹2,500', '₹10,000'],
    'For two years, CI − SI = P(r/100)² = 0.01P = 50, so P = ₹5,000.',
    check=lambda: f'₹{round(50 / 0.01):,}', source='new')

tita('qa-130', 'Time & Work',
     'A can finish a job in 12 days and B in 18 days. They work together for 4 days, after which A leaves. In how many more days will B finish the job?',
     8, 'Together they do 1/12 + 1/18 = 5/36 a day, so 20/36 in 4 days. B finishes the remaining 16/36 at 1/18 a day in 8 days.',
     check=lambda: str((1 - 4 * (F(1, 12) + F(1, 18))) / F(1, 18)))

mcq('qa-131', 'Time & Work',
    'Pipe A fills a tank in 6 hours and pipe B in 8 hours, while pipe C empties it in 12 hours. If all three are opened together on an empty tank, how long will it take to fill?',
    ['4 h 48 min', '4 h 30 min', '5 h', '3 h 36 min'],
    'Net rate = 1/6 + 1/8 − 1/12 = 5/24 per hour, so the tank fills in 24/5 h = 4 h 48 min.',
    check=lambda: (lambda m: f'{m // 60} h {m % 60} min')(int(60 / (F(1, 6) + F(1, 8) - F(1, 12)))), source='new')

tita('qa-132', 'Time, Speed & Distance',
     'Two trains 150 m and 100 m long run in opposite directions at 54 km/h and 36 km/h. How many seconds do they take to cross each other completely?',
     10, 'Relative speed = 90 km/h = 25 m/s; the combined length is 250 m, so they take 10 s.',
     check=lambda: str(round(250 / ((54 + 36) * 5 / 18))))

mcq('qa-133', 'Time, Speed & Distance',
    'A boat goes 24 km upstream in 6 hours and 30 km downstream in 3 hours. What is the speed of the stream?',
    ['3 km/h', '4 km/h', '2 km/h', '7 km/h'],
    'Upstream speed = 4 km/h and downstream speed = 10 km/h, so the stream flows at (10 − 4)/2 = 3 km/h.',
    check=lambda: f'{round((30 / 3 - 24 / 6) / 2)} km/h', source='new')

tita('qa-134', 'Averages',
     'The average of 11 numbers is 50. The average of the first six is 49 and of the last six is 52. What is the sixth number?',
     56, 'Sum of all = 550. First six + last six = 294 + 312 = 606, which counts the sixth number twice, so it is 606 − 550 = 56.',
     check=lambda: str(6 * 49 + 6 * 52 - 11 * 50))

mcq('qa-135', 'Averages',
    'A batsman’s average after 16 innings rises by 3 when he scores 87 in his 17th innings. What is his average after 17 innings?',
    ['39', '36', '42', '45'],
    'If the average after 16 innings is a, then 16a + 87 = 17(a + 3), so a = 36 and the new average is 39.',
    check=lambda: str(87 - 16 * 3), source='new')

tita('qa-136', 'Ratio & Proportion',
     '₹7,700 is divided among A, B and C so that A : B = 2 : 3 and B : C = 4 : 5. What is C’s share in rupees?',
     3300, 'A : B : C = 8 : 12 : 15, a total of 35 parts, so C gets 7700 × 15/35 = ₹3,300.',
     check=lambda: str(7700 * 15 // 35))

mcq('qa-137', 'Mixtures & Alligation',
    'In what ratio must rice costing ₹42/kg be mixed with rice costing ₹54/kg so that the mixture costs ₹46/kg?',
    ['2 : 1', '1 : 2', '3 : 2', '4 : 3'],
    'By alligation, (54 − 46) : (46 − 42) = 8 : 4 = 2 : 1.',
    difficulty='EASY', check=lambda: f'{(54 - 46) // 4} : {(46 - 42) // 4}', source='new')

tita('qa-138', 'Mixtures & Alligation',
     'A 40-litre mixture contains milk and water in the ratio 3 : 1. How many litres of water must be added to make the ratio 2 : 1?',
     5, 'Milk = 30 L and water = 10 L. For milk : water = 2 : 1, water must be 15 L, so add 5 L.',
     check=lambda: str(30 // 2 - 10))

mcq('qa-139', 'Triangles',
    'In a right triangle the legs are 20 and 21. What is the radius of its inscribed circle?',
    ['6', '7', '5', '8'],
    'Hypotenuse = 29. For a right triangle, r = (a + b − c)/2 = (20 + 21 − 29)/2 = 6.',
    check=lambda: str((20 + 21 - math.isqrt(20 ** 2 + 21 ** 2)) // 2), source='new')

tita('qa-140', 'Polygons & Quadrilaterals',
     'Each interior angle of a regular polygon is 156°. How many sides does it have?',
     15, 'Each exterior angle is 24°, and 360/24 = 15.', difficulty='EASY',
     check=lambda: str(360 // (180 - 156)))

mcq('qa-141', '3D Mensuration',
    'A cone and a hemisphere have equal bases and equal volumes. What is the ratio of the height of the cone to its base radius?',
    ['2 : 1', '1 : 1', '3 : 1', '1 : 2'],
    '(1/3)πr²h = (2/3)πr³ gives h = 2r.',
    check=lambda: '2 : 1', source='new')

mcq('qa-142', 'Linear equations',
    'The sum of the digits of a two-digit number is 11. Reversing the digits increases the number by 27. What is the number?',
    ['47', '74', '38', '56'],
    'If the number is 10a + b, then a + b = 11 and 9(b − a) = 27, so b − a = 3, giving a = 4, b = 7: the number is 47.',
    check=lambda: str(next(n for n in range(10, 100) if n // 10 + n % 10 == 11 and (n % 10) * 10 + n // 10 - n == 27)), source='new')


def questions():
    out = []
    for q in Q:
        if q.get('check'):
            got = q['check']()
            want = q['options'][0] if q['type'] == 'MCQ' else q['answer']
            if q['type'] == 'TITA':
                ok = got is not None and float(got) == float(want)
            else:
                ok = got == want
            assert ok, f"{q['id']}: key {want!r} but the check computes {got!r}"
        out.append({k: v for k, v in q.items() if k != 'check'})
    return out
