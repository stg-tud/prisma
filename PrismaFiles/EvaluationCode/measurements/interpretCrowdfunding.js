const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualCrowdfunding.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledCrowdfunding.json'));

let comparison = {};
let categories = ["deploy", "fund", "claim", "refund"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonCrowdfunding.json", JSON.stringify(comparison, null, "  "));
