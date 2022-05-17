const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualMultiSig.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledMultiSig.json'));

let comparison = {};
let categoriesPrefix = ["deploy", "execute", "execute-none-", "execute-one-",
                  "execute-two-", "execute-short-bytes-", "execute-long-bytes-",
                  "execute-coins-"];
let categoriesSuffix = ["2of3","2of4","3of4","3of5","4of5","5of8","8of8","10of10"];

Helper.search(manual, compiled, comparison, categoriesPrefix, "startsWith", "", "*");
Helper.search(manual, compiled, comparison, categoriesSuffix, "endsWith", "*", "")

fs.writeFileSync("./comparisonMultiSig.json", JSON.stringify(comparison, null, "  "));
