// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledTTTLibrary  {
  function checkWin(uint8[][] memory board, uint8 x, uint8 y)  view public {
    if (((((board[uint256(x)][uint256(0)] == board[uint256(x)][uint256(1)]) && (board[uint256(x)][uint256(1)] == board[uint256(x)][uint256(2)])) || ((board[uint256(0)][uint256(y)] == board[uint256(1)][uint256(y)]) && (board[uint256(1)][uint256(y)] == board[uint256(2)][uint256(y)]))) || ((board[uint256(1)][uint256(1)] != uint8(0)) && (((board[uint256(0)][uint256(2)] == board[uint256(1)][uint256(1)]) && (board[uint256(2)][uint256(0)] == board[uint256(1)][uint256(1)])) || ((board[uint256(0)][uint256(0)] == board[uint256(1)][uint256(1)]) && (board[uint256(2)][uint256(2)] == board[uint256(1)][uint256(1)])))))) {
      require(true);
    } else {
      require(false);
    } 
  } 
  constructor() {
    
  } 
  
  
  
  
  
  
  

} 