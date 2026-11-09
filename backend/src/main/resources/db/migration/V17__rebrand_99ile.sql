-- Rebrand: the original practice questions credit 99ile (V12 cannot change once applied).
UPDATE questions SET explanation = replace(explanation, 'Written for CATalyst.', 'Written for 99ile.')
WHERE explanation LIKE '%Written for CATalyst.%';
