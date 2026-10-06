# TalentFlow – Test Results

Run on 2026-10-06 with `java TalentFlowTests --db` against MySQL 8.0.45 (throwaway test database). The same 78 tests also passed on a database already holding the demo data, and that data was left unchanged.

**Result: 78 tests, 78 passed, 0 failed.**

Logic tests (Validation, MatchScorer, Ranking, Shortlist) need no database. Database tests create rows under the test company `ZZ_TEST_CO` and delete them afterwards.


## Validation

| Result | Test |
|---|---|
| PASS | valid email accepted |
| PASS | email without @ rejected |
| PASS | email with unknown domain rejected |
| PASS | null email rejected |
| PASS | valid phone accepted |
| PASS | phone starting with 5 rejected |
| PASS | 9-digit phone rejected |
| PASS | strong password accepted |
| PASS | password without special char rejected |
| PASS | short password rejected |
| PASS | valid name accepted |
| PASS | name with digits rejected |
| PASS | experience 3.5 accepted |
| PASS | negative experience rejected |
| PASS | blank skill rejected |
| PASS | candidate status Available accepted |
| PASS | candidate status Not Available accepted |
| PASS | free-text candidate status rejected |

## MatchScorer

| Result | Test |
|---|---|
| PASS | all required matched, no extras = 100 |
| PASS | half of required matched = 50 |
| PASS | none matched = 0 |
| PASS | matching is case-insensitive |
| PASS | Java does not match JavaScript |
| PASS | missing preferred skills cost at most 30 points |
| PASS | preferred skills all matched = 100 |
| PASS | half preferred matched = 85 |
| PASS | experience below minimum scales the score |
| PASS | experience above minimum does not boost |
| PASS | empty candidate skills = 0 |
| PASS | null candidate skills = 0 |
| PASS | whitespace and trailing commas handled |
| PASS | duplicate skills do not inflate the score |
| PASS | result is always within 0..100 |
| PASS | missing required skills are reported |
| PASS | ampersand separates skills (React & Java) |
| PASS | slash separates skills (Java/SQL) |
| PASS | parseSkills drops empty tokens |

## Ranking

| Result | Test |
|---|---|
| PASS | highest match percentage ranks first |
| PASS | lowest match percentage ranks last |
| PASS | tie on match: more experience first |
| PASS | tie on match and experience: earlier application first |
| PASS | cut-off excludes low scorers |
| PASS | cut-off of 0 keeps everyone, 100 keeps none here |
| PASS | ranking does not modify the input list |

## Shortlist

| Result | Test |
|---|---|
| PASS | top 2 selected from ranked list |
| PASS | applicants not in Applied stage are skipped |
| PASS | N larger than the list is safe |
| PASS | N of zero or negative selects nobody |
| PASS | CSV has a header and one row per applicant |
| PASS | CSV escapes commas in skills |

## Database

| Result | Test |
|---|---|
| PASS | test candidates and job were created |
| PASS | match rules saved for the job |
| PASS | match rules refused for another company |
| PASS | negative minimum experience refused |
| PASS | preview lists every candidate best match first without applying anyone |
| PASS | preview is refused for another company |
| PASS | weighted matching applies only candidates at or above cut-off |
| PASS | running matching again creates no duplicate applications |
| PASS | manual duplicate application is rejected by the UNIQUE rule |
| PASS | strong candidate ranks above medium candidate |
| PASS | shortlist top 1 moves only the first applicant to Interview |
| PASS | withdrawn applicant is left out of the ranked shortlist |
| PASS | withdrawn application no longer shows in the candidate's view |
| PASS | withdrawn application no longer shows in the recruiter's view |
| PASS | recruiter cannot change a withdrawn application |
| PASS | recruiter can still move an active application to Selected |
| PASS | shortlist is refused for another company |
| PASS | CSV export writes a header and one row per applicant |

## Fixes

| Result | Test |
|---|---|
| PASS | recruiter cannot update another company's job |
| PASS | recruiter cannot delete another company's job |
| PASS | recruiter can update their own company's job |
| PASS | old matching engine only uses the recruiter's own jobs |
| PASS | old matching engine no longer matches Java to JavaScript |
| PASS | free-text candidate status is refused |
| PASS | valid candidate status is saved with proper capitals |
| PASS | an application cannot be withdrawn twice |
| PASS | a Selected application cannot be withdrawn |
| PASS | a candidate cannot withdraw another candidate's application |

## Summary

```
TOTAL: 78   PASSED: 78   FAILED: 0
```
