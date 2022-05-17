const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualTTTChannel.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledTTTChannel.json'));

//To JSON

let comparison = {};
let categories = ["deploy", "dispute-1", "dispute-2", "move"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonTTTChannel.json", JSON.stringify(comparison, null, "  "));

//To Latex

let latex = {}
