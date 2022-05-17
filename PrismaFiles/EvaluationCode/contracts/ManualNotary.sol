// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract ManualNotary  {
  struct Data {
    address owner;
    uint256 timestamp;
    bytes data;
  } 
  mapping (bytes32=>Data) notaryStorage;

  function submit(bytes memory theData) public {
    notaryStorage[keccak256(abi.encodePacked(msg.sender, theData))] = Data(payable(msg.sender), block.timestamp, theData);
  }
  
  function getData(bytes32 index)  view public returns (Data memory) {
    return(notaryStorage[index]);
  } 
} 
