let names = {
  testName: "ManualHangman",
  testedHangmanContract : "ManualHangman",
  //beforeAllMethod: "",
  commitMethod: "commit",
  guessMethod: "guess",
  respondMethod: "respond",
  openMethod: "open",
  passPartiesToConstructor: true,
  separateArguments: true,
  missingLetters: "missingletters",
  correctHashing: true,
}

require('./UtilManualHangmanOnchain.js').tests(names, null)
