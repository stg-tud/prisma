const util = require('./UtilToken.js')

let names = {
  testName: "CompiledToken",
  testedContract : "CompiledToken",
  transfer: "transfer",
  approve : "approve",
  transferFrom : "transferFrom"
}

util.tests(names, null)
