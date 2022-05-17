// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledNotary  {
  struct Data {
    address owner;
    uint256 timestamp;
    bytes data;
  } 
  mapping (uint256=>Data) private notaryStorage;
  function getData(uint256 index)  view public returns (Data memory) {
    return(notaryStorage[index]);
  } 
  function submit(bytes memory theData)  public {
    (notaryStorage[uint256(keccak256(abi.encodePacked(payable(msg.sender), theData)))]) = Data(payable(msg.sender), block.timestamp, theData);
    uint8(0);
  } 
  constructor() {
    
  } 
  
  
  
  
  
  
  

} 