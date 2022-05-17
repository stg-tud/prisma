const util = require('./UtilCompiledHangmanOnchain.js')

let names = {
  testName: "CompiledHangman",
  testedHangmanContract : "CompiledHangman",
  //beforeAllMethod: "",
  commitMethod: "commit",
  guessMethod: "guess",
  respondMethod: "respond",
  openMethod: "open",
  passPartiesToConstructor: true,
  missingLetters: "missingletters",
  correctHashing: true,
}

util.tests(names, null)
