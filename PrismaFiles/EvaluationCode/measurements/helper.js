let getMaxMinAverage = function(array){
  let min = array[0];
  let max = array[0];
  let sum = array[0];
  for(let i = 1; i < array.length; i++){
    if(array[i] < min){
      min = array[i];
    }
    if(array[i] > max){
      max = array[i];
    }
    sum = sum + array[i];
  }
  let average = Math.round(sum / array.length);
  return {
    min: (min / 1000).toFixed(2),
    max: (max / 1000).toFixed(2),
    average: (average / 1000).toFixed(2)
  };
}

let getPlot = function(array){
  let result = getMaxMinAverage(array);
  let sorted = array.sort((a, b) => a - b);
  result.plot = {
    lw: (sorted[0] / 1000).toFixed(2),
    lq: (sorted[Math.floor(sorted.length / 4)]/ 1000).toFixed(2),
    m: (sorted[Math.floor(sorted.length / 2)]/ 1000).toFixed(2),
    uq: (sorted[Math.floor(sorted.length * 3 / 4)]/ 1000).toFixed(2),
    uw: (sorted[sorted.length - 1] / 1000).toFixed(2)
  }
  return result;
}

//Some functions are counted more often than others which reflects the real-world executions
let searchGrouped = function(manual, compiled, name) {
  let manualDeployments = [];
  let compiledDeployments = [];
  let manualExecutions = [];
  let compiledExecutions = [];
  let differencesExecution = [];
  let differencesDeployed = [];
  let differencesExecutionRel = [];
  let differencesDeployedRel = [];

  for(let i = 0; i < manual.length; i++){
    if(manual[i].function == "deploy"){
      manualDeployments.push(manual[i].gas);
      differencesDeployed.push(compiled[i].gas - manual[i].gas);
      differencesDeployedRel.push((compiled[i].gas * 1000 * 100 / manual[i].gas ) - 100 * 1000);
      if(manualDeployments.length != 0 && manual[i].gas != manualDeployments[0]){
        console.log("Warning: Different deployments (manual) - " + name);
      }
    } else {
      differencesExecution.push(compiled[i].gas - manual[i].gas)
      differencesExecutionRel.push((compiled[i].gas * 1000 * 100 / manual[i].gas ) - 100 * 1000);
      manualExecutions.push(manual[i].gas);
    }
  }

  for(let i = 0; i < compiled.length; i++){
    if(compiled[i].function == "deploy"){
      compiledDeployments.push(compiled[i].gas);
      if(compiledDeployments.length != 0 && compiled[i].gas != compiledDeployments[0]){
        console.log("Warning: Different deployments (compiled) - " + name);
      }
    } else {
      compiledExecutions.push(compiled[i].gas);
    }
  }

  if(manualDeployments.length != compiledDeployments.length){
    console.log("Warning: Different number of deployments -" + name);
  }
  if(manualDeployments.l)
  if (manualExecutions != compiledExecutions) {
    console.log("Warning: Different number of executions -"+ name);
  }
  if(manual.length != compiled.length){
    console.log("Warning: Different number of measurements -"+ name);
  }

  let MD = getPlot(manualDeployments);
  let CD = getPlot(compiledDeployments);
  let ME = getPlot(manualExecutions);
  let CE = getPlot(compiledExecutions);
  let DE = getPlot(differencesExecution);
  let DD = getPlot(differencesDeployed);
  let DER = getPlot(differencesExecutionRel);
  let DDR = getPlot(differencesDeployedRel);

  return {
    ManualDeploy: MD,
    CompiledDeploy: CD,
    ManualExecution: ME,
    CompiledExecution: CE,
    DiffExec: DE,
    DiffExecRel: DER,
    DiffDeploy: DD,
    DiffDeployRel: DDR
  }
}

let search = function(manual, compiled, comparison, categories, compareFunction, pre, post){

  for(let j = 0; j < categories.length; j++){
      let search1 = [];
      let search2 = [];

      for(let i = 0; i < manual.length; i++){
        if(manual[i].function[compareFunction](categories[j])){
          search1.push(manual[i].gas);
        }
      }

      for(let i = 0; i < compiled.length; i++){
        if(compiled[i].function[compareFunction](categories[j])){
          search2.push(compiled[i].gas);
        }
      }

      let result1 = getMaxMinAverage(search1);
      let result2 = getMaxMinAverage(search2);

      comparison[pre + categories[j] + post] = {
          manually: `${result1.min} - ${result1.max} (${result1.average})`,
          compiled: `${result2.min} - ${result2.max} (${result2.average})`,
          percentage: Math.round( result2.average * 100 / result1.average) + "% (absolute: " + (result2.average - result1.average) + ")"
      }
  }
}

module.exports = {
  search,
  getMaxMinAverage,
  searchGrouped
}
