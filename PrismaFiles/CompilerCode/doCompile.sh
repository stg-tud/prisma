#!/usr/bin/sh

rm out/Test*-q-solidity.sol
sbt "clean; all/clean; testHangman/compile"

echo scala compiled...

#Copy code to the Evaluation part
for i in $(echo out/Test*.sol); do
  NAME=$(echo $i | cut -c9- | cut -d- -f1)
  echo $NAME
  sed -i "s/contract Contract/contract Compiled$NAME/g" $i
  cp $i ../EvaluationCode/contracts/Compiled$NAME.sol
done

echo Freshly compiled contract files copied to the evaluation ...

cd ../EvaluationCode

# Compile to trigger download
echo Compiling

truffle compile

