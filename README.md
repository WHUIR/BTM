# BTM
Our implementation of Biterm Topic Model (BTM), as described in WWW 2013 paper:
**A biterm topic model for short texts**

The BTM is a kind of short text topic model.

## Description

This repository doesn't contain the preprocess steps. So if you want to use this code, you should prepare the data by yourself. 

The data format is described as follows:
>word word word

Each line represents a document, the words in document are separated by a single blank space.

## Parameter Explanation

`beta`: the hyper-parameter beta, and the alpha is calculated as 50/numTopic.

`num_iter`: the number of iteration for gibbs sampling progress.


## Model Result Explanation
`*_pdz.txt`: the topic-level representation for each document. Each line is a topic distribution for one document. This is used for classification task.

`*_phi.txt`: the word-level representation for each topic. Each line is a word distribution for one topic. This is used for PMI Coherence task.

`*_words.txt`: word, wordID map information.
