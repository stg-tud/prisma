```
  _____           _                           
 |  __ \         (_)                          
 | |__) |  _ __   _   ___   _ __ ___     __ _ 
 |  ___/  | '__| | | / __| | '_ ` _ \   / _` |
 | |      | |    | | \__ \ | | | | | | | (_| |
 |_|      |_|    |_| |___/ |_| |_| |_|  \__,_|
                                              
```


# Prisma

A tierless language for enforcing protocols in decentralized applications in Scala.

* ECOOP22 https://2022.ecoop.org/details/ecoop-2022-papers/28/Prisma-A-tierless-language-for-enforcing-contract-client-protocols-in-decentralized-




# ECOOP 22 Artifact Submission

Title of the submitted paper:

Prisma: A tierless language for enforcing contract-client protocols in decentralized apps

ECOOP submission number for the paper:

64

## Overview: What does the artifact comprise?

- Docker image that includes all dependecies to repeat all steps of our evaluation
  (from compilation of the dApps to the aggregation of results to Latex variables)
    - Location docker-squashed.tar.gz
    - Format: Docker Image

- Compiler Code
    - Location: PrismaFiles/CompilerCode/build.sbt
    - Location: PrismaFiles/CompilerCode/project/build.properties
    - Location: PrismaFiles/CompilerCode/src/*
    - Location: PrismaFiles/CompilerCode/doCompile.sh
    - Location: PrismaFiles/CompilerCode/doCompileRunMeasureCount.sh
    - Location: PrismaFiles/CompilerCode/README.md
    - Format: Source code (Scala)

- Prisma Case Studies (dApps implemented in our Prisma programming language)
    - Location: PrismaFiles/CompilerCode/all/*
    - Location: PrismaFiles/CompilerCode/testCalc/*
    - Location: PrismaFiles/CompilerCode/testChineseCheckers/*
    - Location: PrismaFiles/CompilerCode/testCrowdfunding/*
    - Location: PrismaFiles/CompilerCode/testEscrow/*
    - Location: PrismaFiles/CompilerCode/testHangman/*
    - Location: PrismaFiles/CompilerCode/testMultiSig/*
    - Location: PrismaFiles/CompilerCode/testNotary/*
    - Location: PrismaFiles/CompilerCode/testRPS/*
    - Location: PrismaFiles/CompilerCode/testTTT/*
    - Location: PrismaFiles/CompilerCode/testTTTChannel/*
    - Location: PrismaFiles/CompilerCode/testTTTLibrary/*
    - Location: PrismaFiles/CompilerCode/testTTTViaLib/*
    - Format: Source code (Scala)

- Evaluation Code
    - Location: PrismaFiles/EvaluationCode/*
    - Format: Source Code (Solidity + NodeJS)

- Measurement Results
    - (Note: This is not part of the of the artifact.
             It will be created by running the docker image.)
    - Gas consumption
        - Location: PrismaFiles/EvaluationCode/measurements
        - Format: JSON + Tex
    - Lines of Code and Cross-Tier control flow jumps
        - Location: PrismaFiles/EvaluationCode/linesOfCode/results
        - Format: JSON + Tex

- Evaluations (aggregated + human readable)
    - (Note: This is not part of the of the artifact.
             It will be created by running the docker image.)
    - Location: PrismaFiles/DockerEvaluations
    - Format: Markdown

- For the getting started guide below, we also have the following image
    - Location: PrismaFiles/hangman-statically-vs-dynamically-sized-arrays-expected-change.png
    - Format: png

* Which badges do you claim for your artifact?
  functional, reusable, available


# What are the authors' claims about the artifact?

The central component of this artifact is the compiler that compiles Prisma programs
(i) to EVM byte code for the smart contract that runs on the Ethereum blockchain and
(ii) to JVM byte code for client programs that interact with the contract.
It is accompanied by several case studies of decentralized applications written in
Prisma. Other programmers can use the compiler to develop their own dApps.


## What are claims about the artifact’s functionality to be evaluated by the committee?

The artifacts provides a docker image and allows to replicate our whole
evaluation including:
- the compilation of the compiler,
- compilation of the case studies,
- evaluation of case studies, and
- aggregation of the measurement results to Tex-Files
  as used to create some of the figures in our paper.

The result is composed of an aggregation of the measurements in human-readable
form (i.e., as Markdown) as well as latex files defining the macros used by
the figures in our paper to display the results of our evaluation.

`PrismaFiles/DockerEvaluations/DockerEvaluation_$DATETIME`

More concretely:

- The data of Figure 16 will be recreated in the files
  "measurementResults.tex" resp. "humanReadableMeasurementResults.md".

- The data of Appendix-Table-1 and Appendix-Figure-2 will be recreated in the files
  "codeResults.tex" resp. "humanReadableCodeResults.md".


## What are the authors' claims about the artifact's reusability to be evaluated by the committee?

The artifacts also include the compiler code that could be used to implement further case studies.

(Normally, we write Prisma using intellij-idea-community, the scala plugin,
importing the project from existing sources (using the sbt shell inside intellij),
editing case studies and compiling and testing them. How this can be done using
an Ubuntu system is described in the PrismaFiles/CompilerCode/README.md.)

For the purpose of the artifacts reusability we claim a more concrete method to test:
You can edit one of the case studies with a simple change,
and rerun the docker container to compile it.
See below.

For the purpose of understanding the features of the Prisma programming language we refer to the paper section 2.


## For authors claiming an available badge

* On which platform will the artifact be made publicly available?

  This artifact including the docker image and the source code will be published on zenodo.

* Please provide a link to the platform’s retention policy (not required for DARTS, Zenodo, FigShare).

  Zenodos retention policy applies for the publication.

* Under which license will the artifact be published?

  Apache License 2.0

  (A permissive license whose main conditions require preservation of copyright and license notices. Contributors provide an express grant of patent rights. Licensed works, modifications, and larger works may be distributed under different terms and without source code.)

  The full licence can be found in the textfile LICENSE.


## Artifact Requirements

HW: The device you execute the docker image should provide a performance
comparable to modern computers or Notebooks. Embedded devices, e.g., a
Raspberry Pi, might not be sufficient.

SW:
We expect artifact reviewers to have preinstalled
- docker,
- a text editor,
- a pdf viewer,
- pdflatex with the following packages:

    \usepackage{tikz}
    \usetikzlibrary{patterns}
    \usepackage{pgfplots}
    \usepgfplotslibrary{statistics}


## Getting Started

### Reproduce our Evaluation (for the Functional Badge)

Load the docker image:
`docker load -i prisma-squashed.tar`

Start and execute a docker container:
`docker run -it -v $PWD/PrismaFiles:/app -e HOST_UID=$(id -u) prisma-squashed`

Depending on your computer, running the docker image could take
between 5 and 30 minutes (ca. 15 min).

Explanation:

 -  (The argument `-it` will run the container with a text-terminal attached,
    and in interactive mode. This means you can press CTRL-C to stop it.)

 -  (The argument `-v $PWD/PrismaFiles:/app` gives the docker container
    read/write access to the folder PrismaFiles, which contains the source code.
    This is also the place where the results will be placed.)

 -  (The argument `-e HOST_UID=(id -u)` will pass an additional environment
    variable into the docker container that contains your user ID.
    Docker creates files by default as user `root`, so you can get confusing
    "permission denied" errors, if you attempt to modify those files later.
    We use the provided user id to give ownership of created files
    back to the current user.)

Afterwards Check the results of the evaluation in the folder
`PrismaFiles/DockerEvaluation_<timestamp>` with the latest timestamp, e.g.:
`cd PrismaFiles/DockerEvaluations/DockerEvaluation_*`

You can read the markdown files with the generated raw data.

(
To generate the graphics invoke pdflatex:
`pdflatex main.tex </dev/null`
Now, compare the generated `main.pdf` with the graphics from the paper.
They should show the same statistics (the latex font may differ between the paper and the pdf generated here).
).


### How to change a Case Study (for the Reusable Badge)

We describe a simple change to demonstrate that the case studies
can be modified and still be compiled:

Open the file `PrismaFiles/CompilerCode/testHangman/src/main/scala/TestHangman.scala`.
This file contains the case study that implements a game of Hangman.
The case study currently uses statically sized arrays to represent the an array of 26 letters,
which have to be guessed during the Hangman game.
Statically sized arrays are cheaper than dynamically sized arrays.

For the purpose of demonstrating that someone can modify a case study please look at the lines 45/46:

```
 45     //@co @cross val guessed: Arr[U8] = Arr( // FIXME this lines uses dynmically sized arrays
 46     @co @cross val guessed: U8 x 26 = new (U8 x 26)( // FIXME this line uses statically sized arrays
```

If you switch the commenting of the two lines the code will now use dynamically sized arrays:

```
 45     @co @cross val guessed: Arr[U8] = Arr( // FIXME this lines uses dynmically sized arrays
 46     //@co @cross val guessed: U8 x 26 = new (U8 x 26)( // FIXME this line uses statically sized arrays
```

Now, you can rerun the compilation and evaluation process
`docker run -it -v $PWD/PrismaFiles:/app -e HOST_UID=$(id -u) prisma-squashed`.

Afterwards Check the results of the evaluation in the folder
`PrismaFiles/DockerEvaluation_<timestamp>` with the latest timestamp, e.g.:
`cd PrismaFiles/DockerEvaluations/DockerEvaluation_*`.

The numbers should be different with statically vs dynamically sized arrays.
You can read the markdown files with the generated raw data.

(
If you want to look at the graphics directly, you can invoke pdflatex:
`pdflatex main.tex </dev/null`
Now, compare the generated `main.pdf` with the graphics from the paper.
You should see that the dynamically sized array version has higher (worse) overhead.
We have highlighted the expected change in the following file:
`PrismaFiles/hangman-statically-vs-dynamically-sized-arrays-expected-change.png`
)


### Remarks

Depending on your settings and the location of the files,
you might need to execute the docker commands with `sudo`.

