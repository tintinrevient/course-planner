# Project Description

## Agent Design

![course-planner](./pix/course-planner.png)


## Agent Architecture

Rational agents come into the following five types:
* Reflex agents
* Reflex agents with state
* Goal-based agents
* Utility-based agents
* BDI agents

The course planner is built upon the architecture of utility-based agents.
![utility-based-agents](./pix/utility-based-agents.png)

The states consist of world states and agent states:
* World states:
	* Neighbouring agents
	* Expertise of the querying agent
	* Sociability of the querying agent
* Agent states:
	* Courses already studied
	* Available timeslots in its calendar

The actions are as below:
* Take a course

The utilities can be calculated as below:
* Happiness = Count(Taken courses) * Overlap(Courses taken by its friends) * Evaluation(Taken courses)

Computational trust comes into the following three types:
* Local trust
* Institutional trust
* Social trust

Beta-reputation system: 
* Beta density function is used for binary events
* Count the past occurrences and decide based on frequency

Formula to calculate reputation and opinion is as below:
* r = occurrences of positive feedback, s = occurrences of negative feedback
* α = r + 1, β = s + 1

Y's reputation calculated by X:
* reputation expected: E(p) = α / α + β, the result is between [0, 1]
* reputation rating: Rep = (E(p) - 0.5) * 2, the result is between [-1, 1]

X's opinion about Y for belief, disbelief, uncertainty: w = (b, d, u)
* b = r / (r + s + 2)
* d = s / (r + s + 2)
* u = u / (r + s + 2)
* mapping from opinion to reputation: Rep(r, s) = (2*b/u, 2*d/u)

Combining opinion: X's opinion about Y = (bxy, dxy, uxy), Y's opinion about Z = (byz, dyz, uyz), and X's opinion about Z = (bxz, dxz, uxz):
* bxz = bxy * byz
* dxz = bxy * dyz
* uxz = dxy + uxy + bxy * uyz

Forgetting factor based on timestamp i is as below:
<<<<<<< HEAD
* r = 􏰅r1 + ... + rn, s = s1 + ... + sn
=======
* r = r1 + ... + rn, s = s1 + ... + sn
>>>>>>> 352f59f054d889bc496e08aab49edd33d24fbe37
* r = (r1 + ... + rn) ∗ λ<sup>(n−i)</sup>, s = (s1 + ... + sn) ∗ λ<sup>(n−i)</sup>, where 1 ≤ i ≤ n, 0 ≤ λ ≤ 1

Rating-based approaches:
* Sensitive to subjectivity
* Sensitive to variation in context 

Ontology-based experiences:
* Gaussian Model: It estimates the probability of satisfaction
* Case-Based Reasoning (CBR): Score for experience = recency * similarity * satisfaction



## Scenario

## Evaluation

* Correctness ratio


## Performance Metrics

## References

* https://owlcs.github.io/owlapi/apidocs_5/index.html
