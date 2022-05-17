const util = require('./UtilToken.js')

let names = {
  testName: "ManualToken",
  testedContract : "ManualToken",
  transfer: "transfer",
  approve : "approve",
  transferFrom : "transferFrom"
}

util.tests(names, null)
