#!/usr/bin/sh

rm out/Test*-q-solidity.sol
# sbt "clean; all/clean"
sbt "all/compile"

echo scala compiled...

nohup ganache-cli -d > blockchain.log 2>&1 &

# Wait 10 seconds to ensure that ganche BC has started
sleep 10

echo Ganache started...

#Copy code to the Evaluation part
for i in $(echo out/Test*.sol); do
  NAME=$(echo $i | cut -c9- | cut -d- -f1)
  echo $NAME
  sed -i "s/contract Contract/contract Compiled$NAME/g" $i
  cp $i ../EvaluationCode/contracts/Compiled$NAME.sol
done

echo Freshly compiled contract files copied to the evaluation ...

cd ../EvaluationCode

# Execute evaluations
echo Starting measurement now ...

truffle test
sh -c "cd measurements;
       node interpretAll.js;
       node comparisonToLatex.js"

echo Counting lines of code ...
sh -c "cd linesOfCode;
       node countAll.js
       node toLatex.js"

#Copy results to the top level
cd ..
RESULTNAME="DockerEvaluation_$(date +'%Y-%m-%d-%H-%M-%S')"
mkdir -p DockerEvaluations/$RESULTNAME

cp EvaluationCode/linesOfCode/results/humanReadableCodeResults.md DockerEvaluations/$RESULTNAME/humanReadableCodeResults.md
cp EvaluationCode/measurements/humanReadableMeasurementResults.md DockerEvaluations/$RESULTNAME/humanReadableMeasurementResults.md
cp EvaluationCode/linesOfCode/results/codeResults.tex DockerEvaluations/$RESULTNAME/codeResults.tex
cp EvaluationCode/measurements/measurementResults.tex DockerEvaluations/$RESULTNAME/measurementsResults.tex
cp EvaluationCode/main.tex.template DockerEvaluations/$RESULTNAME/main.tex

# https://stackoverflow.com/questions/26500270/understanding-user-file-ownership-in-docker-how-to-avoid-changing-permissions-o
if [ -z "$HOST_UID" ]; then
  echo no host uid found...
else
  echo change ownership to $HOST_UID
  chown -R $HOST_UID:$HOST_UID .
fi

