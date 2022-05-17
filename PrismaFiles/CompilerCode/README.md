# README

## Setup

0. download the solidity compiler

   https://github.com/ethereum/solidity/releases/download/v0.8.4/solc-static-linux

   and put it into a file "solc-static-linux".

1. Eventually you will need to run things on a blockchain,
   for this we expect a evm to listen on port 8545.
   For example, you can start one with the following command:

   docker run -p 8545:8545 trufflesuite/ganache-cli:latest -d -b 5

2. install intellij-community-edition

       sudo snap install intellij-community-edition --classic

3. get scala plugin and import sbt project

   https://stackoverflow.com/questions/26767463/intellij-14-create-import-a-scala-sbt-project

## Running

1. open "sbt shell" tab in intellij

   https://www.jetbrains.com/help/idea/run-debug-and-test-scala.html#run_sbt_scala_app

2. ensure that during the compilation a ganach-cli instance is running in the background.

       docker run -p 8545:8545 trufflesuite/ganache-cli:latest -d -b 5

2. clean, compile and test from the sbt shell

       > clean
       ...

       > all/compile
       ...

       > all/test
       ...


Further usage examples:

1. run only a specific test, for example the test "maxValues" in class "AllTests" in project "all"

       > all/testOnly -- AllTests.maxValues
       ...

2. compile a specific project, for example the "testTTTChannel" project

       > testTTTChannel/compile
       ...

