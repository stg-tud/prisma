const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualEscrow.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledEscrow.json'));

let comparison = {};
let categories = ["deploy", "pay", "arbitrate", "confirmDelivery"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonEscrow.json", JSON.stringify(comparison, null, "  "));
