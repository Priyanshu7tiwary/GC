"""VARC: the 15 passages from the CAT 2026 Original Advanced Question Bank with new, passage-specific questions,
plus new verbal-ability items.

The file reused the same six generic questions (with identical options) for every passage, and each of its four
verbal-ability items 15 times; those are replaced. MCQ options list the correct answer first (the builder shuffles).
Para-jumble and odd-sentence answers are typed in (TITA), as in CAT.
"""

PASSAGES = {}
RC = []
VA = []


def passage(pid, title, text):
    PASSAGES[pid] = f'{title}\n\n{text}'


def rc(pid, topic, prompt, options, explanation, difficulty='MODERATE'):
    RC.append(dict(passage=pid, topic=topic, type='MCQ', prompt=prompt, options=options, explanation=explanation, difficulty=difficulty))


passage('p01', 'The architecture of forgetting', 'Cities often treat memory as an inventory: monuments preserve celebrated events, plaques identify exemplary citizens, and museums arrange the past into a sequence that appears settled. Yet urban memory is less like an archive than a negotiation. A demolished market may survive in the habits of vendors who relocate; a disused railway line may persist as a corridor in the city\'s mental map. Conversely, a monument can remain physically present while its meaning is quietly abandoned. The built environment therefore does not simply store history. It selects, edits and sometimes contradicts the stories residents tell about themselves. Planning authorities frequently describe redevelopment as the replacement of obsolete uses by more efficient ones. This vocabulary is not neutral. It makes displacement sound like technical housekeeping and turns contested social claims into questions of land utilisation. But preserving every structure is neither possible nor desirable. The more difficult task is to recognise that demolition is also an act of authorship: it determines which traces will remain available to future interpretation. A city that remembers responsibly need not freeze its streets; it must, however, make visible the choices through which its past is continually rewritten.')
rc('p01', 'Main idea / Title', 'Which of the following best captures the central claim of the passage?',
   ['Because a city’s fabric shapes what its past means, decisions to demolish or preserve should be recognised and made visible as choices about memory.',
    'Cities should preserve every historic structure so that residents do not lose touch with their past.',
    'Monuments and museums are the most reliable means by which cities store their history.',
    'Redevelopment is a neutral technical process of replacing obsolete land uses with efficient ones.'],
   'The author argues that the built environment “selects, edits and sometimes contradicts” history, rejects total preservation, and asks that the choices be made visible. B is explicitly rejected; C and D are views the author questions.')
rc('p01', 'Inference questions', 'The author mentions a demolished market whose vendors relocate in order to show that',
   ['urban memory can outlive the physical structures associated with it.',
    'relocation is usually forced on vendors by planning authorities.',
    'markets are more historically significant than monuments.',
    'demolition always erases the memory of a place.'],
   'The example illustrates memory “surviving” in habits after the building is gone — the opposite of D. A and C go beyond the text.')
rc('p01', 'Tone & Attitude', 'The author’s attitude towards the planning vocabulary of “obsolete uses” and “efficient” replacement is best described as',
   ['critical, because it presents contested social choices as neutral technical matters.',
    'approving, because it brings rigour to decisions about land.',
    'indifferent, since the choice of words does not affect outcomes.',
    'nostalgic, because it threatens the city’s traditional character.'],
   '“This vocabulary is not neutral. It makes displacement sound like technical housekeeping…” — a pointed critique, not nostalgia.')

passage('p02', 'The limits of prediction', 'Prediction is often confused with understanding because both produce statements about what has not yet happened. A model may forecast demand with impressive accuracy while remaining silent about why consumers behave as they do. This distinction matters when conditions change. A model trained on stable patterns can become brittle precisely when decision-makers most need guidance. Yet the opposite error is to dismiss prediction as intellectually shallow. Forecasts can discipline intuition, expose hidden assumptions and reveal the consequences of uncertainty. The appropriate question is not whether a model understands the world in a human sense, but whether its errors are visible, bounded and consequentially manageable. Institutions often prefer a single forecast because it appears to simplify responsibility. A range of scenarios, by contrast, forces them to acknowledge that choices depend on values as well as probabilities. The most useful forecast may therefore be one that refuses to masquerade as certainty.')
rc('p02', 'Main idea / Title', 'Which title best suits the passage?',
   ['Useful Forecasts: Valuing Prediction Without Mistaking It for Certainty',
    'Why Predictive Models Should Be Abandoned',
    'Accuracy as the Only Test of a Good Model',
    'How Consumers Really Make Decisions'],
   'The passage defends prediction against dismissal while warning against treating it as understanding or certainty. B and C each take one extreme the author rejects; D is a detail.')
rc('p02', 'Inference questions', 'According to the author, a model trained on stable patterns is most likely to fail',
   ['when underlying conditions change, which is when guidance is most needed.',
    'when it is used for routine demand forecasting.',
    'when decision-makers prefer a single forecast.',
    'when its errors are visible and bounded.'],
   '“A model trained on stable patterns can become brittle precisely when decision-makers most need guidance” — that is, when conditions change.')
rc('p02', 'Strengthen / Weaken', 'Which of the following, if true, would most support the author’s view of why institutions prefer single forecasts?',
   ['Officials presented with several scenarios were more often asked to justify the values behind their final choice.',
    'Single forecasts are on average more accurate than scenario ranges.',
    'Scenario planning takes more computing power than point forecasting.',
    'Most consumers ignore official forecasts altogether.'],
   'The author claims single forecasts appear to simplify responsibility while scenarios force acknowledgement of values. A shows exactly that effect. B is about accuracy, not responsibility; C and D are irrelevant.')

passage('p03', 'The economics of attention', 'Attention is routinely described as though it were a commodity that can be transferred intact from one object to another. But attention is also a capacity shaped by the conditions under which it is exercised. A notification does not merely take seconds from a task; it can alter the mental context in which the task was being pursued. Platforms optimise measurable engagement because duration and frequency are legible to their systems. The quality of attention—whether it is reflective, fragmented, curious or coerced—is harder to quantify. This does not mean that all interruption is harmful or that sustained concentration is always virtuous. Some tasks benefit from rapid switching, and some forms of browsing generate unexpected connections. The problem arises when a metric designed to approximate attention becomes a substitute for the human purposes that made attention valuable in the first place.')
rc('p03', 'Main idea / Title', 'The author’s main concern is that',
   ['measures of engagement can come to replace the purposes for which attention matters.',
    'all interruptions damage concentration and should be minimised.',
    'platforms deliberately coerce users into fragmented attention.',
    'attention can be transferred intact between tasks like any commodity.'],
   'The final sentence states the problem: a metric that approximates attention “becomes a substitute for the human purposes”. B and D are positions the author disowns; C overstates.')
rc('p03', 'Specific detail', 'Why, according to the passage, do platforms optimise engagement?',
   ['Duration and frequency are easy for their systems to measure.',
    'Engagement is the best indicator of reflective attention.',
    'Users explicitly ask for more notifications.',
    'Sustained concentration has been shown to be harmful.'],
   '“Platforms optimise measurable engagement because duration and frequency are legible to their systems.”')
rc('p03', 'Inference questions', 'Which of the following would the author most likely accept?',
   ['A task that benefits from rapid switching may be served well by some interruption.',
    'Every notification costs exactly the seconds it takes to read.',
    'Measuring the quality of attention is as easy as measuring its duration.',
    'Browsing never produces anything of value.'],
   'The author concedes “some tasks benefit from rapid switching”. B contradicts the point about altered mental context; C and D contradict explicit statements.')

passage('p04', 'When maps become arguments', 'A map appears to describe territory, but every map also advances a theory of relevance. A transport map exaggerates some distances and suppresses others; a risk map converts uncertain events into coloured zones; a historical map chooses which boundaries deserve emphasis. Such simplification is unavoidable, since a map that represented everything would represent nothing intelligibly. The ethical question is therefore not whether maps distort, but who chooses the distortion and who bears its consequences. A flood-risk map may protect property owners while rendering informal settlements statistically invisible if its data omit unregistered dwellings. In this case, technical precision within the chosen dataset can coexist with profound representational failure. Better mapping requires not only improved instruments but scrutiny of categories, omissions and the decisions that follow.')
rc('p04', 'Main idea / Title', 'The passage is primarily concerned with',
   ['arguing that since all maps simplify, the important questions concern who decides the simplification and who is affected by it.',
    'explaining the technical methods used to produce flood-risk maps.',
    'showing that maps should represent territory as completely as possible.',
    'criticising transport maps for distorting distances.'],
   'The author accepts that distortion is unavoidable and shifts the ethical question to “who chooses the distortion and who bears its consequences”. C is explicitly rejected.')
rc('p04', 'Inference questions', 'The flood-risk example suggests that a map can be',
   ['precise in its own terms while seriously misrepresenting the population at risk.',
    'useless unless it includes every dwelling in an area.',
    'improved only by better measuring instruments.',
    'neutral as long as its data are accurate.'],
   '“Technical precision within the chosen dataset can coexist with profound representational failure.” D is what the passage refutes; C ignores “not only improved instruments”.')
rc('p04', 'Strengthen / Weaken', 'Which finding would most weaken the author’s claim about the flood-risk map?',
   ['Its dataset was found to include every unregistered dwelling in the area.',
    'Property owners consulted the map more often than other residents.',
    'The map used colour coding to show levels of risk.',
    'Flood events in the area were rare and hard to predict.'],
   'The claim depends on the data omitting unregistered dwellings. If they were included, informal settlements would not be statistically invisible.')

passage('p05', 'The paradox of expertise', 'Expertise gives people the ability to notice distinctions that novices cannot see. It also creates the danger that familiar categories become invisible. A seasoned critic may identify subtle departures from convention but overlook a practice that does not fit the discipline\'s established vocabulary. This is not an argument against specialisation. Complex societies depend on accumulated knowledge and trained judgement. Rather, expertise should be understood as a disciplined partial vision: powerful within a domain, incomplete outside it. Institutions sometimes reward confidence more than calibrated uncertainty, encouraging experts to extend claims beyond the evidence. The remedy is not compulsory modesty but procedures that expose assumptions to challenge, especially from people whose experience falls outside the recognised field.')
rc('p05', 'Main idea / Title', 'Which statement best expresses the author’s view of expertise?',
   ['It is a powerful but partial way of seeing, whose blind spots are best checked by procedures that invite outside challenge.',
    'It is overrated, and societies should rely more on novices.',
    'It should be replaced by compulsory modesty among specialists.',
    'It is reliable in every domain once enough knowledge is accumulated.'],
   '“Disciplined partial vision”, defended against dismissal (“not an argument against specialisation”) and with the remedy of “procedures that expose assumptions to challenge”. C is explicitly rejected.')
rc('p05', 'Inference questions', 'The example of the seasoned critic illustrates that',
   ['the categories that sharpen an expert’s perception can also hide what falls outside them.',
    'critics are less perceptive than novices.',
    'conventions in the arts change too quickly for experts to follow.',
    'experts deliberately ignore unconventional work.'],
   'The critic sees “subtle departures from convention” but misses what does not fit “the discipline’s established vocabulary” — the paradox of the title. Nothing suggests deliberate neglect (D).')
rc('p05', 'Specific detail', 'According to the passage, what encourages experts to make claims beyond the evidence?',
   ['Institutions that reward confidence more than calibrated uncertainty.',
    'Challenges from people outside the recognised field.',
    'The accumulated knowledge of complex societies.',
    'A lack of specialised training.'],
   '“Institutions sometimes reward confidence more than calibrated uncertainty, encouraging experts to extend claims beyond the evidence.”')

passage('p06', 'The public life of statistics', 'Numbers gain authority partly because they seem to speak without an accent. Yet statistics are produced through decisions about classification, sampling and thresholds. A rise in average income can coexist with stagnating wages for most households if gains are concentrated at the top. A declining crime rate may reflect reduced reporting rather than reduced harm. These possibilities do not make statistics arbitrary. They make interpretation indispensable. The responsible reader asks what population is counted, what is excluded, which comparison is chosen and what causal story is being smuggled into a descriptive measure. Numerical literacy is therefore not merely the ability to calculate; it is the ability to interrogate the institutional process that made a number meaningful.')
rc('p06', 'Inference questions', 'By saying that numbers “seem to speak without an accent”, the author means that they',
   ['appear neutral, free of the perspective of whoever produced them.',
    'are easy for people of every background to calculate.',
    'are usually reported in a single standard format.',
    'carry more authority in some languages than in others.'],
   'An accent marks where a speaker comes from; numbers seem to come from nowhere in particular, i.e. to be neutral — which the rest of the passage disputes.')
rc('p06', 'Specific detail', 'Which of the following does the author give as a reason a crime rate might fall?',
   ['Fewer crimes being reported, even if harm has not decreased.',
    'Gains being concentrated among the richest households.',
    'Changes in how average income is calculated.',
    'A rise in the ability of citizens to calculate.'],
   '“A declining crime rate may reflect reduced reporting rather than reduced harm.”')
rc('p06', 'Main idea / Title', 'The author’s conclusion about numerical literacy is that it',
   ['includes questioning how and why a number was produced, not just calculating with it.',
    'is unnecessary, since statistics are arbitrary.',
    'consists mainly of the ability to perform calculations accurately.',
    'should be left to institutions that produce statistics.'],
   'The last sentence: it is “not merely the ability to calculate” but “the ability to interrogate the institutional process”. B contradicts “do not make statistics arbitrary”.')

passage('p07', 'Craft and automation', 'Automation is commonly framed as the substitution of machines for human effort. In practice, it often redistributes effort rather than eliminating it. A tool that simplifies production may increase the demand for monitoring, maintenance and exception handling. It may also change what counts as skilled work: dexterity becomes less valuable while diagnosis and coordination become more important. The resulting transformation is not uniform. Workers who control the design of a system may gain autonomy, while those who must accommodate its rigid outputs may lose it. The central political question is thus not whether technology replaces labour in the abstract, but how the gains, risks and discretion created by automation are distributed.')
rc('p07', 'Main idea / Title', 'Which of the following best summarises the passage?',
   ['Automation reshapes and redistributes work and autonomy, so the key question is who gains and who loses.',
    'Automation eliminates human effort and therefore threatens all workers equally.',
    'Automation makes dexterity the most valuable workplace skill.',
    'The political debate about automation should focus on whether machines replace labour.'],
   'The passage argues effort is redistributed, skills change, effects are uneven, and the real question is distribution. D is the framing the author sets aside.')
rc('p07', 'Specific detail', 'According to the passage, which skills become more important as work is automated?',
   ['Diagnosis and coordination',
    'Dexterity and speed',
    'Physical endurance',
    'Rote production'],
   '“Dexterity becomes less valuable while diagnosis and coordination become more important.”')
rc('p07', 'Inference questions', 'The author would most likely agree that a worker who must adapt to an automated system’s rigid outputs',
   ['may lose discretion even if the system makes production simpler.',
    'will always gain autonomy from automation.',
    'is unaffected because the work has been eliminated.',
    'controls the design of the system.'],
   '“Those who must accommodate its rigid outputs may lose” autonomy.')

passage('p08', 'The case for slow institutions', 'Speed is frequently treated as evidence of competence. Organisations celebrate rapid decisions, short approval chains and immediate responses. But some institutional delays are not failures; they are mechanisms for making disagreement legible. Consultation, appeal and review can prevent a decision from being efficient for the powerful and costly for everyone else. This is not a defence of procedural accumulation for its own sake. Delay becomes wasteful when it repeats checks without changing the evidence or clarifying responsibility. The challenge is to distinguish friction that protects public reasoning from friction that merely protects an organisation against accountability.')
rc('p08', 'Main idea / Title', 'The author’s central argument is that',
   ['some delays serve a protective purpose, so the task is to tell useful friction from wasteful friction.',
    'all institutional delay is a sign of incompetence.',
    'organisations should add as many review stages as possible.',
    'speed is the best evidence of an organisation’s competence.'],
   'The author defends delays that make disagreement legible but rejects “procedural accumulation for its own sake” and ends with the need to distinguish the two kinds of friction.')
rc('p08', 'Inference questions', 'Which of these would the author regard as wasteful delay?',
   ['A fourth review stage that re-examines the same evidence without clarifying who is responsible.',
    'A public consultation that brings new objections to light.',
    'An appeal that lets an affected group contest a decision.',
    'A review that changes the evidence on which a decision rests.'],
   'Delay is wasteful “when it repeats checks without changing the evidence or clarifying responsibility”. B–D make disagreement legible or change the evidence.')
rc('p08', 'Specific detail', 'According to the passage, consultation, appeal and review can prevent a decision from being',
   ['efficient for the powerful but costly for everyone else.',
    'reached quickly by a short approval chain.',
    'accountable to the public.',
    'based on disagreement.'],
   'Stated directly in the fourth sentence.')

passage('p09', 'Scientific disagreement', 'Public accounts of science often present disagreement as a temporary defect that disappears once enough evidence accumulates. In reality, disagreement can persist because researchers ask different questions, use different scales or assign different importance to competing explanations. A debate may be productive even when it does not end in consensus: rival hypotheses can reveal assumptions and suggest new measurements. The danger lies in treating every disagreement as proof that nothing is known, or in presenting provisional agreement as absolute certainty. Scientific authority is strongest when it explains not only what is currently supported but also where the boundaries of support lie.')
rc('p09', 'Main idea / Title', 'The passage mainly argues that',
   ['scientific disagreement can be productive, and authority is strengthened by being clear about the limits of current evidence.',
    'scientists disagree only because there is not yet enough evidence.',
    'persistent disagreement proves that science cannot produce knowledge.',
    'science should always present its findings as certain.'],
   'The author rejects both errors named in the fourth sentence (C and D) and the public account in the first (B).')
rc('p09', 'Specific detail', 'Which of the following is NOT given as a reason why scientific disagreement may persist?',
   ['Some researchers deliberately ignore evidence that contradicts them.',
    'Researchers ask different questions.',
    'Researchers use different scales.',
    'Researchers weigh competing explanations differently.'],
   'The passage lists different questions, scales and weightings; it never attributes disagreement to deliberately ignoring evidence.')
rc('p09', 'Inference questions', 'The author would most likely describe a debate that ends without consensus but leads to new measurements as',
   ['productive.',
    'a failure of the scientific method.',
    'proof that nothing is known about the subject.',
    'a temporary defect to be hidden from the public.'],
   '“A debate may be productive even when it does not end in consensus: rival hypotheses can … suggest new measurements.”')

passage('p10', 'The value of imitation', 'Imitation is usually placed opposite creativity, as though originality required creation from nothing. Yet most innovation begins with the adoption of an existing form and its alteration under new constraints. A musical tradition develops through quotation; a scientific method travels by being adapted; a business model changes when transplanted into a different institutional setting. The relevant distinction is not between imitation and invention, but between copying that conceals dependence and adaptation that makes its choices visible. Originality may consist less in the absence of influence than in the intelligence with which influence is transformed.')
rc('p10', 'Main idea / Title', 'Which of the following best states the main point of the passage?',
   ['Originality lies in how intelligently existing forms are transformed, not in the absence of influence.',
    'Imitation and creativity are opposites, and only invention from nothing is original.',
    'All copying is a form of creativity and deserves equal credit.',
    'Business models are the clearest example of pure invention.'],
   'The closing sentence states it. C ignores the distinction between concealed copying and visible adaptation; B is the view the author rejects.')
rc('p10', 'Inference questions', 'Which of the following would the author consider the more valuable kind of imitation?',
   ['A composer who openly quotes an older melody and reworks it for a new setting.',
    'A writer who copies another’s plot and hides the borrowing.',
    'An engineer who reproduces a design exactly without changes.',
    'A firm that claims to have invented a model it borrowed.'],
   'The author values “adaptation that makes its choices visible” over “copying that conceals dependence”.')
rc('p10', 'Specific detail', 'The examples of music, scientific method and business models are used to show that',
   ['innovation commonly begins by adopting and altering an existing form.',
    'these three fields resist new ideas.',
    'imitation is more common in the arts than in science.',
    'originality requires creation from nothing.'],
   'They illustrate the preceding sentence: innovation “begins with the adoption of an existing form and its alteration under new constraints”.')

passage('p11', 'The politics of maintenance', 'Public debate favours construction because new projects are visible and can be attached to a date, a budget and a ribbon-cutting ceremony. Maintenance is less dramatic. It preserves the reliability of pipes, bridges, databases and administrative routines, often by preventing events that never become news. This invisibility creates a political disadvantage: successful maintenance can look like nothing happened. Yet systems that are not maintained accumulate small failures until repair becomes expensive or impossible. A mature account of progress must therefore value continuity as well as novelty, and must ask who performs the often unnoticed labour that keeps shared systems usable.')
rc('p11', 'Inference questions', 'Why, according to the passage, is maintenance politically disadvantaged?',
   ['Its successes take the form of failures that never happen, so they go unnoticed.',
    'It costs more than new construction.',
    'It cannot be given a budget.',
    'It is performed by unskilled workers.'],
   '“Successful maintenance can look like nothing happened.” B, C and D are not stated.')
rc('p11', 'Main idea / Title', 'Which title best captures the passage?',
   ['Valuing the Unseen Work That Keeps Systems Running',
    'Why New Infrastructure Should Not Be Built',
    'The Ceremony of Ribbon-Cutting',
    'How Databases Fail'],
   'The passage argues for valuing continuity and the “unnoticed labour” of maintenance. B overstates; C and D are details.')
rc('p11', 'Strengthen / Weaken', 'Which of the following would best support the author’s claim about neglected systems?',
   ['A study finding that deferred repairs on bridges cost several times more than timely upkeep.',
    'A survey showing that voters prefer new projects to repairs.',
    'A report that most maintenance workers are poorly paid.',
    'A city that opened three new bridges in one year.'],
   'The claim is that unmaintained systems “accumulate small failures until repair becomes expensive or impossible”; A directly supports it. B supports a different point.')

passage('p12', 'Language and measurement', 'To measure a phenomenon, institutions first need a name for it. The name determines what can be counted, compared and governed. Once a category enters official forms, however, it can acquire an appearance of naturalness. People whose experiences do not fit the category may disappear from the data or be forced into an ill-fitting box. Revising classifications is therefore not simply a technical exercise; it can alter which needs become visible and which claims become administratively intelligible. But endlessly multiplying categories can also make comparison impossible. The task is to create classifications that are stable enough to support analysis and revisable enough to reflect changing realities.')
rc('p12', 'Main idea / Title', 'The author’s position on official categories is that they should be',
   ['stable enough to allow comparison but open to revision as realities change.',
    'multiplied until every experience has its own category.',
    'fixed permanently so that data remain comparable.',
    'abandoned, since naming distorts measurement.'],
   'The final sentence states this balance; B and C are each one of the two dangers named.')
rc('p12', 'Inference questions', 'When a category acquires “an appearance of naturalness”, the result is most likely that',
   ['people who do not fit it are overlooked or misclassified.',
    'the category becomes easier to revise.',
    'comparisons across time become impossible.',
    'institutions stop needing names for phenomena.'],
   'The next sentence explains the consequence: people may “disappear from the data or be forced into an ill-fitting box”.')
rc('p12', 'Specific detail', 'According to the passage, what is the risk of endlessly multiplying categories?',
   ['Comparison becomes impossible.',
    'Needs become more visible.',
    'Categories start to look natural.',
    'Measurement becomes a purely technical exercise.'],
   '“Endlessly multiplying categories can also make comparison impossible.”')

passage('p13', 'The museum of the future', 'Museums increasingly collect not only objects but records of ordinary digital life: messages, interfaces, software and fragments of online communities. Such collections challenge familiar ideas of preservation. A physical object can survive while its original use becomes obscure; a digital object may become unreadable when the systems needed to interpret it disappear. Preserving a file is not the same as preserving the experience it enabled. Curators must decide whether to retain the original code, emulate its environment, document user practices or some combination of these. No method is neutral. Each privileges a different account of what the object was and why it mattered.')
rc('p13', 'Inference questions', 'The author suggests that preserving a digital object differs from preserving a physical one because',
   ['a digital object can become unreadable when the systems that interpret it disappear.',
    'digital objects are less historically important.',
    'physical objects never lose their original meaning.',
    'digital objects cannot be stored in museums.'],
   'Contrasted directly: the physical object “can survive while its original use becomes obscure”, but the digital object “may become unreadable”. C contradicts the passage.')
rc('p13', 'Main idea / Title', 'The statement “No method is neutral” implies that',
   ['each way of preserving a digital object reflects a particular view of what it was and why it mattered.',
    'curators should avoid preserving digital objects.',
    'emulation is the only acceptable preservation method.',
    'museums are biased against digital culture.'],
   'The following sentence explains: “Each privileges a different account of what the object was and why it mattered.”')
rc('p13', 'Specific detail', 'Which of the following is NOT mentioned as an option open to curators?',
   ['Printing the code on paper for display',
    'Retaining the original code',
    'Emulating the original environment',
    'Documenting how users interacted with it'],
   'The passage lists retaining code, emulating the environment and documenting user practices.')

passage('p14', 'Risk and responsibility', 'Modern institutions often divide responsibility according to the probability of harm. A low-probability event may receive little attention even when its consequences would be catastrophic. This is rational only under assumptions about what can be compared and whose losses count. Communities exposed to risk may value protection differently from organisations that calculate aggregate expected cost. Moreover, probabilities themselves may be uncertain when events are rare or conditions are changing. Good risk governance does not abandon calculation; it supplements it with questions about reversibility, distribution and the capacity to recover. A decision can be statistically defensible and still be politically irresponsible.')
rc('p14', 'Main idea / Title', 'The passage argues that good risk governance',
   ['combines probability calculations with attention to reversibility, who bears the harm and the ability to recover.',
    'should abandon probability calculations altogether.',
    'should consider only the expected aggregate cost.',
    'should ignore events that are unlikely to occur.'],
   'Stated directly: it “does not abandon calculation; it supplements it with questions about reversibility, distribution and the capacity to recover”.')
rc('p14', 'Inference questions', 'The final sentence suggests that',
   ['a decision justified by expected-cost figures may still fail the people who bear the risk.',
    'statistics are always politically biased.',
    'politicians should not use statistics.',
    'irresponsible decisions are always statistically weak.'],
   'Statistical defensibility and political responsibility can diverge, as the passage’s points about distribution and community values show.')
rc('p14', 'Strengthen / Weaken', 'Which of the following, if true, would most strengthen the author’s concern about relying on probabilities?',
   ['Historical data on a rare flood type turned out to badly understate how often it occurs as the climate changes.',
    'Most institutions have sophisticated risk models.',
    'Low-probability events rarely have large consequences.',
    'Communities and organisations usually agree on the value of protection.'],
   'The author notes “probabilities themselves may be uncertain when events are rare or conditions are changing”; A is a case of exactly that. C and D weaken the argument.')

passage('p15', 'The meaning of leisure', 'Leisure is often defined as time left over after work. This definition makes it dependent on the very activity from which it is supposed to offer release. Historically, leisure has also meant the freedom to pursue activities without an immediate external purpose. Yet access to such freedom is unequally distributed, and the ideal can conceal the unpaid labour that makes another person\'s leisure possible. Contemporary productivity culture turns even recreation into an optimisation project, complete with metrics and targets. The point is not to romanticise idleness, but to recover the possibility that an activity can be worthwhile without producing a measurable return.')
rc('p15', 'Main idea / Title', 'What does the author want to recover?',
   ['The possibility that an activity can be worthwhile without producing a measurable return.',
    'A culture of idleness free of all effort.',
    'A definition of leisure as time left over after work.',
    'Metrics that make recreation more efficient.'],
   'Stated in the last sentence. B is explicitly disclaimed (“not to romanticise idleness”); C and D are views the author criticises.')
rc('p15', 'Inference questions', 'The author’s objection to defining leisure as “time left over after work” is that it',
   ['makes leisure dependent on the very activity it is meant to provide release from.',
    'ignores the history of paid holidays.',
    'overstates how much free time people have.',
    'treats recreation as an optimisation project.'],
   'The second sentence states the objection directly.')
rc('p15', 'Tone & Attitude', 'The author’s attitude towards “productivity culture” as applied to recreation is',
   ['critical',
    'enthusiastic',
    'neutral',
    'nostalgic'],
   'Turning recreation into “an optimisation project, complete with metrics and targets” is presented as a loss of something worthwhile.', difficulty='EASY')

# ------------------------------------------------------------------ verbal ability (new)


def va(id, topic, prompt, explanation, options=None, answer=None, difficulty='MODERATE'):
    VA.append(dict(id=id, topic=topic, type='MCQ' if options else 'TITA', prompt=prompt, options=options, answer=answer,
                   explanation=explanation, difficulty=difficulty))


SUMMARY = 'The passage given below is followed by four summaries. Choose the option that best captures the essence of the passage.\n\n'
va('va-01', 'Para summary', SUMMARY + 'Cities that build more roads to ease congestion often find traffic returning to its old levels within a few years. New capacity lowers the cost of driving, so people make trips they previously avoided, choose to live further away or switch from public transport. The extra space is soon filled. Planners who treat demand as fixed therefore keep building without solving the problem, while measures that manage demand—pricing, better transit, mixed land use—receive less attention.',
   'The passage explains induced demand and its policy implication. Option A says both; B ignores the cause; C and D distort it.',
   options=['Because new road capacity generates new traffic, congestion is better addressed by managing demand than by building more roads.',
            'Traffic congestion is caused mainly by people living too far from where they work.',
            'Public transport should be abolished in favour of road building.',
            'Road building never has any effect on traffic levels.'])
va('va-02', 'Para summary', SUMMARY + 'A translator is often praised for being invisible, as if the best translation read like an original. But invisibility has costs. It can erase the strangeness that made a work worth translating, smoothing its rhythms and references into the familiar habits of the target language. Some translators therefore choose to leave traces of the original—an unfamiliar word, an unusual syntax—so that readers feel the distance they are crossing.',
   'A captures the tension between fluency and preserving foreignness. B overgeneralises; C reverses the argument; D is a detail.',
   options=['A translation that reads too smoothly may erase what is distinctive about the original, so some translators deliberately keep traces of it.',
            'Translations are always inferior to originals.',
            'The best translations hide every sign that they are translations.',
            'Unfamiliar words make translations hard to read.'])
va('va-03', 'Para summary', SUMMARY + 'Small farms are often described as inefficient because their output per worker is low. Measured per hectare, however, many small farms produce more than large ones, since farmers work the land intensively and combine several crops. Which measure matters depends on what is scarce: where labour is plentiful and land is limited, output per hectare is the more relevant test of efficiency.',
   'A states the conditional argument. B and C each take one measure as absolute; D is unsupported.',
   options=['Whether small farms are efficient depends on the measure used, and where land is scarce, output per hectare is the better test.',
            'Small farms are inefficient because they produce little per worker.',
            'Large farms are always more productive per hectare.',
            'Farmers on small farms work less hard than those on large ones.'])
va('va-04', 'Para summary', SUMMARY + 'Early sound recordings were valued as faithful copies of live performance. Over time, the relationship reversed. Musicians began composing for the studio, using overdubbing, editing and effects that could not be reproduced on stage, and audiences came to judge concerts by how closely they matched the recordings.',
   'A captures the reversal. The others misstate or narrow it.',
   options=['Recording, once a copy of live performance, became the standard against which live performance was judged.',
            'Live concerts have always been better than studio recordings.',
            'Studio effects made recordings less popular with audiences.',
            'Musicians stopped performing live once recording became possible.'])
va('va-05', 'Para summary', SUMMARY + 'Generous refund policies seem costly to retailers, yet many find they increase profits. Customers who know they can return a product buy more readily and are more willing to try unfamiliar brands. Most never use the policy, so the retailer gains more from the extra sales than it loses from returns.',
   'A gives the claim and its mechanism.',
   options=['Easy refunds can raise profits, because they encourage more purchases than they cost in returns.',
            'Retailers lose money on refund policies because most customers return goods.',
            'Customers prefer familiar brands whatever the refund policy.',
            'Refund policies matter only for expensive products.'])
va('va-06', 'Para summary', SUMMARY + 'Ancient Roman concrete has lasted for two thousand years in sea walls that would destroy modern concrete within decades. Researchers have found that seawater seeping into the Roman mix reacted with volcanic ash to grow new minerals in its cracks, strengthening the material over time. Modern concrete, designed to be inert, has no such capacity for self-repair.',
   'A states the finding and the contrast.',
   options=['Roman marine concrete endures because seawater triggers reactions that strengthen it, unlike inert modern concrete.',
            'Modern concrete is stronger than Roman concrete when first made.',
            'Volcanic ash weakens concrete exposed to seawater.',
            'The Romans knew the chemistry of their concrete in detail.'])

COMPLETION = 'Choose the option that best completes the paragraph.\n\n'
va('va-07', 'Sentence placement', COMPLETION + 'A city that wants people to cycle can paint lanes on its roads, but paint alone protects no one. Riders judge safety by how they feel when a lorry passes, not by markings on a map. ____',
   'The paragraph argues that felt safety, not markings, drives cycling, so the completion should name what produces felt safety.',
   options=['Physical separation from traffic therefore does more to encourage cycling than additional lanes of paint.',
            'Painted lanes are therefore the most cost-effective way to increase cycling.',
            'Lorries should therefore be banned from city centres altogether.',
            'Maps of cycle lanes should therefore be published more widely.'])
va('va-08', 'Sentence placement', COMPLETION + 'For centuries, physicians bled patients to treat almost every illness, and many recovered. ____ Only when doctors began comparing treated patients with untreated ones did the practice’s harm become clear.',
   'The blank must explain why recoveries misled doctors, setting up the point about comparison.',
   options=['Those recoveries seemed to confirm the treatment, though most patients would have recovered anyway.',
            'Bleeding was eventually banned by law in most countries.',
            'Patients often asked to be bled because it was fashionable.',
            'Modern medicine has also abandoned many other treatments.'])
va('va-09', 'Sentence placement', COMPLETION + 'Most people assume that a crowd makes a dangerous situation safer, since someone is bound to help. Studies suggest the opposite. ____',
   'The completion must explain why help becomes less likely in a crowd (diffusion of responsibility).',
   options=['Each bystander assumes another will act, so the larger the crowd, the less likely any one person is to intervene.',
            'Crowds are usually made up of people trained in first aid.',
            'Dangerous situations rarely occur in crowded places.',
            'People in crowds are generally more alert than people alone.'])
va('va-10', 'Sentence placement', COMPLETION + 'Libraries once measured success by the number of books on their shelves. As information moved online, that measure lost its meaning. ____',
   'The completion should propose what replaces the old measure.',
   options=['Many now judge themselves instead by how well they help people find, evaluate and use information.',
            'Libraries have therefore stopped buying books entirely.',
            'Online information is always more accurate than printed books.',
            'The number of books in libraries has tripled since then.'])
va('va-11', 'Sentence placement', COMPLETION + 'A company that rewards salespeople only for closing deals may find its revenue rising while customer complaints grow. ____',
   'The completion explains the mechanism: the incentive encourages behaviour that harms customers.',
   options=['The incentive encourages staff to push sales that do not serve customers, storing up problems for later.',
            'Salespeople should therefore not be paid at all.',
            'Customer complaints are usually unrelated to how staff are paid.',
            'Rising revenue proves that the incentive scheme works perfectly.'])
va('va-12', 'Sentence placement', COMPLETION + 'Weather forecasts are now accurate several days ahead, yet many people still distrust them. Part of the reason is how probabilities are read. ____',
   'The completion must link the reading of probabilities to distrust.',
   options=['A “30% chance of rain” is often taken as a promise of a dry day, so the forecast seems wrong whenever it rains.',
            'Forecasters deliberately overstate the chance of rain.',
            'Most people do not check the weather forecast at all.',
            'Weather cannot be predicted more than a day ahead.'])

JUMBLE = 'The four sentences (labelled 1, 2, 3 and 4) given below, when properly sequenced, form a coherent paragraph. Decide on the proper order and key in the sequence of the four numbers as your answer.\n\n'
va('va-13', 'Para jumbles', JUMBLE + '1. At first the scheme seemed a success: the number of cobras killed rose sharply.\n2. To reduce the number of venomous cobras, the colonial government offered a reward for every dead snake.\n3. When officials noticed and cancelled the reward, the breeders released their now-worthless snakes, leaving more cobras than before.\n4. But enterprising residents soon began breeding cobras to collect the bounty.',
   'The scheme (2) → its apparent success (1) → the exploitation (4) → the cancellation and backfire (3).', answer='2143')
va('va-14', 'Para jumbles', JUMBLE + '1. This simple observation led him to design the first successful vaccine.\n2. Edward Jenner noticed that milkmaids who had caught cowpox rarely fell ill with smallpox.\n3. Over time, the method spread worldwide and smallpox was eventually eradicated.\n4. He deliberately infected a boy with cowpox and later showed that the boy was protected against smallpox.',
   'Observation (2) → led to a design (1) → the experiment (4) → the long-term outcome (3).', answer='2143')
va('va-15', 'Para jumbles', JUMBLE + '1. These bubbles were eventually burst, often ruining those who bought last.\n2. In each case, prices rose far above any reasonable estimate of value because buyers expected to sell to someone else at a higher price.\n3. History records many speculative manias, from tulip bulbs in seventeenth-century Holland to railway shares in nineteenth-century Britain.\n4. The pattern suggests that such episodes are driven less by the assets themselves than by the psychology of the market.',
   'General claim with examples (3) → the common mechanism “in each case” (2) → the ending (1) → the lesson (4).', answer='3214')
va('va-16', 'Para jumbles', JUMBLE + '1. Yet the same chemicals that make it useful also mean it barely breaks down.\n2. Plastic became popular because it is cheap, light and durable.\n3. As a result, most of the plastic ever made still exists in some form today.\n4. Durability, the material’s greatest strength, has become its greatest environmental cost.',
   'Why it spread (2) → the catch, “yet” (1) → the consequence, “as a result” (3) → the conclusion (4).', answer='2134')
va('va-17', 'Para jumbles', JUMBLE + '1. Once one firm adopted it, competitors had to follow or lose customers.\n2. Within a decade, the container had become the standard for global shipping.\n3. Before the 1950s, cargo was loaded onto ships piece by piece, a slow and costly process.\n4. Then an American trucking entrepreneur proposed packing goods into standard steel boxes.',
   'The old way (3) → the innovation, “then” (4) → adoption spreads (1) → the outcome (2).', answer='3412')
va('va-18', 'Para jumbles', JUMBLE + '1. The difference lay not in their talent but in how they practised.\n2. Researchers compared violinists rated as good with those rated as outstanding.\n3. The outstanding players had spent far more hours on focused, solitary practice aimed at their weaknesses.\n4. This finding helped popularise the idea of “deliberate practice”.',
   'The study (2) → what distinguished the groups (1) → the specific finding (3) → its influence (4).', answer='2134')

ODD = 'Five jumbled sentences (labelled 1–5), related to a topic, are given below. Four of them can be put together to form a coherent paragraph. Identify the odd one out and key in its number as your answer.\n\n'
va('va-19', 'Odd sentence out', ODD + '1. Honeybees communicate the location of food through a “waggle dance”.\n2. The angle of the dance relative to vertical indicates the direction of the food source relative to the sun.\n3. The duration of the waggle run signals how far away the food is.\n4. Bees returning to the hive use these cues to fly directly to the flowers.\n5. Honey has been used as a natural sweetener for thousands of years.',
   'Sentences 1–4 explain the waggle dance; 5 is about honey as food.', answer='5')
va('va-20', 'Odd sentence out', ODD + '1. Remote work removed the daily commute for millions of employees.\n2. Many used the saved time for exercise, family or extra rest.\n3. Office rents in city centres have been rising for decades.\n4. Surveys found that this reclaimed time was among the benefits workers valued most.\n5. Some firms now cite it when recruiting staff who prefer flexible arrangements.',
   'The paragraph is about time saved by not commuting; 3 is about long-term rent trends.', answer='3')
va('va-21', 'Odd sentence out', ODD + '1. A placebo is a treatment with no active ingredient.\n2. Patients who receive one sometimes report real improvements in their symptoms.\n3. This effect is strongest for conditions such as pain, where perception plays a large role.\n4. Clinical trials therefore compare new drugs against placebos rather than against no treatment.\n5. Many medicines are derived from plants used in traditional remedies.',
   '1–4 develop the placebo effect and its use in trials; 5 is about where medicines come from.', answer='5')
va('va-22', 'Odd sentence out', ODD + '1. Many languages have no single word for “blue”, grouping it with green.\n2. Speakers of such languages can still see the difference between the colours.\n3. But some studies find they are slightly slower to sort shades across the boundary that their language does not mark.\n4. The finding suggests that vocabulary may subtly shape how quickly we categorise what we see.\n5. Blue pigments were rare and expensive in the ancient world.',
   '1–4 concern language and colour perception; 5 is about the history of pigments.', answer='5')
va('va-23', 'Odd sentence out', ODD + '1. Early railways ran on “local time”, with each town setting its clocks by the sun.\n2. As train networks grew, timetables listing dozens of local times became confusing and dangerous.\n3. Steam locomotives were later replaced by diesel and electric engines.\n4. Railway companies therefore adopted standard time zones.\n5. Governments soon followed, and standard time became part of everyday life.',
   'The paragraph traces how railways led to standard time; 3 is about engine technology.', answer='3')
va('va-24', 'Odd sentence out', ODD + '1. Microfinance lenders provide small loans to people without access to banks.\n2. Early studies celebrated it as a powerful tool against poverty.\n3. Later randomised trials found more modest effects on household income.\n4. Most banks today offer mobile apps for checking balances.\n5. Researchers now see microfinance as useful but not transformative.',
   '1, 2, 3 and 5 trace changing views of microfinance; 4 is about banking apps.', answer='4')


def items():
    """Every VARC question, validated: four distinct options for MCQ, a sensible typed answer for TITA."""
    out = []
    for i, q in enumerate(RC, 1):
        assert len(q['options']) == 4 and len(set(q['options'])) == 4, q['prompt']
        out.append(dict(id=f'varc-rc-{q["passage"]}-{i:03d}', section='VARC', passage=PASSAGES[q['passage']], **{k: v for k, v in q.items() if k != 'passage'}))
    for q in VA:
        if q['type'] == 'MCQ':
            assert len(q['options']) == 4 and len(set(q['options'])) == 4, q['id']
        else:
            assert q['answer'] and q['answer'].isdigit(), q['id']
            if q['topic'] == 'Para jumbles':
                assert sorted(q['answer']) == list('1234'), q['id']
        out.append(dict(section='VARC', passage=None, **q))
    return out
