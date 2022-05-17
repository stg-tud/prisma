const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualChineseCheckers.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledChineseCheckers.json'));

let comparison = {};
let categories = ["deploy", "move"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonChineseCheckers.json", JSON.stringify(comparison, null, "  "));
